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

package uk.gov.hmrc.nationalinsurancerecord.config

import akka.Done
import com.google.inject.Inject
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

abstract class InternalAuthTokenInitializer {
  val initializeToken: Future[Done]
}

class InternalAuthTokenInitializerNonLocal extends InternalAuthTokenInitializer {
  override val initializeToken: Future[Done] = Future.successful(Done)
}

class InternalAuthTokenInitializerImpl @Inject()(
                                                  appContext: AppContext,
                                                  appConfig: ApplicationConfig,
                                                  httpClient: HttpClientV2
                                                )(implicit ec: ExecutionContext) extends InternalAuthTokenInitializer {

  private val appName = appContext.appName
  private val authToken = appContext.internalAuthToken
  private val internalAuthServiceUrl = appConfig.internalAuthUrl

  override val initializeToken: Future[Done] =
    validateToken(authToken).flatMap{ isValid =>
      if(isValid) Future.successful(Done) else reinitializeToken(authToken)
    }

  Await.result(initializeToken, 30.seconds)

  private def validateToken(token: String): Future[Boolean] =
    httpClient.get(url"$internalAuthServiceUrl/test-only/token")(HeaderCarrier())
      .setHeader("Authorization" -> token)
      .execute
      .map(_.status == 200)

  private def reinitializeToken(token: String): Future[Done] =
    httpClient.post(url"$internalAuthServiceUrl/test-only/token")(HeaderCarrier())
      .withBody(Json.obj(
        "token" -> token,
        "principal" -> appName,
        "permissions" -> Seq(
          Json.obj(
            "resourceType" -> "ni-and-sp-proxy-cache",
            "resourceLocation" -> "*",
            "actions" -> List("READ")
          )
        )
      ))
      .execute.flatMap { response =>
      if (response.status == 201) {
        Future.successful(Done)
      }
      else {
        Future.failed(new RuntimeException("Failed to initialize internal-auth token"))
      }
    }
}

