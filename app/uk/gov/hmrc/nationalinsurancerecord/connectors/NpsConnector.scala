/*
 * Copyright 2017 HM Revenue & Customs
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

import play.api.data.validation.ValidationError
import play.api.libs.json.{Format, JsPath, OFormat, Reads}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.domain.nps.{NpsLiabilities, NpsNIRecord, NpsSummary}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpReads, HttpResponse}
import uk.gov.hmrc.nationalinsurancerecord.WSHttp
import uk.gov.hmrc.nationalinsurancerecord.cache._
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes.APITypes
import uk.gov.hmrc.nationalinsurancerecord.services.CachingService
import uk.gov.hmrc.nationalinsurancerecord.util.NIRecordConstants

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object NpsConnector extends NpsConnector with ServicesConfig {
  override val serviceUrl = baseUrl("nps-hod")
  override val serviceOriginatorIdKey = getConfString("nps-hod.originatoridkey", "")
  override val serviceOriginatorId = getConfString("nps-hod.originatoridvalue", "")
  override def http: HttpGet = WSHttp

  override val summaryRepository: CachingService[SummaryCache, NpsSummary] = SummaryRepository()
  override val liabilitiesRepository: CachingService[LiabilitiesCache, NpsLiabilities] = LiabilitiesRepository()
  override val nirecordRepository: CachingService[NIRecordCache, NpsNIRecord] = NIRecordRepository()
}

trait NpsConnector {

  val serviceUrl: String
  val serviceOriginatorIdKey: String
  val serviceOriginatorId: String
  val summaryRepository: CachingService[SummaryCache, NpsSummary]
  val liabilitiesRepository: CachingService[LiabilitiesCache, NpsLiabilities]
  val nirecordRepository: CachingService[NIRecordCache, NpsNIRecord]

  class JsonValidationException(message: String) extends Exception(message)

  def http: HttpGet
  def url(path: String): String = s"$serviceUrl$path"
  def requestHeaderCarrier(implicit hc: HeaderCarrier): HeaderCarrier = hc.withExtraHeaders(serviceOriginatorIdKey -> serviceOriginatorId)
  private def ninoWithoutSuffix(nino: Nino): String = nino.value.substring(0, NIRecordConstants.ninoLengthWithoutSuffix)

  def getLiabilities(nino: Nino)(implicit hc: HeaderCarrier): Future[NpsLiabilities] = {
    val urlToRead = url(s"/nps-rest-service/services/nps/pensions/${ninoWithoutSuffix(nino)}/liabilities")
    connectToCache[NpsLiabilities, LiabilitiesCache](
      nino,
      urlToRead,
      APITypes.Liabilities,
      liabilitiesRepository)
  }

  def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier): Future[NpsNIRecord] = {
    val urlToRead = url(s"/nps-rest-service/services/nps/pensions/${ninoWithoutSuffix(nino)}/ni_record")
    connectToCache[NpsNIRecord, NIRecordCache](
      nino,
      urlToRead,
      APITypes.NIRecord,
      nirecordRepository)
  }

  def getSummary(nino: Nino)(implicit hc: HeaderCarrier): Future[NpsSummary] = {
    val urlToRead = url(s"/nps-rest-service/services/nps/pensions/${ninoWithoutSuffix(nino)}/sp_summary")
    connectToCache[NpsSummary, SummaryCache](
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
        connectToNps(url, api, requestHeaderCarrier)(hc, formatA) map {
          response =>
            repository.insertByNino(nino, response);
            response
        }
    }
  }

  private def connectToNps[A](url: String, api: APITypes, requestHc: HeaderCarrier)(implicit hc: HeaderCarrier, reads: Reads[A]): Future[A] = {
    val futureResponse = http.GET[HttpResponse](url)(hc = requestHc, rds = HttpReads.readRaw)

    futureResponse.map { httpResponse =>
      Try(httpResponse.json.validate[A](reads)).flatMap( jsResult =>
        jsResult.fold(errs => Failure(new JsonValidationException(formatJsonErrors(errs))), valid => Success(valid))
      )
    } recover {
      // http-verbs throws exceptions, convert to Try
      case ex => Failure(ex)
    } flatMap (handleResult(api, url, _))
  }

  private def handleResult[A](api: APITypes, url: String, tryResult: Try[A]): Future[A] = {
    tryResult match {
      case Failure(ex) =>
        Future.failed(ex)
      case Success(value) =>
        Future.successful(value)
    }
  }

  private def formatJsonErrors(errors: Seq[(JsPath, Seq[ValidationError])]): String = {
    "JSON Validation Error: " + errors.map(p => p._1 + " - " + p._2.map(_.message).mkString(",")).mkString(" | ")
  }

}