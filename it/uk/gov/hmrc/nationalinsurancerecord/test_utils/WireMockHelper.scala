/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.nationalinsurancerecord.test_utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.{get, post, status, urlEqualTo, equalTo}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import play.api.http.Status

trait WireMockHelper extends BeforeAndAfterAll with BeforeAndAfterEach with Status {
  this: Suite =>

  protected val server: WireMockServer = new WireMockServer(wireMockConfig().dynamicPort())

  override def beforeAll(): Unit = {
    server.start()
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    server.resetAll()
    super.beforeEach()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    server.stop()
  }

  def gatewayTimeout() = status(GATEWAY_TIMEOUT)
  def badGateway() = status(BAD_GATEWAY)

  def stubPostServer(willReturn: ResponseDefinitionBuilder, url: String): StubMapping =
    server.stubFor(
      post(urlEqualTo(url))
        .willReturn(
          willReturn
        )
    )

  def stubGetServer(willReturn: ResponseDefinitionBuilder, url: String): StubMapping =
    server.stubFor(
      get(urlEqualTo(url))
        .willReturn(
          willReturn
        )
    )
}