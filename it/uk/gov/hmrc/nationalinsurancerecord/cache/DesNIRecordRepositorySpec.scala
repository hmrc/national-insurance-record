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
import play.api.cache.AsyncCacheApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsDefined, JsObject, JsString, Json}
import play.api.test.Helpers.running
import uk.gov.hmrc.nationalinsurancerecord.domain.des.DesNIRecord
import uk.gov.hmrc.nationalinsurancerecord.test_utils.IntegrationBaseSpec

import java.time.{Instant, LocalDate}

class DesNIRecordRepositorySpec
  extends IntegrationBaseSpec {
  // scalastyle:off magic.number
  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(bind[AsyncCacheApi].toInstance(mockCacheApi))
      .build()

  val desNIRecord: DesNIRecord =
    DesNIRecord(
      dateOfEntry = Some(LocalDate.of(2022, 1, 1)),
      niTaxYears = List()
    )

  val desNIRecordCache: DesNIRecordCache =
    DesNIRecordCache(
      key = "blah",
      response = desNIRecord,
      expiresAt = Instant.ofEpochMilli(
        1640998860000L
      ).plusSeconds(60)
    )

  val desNIRecordCacheJson: JsObject =
    Json.obj(
      "key" -> "blah",
      "response" -> Json.obj(
        "numberOfQualifyingYears" -> 0,
        "nonQualifyingYears" -> 0,
        "nonQualifyingYearsPayable" -> 0,
        "pre75CcCount" -> 0,
        "dateOfEntry" -> "2022-01-01",
        "taxYears" -> Json.arr()
      ),
      "expiresAt" -> Json.obj(
        "$date" -> Json.obj(
          "$numberLong" -> "1640998920000"
        )
      )
    )

  "DesNIRecordRepository" must {
    "get/set liabilities" in {

      val app = fakeApplication()

      running(app) {
        val repo: DesNIRecordRepository =
          app.injector.instanceOf[DesNIRecordRepository]

        repo().collection.drop()

        val nino = generateNino

        whenReady(
          repo()
            .insertByNino(nino = nino, response = desNIRecord)
            .flatMap(_ => repo().findByNino(nino))
        ) {
          result =>
            result shouldBe Some(desNIRecord)
        }
      }
    }
  }

  "DesNIRecordCache" must {
    "serialise and de-serialise correctly" in {
      Json.toJson(desNIRecordCache) shouldBe
        desNIRecordCacheJson

      desNIRecordCacheJson.as[DesNIRecordCache] shouldBe
        desNIRecordCache
    }

    "parse expiresAt as LocalDateTime correctly for Mongo" in {
      (Json.toJson(desNIRecordCache) \ "expiresAt" \ "$date" \ "$numberLong") shouldBe
        JsDefined(JsString("1640998920000"))
    }
  }
}
