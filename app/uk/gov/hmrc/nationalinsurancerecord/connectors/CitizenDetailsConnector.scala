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

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.WSHttp
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse, Upstream4xxResponse}
import play.api.http.Status._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

object CitizenDetailsConnector extends CitizenDetailsConnector with ServicesConfig {
  override val serviceUrl = baseUrl("citizen-details")
  override val http: HttpGet = WSHttp
}


trait CitizenDetailsConnector {
  val serviceUrl: String
  val http: HttpGet

  private def url(nino: Nino) = s"$serviceUrl/citizen-details/$nino/designatory-details/"

  def retrieveMCIStatus(nino: Nino)(implicit hc: HeaderCarrier): Future[Int] = {
    http.GET[HttpResponse](url(nino)) map {
      personResponse =>
        Success(personResponse.status)
    } recover {
      case ex: Upstream4xxResponse if ex.upstreamResponseCode == LOCKED => Success(ex.upstreamResponseCode)
      case ex: Throwable => Failure(ex)
    } flatMap (handleResult(url(nino), _))
  }

  private def handleResult[A](url: String, tryResult: Try[A]): Future[A] = {
    tryResult match {
      case Failure(ex) =>
        Future.failed(ex)
      case Success(value) =>
        Future.successful(value)
    }
  }

}
