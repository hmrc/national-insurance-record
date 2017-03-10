/*
 * Copyright 2017 HM Revenue & Customs
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

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import reactivemongo.api.indexes.CollectionIndexesManager
import reactivemongo.json.collection.JSONCollection
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.nps.{NpsLiabilities, NpsLiability}
import uk.gov.hmrc.nationalinsurancerecord.services.CachingMongoService
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class LiabilitiesRespositorySpec extends NationalInsuranceRecordUnitSpec with OneServerPerSuite with MongoSpecSupport with MockitoSugar {
  // scalastyle:off magic.number

  val testLiablitiesModel = NpsLiabilities(List(NpsLiability(15), NpsLiability(100)))

  "LiabilitiesRepository" should {

    val nino = generateNino()
    val excluedNino = generateNino()
    val service = new CachingMongoService[LiabilitiesCache, NpsLiabilities](LiabilitiesCache.formats,
      LiabilitiesCache.apply, APITypes.Liabilities, StubApplicationConfig)

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

    "return None when there is a Mongo error" in {

      val stubCollection = mock[JSONCollection]
      val stubIndexesManager = mock[CollectionIndexesManager]

      when(stubCollection.indexesManager).thenReturn(stubIndexesManager)

      class TestSummaryMongoService extends CachingMongoService[LiabilitiesCache, NpsLiabilities
        ](LiabilitiesCache.formats, LiabilitiesCache.apply, APITypes.Liabilities, StubApplicationConfig) {
        override lazy val collection = stubCollection
      }
      when(stubCollection.find(Matchers.any())(Matchers.any())).thenThrow(new RuntimeException)
      when(stubCollection.indexesManager.ensure(Matchers.any())).thenReturn(Future.successful(true))

      val testRepository = new TestSummaryMongoService

      val found = await(testRepository.findByNino(excluedNino))
      found shouldBe None
    }

    "multiple calls to insertByNino should be fine (upsert)" in {
      await(service.insertByNino(nino, testLiablitiesModel)) shouldBe true
      await(service.insertByNino(nino, testLiablitiesModel)) shouldBe true
    }

  }


}
