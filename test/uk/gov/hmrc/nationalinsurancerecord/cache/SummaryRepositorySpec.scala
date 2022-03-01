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

import org.joda.time.LocalDate
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.des.DesSummary
import uk.gov.hmrc.nationalinsurancerecord.services.{CachingMongoService, MetricsService}

import scala.concurrent.ExecutionContext.Implicits.global

  class SummaryRepositorySpec extends NationalInsuranceRecordUnitSpec with MongoSupport with GuiceOneAppPerSuite {
  // scalastyle:off magic.number

  val testSummaryModel = DesSummary(
    rreToConsider = false,
    dateOfDeath = None,
    earningsIncludedUpTo = Some(new LocalDate(2014, 4, 5)),
    dateOfBirth = Some(new LocalDate(1952, 11, 21)),
    finalRelevantYear = Some(2016)
  )

  "SummaryMongoService" must{
    val stubApplicationConfig = app.injector.instanceOf[StubApplicationConfig]
    val mockMetrics = mock[MetricsService]
    val nino = generateNino()
    val service = new CachingMongoService[DesSummaryCache, DesSummary](mongoComponent, DesSummaryCache.formats, DesSummaryCache.apply,
      APITypes.Summary, stubApplicationConfig, mockMetrics) {
      override val timeToLive = 30
    }

    "persist a SummaryModel in the repo" in {
      reset(mockMetrics)
      val resultF = service.insertByNino(nino, testSummaryModel)
      await(resultF) shouldBe true
      verify(mockMetrics, Mockito.atLeastOnce()).cacheWritten()
    }

    "find a SummaryModel in the repo" in {
      reset(mockMetrics)
      val resultF = service.findByNino(nino)
      resultF.get shouldBe testSummaryModel
      verify(mockMetrics, Mockito.atLeastOnce()).cacheRead()
      verify(mockMetrics, Mockito.atLeastOnce()).cacheReadFound()
    }

    "return None when there is nothing in the repo" in {
      reset(mockMetrics)
      val resultF = service.findByNino(generateNino())
      await(resultF) shouldBe None
      verify(mockMetrics, Mockito.atLeastOnce()).cacheRead()
      verify(mockMetrics, Mockito.atLeastOnce()).cacheReadNotFound()
    }

    //TODO: Replace with Integration Test
/*    "return None when there is a Mongo error" in {
      import scala.concurrent.ExecutionContext.Implicits.global

      val stubCollection = mock[JSONCollection]
      val stubIndexesManager = mock[CollectionIndexesManager]

      when(stubCollection.indexesManager).thenReturn(stubIndexesManager)

      class TestSummaryMongoService extends CachingMongoService[DesSummaryCache, DesSummary](
        DesSummaryCache.formats, DesSummaryCache.apply, APITypes.Summary, StubApplicationConfig, mockMetrics) {
        override lazy val collection: JSONCollection = stubCollection
        override val timeToLive = 30
      }
      reset(mockMetrics)
      when(stubCollection.find(Matchers.any())(Matchers.any())).thenThrow(new RuntimeException)
      when(stubCollection.indexesManager.ensure(Matchers.any())).thenReturn(Future.successful(true))

      val testRepository = new TestSummaryMongoService

      val found = await(testRepository.findByNino(generateNino()))
      found shouldBe None
      verify(mockMetrics, Mockito.atLeastOnce()).cacheRead()
      verify(mockMetrics, Mockito.atLeastOnce()).cacheReadNotFound()
    }*/

    "multiple calls to insertByNino should be fine (upsert)" in {
      await(service.insertByNino(generateNino(), testSummaryModel)) shouldBe true
      await(service.insertByNino(generateNino(), testSummaryModel)) shouldBe true
    }

  }

}
