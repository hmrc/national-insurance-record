/*
 * Copyright 2019 HM Revenue & Customs
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

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.des.{DesLiabilities, DesLiability}
import uk.gov.hmrc.nationalinsurancerecord.services.{CachingMongoService, MetricsService}

import scala.concurrent.ExecutionContext.Implicits.global

class LiabilitiesRespositorySpec extends NationalInsuranceRecordUnitSpec with OneServerPerSuite with MongoSpecSupport with MockitoSugar {
  // scalastyle:off magic.number

  val stubApplicationConfig = app.injector.instanceOf[StubApplicationConfig]
  val testLiablitiesModel = DesLiabilities(List(DesLiability(Some(15)), DesLiability(Some(100))))

  "LiabilitiesRepository" should {

    val nino = generateNino()
    val excluedNino = generateNino()
    val service = new CachingMongoService[DesLiabilitiesCache, DesLiabilities](DesLiabilitiesCache.formats,
      DesLiabilitiesCache.apply, APITypes.Liabilities, stubApplicationConfig, mock[MetricsService])

    "persist a Liabilities in the repo" in {

      val resultF = service.insertByNino(nino, testLiablitiesModel)
      await(resultF) shouldBe true
    }

    "find a Liabilities in the repo" in {
      val resultF = service.findByNino(nino)
      resultF.get shouldBe testLiablitiesModel
    }

    "return None when there is nothing in the repo" in {
      val resultF = service.findByNino(excluedNino)
      await(resultF) shouldBe None
    }

    //TODO: Replace with Integration Test
    /*"return None when there is a Mongo error" in {

      val stubCollection = mock[JSONCollection]
      val stubIndexesManager = mock[CollectionIndexesManager]

      when(stubCollection.indexesManager).thenReturn(stubIndexesManager)

      class TestSummaryMongoService extends CachingMongoService[DesLiabilitiesCache, DesLiabilities](
        DesLiabilitiesCache.formats,
        DesLiabilitiesCache.apply,
        APITypes.Liabilities,
        StubApplicationConfig,
        mock[MetricsService]
      ) {
        override lazy val collection: JSONCollection = stubCollection
      }
      when(stubCollection.find(Matchers.any())(Matchers.any())).thenThrow(new RuntimeException)
      when(stubCollection.indexesManager.ensure(Matchers.any())).thenReturn(Future.successful(true))

      val testRepository = new TestSummaryMongoService

      val found = await(testRepository.findByNino(excluedNino))
      found shouldBe None
    }
*/
    "multiple calls to insertByNino should be fine (upsert)" in {
      await(service.insertByNino(nino, testLiablitiesModel)) shouldBe true
      await(service.insertByNino(nino, testLiablitiesModel)) shouldBe true
    }

  }

}
