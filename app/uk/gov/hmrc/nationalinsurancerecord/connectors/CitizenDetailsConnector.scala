/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.Mode.Mode
import play.api.http.Status._
import play.api.{Configuration, Environment, http}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpResponse, Upstream4xxResponse}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.nationalinsurancerecord.services.MetricsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class CitizenDetailsConnector @Inject()(environment: Environment,
                                        configuration: Configuration,
                                        metrics: MetricsService) extends ServicesConfig with BackendController {

  val serviceUrl = baseUrl("citizen-details")

  override protected def mode: Mode = environment.mode
  override protected def runModeConfiguration: Configuration = configuration

  private def url(nino: Nino) = s"$serviceUrl/citizen-details/$nino/designatory-details/"

  def retrieveMCIStatus(nino: Nino)(implicit hc: HeaderCarrier): Future[Int] = {
    val timerContext = metrics.startCitizenDetailsTimer()
    HttpGet.GET[HttpResponse](url(nino)) map {
      personResponse =>
        timerContext.stop()
        Success(personResponse.status)
    } recover {
      case ex: Upstream4xxResponse if ex.upstreamResponseCode == LOCKED => Success(ex.upstreamResponseCode)
      case ex: Throwable => timerContext.stop(); Failure(ex)
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
