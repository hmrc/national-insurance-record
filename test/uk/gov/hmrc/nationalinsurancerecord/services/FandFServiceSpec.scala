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

package uk.gov.hmrc.nationalinsurancerecord.services

import org.mockito.Mockito.when
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.domain.{NinoGenerator, Nino}
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.connectors.FandFConnector

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class FandFServiceSpec extends NationalInsuranceRecordUnitSpec {

  val mockFandFConnector: FandFConnector = mock[FandFConnector]
  implicit val ec: ExecutionContext = ExecutionContext.global
  val nino: String = Nino(new NinoGenerator(new Random()).nextNino.nino).toString

  val fandfService = new FandFService(mockFandFConnector)

  "getTrustedHelperNino" should {

    "return Some(nino) when TrustedHelper and principalNino are present" in {
      val trustedHelper = TrustedHelper(nino, "", "", None)

      when(mockFandFConnector.getTrustedHelper)
        .thenReturn(Future.successful(Some(trustedHelper)))

      fandfService.getTrustedHelperNino.map { result =>
        result shouldBe Some(nino)
      }
    }

    "return None when TrustedHelper is present but principalNino is missing" in {
      val trustedHelper = TrustedHelper("", "", "", None)

      when(mockFandFConnector.getTrustedHelper)
        .thenReturn(Future.successful(Some(trustedHelper)))

      fandfService.getTrustedHelperNino.map { result =>
        result shouldBe None
      }
    }

    "return None when TrustedHelper is not present" in {
      when(mockFandFConnector.getTrustedHelper)
        .thenReturn(Future.successful(None))

      fandfService.getTrustedHelperNino.map { result =>
        result shouldBe None
      }
    }
  }

}
