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

package uk.gov.hmrc.nationalinsurancerecord.connectors

import com.codahale.metrics.Timer
import org.joda.time.LocalDate
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.cache.{LiabilitiesCache, NIRecordCache, SummaryCache}
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.nps.{NpsLiabilities, NpsNIRecord, NpsSummary}
import uk.gov.hmrc.nationalinsurancerecord.services.{CachingService, MetricsService}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}
import NpsConnector.JsonValidationException

import scala.concurrent.Future

class NpsConnectorSpec extends NationalInsuranceRecordUnitSpec with MockitoSugar with OneAppPerSuite with ScalaFutures {
  // scalastyle:off magic.number

  val mockSummaryRepo = mock[CachingService[SummaryCache, NpsSummary]]
  val mockLiabilitiesRepo = mock[CachingService[LiabilitiesCache, NpsLiabilities]]
  val mockNIRecordRepo = mock[CachingService[NIRecordCache, NpsNIRecord]]
  val testNIRecordJson = Json.parse(
    """
      | {
      | "years_to_fry": 1,
      | "non_qualifying_years": 13,
      | "date_of_entry": "1973-10-01",
      | "npsLniemply": [],
      | "pre_75_cc_count": 51,
      | "number_of_qualifying_years": 27,
      | "npsErrlist": {
      |   "count": 0,
      | "mgt_check": 0,
      | "commit_status": 2,
      | "npsErritem": [],
      | "bfm_return_code": 0,
      | "data_not_found": 0
      |},
      |"non_qualifying_years_payable": 0,
      |  "npsLnitaxyr": [
      | {
      | "class_three_payable_by_penalty": null,
      | "class_two_outstanding_weeks": null,
      | "class_two_payable": null,
      | "qualifying": 1,
      | "under_investigation_flag": 0,
      | "class_two_payable_by": null,
      | "co_class_one_paid": null,
      | "class_two_payable_by_penalty": null,
      | "co_primary_paid_earnings": null,
      | "payable": 0,
      | "rattd_tax_year": 2012,
      | "ni_earnings": null,
      | "amount_needed": null,
      | "primary_paid_earnings": "21750.0000",
      | "class_three_payable": null,
      | "ni_earnings_employed": "1698.9600",
      | "npsLothcred": [
      |   {
      |      "credit_source_type": 2,
      |      "cc_type": 23,
      |      "no_of_credits_and_conts": 4
      |   }
      | ],
      | "ni_earnings_self_employed": null,
      | "class_three_payable_by": null,
      | "ni_earnings_voluntary": null
      |},
      |{
      | "class_three_payable_by_penalty": "2023-04-05",
      | "class_two_outstanding_weeks": null,
      | "class_two_payable": null,
      | "qualifying": 0,
      | "under_investigation_flag": 1,
      | "class_two_payable_by": null,
      | "co_class_one_paid": null,
      | "class_two_payable_by_penalty": null,
      | "co_primary_paid_earnings": null,
      | "payable": 1,
      | "rattd_tax_year": 2013,
      | "ni_earnings": null,
      | "amount_needed": null,
      | "primary_paid_earnings": null,
      | "class_three_payable": 722.80,
      | "ni_earnings_employed": null,
      | "npsLothcred": [],
      | "ni_earnings_self_employed": "52",
      | "class_three_payable_by": "2019-04-05",
      | "ni_earnings_voluntary": null
      |}
      |],
      | "nino": "<NINO>"
      |}
    """.stripMargin
  )

  val niRecord = testNIRecordJson.as[NpsNIRecord]

