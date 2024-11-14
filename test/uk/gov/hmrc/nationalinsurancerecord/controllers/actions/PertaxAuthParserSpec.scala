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

package uk.gov.hmrc.nationalinsurancerecord.controllers.actions

import cats.data.EitherT
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, RecoverMethods}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HttpException, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PertaxAuthParserSpec
    extends AnyWordSpec
    with ScalaFutures
    with IntegrationPatience
    with Matchers
    with EitherValues
    with RecoverMethods {

  "PertaxAuthParser" should {
    "return right PertaxAuthResponse" in {
      val request: Future[Either[UpstreamErrorResponse, HttpResponse]] =
        Future.successful(
          Right(
            HttpResponse(
              OK,
              Json.prettyPrint(
                Json.obj(
                  "code"    -> "ACCESS_GRANTED",
                  "message" -> "Access granted"
                )
              )
            )
          )
        )

      val parser: PertaxAuthParser =
        new PertaxAuthParser()

      val result: EitherT[Future, UpstreamErrorResponse, PertaxAuthResponse] =
        parser(request)

      whenReady(result.value) { res =>
        res.value shouldBe a[PertaxAuthResponse]
        res.value.code shouldBe "ACCESS_GRANTED"
        res.value.message shouldBe "Access granted"
      }
    }

    "return left UpstreamErrorResponse" in {
      List(
        499,
        TOO_MANY_REQUESTS,
        NOT_FOUND,
        LOCKED
      ).foreach { error =>
        val request: Future[Either[UpstreamErrorResponse, HttpResponse]] =
          Future.successful(Left(UpstreamErrorResponse("broke", error)))

        val parser: PertaxAuthParser =
          new PertaxAuthParser()

        val result: EitherT[Future, UpstreamErrorResponse, PertaxAuthResponse] =
          parser(request)

        whenReady(result.value) { res =>
          res.left.value shouldBe a[UpstreamErrorResponse]
          res.left.value.statusCode shouldBe error
          res.left.value.message shouldBe "broke"
        }
      }
    }

    "return left UpstreamErrorResponse BAD_GATEWAY for HttpException" in {
      val request: Future[Either[UpstreamErrorResponse, HttpResponse]] =
        Future.failed(new HttpException("broke", INTERNAL_SERVER_ERROR))

      val parser: PertaxAuthParser =
        new PertaxAuthParser()

      val result: EitherT[Future, UpstreamErrorResponse, PertaxAuthResponse] =
        parser(request)

      whenReady(result.value) { res =>
        res.left.value shouldBe a[UpstreamErrorResponse]
        res.left.value.statusCode shouldBe BAD_GATEWAY
        res.left.value.reportAs shouldBe BAD_GATEWAY
        res.left.value.message shouldBe "broke"
      }
    }

    "throw a Throwable when request returns an Exception" in {
      val request: Future[Either[UpstreamErrorResponse, HttpResponse]] =
        Future.failed(new Exception("broke"))

      val parser: PertaxAuthParser =
        new PertaxAuthParser()

      val result: EitherT[Future, UpstreamErrorResponse, PertaxAuthResponse] =
        parser(request)

      intercept[Exception] {
        await(result.value)
      } shouldBe a[Throwable]
    }
  }
}
