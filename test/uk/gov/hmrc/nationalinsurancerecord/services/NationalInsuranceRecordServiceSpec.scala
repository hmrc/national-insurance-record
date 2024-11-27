/*
 * Copyright 2024 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.domain._
import uk.gov.hmrc.nationalinsurancerecord.domain.des._

import scala.concurrent.{ExecutionContext, Future}

class NationalInsuranceRecordServiceSpec
  extends NationalInsuranceRecordUnitSpec
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with ScalaFutures {
  // scalastyle:off magic.number
  implicit val executionContext: ExecutionContext =
    app.injector.instanceOf[ExecutionContext]

  private val nino = generateNino()

  private val mockCitizenDetailsService = mock[CitizenDetailsService]
  private val mockMetrics = mock[MetricsService]

  override def beforeEach(): Unit = {
    when(mockCitizenDetailsService.checkManualCorrespondenceIndicator(nino))
      .thenReturn(Future.successful(false))
  }

  def exclusionsAssertions(service: NationalInsuranceRecordService): Unit = "return NI Summary with exclusions" in {
    val result: Either[DesError, NationalInsuranceRecordResult] = await(service.getNationalInsuranceRecord(nino))

    result map {_.recordResult.left.map { niExclusion =>
      niExclusion.exclusionReasons shouldBe List(Exclusion.IsleOfMan)
      }
    }

    result map {
      _.recordResult.left.map { _ =>
        verify(mockMetrics, atLeastOnce()).exclusion(ArgumentMatchers.eq(Exclusion.IsleOfMan))
      }
    }
  }
}
