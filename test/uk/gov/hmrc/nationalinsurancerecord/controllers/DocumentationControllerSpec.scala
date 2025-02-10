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

package uk.gov.hmrc.nationalinsurancerecord.controllers

import controllers.Assets
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.Play.materializer
import play.api.libs.json.{JsDefined, JsString}
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.nationalinsurancerecord.config.AppContext
import uk.gov.hmrc.nationalinsurancerecord.controllers.documentation.DocumentationController
import uk.gov.hmrc.nationalinsurancerecord.util.UnitSpec

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.language.postfixOps

class DocumentationControllerSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting {

  val fixTimeoutOnM1Mac: FiniteDuration = 6 seconds

  "respond to GET /api/definition" in {
    val result = route(app, FakeRequest(GET, "/api/definition"))
    status(result.get)(timeout = fixTimeoutOnM1Mac) shouldNot be(NOT_FOUND)
  }

  val controllerComponents: ControllerComponents = stubControllerComponents()
  val assets: Assets = inject[Assets]

  def getDefinitionResultFromConfig(apiConfig: Option[Configuration] = None, apiStatus: Option[String] = None): Result = {

    val appContext = new AppContext(app.configuration) {
      override lazy val appName: String = ""

      override lazy val apiGatewayContext: String = ""

      override lazy val access: Option[Configuration] = apiConfig

      override lazy val status: Option[String] = apiStatus

    }

    new DocumentationController(appContext, assets, controllerComponents).definition()(FakeRequest())

  }

  "/definition access" must{

    "return PRIVATE if there is no application config" in {

      val result = getDefinitionResultFromConfig(apiConfig = None)
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "access" \ "type" shouldBe JsDefined(JsString("PRIVATE"))
    }

    "return PRIVATE if the application config says PRIVATE" in {

      val result = getDefinitionResultFromConfig(apiConfig = Some(Configuration.from(Map("type" -> "PRIVATE"))))
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "access" \ "type" shouldBe JsDefined(JsString("PRIVATE"))
    }

    "return PUBLIC if the application config says PUBLIC" in {

      val result = getDefinitionResultFromConfig(apiConfig = Some(Configuration.from(Map("type" -> "PUBLIC"))))
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "access" \ "type" shouldBe JsDefined(JsString("PUBLIC"))
    }
  }

  "/definition status" must {


    "return BETA if there is no application config" in {

      val result = getDefinitionResultFromConfig(apiStatus = None)
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "status" shouldBe JsDefined(JsString("BETA"))
    }

    "return BETA if the application config says BETA" in {

      val result = getDefinitionResultFromConfig(apiStatus = Some("BETA"))
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "status" shouldBe JsDefined(JsString("BETA"))
    }

    "return STABLE if the application config says STABLE" in {

      val result = getDefinitionResultFromConfig(apiStatus = Some("STABLE"))
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "status" shouldBe JsDefined(JsString("STABLE"))

    }

  }

  "/conf" should {
    "return documentation" in {
      val appContext = new AppContext(app.configuration) {
        override lazy val appName: String = ""

        override lazy val apiGatewayContext: String = ""

        override lazy val access: Option[Configuration] = None

        override lazy val status: Option[String] = None

      }

      val result = new DocumentationController(appContext, assets, controllerComponents).conf("1.0", "/docs/overview.md")(FakeRequest())

      status(result) shouldBe OK
    }
  }
}
