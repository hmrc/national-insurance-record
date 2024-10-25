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

import cats.data.EitherT
import play.api.http.HeaderNames
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.nationalinsurancerecord.config.ApplicationConfig
import uk.gov.hmrc.nationalinsurancerecord.controllers.actions.{PertaxAuthParser, PertaxAuthResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PertaxConnector @Inject()(
    httpClientV2:     HttpClientV2,
    appConfig:        ApplicationConfig,
    pertaxAuthParser: PertaxAuthParser
) {
  def authorise(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext
  ): EitherT[Future, UpstreamErrorResponse, PertaxAuthResponse] =
    pertaxAuthParser(
      httpClientV2
        .post(url"${appConfig.pertaxBaseUrl}/pertax/authorise")
        .setHeader(HeaderNames.ACCEPT -> "application/vnd.hmrc.2.0+json")
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
    )
}
