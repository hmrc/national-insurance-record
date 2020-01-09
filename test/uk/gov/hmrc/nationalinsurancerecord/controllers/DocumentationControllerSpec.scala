/*
 * Copyright 2020 HM Revenue & Customs
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

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import play.api.http.LazyHttpErrorHandler
import play.api.libs.json.{JsArray, JsDefined, JsString, JsUndefined}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.nationalinsurancerecord.config.AppContext
import uk.gov.hmrc.nationalinsurancerecord.controllers.auth.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.nationalinsurancerecord.controllers.documentation.DocumentationController
import uk.gov.hmrc.play.test.UnitSpec


class DocumentationControllerSpec extends UnitSpec with OneAppPerSuite with MockitoSugar {
  "respond to GET /api/definition" in {
    val result = route(app, FakeRequest(GET, "/api/definition"))
    status(result.get) shouldNot be(NOT_FOUND)
  }

  def getDefinitionResultFromConfig(apiConfig: Option[Configuration] = None, apiStatus: Option[String] = None): Result = {

    val appContext = new AppContext(app.configuration) {
      override lazy val appName: String = ""

      override lazy val apiGatewayContext: String = ""

      override lazy val access: Option[Configuration] = apiConfig

      override lazy val status: Option[String] = apiStatus

      override lazy val connectToHOD: Boolean = false
    }

    new DocumentationController(LazyHttpErrorHandler, appContext).definition()(FakeRequest())

  }

  "/definition access" should {

    "return PRIVATE and no Whitelist IDs if there is no application config" in {

      val result = getDefinitionResultFromConfig(apiConfig = None)
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "access" \ "type" shouldBe JsDefined(JsString("PRIVATE"))
      (contentAsJson(result) \ "api" \ "versions") (0) \ "access" \ "whitelistedApplicationIds" shouldBe JsDefined(JsArray())
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

    "return No Whitelist IDs if the application config has an entry for whiteListIds but no Ids" in {

      val result = getDefinitionResultFromConfig(apiConfig = Some(Configuration.from(Map("type" -> "PRIVATE", "whitelist.applicationIds" -> Seq()))))
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "access" \ "whitelistedApplicationIds" shouldBe JsDefined(JsArray())

    }

    "return Whitelist IDs 'A', 'B', 'C' if the application config has an entry with 'A', 'B', 'C' " in {

      val result = getDefinitionResultFromConfig(apiConfig = Some(Configuration.from(Map("type" -> "PRIVATE", "whitelist.applicationIds" -> Seq("A", "B", "C")))))
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "access" \ "whitelistedApplicationIds" shouldBe JsDefined(JsArray(Seq(JsString("A"), JsString("B"), JsString("C"))))

    }

    "return no whitelistApplicationIds json entry if the entry is PUBLIC" in {
      val result = getDefinitionResultFromConfig(apiConfig = Some(Configuration.from(Map("type" -> "PUBLIC", "whitelist.applicationIds" -> Seq()))))
      status(result) shouldBe OK
      (contentAsJson(result) \ "api" \ "versions") (0) \ "access" \ "whitelistedApplicationIds" shouldBe a [JsUndefined]
    }
  }

  "/definition status" should {


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
}