  val testLiabilitiesJson = Json.parse(
    """
      |{
      |  "npsErrlist": {
      |    "count": 0,
      |    "mgt_check": 0,
      |    "commit_status": 2,
      |    "npsErritem": [],
      |    "bfm_return_code": 0,
      |    "data_not_found": 0
      |  },
      |  "npsLcdo004d": [
      |    {
      |      "liability_type_end_date": "2012-06-23",
      |      "liability_occurrence_no": 1,
      |      "liability_type_start_date": "2011-08-21",
      |      "liability_type_end_date_reason": "END DATE HELD",
      |      "liability_type": 13
      |    },
      |    {
      |      "liability_type_end_date": "1998-05-03",
      |      "liability_occurrence_no": 1,
      |      "liability_type_start_date": "1989-08-28",
      |      "liability_type_end_date_reason": "END DATE HELD",
      |      "liability_type": 16
      |    },
      |    {
      |      "liability_type_end_date": "2005-08-23",
      |      "liability_occurrence_no": 2,
      |      "liability_type_start_date": "1998-05-04",
      |      "liability_type_end_date_reason": "END DATE HELD",
      |      "liability_type": 16
      |    },
      |    {
      |      "liability_type_end_date": "1995-02-11",
      |      "liability_occurrence_no": 1,
      |      "liability_type_start_date": "1995-02-06",
      |      "liability_type_end_date_reason": "END DATE HELD",
      |      "liability_type": 34
      |    },
      |    {
      |      "liability_type_end_date": "1998-06-13",
      |      "liability_occurrence_no": 2,
      |      "liability_type_start_date": "1997-04-06",
      |      "liability_type_end_date_reason": "END DATE HELD",
      |      "liability_type": 34
      |    },
      |    {
      |      "liability_type_end_date": "2000-09-25",
      |      "liability_occurrence_no": 1,
      |      "liability_type_start_date": "2000-03-28",
      |      "liability_type_end_date_reason": "END DATE HELD",
      |      "liability_type": 71
      |    },
      |    {
      |      "liability_type_end_date": "2004-04-05",
      |      "liability_occurrence_no": 1,
      |      "liability_type_start_date": "2003-09-12",
      |      "liability_type_end_date_reason": "END DATE HELD",
      |      "liability_type": 83
      |    }
      |  ]
      |}
    """.stripMargin
  )
  val liabilities = testLiabilitiesJson.as[NpsLiabilities]
  val mockMetrics = mock[MetricsService]
  val mockTimerContext = mock[Timer.Context]

