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
import play.api.libs.json.{JsDefined, JsObject, JsString, Json}
import play.api.test.Helpers.running
import uk.gov.hmrc.nationalinsurancerecord.domain.des.DesSummary
import uk.gov.hmrc.nationalinsurancerecord.test_utils.IntegrationBaseSpec

import java.time.{LocalDate, LocalDateTime, LocalTime}

class DesSummaryRepositorySpec
  extends IntegrationBaseSpec {
  // scalastyle:off magic.number
  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .build()

  val desSummary: DesSummary =
    DesSummary(
      dateOfDeath = Some(LocalDate.of(2022, 1, 1)),
      earningsIncludedUpTo = Some(LocalDate.of(2022, 1, 1)),
      dateOfBirth = Some(LocalDate.of(1998, 1, 1)),
      finalRelevantYear = Some(0)
    )

  val desSummaryCache: DesSummaryCache =
    DesSummaryCache(
      key = "blah",
      response = desSummary,
      expiresAt = LocalDateTime.of(
        LocalDate.of(2022, 1, 1),
        LocalTime.of(1, 1)
      ).plusSeconds(60)
    )

  val desSummaryCacheJson: JsObject =
    Json.obj(
      "key" -> "blah",
      "response" -> Json.obj(
        "reducedRateElectionToConsider" -> false,
        "earningsIncludedUpto" -> "2022-01-01",
        "dateOfBirth" -> "1998-01-01",
        "dateOfDeath" -> "2022-01-01",
        "finalRelevantYear" -> 0
      ),
      "expiresAt" -> Json.obj(
        "$date" -> Json.obj(
          "$numberLong" -> "1640998920000"
        )
      )
    )

  "DesSummaryRepository" must {
    "get/set liabilities" in {

      val app = fakeApplication()

      running(app) {
        val repo: DesSummaryRepository =
          app.injector.instanceOf[DesSummaryRepository]

        repo().collection.drop()

        val nino = generateNino

        whenReady(
          repo()
            .insertByNino(nino = nino, response = desSummary)
            .flatMap(_ => repo().findByNino(nino))
        ) {
          result =>
            result shouldBe Some(desSummary)
        }
      }
    }
  }

  "DesSummaryCache" must {
    "serialise and de-serialise correctly" in {
      Json.toJson(desSummaryCache) shouldBe
        desSummaryCacheJson

      desSummaryCacheJson.as[DesSummaryCache] shouldBe
        desSummaryCache
    }

    "parse expiresAt as LocalDateTime correctly for Mongo" in {
      (Json.toJson(desSummaryCache) \ "expiresAt" \ "$date" \ "$numberLong") shouldBe
        JsDefined(JsString("1640998920000"))
    }
  }
}
