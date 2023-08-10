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

import play.api.Logging
import play.api.libs.json._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.nationalinsurancerecord.config.{AppContext, ApplicationConfig}
import uk.gov.hmrc.nationalinsurancerecord.domain.des.ProxyCacheData
import uk.gov.hmrc.nationalinsurancerecord.util.JsonDepersonaliser.{depersonalise, formatJsonErrors}

import java.util.UUID
import javax.inject.Inject
import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class ProxyCacheConnector @Inject () (
  httpClient: HttpClient,
  appContext: AppContext,
  appConfig: ApplicationConfig
)(
  implicit val executionContext: ExecutionContext
) extends Logging {

  def getProxyCacheData(
    nino: Nino
  )(
    implicit headerCarrier: HeaderCarrier
  ): Future[ProxyCacheData] =
    connectToProxyCache(nino).flatMap {
      case Right(proxyCacheData) =>
        Future.successful(proxyCacheData)
      case Left(error) =>
        Future.failed(error)
    }

  private def connectToProxyCache(
    nino: Nino
  )(
    implicit headerCarrier: HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, ProxyCacheData]] = {
    httpClient
      .GET[Either[UpstreamErrorResponse, HttpResponse]](
        url     = s"${appConfig.proxyCacheUrl}/ni-and-sp-proxy-cache/$nino",
        headers = Seq(
          HeaderNames.authorisation -> appContext.internalAuthToken,
          "Originator-Id"           -> "DA_PF",
          "Environment"             -> appConfig.desEnvironment,
          "CorrelationId"           -> UUID.randomUUID().toString,
          HeaderNames.xRequestId    -> headerCarrier.requestId.fold("-")(_.value)
        )
      )(
        rds = readEitherOf,
        hc  = headerCarrier,
        ec  = executionContext
      ) map {
        case Right(response) =>
          response
            .json
            .validate[ProxyCacheData]
            .fold(
              errors => {
                val formattedErrors: String =
                  formatJsonErrors(errors.asInstanceOf[immutable.Seq[(JsPath, immutable.Seq[JsonValidationError])]])

                Left(UpstreamErrorResponse.apply(
                  message    = s"Unable to de-serialise proxy cache data: $formattedErrors\n${depersonalise(response.json)}",
                  statusCode = response.status
                ))
              },
              success =>
                Right(success)
            )
        case Left(errorResponse) =>
          Left(errorResponse)
      }
  }
}