    "NpsConnector - No Caching" should {
    val connector = new NpsConnector {
      override val serviceUrl: String = ""
      override val serviceOriginatorIdKey: String = "id"
      override val serviceOriginatorId: String = "key"
      override val http: HttpGet = mock[HttpGet]
      override val summaryRepository: CachingService[SummaryCache, NpsSummary] = mockSummaryRepo
      override val liabilitiesRepository: CachingService[LiabilitiesCache, NpsLiabilities] = mockLiabilitiesRepo
      override val nirecordRepository: CachingService[NIRecordCache, NpsNIRecord] = mockNIRecordRepo
      override def metrics: MetricsService = mockMetrics
    }

    val nino = generateNino()

    "return valid Summary in HTTP response" in {
      reset(mockMetrics)
      when(mockMetrics.startTimer(APITypes.Summary)).thenReturn(mockTimerContext)
      when(mockSummaryRepo.findByNino(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      when(connector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(
          Future.successful(
            HttpResponse(
              200,
              Some(Json.parse(
                """
                  |{
                  | "final_relevant_year": 2016,
                  | "date_of_death": null,
                  | "date_of_birth": "1952-11-21",
                  | "rre_to_consider": 0,
                  | "earnings_included_upto": "2014-04-05"
                  |}
                """.stripMargin
              ))))
        )
      val npsSummaryF = await(connector.getSummary(nino)(HeaderCarrier()))
      npsSummaryF.rreToConsider shouldBe false
      npsSummaryF.finalRelevantYear shouldBe 2016
      npsSummaryF.dateOfBirth shouldBe new LocalDate(1952,11,21)
      npsSummaryF.dateOfDeath shouldBe None
      npsSummaryF.earningsIncludedUpTo shouldBe new LocalDate(2014,4,5)
    }

    "log correct Summary metrics" in {
      verify(mockMetrics, Mockito.atLeastOnce()).incrementCounter(APITypes.Summary)
      verify(mockMetrics, Mockito.atLeastOnce()).startTimer(APITypes.Summary)
      verify(mockTimerContext, Mockito.atLeastOnce()).stop()
    }

    "return valid NIRecord response" in {
      reset(mockMetrics)
      when(mockMetrics.startTimer(APITypes.NIRecord)).thenReturn(mock[Timer.Context])
      when(mockNIRecordRepo.findByNino(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      when(connector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(
          Future.successful(
            HttpResponse(
              200,
              Some(testNIRecordJson)
            ))
        )
      val npsNIRecordF = await(connector.getNationalInsuranceRecord(nino)(HeaderCarrier()))
      npsNIRecordF.nonQualifyingYearsPayable shouldBe 0
      npsNIRecordF.nonQualifyingYears shouldBe 13
      npsNIRecordF.numberOfQualifyingYears shouldBe 27
      npsNIRecordF.pre75ContributionCount shouldBe 51
      npsNIRecordF.dateOfEntry shouldBe new LocalDate(1973,10,1)
      npsNIRecordF.niTaxYears.head.qualifying shouldBe true
      npsNIRecordF.niTaxYears(1).qualifying shouldBe false
      npsNIRecordF.niTaxYears.head.underInvestigation shouldBe false
      npsNIRecordF.niTaxYears(1).underInvestigation shouldBe true
      npsNIRecordF.niTaxYears.head.payable shouldBe false
      npsNIRecordF.niTaxYears(1).payable shouldBe true
      npsNIRecordF.niTaxYears.head.classThreePayable shouldBe 0
      npsNIRecordF.niTaxYears(1).classThreePayable shouldBe 722.80
      npsNIRecordF.niTaxYears.head.classThreePayableBy shouldBe None
      npsNIRecordF.niTaxYears(1).classThreePayableBy shouldBe Some(new LocalDate(2019, 4, 5))
      npsNIRecordF.niTaxYears.head.classThreePayableByPenalty shouldBe None
      npsNIRecordF.niTaxYears(1).classThreePayableByPenalty shouldBe Some(new LocalDate(2023, 4, 5))
      npsNIRecordF.niTaxYears.head.classOneContribution shouldBe 1698.9600
      npsNIRecordF.niTaxYears(1).classOneContribution shouldBe 0
      npsNIRecordF.niTaxYears.head.classTwoCredits shouldBe 0
      npsNIRecordF.niTaxYears(1).classTwoCredits shouldBe 52
      npsNIRecordF.niTaxYears.head.otherCredits.head.creditSourceType shouldBe 2
      npsNIRecordF.niTaxYears.head.otherCredits.head.creditContributionType shouldBe 23
      npsNIRecordF.niTaxYears.head.otherCredits.head.numberOfCredits shouldBe 4
      npsNIRecordF.niTaxYears(1).otherCredits shouldBe List()
    }

    "log correct NIRecord metrics" in {
      verify(mockMetrics, Mockito.atLeastOnce()).incrementCounter(APITypes.NIRecord)
      verify(mockMetrics, Mockito.atLeastOnce()).startTimer(APITypes.NIRecord)
      verify(mockTimerContext, Mockito.atLeastOnce()).stop()
    }

    "return a failed NIRecord when there is an http error and pass on the exception" in {
      when(connector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new BadRequestException("Bad Request exception")))
      ScalaFutures.whenReady(connector.getNationalInsuranceRecord(generateNino()).failed) { ex =>
        ex shouldBe a[BadRequestException]
      }
    }

    "return valid Liabilities response" in {
      reset(mockMetrics)
      when(mockMetrics.startTimer(APITypes.Liabilities)).thenReturn(mock[Timer.Context])
      when(mockLiabilitiesRepo.findByNino(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      when(connector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(
          Future.successful(
            HttpResponse(
              200,
              Some(testLiabilitiesJson)))
        )

      val npsLiabilitiesF = await(connector.getLiabilities(nino)(HeaderCarrier()))
      npsLiabilitiesF.liabilities.head.liabilityType shouldBe 13
      npsLiabilitiesF.liabilities(4).liabilityType shouldBe 34
    }

    "log correct Liabilities metrics" in {
      verify(mockMetrics, Mockito.atLeastOnce()).incrementCounter(APITypes.Liabilities)
      verify(mockMetrics, Mockito.atLeastOnce()).startTimer(APITypes.Liabilities)
      verify(mockTimerContext, Mockito.atLeastOnce()).stop()
    }

    "return a failed Liabilities when there is an http error and pass on the exception" in {
      when(connector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new BadRequestException("Bad Request exception")))
      ScalaFutures.whenReady(connector.getLiabilities(generateNino()).failed) { ex =>
        ex shouldBe a[BadRequestException]
      }
    }

    "return a depersonalised JSON structure following validation error" in {
      val invalidJson = Json.parse(
        """
        | {
        | "years_to_fry": "a",
        | "non_qualifying_years": 13,
        | "date_of_entry": "1973-10-01",
        | "npsLniemply": [],
        | "pre_75_cc_count": 51,
        | "number_of_qualifying_years": 27,
        | "npsErrlist": {
        |   "count": 0,
        |   "mgt_check": 0,
        |   "commit_status": 2,
        |   "npsErritem": [],
        |   "bfm_return_code": 0,
        |   "data_not_found": 0
        |  }
        |}
        """.stripMargin)

      reset(mockMetrics)
      when(mockMetrics.startTimer(APITypes.NIRecord)).thenReturn(mock[Timer.Context])
      when(connector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(
        Future.successful(
          HttpResponse(
            200,
            Some(invalidJson)
          ))
      )

      ScalaFutures.whenReady(connector.getNationalInsuranceRecord(nino)(HeaderCarrier()).failed) { ex =>
        ex shouldBe a[JsonValidationException]
        ex.getMessage.contains("1973-10-01") shouldBe false
        ex.getMessage.contains("1111-11-11") shouldBe true
      }
    }
  }

  "NpsConnector - Caching" should {
    val connector = new NpsConnector {
      override val serviceUrl: String = ""
      override val serviceOriginatorIdKey: String = "id"
      override val serviceOriginatorId: String = "key"
      override val http: HttpGet = mock[HttpGet]
      override val summaryRepository: CachingService[SummaryCache, NpsSummary] = mockSummaryRepo
      override val liabilitiesRepository: CachingService[LiabilitiesCache, NpsLiabilities] = mockLiabilitiesRepo
      override val nirecordRepository: CachingService[NIRecordCache, NpsNIRecord] = mockNIRecordRepo
      override def metrics: MetricsService = mock[MetricsService]
    }

    val nino = generateNino()
    val testSummaryModel = NpsSummary(rreToConsider = false, dateOfDeath = None, earningsIncludedUpTo = new LocalDate(2014, 4, 5),
      dateOfBirth = new LocalDate(1952, 11, 21), finalRelevantYear = 2016)

    "return valid Summary response" in {
      when(mockSummaryRepo.findByNino(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testSummaryModel)))
      val npsSummaryF = connector.getSummary(generateNino())(HeaderCarrier())
      npsSummaryF.rreToConsider shouldBe false
      npsSummaryF.finalRelevantYear shouldBe 2016
      npsSummaryF.dateOfBirth shouldBe new LocalDate(1952, 11, 21)
      npsSummaryF.dateOfDeath shouldBe None
      npsSummaryF.earningsIncludedUpTo shouldBe new LocalDate(2014, 4, 5)
    }

    "return valid Summary from cache" in {
      val npsSummaryF = connector.getSummary(generateNino())(HeaderCarrier())
      await(npsSummaryF) shouldBe testSummaryModel
      verify(connector.http, never()).GET(Matchers.any())(Matchers.any(), Matchers.any())
    }

    "return valid NIRecord response" in {
      when(mockNIRecordRepo.findByNino(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(niRecord)))
      val npsNIRecordF = connector.getNationalInsuranceRecord(generateNino())(HeaderCarrier())
      await(npsNIRecordF) shouldBe niRecord
    }

    "return valid NIRecord from cache" in {
      val npNIRecordF = connector.getNationalInsuranceRecord(generateNino())(HeaderCarrier())
      await(npNIRecordF) shouldBe niRecord
      verify(connector.http, never()).GET(Matchers.any())(Matchers.any(), Matchers.any())
    }

    "return valid Liabilities response" in {
      when(mockLiabilitiesRepo.findByNino(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(liabilities)))
      val npsLiabilitiesF = connector.getLiabilities(generateNino())(HeaderCarrier())
      await(npsLiabilitiesF) shouldBe liabilities
    }

    "return valid Liabilities from cache" in {
      val npsLiabilitiesF = connector.getLiabilities(generateNino())(HeaderCarrier())
      await(npsLiabilitiesF) shouldBe liabilities
      verify(connector.http, never()).GET(Matchers.any())(Matchers.any(), Matchers.any())
    }

  }
}
