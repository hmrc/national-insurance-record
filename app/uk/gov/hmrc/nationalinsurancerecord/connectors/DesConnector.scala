/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.nationalinsurancerecord.connectors

import com.google.inject.Inject
import play.api.Logging
import play.api.libs.json._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpReadsInstances.readEitherOf
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.nationalinsurancerecord.cache._
import uk.gov.hmrc.nationalinsurancerecord.config.ApplicationConfig
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.des.{DesError, DesLiabilities, DesNIRecord, DesSummary}
import uk.gov.hmrc.nationalinsurancerecord.services.{CachingService, MetricsService}
import uk.gov.hmrc.nationalinsurancerecord.util.NIRecordConstants

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class DesConnector @Inject()(
  desSummaryRepository: DesSummaryRepository,
  desNIRecordRepository: DesNIRecordRepository,
  desLiabilitiesRepository: DesLiabilitiesRepository,
  metrics: MetricsService,
  http: HttpClientV2,
  appConfig: ApplicationConfig,
  implicit val executionContext: ExecutionContext,
  connectorUtil: ConnectorUtil
) extends Logging {

  private val authToken: String = appConfig.authorization
  private val desEnvironment: String = appConfig.desEnvironment
  private val summaryRepository: CachingService[DesSummaryCache, DesSummary] = desSummaryRepository()
  private val liabilitiesRepository: CachingService[DesLiabilitiesCache, DesLiabilities] = desLiabilitiesRepository()
  private val niRecordRepository: CachingService[DesNIRecordCache, DesNIRecord] = desNIRecordRepository()

  private def ninoWithoutSuffix(nino: Nino): String =
    nino.value.substring(0, NIRecordConstants.ninoLengthWithoutSuffix)

  def getLiabilities(
    nino: Nino
  )(
    implicit hc: HeaderCarrier
  ): Future[Either[DesError, DesLiabilities]] =
    get[DesLiabilities, DesLiabilitiesCache](
      nino       = nino,
      apiTypes   = APITypes.Liabilities,
      path       = "liabilities",
      repository = liabilitiesRepository
    )

  def getNationalInsuranceRecord(
    nino: Nino
  )(
    implicit hc: HeaderCarrier
  ): Future[Either[DesError, DesNIRecord]] =
    get[DesNIRecord, DesNIRecordCache](
      nino       = nino,
      apiTypes   = APITypes.NIRecord,
      path       = "ni",
      repository = niRecordRepository
    )

  def getSummary(
    nino: Nino
  )(
    implicit hc: HeaderCarrier
  ): Future[Either[DesError, DesSummary]] =
    get[DesSummary, DesSummaryCache](
      nino       = nino,
      apiTypes   = APITypes.Summary,
      path       = "summary",
      repository = summaryRepository
    )

  private def get[A, B](
    nino: Nino,
    apiTypes: APITypes.Value,
    path: String,
    repository: CachingService[B, A]
  )(
    implicit hc: HeaderCarrier,
    formatA: Format[A],
    formatB: OFormat[B]
  ): Future[Either[DesError, A]] = {
    metrics.incrementCounter(apiTypes)

    connectToCache[A, B](
      nino       = nino,
      url        = s"${appConfig.desUrl}/individuals/${ninoWithoutSuffix(nino)}/pensions/$path",
      api        = apiTypes,
      repository = repository
    )
  }

  private def connectToCache[A, B](
    nino: Nino,
    url: String,
    api: APITypes,
    repository: CachingService[B, A]
  )(
    implicit hc: HeaderCarrier,
    formatA: Format[A],
    formatB: OFormat[B]
  ): Future[Either[DesError, A]] =
    repository.findByNino(nino).flatMap {
      case Some(responseModel) =>
        Future.successful(Right(responseModel))
      case None =>
         connectToDes(url, api)(hc, formatA) map { eitherR => //TODO better name or use EitherT
            eitherR.map{ response =>
              logger.info(s"*~* - writing nino to cache: $nino")
              repository.insertByNino(nino, response)
              response
            }
      }
    }

  private def connectToDes[A](
    url: String,
    api: APITypes
  )(
    implicit hc: HeaderCarrier,
    reads: Reads[A]
  ): Future[Either[DesError, A]] = {
    val timerContext = metrics.startTimer(api)
    connectorUtil.handleConnectorResponse(
      futureResponse = http
        .get(url"$url")
        .setHeader(HeaderNames.authorisation -> authToken)
        .setHeader("Originator-Id"           -> "DA_PF")
        .setHeader("Environment"             -> desEnvironment)
        .setHeader("CorrelationId"           -> UUID.randomUUID().toString)
        .setHeader(HeaderNames.xRequestId    -> hc.requestId.fold("-")(_.value))
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
        .transform {
          result =>
            timerContext.stop()
            result
        },
      jsonParseError = api.toString
    ) map {
      result =>
        result match {
          case Left(_) =>
            metrics.incrementFailedCounter(api)
          case Right(_) =>
            ()
        }
        result
    }
  }
}
