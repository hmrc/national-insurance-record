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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HttpClient, UpstreamErrorResponse}
import uk.gov.hmrc.nationalinsurancerecord.config.ApplicationConfig
import uk.gov.hmrc.nationalinsurancerecord.services.MetricsService

import scala.concurrent.{ExecutionContext, Future}

class CitizenDetailsConnector @Inject()(appConfig: ApplicationConfig,
                                        http: HttpClient,
                                        metrics: MetricsService,
                                        implicit val executionContext: ExecutionContext
                                       ) {

  val serviceUrl: String = appConfig.citizenDetailsUrl
//  private def url(nino: Nino) = s"$serviceUrl/citizen-details/$nino/designatory-details/"

  def retrieveMCIStatus(nino: Nino): Future[Either[UpstreamErrorResponse, Int]] = {
    Future.successful(Right(200))
  }
}
