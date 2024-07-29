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

package uk.gov.hmrc.nationalinsurancerecord.config

import com.github.tomakehurst.wiremock.client.WireMock.{created, notFound, ok, serverError}
import org.apache.pekko.Done
import org.scalatest.BeforeAndAfter
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.cache.AsyncCacheApi
import play.api.{Application, inject}
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import utils.WireMockHelper

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

class InternalAuthTokenInitializerSpec
  extends NationalInsuranceRecordUnitSpec
    with GuiceOneAppPerSuite
    with WireMockHelper
    with BeforeAndAfter {

  val mockAppContext = mock[AppContext]
  val mockAppConfig = mock[ApplicationConfig]

  val mockCacheApi: AsyncCacheApi = new AsyncCacheApi {
    override def set(key: String, value: Any, expiration: Duration): Future[Done] = ???

    override def remove(key: String): Future[Done] = Future.successful(Done)

    override def getOrElseUpdate[A](key: String, expiration: Duration)(orElse: => Future[A])
                                   (implicit evidence$1: ClassTag[A]): Future[A] = orElse

    override def get[T](key: String)(implicit evidence$2: ClassTag[T]): Future[Option[T]] = ???

    override def removeAll(): Future[Done] = ???
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.internal-auth.port" -> server.port(),
      "microservice.services.internal-auth.host" -> "127.0.0.1",
      "internal-auth.token" -> "authToken",
      "appName" -> "appName"
    )
    .overrides(
      inject.bind[AsyncCacheApi].toInstance(mockCacheApi)
    )
    .build()

  "initializeToken" should {
    "return done when GET internal auth returns a 200" in {

      stubGetServer(ok(), "/test-only/token")

      await(app.injector.instanceOf[InternalAuthTokenInitializerImpl].initializeToken) shouldBe Done
    }

    "return done when POST internal auth returns a 201" in {
      stubGetServer(serverError(), "/test-only/token")
      stubPostServer(created(), "/test-only/token")

      await(app.injector.instanceOf[InternalAuthTokenInitializerImpl].initializeToken) shouldBe Done
    }

    "return an error reinitialise fails" in {
      stubGetServer(serverError(), "/test-only/token")
      stubPostServer(notFound(), "/test-only/token")

      an[RuntimeException] should be thrownBy await(app.injector.instanceOf[InternalAuthTokenInitializerImpl].initializeToken)
    }
  }
}
