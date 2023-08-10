/*
 * Copyright 2023 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.connectors.{DesConnector, ProxyCacheConnector}
import uk.gov.hmrc.nationalinsurancerecord.domain._
import uk.gov.hmrc.nationalinsurancerecord.domain.des._

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class NationalInsuranceRecordServiceSpec
  extends NationalInsuranceRecordUnitSpec
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with ScalaFutures {
  // scalastyle:off magic.number
  implicit val executionContext: ExecutionContext =
    app.injector.instanceOf[ExecutionContext]

  private val niRecordDES = DesNIRecord(
    numberOfQualifyingYears = 36,
    nonQualifyingYears = 10,
    nonQualifyingYearsPayable = 4,
    dateOfEntry = Some(LocalDate.of(1969,8,1)),
    pre75ContributionCount = 250,
    niTaxYears = List(
      DesNITaxYear(
        startTaxYear = 2015,
        qualifying = true,
        classThreePayableBy = None,
        classThreePayableByPenalty = None,
        classOneContribution = 2430.24,
        otherCredits = List()
      ),
      DesNITaxYear(
        startTaxYear = 2014,
        payable = true,
        classThreePayable = 9,
        classThreePayableBy = Some(LocalDate.of(2019, 4, 5)),
        classThreePayableByPenalty = None,
        classOneContribution = 430.4,
        otherCredits = List()
      ),
      DesNITaxYear(
        startTaxYear = 2013,
        qualifying = true,
        payable = true,
        classThreePayable = 720,
        classThreePayableBy = Some(LocalDate.of(2019, 4, 5)),
        classThreePayableByPenalty = Some(LocalDate.of(2023, 4, 5)),
        classTwoCredits = 10,
        classThreeCredits = 3,
        otherCredits = List(DesOtherCredits(Some(1),Some(2),Some(7)))
      )
    )
  )

  private val niTaxYear =
    NationalInsuranceTaxYear(
      taxYear = "2014-15",
      qualifying = false,
      classOneContributions = 430.4,
      classTwoCredits = 0,
      classThreeCredits = 0,
      otherCredits = 0,
      classThreePayable = 9,
      classThreePayableBy = Some(LocalDate.of(2019, 4, 5)),
      classThreePayableByPenalty = None,
      payable = true,
      underInvestigation = false
    )

  private val niRecord =
    NationalInsuranceRecord(
      qualifyingYears = 36,
      qualifyingYearsPriorTo1975 = 5,
      numberOfGaps = 1,
      numberOfGapsPayable = 1,
      dateOfEntry = Some(LocalDate.of(1969,8,1)),
      homeResponsibilitiesProtection = true,
      earningsIncludedUpTo = LocalDate.of(2016,4,5),
      taxYears = List(
        NationalInsuranceTaxYear(
          taxYear = "2015-16",
          qualifying = true,
          classOneContributions = 2430.24,
          classTwoCredits = 0,
          classThreeCredits = 0,
          otherCredits = 0,
          classThreePayable = 0,
          classThreePayableBy = None,
          classThreePayableByPenalty = None,
          payable = false,
          underInvestigation = false
        ),
        niTaxYear,
        NationalInsuranceTaxYear(
          taxYear = "2013-14",
          qualifying = true,
          classOneContributions = 0,
          classTwoCredits = 10,
          classThreeCredits = 3,
          otherCredits = 7,
          classThreePayable = 720,
          classThreePayableBy = Some(LocalDate.of(2019, 4, 5)),
          classThreePayableByPenalty = Some(LocalDate.of(2023, 4, 5)),
          payable = true,
          underInvestigation = false
        )
      ),
      reducedRateElection = false
    )

  private val desSummary =
    DesSummary(
      dateOfDeath          = None,
      earningsIncludedUpTo = Some(LocalDate.of(2016, 4, 5)),
      dateOfBirth          = Some(LocalDate.of(1951, 4, 5)),
      finalRelevantYear    = Some(2017)
    )

  private val exclusionsSummary = DesSummary(
    rreToConsider = true,
    dateOfDeath = None,
    earningsIncludedUpTo = Some(LocalDate.of(1954, 4, 5)),
    dateOfBirth = Some(LocalDate.of(1954, 7, 7)),
    finalRelevantYear = Some(2049)
  )

  private val liabilities =
    DesLiabilities(List(DesLiability(Some(14))))

  private def proxyCacheData(summary: DesSummary = desSummary) =
    ProxyCacheData(
      summary     = summary,
      nIRecord    = niRecordDES,
      liabilities = liabilities
    )

  private val nino = generateNino()

  private val mockFeatureFlagService = mock[FeatureFlagService]
  private val mockCitizenDetailsService = mock[CitizenDetailsService]
  private val mockMetrics = mock[MetricsService]
  private val mockProxyCacheConnector = mock[ProxyCacheConnector]
  private val mockDesConnector = mock[DesConnector]

  override def beforeEach(): Unit = {
    when(mockCitizenDetailsService.checkManualCorrespondenceIndicator(nino))
      .thenReturn(Future.successful(false))
  }

  "NationalInsuranceRecordService when Proxy Cache toggle disabled" when {
    val mockDesConnector = mock[DesConnector]
    val mockFeatureFlagService = mock[FeatureFlagService]

    when(mockFeatureFlagService.get(any()))
      .thenReturn(Future.successful(FeatureFlag(ProxyCacheToggle, isEnabled = false, None)))

    when(mockDesConnector.getLiabilities(nino))
      .thenReturn(Future.successful(liabilities))
    when(mockDesConnector.getNationalInsuranceRecord(nino))
      .thenReturn(Future.successful(niRecordDES))
    when(mockDesConnector.getSummary(nino))
      .thenReturn(Future.successful(desSummary))

    val service: NationalInsuranceRecordService =
      new NationalInsuranceRecordService(
        mockDesConnector,
        mockProxyCacheConnector,
        mockCitizenDetailsService,
        mockMetrics,
        executionContext,
        mockFeatureFlagService
      )

    niRecordAssertions(service)
    pre75Assertions(service)
  }

  "NationalInsuranceRecordService when Proxy Cache toggle enabled" when {
    val mockProxyCacheConnector = mock[ProxyCacheConnector]
    val mockFeatureFlagService = mock[FeatureFlagService]

    when(mockFeatureFlagService.get(any()))
      .thenReturn(Future.successful(FeatureFlag(ProxyCacheToggle, isEnabled = true, None)))

    when(mockProxyCacheConnector.getProxyCacheData(nino))
      .thenReturn(Future.successful(proxyCacheData()))

    val service: NationalInsuranceRecordService =
      new NationalInsuranceRecordService(
        mockDesConnector,
        mockProxyCacheConnector,
        mockCitizenDetailsService,
        mockMetrics,
        executionContext,
        mockFeatureFlagService
      )

    niRecordAssertions(service)
    pre75Assertions(service)
  }

  "Exclusions when Proxy Cache toggle is disabled" should {
    val mockDesConnector = mock[DesConnector]
    val mockMetrics = mock[MetricsService]
    val mockFeatureFlagService = mock[FeatureFlagService]

    when(mockFeatureFlagService.get(any()))
      .thenReturn(Future.successful(FeatureFlag(ProxyCacheToggle, isEnabled = false, None)))

    when(mockDesConnector.getLiabilities(nino))
      .thenReturn(Future.successful(liabilities))
    when(mockDesConnector.getNationalInsuranceRecord(nino))
      .thenReturn(Future.successful(niRecordDES))
    when(mockDesConnector.getSummary(nino))
      .thenReturn(Future.successful(exclusionsSummary))

    val service: NationalInsuranceRecordService =
      new NationalInsuranceRecordService(
        mockDesConnector,
        mockProxyCacheConnector,
        mockCitizenDetailsService,
        mockMetrics,
        executionContext,
        mockFeatureFlagService
      )

    exclusionsAssertions(service)
  }

  "Exclusions when Proxy Cache toggle is enabled" should {
    val mockProxyCacheConnector = mock[ProxyCacheConnector]
    val mockMetrics = mock[MetricsService]

    when(mockFeatureFlagService.get(any()))
      .thenReturn(Future.successful(FeatureFlag(ProxyCacheToggle, isEnabled = true, None)))

    val service: NationalInsuranceRecordService =
      new NationalInsuranceRecordService(
        mockDesConnector,
        mockProxyCacheConnector,
        mockCitizenDetailsService,
        mockMetrics,
        executionContext,
        mockFeatureFlagService
      )

    when(mockProxyCacheConnector.getProxyCacheData(nino))
      .thenReturn(Future.successful(proxyCacheData(exclusionsSummary)))

    exclusionsAssertions(service)
  }

  "NationalInsuranceRecordService" when {
    val mockDesConnector = mock[DesConnector]
    val mockProxyCacheConnector = mock[ProxyCacheConnector]
    val mockFeatureFlagService = mock[FeatureFlagService]

    when(mockFeatureFlagService.get(any()))
      .thenReturn(Future.successful(FeatureFlag(ProxyCacheToggle, isEnabled = true, None)))
    when(mockProxyCacheConnector.getProxyCacheData(nino))
      .thenReturn(Future.successful(proxyCacheData()))

    "proxy cache toggle is enabled" must {

      val service: NationalInsuranceRecordService =
        new NationalInsuranceRecordService(
          mockDesConnector,
          mockProxyCacheConnector,
          mockCitizenDetailsService,
          mockMetrics,
          executionContext,
          mockFeatureFlagService
        )

      "connect to ProxyCacheConnector for ni record" in {

        val result = await(service.getNationalInsuranceRecord(nino))

        result.map {
          ni =>
            ni shouldBe niRecord
            verify(mockDesConnector, times(0)).getSummary(nino)
            verify(mockDesConnector, times(0)).getLiabilities(nino)
            verify(mockDesConnector, times(0)).getNationalInsuranceRecord(nino)
        }
      }

      "connect to ProxyCacheConnector for tax year" in {
        val result = await(service.getTaxYear(nino, TaxYear("2014-15")))

        result.map {
          ty =>
            ty shouldBe niTaxYear
            verify(mockDesConnector, times(0)).getNationalInsuranceRecord(nino)
            verify(mockDesConnector, times(0)).getSummary(nino)
            verify(mockDesConnector, times(0)).getLiabilities(nino)
        }
      }
    }
  }

  "NationalInsuranceRecordService" when {
    val mockDesConnector = mock[DesConnector]
    val mockProxyCacheConnector = mock[ProxyCacheConnector]
    val mockFeatureFlagService = mock[FeatureFlagService]

    when(mockFeatureFlagService.get(any()))
      .thenReturn(Future.successful(FeatureFlag(ProxyCacheToggle, isEnabled = false, None)))
    when(mockDesConnector.getLiabilities(nino))
      .thenReturn(Future.successful(liabilities))
    when(mockDesConnector.getNationalInsuranceRecord(nino))
      .thenReturn(Future.successful(niRecordDES))
    when(mockDesConnector.getSummary(nino))
      .thenReturn(Future.successful(desSummary))

    "proxy cache toggle is disabled" must {

      val service: NationalInsuranceRecordService =
        new NationalInsuranceRecordService(
          mockDesConnector,
          mockProxyCacheConnector,
          mockCitizenDetailsService,
          mockMetrics,
          executionContext,
          mockFeatureFlagService
        )

      "connect to DesConnector for ni record" in {

        val result = await(service.getNationalInsuranceRecord(nino))

        result.map {
          ni =>
            ni shouldBe niRecord
            verify(mockProxyCacheConnector, times(0)).getProxyCacheData(nino)
        }
      }

      "connect to DesConnector for tax year" in {
        val result = await(service.getTaxYear(nino, TaxYear("2014-15")))

        result.map {
          ty =>
            ty shouldBe niTaxYear
            verify(mockProxyCacheConnector, times(0)).getProxyCacheData(nino)
        }
      }
    }
  }

  private def niRecordAssertions(service: NationalInsuranceRecordService): Unit = "regular ni record" must {

    lazy val niRecordResponse =
      await(service.getNationalInsuranceRecord(nino))
    lazy val niTaxYearResponse =
      await(service.getTaxYear(nino, TaxYear("2014-15")))

    "return qualifying years to be 36" in {
      niRecordResponse.map { ni =>
        ni.qualifyingYears shouldBe 36
      }
    }
    "return qualifying years pre 1975 to be 5"  in {
      niRecordResponse.map { ni =>
        ni.qualifyingYearsPriorTo1975 shouldBe 5
      }
    }
    "return number of gaps to be 1"  in {
      niRecordResponse.map { ni =>
        ni.numberOfGaps shouldBe 1
      }
    }
    "return number of gaps payable to be 1"  in {
      niRecordResponse.map { ni =>
        ni.numberOfGapsPayable shouldBe 1
      }
    }
    "return date of entry to be 1969/8/1"  in {
      niRecordResponse.map { ni =>
        ni.dateOfEntry shouldBe Some(LocalDate.of(1969,8,1))
      }
    }
    "return homeResponsibilities to be true"  in {
      niRecordResponse.map { ni =>
        ni.homeResponsibilitiesProtection shouldBe true
      }
    }
    "return earnings included upto to be 2016/8/1"  in {
      niRecordResponse.map { ni =>
        ni.earningsIncludedUpTo shouldBe LocalDate.of(2016,4,5)
      }
    }
    "return taxYear to be 2015-16"  in {
      niRecordResponse.map { ni =>
        ni.taxYears.head.taxYear shouldBe "2015-16"
      }
    }
    "return qualifying status true for tax year 2015-16 to be"  in {
      niRecordResponse.map { ni =>
        ni.taxYears.head.qualifying shouldBe true
      }
    }
    "return classOneContributions to be 2430.24 for tax year 2015-16"  in {
      niRecordResponse.map { ni =>
        ni.taxYears.head.classOneContributions shouldBe 2430.24
      }
    }
    "return classTwoCredits to be 0 for tax year 2015-16"  in {
      niRecordResponse.map { ni =>
        ni.taxYears.head.classTwoCredits shouldBe 0
      }
    }
    "return classThreeCredits to be 0 for tax year 2015-16"  in {
      niRecordResponse.map { ni =>
        ni.taxYears.head.classThreeCredits shouldBe 0
      }
    }
    "return otherCredits to be 0 for tax year 2015-16"  in {
      niRecordResponse.map { ni =>
        ni.taxYears.head.otherCredits shouldBe 0
      }
    }
    "return classThreePayable to be 0 for tax year 2015-16"  in {
      niRecordResponse.map { ni =>
        ni.taxYears.head.classThreePayable shouldBe 0
        ni.taxYears(2).classThreePayable shouldBe 720
      }
    }
    "return classThreePayableBy to be None for tax year 2015-16"  in {
      niRecordResponse.map { ni =>
        ni.taxYears.head.classThreePayableBy shouldBe None
        ni.taxYears(2).classThreePayableBy shouldBe Some(LocalDate.of(2019,4,5))
      }
    }
    "return classThreePayableByPenalty to be None for tax year 2015-16"  in {
      niRecordResponse.map { ni =>
        ni.taxYears.head.classThreePayableByPenalty shouldBe None
        ni.taxYears(2).classThreePayableByPenalty shouldBe Some(LocalDate.of(2023,4,5))
      }
    }
    "return payable and under investigation flag to be false for tax year 2015-16"  in {
      niRecordResponse.map { ni =>
        ni.taxYears.head.payable shouldBe false
        ni.taxYears.head.underInvestigation shouldBe false
      }
    }
    "return tax Year details correctly" in {
      niTaxYearResponse.map { ty =>
        ty shouldBe niTaxYear
      }
    }
  }

  private def pre75Assertions(service: NationalInsuranceRecordService): Unit = "calculating pre75 years" should {
    "return 3 when the number of conts in 157 and the date of entry is 04/10/1972 and their date of birth is 04/10/1956" in {
      service.calcPre75QualifyingYears(
        157, Some(LocalDate.of(1972, 10, 4)), LocalDate.of(1956, 10, 4)
      ) shouldBe Some(3)
    }
    "return 8 when the number of conts in 408 and the date of entry is 08/01/1968 and their date of birth is 08/01/1952" in {
      service.calcPre75QualifyingYears(
        408, Some(LocalDate.of(1968, 1, 8)), LocalDate.of(1952, 1, 8)
      ) shouldBe Some(8)
    }
    "return 2 when the number of conts in 157 and the date of entry is 06/04/1973 and their date of birth is 04/10/1956" in {
      service.calcPre75QualifyingYears(
        157, Some(LocalDate.of(1973, 4, 6)), LocalDate.of(1956, 10, 4)
      ) shouldBe Some(2)
    }
    "return 1 when the number of conts in 157 and the date of entry is 06/04/1973 and their date of birth is 06/04/1958" in {
      service.calcPre75QualifyingYears(
        157, Some(LocalDate.of(1973, 4, 6)), LocalDate.of(1958, 4, 6)
      ) shouldBe Some(1)
    }
    "return null when the number of conts in 157 and the date of entry is 06/04/1973 and their date of birth is 24/05/1996" in {
      service.calcPre75QualifyingYears(
        157, Some(LocalDate.of(1973, 4, 6)), LocalDate.of(1996, 5, 24)
      ) shouldBe None
    }
    "return null when the number of conts in 157 and the date of entry is 06/04/1976 and their date of birth is 06/04/1960" in {
      service.calcPre75QualifyingYears(
        157, Some(LocalDate.of(1976, 4, 6)), LocalDate.of(1960, 4, 6)
      ) shouldBe None
    }
    "return null when the number of conts in 157 and the date of entry is 06/04/2005 and their date of birth is 06/04/1958" in {
      service.calcPre75QualifyingYears(
        157, Some(LocalDate.of(2005, 4, 6)), LocalDate.of(1958, 4, 6)
      ) shouldBe None
    }
    "when the date_of_entry is null, should still perform calc" in {
      service.calcPre75QualifyingYears(
        157, None, LocalDate.of(1922, 4, 6)
      ) shouldBe Some(4)
    }
    "when the date_of_entry is null, should still restrict by 16th Birthday calc" in {
      service.calcPre75QualifyingYears(
        157, None, LocalDate.of(1957, 4, 6)
      ) shouldBe Some(2)
    }
  }

  def exclusionsAssertions(service: NationalInsuranceRecordService): Unit = "return NI Summary with exclusions" in {

    val result = await(service.getNationalInsuranceRecord(nino))

    result.left.map { niExclusion =>
      niExclusion.exclusionReasons shouldBe List(Exclusion.IsleOfMan)
    }

    result.left.map { _ =>
      verify(mockMetrics, atLeastOnce()).exclusion(ArgumentMatchers.eq(Exclusion.IsleOfMan))
    }
  }
}
