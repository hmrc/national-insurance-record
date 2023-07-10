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
import play.api.http.Status.BAD_GATEWAY
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, HttpException, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpReadsInstances.readEitherOf
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.nationalinsurancerecord.cache._
import uk.gov.hmrc.nationalinsurancerecord.config.ApplicationConfig
import uk.gov.hmrc.nationalinsurancerecord.domain.{APITypes, ProxyCacheToggle}
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.des.{DesError, DesLiabilities, DesNIRecord, DesSummary}
import uk.gov.hmrc.nationalinsurancerecord.services.{CachingService, MetricsService}
import uk.gov.hmrc.nationalinsurancerecord.util.{JsonDepersonaliser, NIRecordConstants}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.collection.immutable

class DesConnector @Inject()(
  desSummaryRepository: DesSummaryRepository,
  desNIRecordRepository: DesNIRecordRepository,
  desLiabilitiesRepository: DesLiabilitiesRepository,
  metrics: MetricsService,
  http: HttpClient,
  appConfig: ApplicationConfig,
  implicit val executionContext: ExecutionContext,
  featureFlagService: FeatureFlagService
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
  ): Future[DesLiabilities] =
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
  ): Future[DesNIRecord] =
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
  ): Future[DesSummary] =
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
  ): Future[A] = {
    metrics.incrementCounter(apiTypes)

    featureFlagService.get(ProxyCacheToggle) flatMap {
      proxyCache =>
        val baseUrl =
          if (proxyCache.isEnabled) appConfig.proxyCacheUrl else appConfig.desUrl

        connectToCache[A, B](
          nino       = nino,
          url        = s"$baseUrl/individuals/${ninoWithoutSuffix(nino)}/pensions/$path",
          api        = apiTypes,
          repository = repository
        )
    }
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
  ): Future[A] =
    repository.findByNino(nino).flatMap {
      case Some(responseModel) =>
        Future.successful(responseModel)
      case None =>
        connectToDes(url, api)(hc, formatA).flatMap {
          case Right(response) =>
            logger.debug("*~* - writing nino to cache:" + nino)
            repository.insertByNino(nino, response)
            Future.successful(response)
          case Left(error) =>
            Future.failed(error)
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
    val headers = Seq(
      HeaderNames.authorisation -> authToken,
      "Originator-Id" -> "DA_PF",
      "Environment" -> desEnvironment,
      "CorrelationId" -> UUID.randomUUID().toString,
      HeaderNames.xRequestId -> hc.requestId.fold("-")(_.value)
    )

    http
      .GET[Either[UpstreamErrorResponse, HttpResponse]](url, headers = headers)
      .transform {
        result =>
          timerContext.stop()
          result
      }.map {
        case Right(response) =>
          response.json.validate[A].fold(
            errs => {
              val json = JsonDepersonaliser.depersonalise(response.json) match {
                case Success(s) =>
                  s"Depersonalised JSON\n$s"
                case Failure(e) =>
                  s"JSON could not be depersonalised\n${e.toString}"
              }
              Left(DesError.JsonValidationError(
                s"Unable to deserialise $api: ${formatJsonErrors(errs.asInstanceOf[immutable.Seq[(JsPath, immutable.Seq[JsonValidationError])]])}\n$json"
              ))
            },
            valid =>
              Right(valid)
          )
        case Left(error) =>
          Left(DesError.HttpError(error))
      } recover {
        case error: HttpException =>
          Left(DesError.HttpError(UpstreamErrorResponse(error.message, BAD_GATEWAY)))
        case error =>
          Left(DesError.OtherError(error))
      } map {
        result =>
          handleResult(api, result)
          result
      }
  }

  private def handleResult[A](api: APITypes, result: Either[DesError, A]): Unit =
    result match {
      case Left(_) => metrics.incrementFailedCounter(api)
      case Right(_) => ()
    }

  private def formatJsonErrors(errors: immutable.Seq[(JsPath, immutable.Seq[JsonValidationError])]): String = {
    "JSON Validation Error: " + errors.map(p => p._1.toString() + " - " + p._2.map(e => removeJson(e.message)).mkString(",")).mkString(" | ")
  }

  private def removeJson(message: String): String =
    message.indexOf("{") match {
      case i if i != -1  => message.substring(0, i - 1) + " [JSON removed]"
      case _ => message
    }
}
