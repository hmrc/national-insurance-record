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

import play.api.Logging
import play.api.libs.json._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.nationalinsurancerecord.config.{AppContext, ApplicationConfig}
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.des.{DesError, ProxyCacheData}
import uk.gov.hmrc.nationalinsurancerecord.services.MetricsService

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProxyCacheConnector @Inject () (
  httpClient: HttpClientV2,
  metrics: MetricsService,
  appContext: AppContext,
  appConfig: ApplicationConfig,
  connectorUtil: ConnectorUtil
)(
  implicit val executionContext: ExecutionContext
) extends Logging {

  def get(
    nino: Nino
  )(
    implicit headerCarrier: HeaderCarrier
  ): Future[Either[DesError, ProxyCacheData]] = connect(nino)

  private def connect(
    nino: Nino
  )(
    implicit headerCarrier: HeaderCarrier,
    reads: Reads[ProxyCacheData]
  ): Future[Either[DesError, ProxyCacheData]] = {
    val timerContext = metrics.startTimer(APITypes.ProxyCache)

    connectorUtil.handleConnectorResponse(
      futureResponse = httpClient
        .get(url"${appConfig.proxyCacheUrl}/ni-and-sp-proxy-cache/$nino")
        .setHeader(HeaderNames.authorisation -> appContext.internalAuthToken)
        .setHeader("Originator-Id"           -> "DA_PF")
        .setHeader("Environment"             -> appConfig.desEnvironment)
        .setHeader("CorrelationId"           -> UUID.randomUUID().toString)
        .setHeader(HeaderNames.xRequestId    -> headerCarrier.requestId.fold("-")(_.value))
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
        .transform {
          result =>
            timerContext.stop()
            result
        },
      jsonParseError = "proxy cache data"
    ) map { _.left.map(desError => {
        metrics.incrementFailedCounter(APITypes.ProxyCache)
        desError
      })
    }
  }
}
