/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.nationalinsurancerecord.cache

import org.scalatest.concurrent.ScalaFutures.whenReady
import play.api.Application
import play.api.cache.AsyncCacheApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsDefined, JsObject, JsString, Json}
import play.api.test.Helpers.running
import uk.gov.hmrc.nationalinsurancerecord.domain.des.{DesLiabilities, DesLiability}
import uk.gov.hmrc.nationalinsurancerecord.test_utils.IntegrationBaseSpec

import java.time.Instant

class DesLiabilitiesRepositorySpec
  extends IntegrationBaseSpec {
  // scalastyle:off magic.number
  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(bind[AsyncCacheApi].toInstance(mockCacheApi))
      .configure("internal-auth.isTestOnlyEndpoint" -> false)
      .build()

  val desLiabilities: DesLiabilities =
    DesLiabilities(
      liabilities = List(DesLiability(Some(100)))
    )

  val desLiabilitiesCache: DesLiabilitiesCache =
    DesLiabilitiesCache(
      key = "blah",
      response = desLiabilities,
      expiresAt = Instant.ofEpochMilli(
        1640998860000L
      ).plusSeconds(60)
    )

  val desLiabilitiesCacheJson: JsObject =
    Json.obj(
      "key" -> "blah",
      "response" -> Json.obj(
        "liabilities" -> Json.arr(
          Json.obj("liabilityType" -> 100)
        )
      ),
      "expiresAt" -> Json.obj(
        "$date" -> Json.obj(
          "$numberLong" -> "1640998920000"
        )
      )
    )

  "DesLiabilitiesRepository" must {
    "get/set liabilities" in {

      val app = fakeApplication()

      running(app) {
        val repo: DesLiabilitiesRepository =
          app.injector.instanceOf[DesLiabilitiesRepository]

        repo().collection.drop()

        val nino = generateNino

        whenReady(
          repo()
            .insertByNino(nino = nino, response = desLiabilities)
            .flatMap(_ => repo().findByNino(nino))
        ) {
          result =>
            result shouldBe Some(desLiabilities)
        }
      }
    }
  }

  "DesLiabilitiesCache" must {
    "serialise and de-serialise correctly" in {
      Json.toJson(desLiabilitiesCache) shouldBe
        desLiabilitiesCacheJson

      desLiabilitiesCacheJson.as[DesLiabilitiesCache] shouldBe
        desLiabilitiesCache
    }

    "parse expiresAt as LocalDateTime correctly for Mongo" in {
      (Json.toJson(desLiabilitiesCache) \ "expiresAt" \ "$date" \ "$numberLong") shouldBe
        JsDefined(JsString("1640998920000"))
    }
  }
}
