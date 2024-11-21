/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.http.Status.{FORBIDDEN, NOT_FOUND}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.nationalinsurancerecord.config.ApplicationConfig

import scala.concurrent.{ExecutionContext, Future}

abstract class StatePensionConnector @Inject()(httpClient: HttpClientV2, applicationConfig: ApplicationConfig)(implicit ec: ExecutionContext) {

  val routeUrl: String
  val serviceUrl: String = applicationConfig.statePensionUrl

  class NotCopeException extends Exception

  def getCopeCase(nino: Nino)(implicit hc: HeaderCarrier): Future[Option[HttpResponse]] = {
    implicit val copeReads: HttpReads[HttpResponse] = (_: String, _: String, response: HttpResponse) => {
      if (response.status == FORBIDDEN) response
      else if (response.status == NOT_FOUND) throw new NotCopeException
      else throw UpstreamErrorResponse(response.body, response.status)
    }

    val url = s"$serviceUrl/$routeUrl$nino"
    httpClient.get(url"$url")
      .setHeader("accept" -> "application/vnd.hmrc.1.0+json")
      .execute[HttpResponse]
      .map(Some(_)).recover { case _: NotCopeException => None }
  }
}
