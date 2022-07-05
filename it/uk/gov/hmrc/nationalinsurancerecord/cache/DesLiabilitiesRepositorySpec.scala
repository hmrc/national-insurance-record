/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.running
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.domain.des.{DesLiabilities, DesLiability}
import uk.gov.hmrc.nationalinsurancerecord.services.CachingMongoService
import uk.gov.hmrc.nationalinsurancerecord.test_utils.IntegrationBaseSpec

import java.time.{LocalDate, LocalDateTime, LocalTime}

class DesLiabilitiesRepositorySpec
  extends IntegrationBaseSpec {

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.port" -> server.port(),
      "microservice.services.des-hod.host" -> "127.0.0.1",
      "microservice.services.des-hod.port" -> server.port(),
      "auditing.enabled" -> false
    ).build()

  val response: DesLiabilities =
    DesLiabilities(
      liabilities = List(DesLiability(Some(100)))
    )

  val desLiabilitiesCache: DesLiabilitiesCache =
    DesLiabilitiesCache(
      key = "blah",
      response = response,
      expiresAt = LocalDateTime.of(
        LocalDate.of(2022, 1, 1),
        LocalTime.of(1, 1)
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
        val repo: CachingMongoService[DesLiabilitiesCache, DesLiabilities] =
          app.injector.instanceOf[DesLiabilitiesRepository].apply()

        repo.collection.drop()

        val nino = Nino("AB123456A")

        whenReady(
          repo
            .insertByNino(nino = nino, response)
            .flatMap(_ => repo.findByNino(nino))
        ) {
          result =>
            result shouldBe Some(desLiabilitiesCache.response)
            desLiabilitiesCache.expiresAt shouldBe a[LocalDateTime]
        }
      }
    }
  }

  "DesLiabilitiesCache" must {
    "parse LocalDateTime successfully for Mongo" in {
      Json.toJson(desLiabilitiesCache) shouldBe desLiabilitiesCacheJson
    }
  }
}
