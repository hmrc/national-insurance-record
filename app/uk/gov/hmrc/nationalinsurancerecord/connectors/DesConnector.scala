/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, HttpReads, HttpResponse}
import uk.gov.hmrc.nationalinsurancerecord.cache._
import uk.gov.hmrc.nationalinsurancerecord.config.ApplicationConfig
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.des.{DesLiabilities, DesNIRecord, DesSummary}
import uk.gov.hmrc.nationalinsurancerecord.services.{CachingService, MetricsService}
import uk.gov.hmrc.nationalinsurancerecord.util.{JsonDepersonaliser, NIRecordConstants}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class DesConnector @Inject()(desSummaryRepository: DesSummaryRepository,
                             desNIRecordRepository: DesNIRecordRepository,
                             desLiabilitiesRepository: DesLiabilitiesRepository,
                             metrics: MetricsService,
                             http: HttpClient,
                             appConfig: ApplicationConfig) extends Logging {

  val serviceUrl: String = appConfig.desUrl
  val authToken: String = appConfig.authorization
  val desEnvironment: String = appConfig.desEnvironment
  val summaryRepository: CachingService[DesSummaryCache, DesSummary] = desSummaryRepository()
  val liabilitiesRepository: CachingService[DesLiabilitiesCache, DesLiabilities] = desLiabilitiesRepository()
  val nirecordRepository: CachingService[DesNIRecordCache, DesNIRecord] = desNIRecordRepository()

  class JsonValidationException(message: String) extends Exception(message)

  def url(path: String): String = s"$serviceUrl$path"

  private def ninoWithoutSuffix(nino: Nino): String = nino.value.substring(0, NIRecordConstants.ninoLengthWithoutSuffix)

  def getLiabilities(nino: Nino)(implicit hc: HeaderCarrier): Future[DesLiabilities] = {
    val urlToRead = url(s"/individuals/${ninoWithoutSuffix(nino)}/pensions/liabilities")
    metrics.incrementCounter(APITypes.Liabilities)
    connectToCache[DesLiabilities, DesLiabilitiesCache](
      nino,
      urlToRead,
      APITypes.Liabilities,
      liabilitiesRepository)
  }

  def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier): Future[DesNIRecord] = {
    val urlToRead = url(s"/individuals/${ninoWithoutSuffix(nino)}/pensions/ni")
    metrics.incrementCounter(APITypes.NIRecord)
    connectToCache[DesNIRecord, DesNIRecordCache](
      nino,
      urlToRead,
      APITypes.NIRecord,
      nirecordRepository)
  }

  def getSummary(nino: Nino)(implicit hc: HeaderCarrier): Future[DesSummary] = {
    val urlToRead = url(s"/individuals/${ninoWithoutSuffix(nino)}/pensions/summary")
    metrics.incrementCounter(APITypes.Summary)
    connectToCache[DesSummary, DesSummaryCache](
      nino,
      urlToRead,
      APITypes.Summary,
      summaryRepository)
  }

  private def connectToCache[A, B](nino: Nino, url: String, api: APITypes, repository: CachingService[B, A])
                                  (implicit hc: HeaderCarrier, formatA: Format[A], formatB: OFormat[B]) = {
    repository.findByNino(nino).flatMap {
      case Some(responseModel) => Future.successful(responseModel)
      case None =>
        connectToDes(url, api)(hc, formatA) map {
          response =>
            logger.debug("*~* - writing nino to cache:" + nino)
            repository.insertByNino(nino, response)
            response
        }
    }
  }

  private def connectToDes[A](url: String, api: APITypes)(implicit hc: HeaderCarrier, reads: Reads[A]): Future[A] = {
    val timerContext = metrics.startTimer(api)
    val headers = Seq(
      HeaderNames.authorisation -> authToken,
      "Originator-Id" -> "DA_PF",
      "Environment" -> desEnvironment,
      "CorrelationId" -> UUID.randomUUID().toString,
      HeaderNames.xRequestId -> hc.requestId.fold("-")(_.value)
    )

    val futureResponse = http.GET[HttpResponse](url, headers = headers)(hc = hc, rds = HttpReads.readRaw, ec = global)

    futureResponse.map { httpResponse =>
      timerContext.stop()

      httpResponse.json.validate[A].fold(
        errs => {
          val json = JsonDepersonaliser.depersonalise(httpResponse.json) match {
            case Success(s) => s"Depersonalised JSON\n$s"
            case Failure(e) => s"JSON could not be depersonalised\n${e.toString}"
          }
          throw new JsonValidationException(s"Unable to deserialise $api: ${formatJsonErrors(errs)}\n$json")
        },
        valid => Success(valid)
      )
    } recover {
      // http-verbs throws exceptions, convert to Try
      case ex => Failure(ex)
    } flatMap (handleResult(api, url, _))
  }

  private def handleResult[A](api: APITypes, url: String, tryResult: Try[A]): Future[A] = {
    tryResult match {
      case Failure(ex) =>
        metrics.incrementFailedCounter(api)
        Future.failed(ex)
      case Success(value) =>
        Future.successful(value)
    }
  }

  private def formatJsonErrors(errors: Seq[(JsPath, Seq[JsonValidationError])]): String = {
    "JSON Validation Error: " + errors.map(p => p._1 + " - " + p._2.map(e => removeJson(e.message)).mkString(",")).mkString(" | ")
  }

  private def removeJson(message: String): String = {
    message.indexOf("{") match {
      case i if i != -1  => message.substring(0, i - 1) + " [JSON removed]"
      case _ => message
    }
  }

}
