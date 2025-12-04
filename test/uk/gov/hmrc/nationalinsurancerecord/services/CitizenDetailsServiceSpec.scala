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
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{LOCKED, NOT_FOUND}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.connectors.CitizenDetailsConnector

import scala.concurrent.{ExecutionContext, Future}

class CitizenDetailsServiceSpec
  extends NationalInsuranceRecordUnitSpec
    with GuiceOneAppPerSuite
    with ScalaFutures {

  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  implicit val executionContext: ExecutionContext =
    app.injector.instanceOf[ExecutionContext]

  val service: CitizenDetailsService = new CitizenDetailsService(
    mockCitizenDetailsConnector,
    executionContext
  )

  val nino: Nino = generateNino()

  "checkManualCorrespondenceIndicator" when {
    "retrieving the MCI status" should {
      "return true when the connector is successful and it returns a locked status" in {
        when(mockCitizenDetailsConnector.retrieveMCIStatus(nino)).thenReturn(Future.successful(Right(LOCKED)))

        await(service.checkManualCorrespondenceIndicator(nino)) shouldBe true
      }
      "return false when the connector is successful and it returns not a locked status" in {
        when(mockCitizenDetailsConnector.retrieveMCIStatus(nino)).thenReturn(Future.successful(Right(NOT_FOUND)))

        await(service.checkManualCorrespondenceIndicator(nino)) shouldBe false
      }
      "return a future failed when an error occurs" in {
        val error = UpstreamErrorResponse.apply("Error", NOT_FOUND)

        when(mockCitizenDetailsConnector.retrieveMCIStatus(nino)).thenReturn(Future.successful(Left(error)))

        service.checkManualCorrespondenceIndicator(nino).failed.futureValue shouldBe error
      }
    }
  }
}
