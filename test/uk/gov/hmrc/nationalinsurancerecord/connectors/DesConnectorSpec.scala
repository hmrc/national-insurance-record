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

package uk.gov.hmrc.nationalinsurancerecord.connectors

import com.codahale.metrics.Timer
import com.github.tomakehurst.wiremock.client.WireMock.{reset => _, verify => _, _}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HeaderNames, RequestId}
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.cache._
import uk.gov.hmrc.nationalinsurancerecord.config.ApplicationConfig
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.des.{DesError, DesLiabilities, DesNIRecord, DesSummary}
import uk.gov.hmrc.nationalinsurancerecord.services.MetricsService
import uk.gov.hmrc.nationalinsurancerecord.util.WireMockHelper

import java.time.LocalDate
import scala.concurrent.Future
import scala.util.Random

class DesConnectorSpec
  extends NationalInsuranceRecordUnitSpec
    with GuiceOneAppPerSuite
    with WireMockHelper
    with ScalaFutures {
  // scalastyle:off magic.number

  val mockSummaryRepo: DesSummaryRepository = mock[DesSummaryRepository](Mockito.RETURNS_DEEP_STUBS)

  val mockLiabilitiesRepo: DesLiabilitiesRepository = mock[DesLiabilitiesRepository](Mockito.RETURNS_DEEP_STUBS)

  val mockNIRecordRepo: DesNIRecordRepository = mock[DesNIRecordRepository](Mockito.RETURNS_DEEP_STUBS)

  val mockMetrics: MetricsService = mock[MetricsService]

  val mockTimerContext: Timer.Context = mock[Timer.Context]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.des-hod.port" -> server.port(),
      "microservice.services.des-hod.host" -> "127.0.0.1"
    )
    .overrides(
      bind[MetricsService].toInstance(mockMetrics),
      bind[Timer.Context].toInstance(mockTimerContext),
      bind[DesNIRecordRepository].toInstance(mockNIRecordRepo),
      bind[DesSummaryRepository].toInstance(mockSummaryRepo),
      bind[DesLiabilitiesRepository].toInstance(mockLiabilitiesRepo)
    )
    .build()

  lazy val connector: DesConnector = app.injector.instanceOf[DesConnector]
  lazy val appConfig = app.injector.instanceOf[ApplicationConfig]

  val testSummaryRecordJson = Json.parse(
    """
      | {
      |   "finalRelevantYear": 2016,
      |   "dateOfDeath": null,
      |   "dateOfBirth": "1952-11-21",
      |   "reducedRateElectionToConsider": false,
      |   "earningsIncludedUpto": "2014-04-05"
      | }""".stripMargin
  )

  val testNIRecordJson: JsValue = Json.parse(
    """
      | {
      | "years_to_fry": 1,
      | "nonQualifyingYears": 13,
      | "dateOfEntry": "1973-10-01",
      | "npsLniemply": [],
      | "pre75CcCount": 51,
      | "numberOfQualifyingYears": 27,
      | "npsErrlist": {
      |   "count": 0,
      | "mgt_check": 0,
      | "commit_status": 2,
      | "npsErritem": [],
      | "bfm_return_code": 0,
      | "data_not_found": 0
      |},
      |"nonQualifyingYearsPayable": 0,
      |  "taxYears": [
      | {
      | "classThreePayableBYPenalty": null,
      | "class_two_outstanding_weeks": null,
      | "class_two_payable": null,
      | "qualifying": true,
      | "underInvestigationFlag": false,
      | "class_two_payable_by": null,
      | "co_class_one_paid": null,
      | "class_two_payable_by_penalty": null,
      | "co_primary_paid_earnings": null,
      | "payable": false,
      | "rattdTaxYear": "2012",
      | "ni_earnings": null,
      | "amount_needed": null,
      | "primary_paid_earnings": "21750.0000",
      | "classThreePayable": 0.0,
      | "niEarningsEmployed": 1698.9600,
      | "otherCredits": [
      |   {
      |      "creditSourceType": 2,
      |      "ccType": 23,
      |      "numberOfCredits": 4
      |   }
      | ],
      | "niEarningsSelfEmployed": 0,
      | "classThreePayableBY": null,
      | "niEarningsVoluntary": 0
      |},
      |{
      | "classThreePayableByPenalty": "2023-04-05",
      | "class_two_outstanding_weeks": null,
      | "class_two_payable": null,
      | "qualifying": false,
      | "underInvestigationFlag": true,
      | "class_two_payable_by": null,
      | "co_class_one_paid": null,
      | "class_two_payable_by_penalty": null,
      | "co_primary_paid_earnings": null,
      | "payable": true,
      | "rattdTaxYear": "2013",
      | "ni_earnings": null,
      | "amount_needed": null,
      | "primary_paid_earnings": null,
      | "classThreePayable": 722.80,
      | "niEarningsEmployed": 0.0,
      | "otherCredits": [],
      | "niEarningsSelfEmployed": 52,
      | "classThreePayableBy": "2019-04-05",
      | "niEarningsVoluntary": 0
      |}
      |],
      | "nino": "<NINO>"
      |}
    """.stripMargin
  )

  val niRecord: DesNIRecord = testNIRecordJson.as[DesNIRecord]

  val testNIRecordNoTaxYearsJson = Json.parse(
    """
      | {
      | "years_to_fry": 1,
      | "nonQualifyingYears": 13,
      | "dateOfEntry": "1973-10-01",
      | "npsLniemply": [],
      | "pre75CcCount": 51,
      | "numberOfQualifyingYears": 27,
      | "npsErrlist": {
      |   "count": 0,
      | "mgt_check": 0,
      | "commit_status": 2,
      | "npsErritem": [],
      | "bfm_return_code": 0,
      | "data_not_found": 0
      |},
      |"nonQualifyingYearsPayable": 0,
      | "nino": "<NINO>"
      |}
    """.stripMargin
  )

  val testLiabilitiesJson: JsValue = Json.parse(
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
      |  "liabilities": [
      |    {
      |      "liability_type_end_date": "2012-06-23",
      |      "liability_occurrence_no": 1,
      |      "liability_type_start_date": "2011-08-21",
      |      "liability_type_end_date_reason": "END DATE HELD",
      |      "liabilityType": 13
      |    },
      |    {
      |      "liability_type_end_date": "1998-05-03",
      |      "liability_occurrence_no": 1,
      |      "liability_type_start_date": "1989-08-28",
      |      "liability_type_end_date_reason": "END DATE HELD",
      |      "liabilityType": 16
      |    },
      |    {
      |      "liability_type_end_date": "2005-08-23",
      |      "liability_occurrence_no": 2,
      |      "liability_type_start_date": "1998-05-04",
      |      "liability_type_end_date_reason": "END DATE HELD",
      |      "liabilityType": 16
      |    },
      |    {
      |      "liability_type_end_date": "1995-02-11",
      |      "liability_occurrence_no": 1,
      |      "liability_type_start_date": "1995-02-06",
      |      "liability_type_end_date_reason": "END DATE HELD",
      |      "liabilityType": 34
      |    },
      |    {
      |      "liability_type_end_date": "1998-06-13",
      |      "liability_occurrence_no": 2,
      |      "liability_type_start_date": "1997-04-06",
      |      "liability_type_end_date_reason": "END DATE HELD",
      |      "liabilityType": 34
      |    },
      |    {
      |      "liability_type_end_date": "2000-09-25",
      |      "liability_occurrence_no": 1,
      |      "liability_type_start_date": "2000-03-28",
      |      "liability_type_end_date_reason": "END DATE HELD",
      |      "liabilityType": 71
      |    },
      |    {
      |      "liability_type_end_date": "2004-04-05",
      |      "liability_occurrence_no": 1,
      |      "liability_type_start_date": "2003-09-12",
      |      "liability_type_end_date_reason": "END DATE HELD",
      |      "liabilityType": 83
      |    }
      |  ]
      |}
    """.stripMargin
  )

  val testEmptyLiabilitiesJson: JsValue = Json.parse(
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
      |  "liabilities": [
      |    {
      |    }
      |  ]
      |}
    """.stripMargin)

  val liabilities: DesLiabilities = testLiabilitiesJson.as[DesLiabilities]



  "DesConnector - No Caching" should {

    val nino = generateNino()

    "return valid Summary in HTTP response" in {
      reset(mockMetrics)
      when(mockMetrics.startTimer(APITypes.Summary)).thenReturn(mockTimerContext)
      when(mockSummaryRepo().findByNino(any())(any(), any())).thenReturn(Future.successful(None))

      server.stubFor(get(urlEqualTo(s"/individuals/${nino.withoutSuffix}/pensions/summary"))
        .willReturn(ok(Json.stringify(testSummaryRecordJson))))

      val desSummaryF = await(connector.getSummary(nino)(HeaderCarrier()))
      desSummaryF.rreToConsider shouldBe false
      desSummaryF.finalRelevantYear shouldBe Some(2016)
      desSummaryF.dateOfBirth shouldBe Some(LocalDate.of(1952,11,21))
      desSummaryF.dateOfDeath shouldBe None
      desSummaryF.earningsIncludedUpTo shouldBe Some(LocalDate.of(2014,4,5))
    }

    "log correct Summary metrics" in {
      verify(mockMetrics, Mockito.atLeastOnce()).incrementCounter(APITypes.Summary)
      verify(mockMetrics, Mockito.atLeastOnce()).startTimer(APITypes.Summary)
      verify(mockTimerContext, Mockito.atLeastOnce()).stop()
    }

    "return valid NIRecord response" in {
      reset(mockMetrics)
      when(mockMetrics.startTimer(APITypes.NIRecord)).thenReturn(mock[Timer.Context])
      when(mockNIRecordRepo().findByNino(any())(any(), any())).thenReturn(Future.successful(None))

      server.stubFor(get(urlEqualTo(s"/individuals/${nino.withoutSuffix}/pensions/ni"))
        .willReturn(ok(Json.stringify(testNIRecordJson))))

      val desNIRecordF = await(connector.getNationalInsuranceRecord(nino)(HeaderCarrier()))
      desNIRecordF.nonQualifyingYearsPayable shouldBe 0
      desNIRecordF.nonQualifyingYears shouldBe 13
      desNIRecordF.numberOfQualifyingYears shouldBe 27
      desNIRecordF.pre75ContributionCount shouldBe 51
      desNIRecordF.dateOfEntry shouldBe Some(LocalDate.of(1973,10,1))
      desNIRecordF.niTaxYears.head.qualifying shouldBe true
      desNIRecordF.niTaxYears(1).qualifying shouldBe false
      desNIRecordF.niTaxYears.head.underInvestigation shouldBe false
      desNIRecordF.niTaxYears(1).underInvestigation shouldBe true
      desNIRecordF.niTaxYears.head.payable shouldBe false
      desNIRecordF.niTaxYears(1).payable shouldBe true
      desNIRecordF.niTaxYears.head.classThreePayable shouldBe 0
      desNIRecordF.niTaxYears(1).classThreePayable shouldBe 722.80
      desNIRecordF.niTaxYears.head.classThreePayableBy shouldBe None
      desNIRecordF.niTaxYears(1).classThreePayableBy shouldBe Some(LocalDate.of(2019, 4, 5))
      desNIRecordF.niTaxYears.head.classThreePayableByPenalty shouldBe None
      desNIRecordF.niTaxYears(1).classThreePayableByPenalty shouldBe Some(LocalDate.of(2023, 4, 5))
      desNIRecordF.niTaxYears.head.classOneContribution shouldBe 1698.9600
      desNIRecordF.niTaxYears(1).classOneContribution shouldBe 0
      desNIRecordF.niTaxYears.head.classTwoCredits shouldBe 0
      desNIRecordF.niTaxYears(1).classTwoCredits shouldBe 52
      desNIRecordF.niTaxYears.head.otherCredits.head.creditSourceType.get shouldBe 2
      desNIRecordF.niTaxYears.head.otherCredits.head.creditContributionType.get shouldBe 23
      desNIRecordF.niTaxYears.head.otherCredits.head.numberOfCredits.get shouldBe 4
      desNIRecordF.niTaxYears(1).otherCredits shouldBe List()
    }

    "log correct NIRecord metrics" in {
      verify(mockMetrics, Mockito.atLeastOnce()).incrementCounter(APITypes.NIRecord)
      verify(mockMetrics, Mockito.atLeastOnce()).startTimer(APITypes.NIRecord)
      verify(mockTimerContext, Mockito.atLeastOnce()).stop()
    }

    "return a failed NIRecord when there is an http error and pass on the exception" in {
      server.stubFor(get(urlEqualTo(s"/individuals/${nino.withoutSuffix}/pensions/ni"))
        .willReturn(badRequest()))
      when(mockNIRecordRepo().findByNino(any())(any(), any())).thenReturn(Future.successful(None))

      ScalaFutures.whenReady(connector.getNationalInsuranceRecord(nino).failed) { ex =>
        ex shouldBe a[DesError.HttpError]
      }
    }

    "return valid Liabilities response" in {
      reset(mockMetrics)
      when(mockMetrics.startTimer(APITypes.Liabilities)).thenReturn(mock[Timer.Context])
      when(mockLiabilitiesRepo().findByNino(any())(any(), any())).thenReturn(Future.successful(None))

      server.stubFor(get(urlEqualTo(s"/individuals/${nino.withoutSuffix}/pensions/liabilities"))
        .willReturn(ok(Json.stringify(testLiabilitiesJson))))

      val desLiabilitiesF = await(connector.getLiabilities(nino)(HeaderCarrier()))
      desLiabilitiesF.liabilities.head.liabilityType shouldBe Some(13)
      desLiabilitiesF.liabilities(4).liabilityType shouldBe Some(34)
    }

    "return valid NIRecord response when no tax years are present" in {
      reset(mockMetrics)
      when(mockMetrics.startTimer(APITypes.NIRecord)).thenReturn(mock[Timer.Context])
      when(mockNIRecordRepo().findByNino(any())(any(), any())).thenReturn(Future.successful(None))

      server.stubFor(get(urlEqualTo(s"/individuals/${nino.withoutSuffix}/pensions/ni"))
        .willReturn(ok(Json.stringify(testNIRecordNoTaxYearsJson))))

      val desNIRecordF = await(connector.getNationalInsuranceRecord(nino)(HeaderCarrier()))
      desNIRecordF.niTaxYears shouldBe List.empty
    }

    "return an empty Liabilities list" in {
      reset(mockMetrics)
      when(mockMetrics.startTimer(APITypes.Liabilities)).thenReturn(mock[Timer.Context])
      when(mockLiabilitiesRepo().findByNino(any())(any(), any())).thenReturn(Future.successful(None))

      server.stubFor(get(urlEqualTo(s"/individuals/${nino.withoutSuffix}/pensions/liabilities"))
        .willReturn(ok(Json.stringify(testEmptyLiabilitiesJson))))

      val desLiabilitiesF = await(connector.getLiabilities(nino)(HeaderCarrier()))
      desLiabilitiesF.liabilities.size shouldBe 0
    }

    "log correct Liabilities metrics" in {
      verify(mockMetrics, Mockito.atLeastOnce()).incrementCounter(APITypes.Liabilities)
      verify(mockMetrics, Mockito.atLeastOnce()).startTimer(APITypes.Liabilities)
      verify(mockTimerContext, Mockito.atLeastOnce()).stop()
    }

    "return a failed Liabilities when there is an http error and pass on the exception" in {
      when(mockLiabilitiesRepo().findByNino(any())(any(), any())).thenReturn(Future.successful(None))

      server.stubFor(get(urlEqualTo(s"/individuals/${nino.withoutSuffix}/pensions/liabilities"))
        .willReturn(badRequest()))
      ScalaFutures.whenReady(connector.getLiabilities(nino).failed) { ex =>
        ex shouldBe a[DesError.HttpError]
      }
    }

    "return a depersonalised JSON structure following validation error" in {
      val invalidJson =
        """
          | {
          | "years_to_fry": "a",
          | "nonQualifyingYears": "13",
          | "dateOfEntry": "1973-10-01",
          | "npsLniemply": [],
          | "pre75CcCount": 51,
          | "numberOfQualifyingYears": 27,
          | "npsErrlist": {
          |   "count": 0,
          |   "mgt_check": 0,
          |   "commit_status": 2,
          |   "npsErritem": [],
          |   "bfm_return_code": 0,
          |   "data_not_found": 0
          |  }
          |}
        """.stripMargin

      reset(mockMetrics)
      when(mockMetrics.startTimer(APITypes.NIRecord)).thenReturn(mock[Timer.Context])
      when(mockNIRecordRepo().findByNino(any())(any(), any())).thenReturn(Future.successful(None))
      server.stubFor(get(urlEqualTo(s"/individuals/${nino.withoutSuffix}/pensions/ni"))
        .willReturn(ok(invalidJson)))

      ScalaFutures.whenReady(connector.getNationalInsuranceRecord(nino)(HeaderCarrier()).failed) { ex =>
        ex shouldBe a[DesError.JsonValidationError]
        ex.getMessage.contains("1973-10-01") shouldBe false
        ex.getMessage.contains("1111-11-11") shouldBe true
      }
    }
  }

  "DesConnector - Caching" should {

    val testSummaryModel = DesSummary(rreToConsider = false, dateOfDeath = None, earningsIncludedUpTo = Some(LocalDate.of(2014, 4, 5)),
      dateOfBirth = Some(LocalDate.of(1952, 11, 21)), finalRelevantYear = Some(2016))

    "return valid Summary response" in {
      when(mockSummaryRepo().findByNino(any())(any(), any())).thenReturn(Future.successful(Some(testSummaryModel)))
      val desSummaryF = await(connector.getSummary(generateNino())(HeaderCarrier()))
      desSummaryF.rreToConsider shouldBe false
      desSummaryF.finalRelevantYear shouldBe Some(2016)
      desSummaryF.dateOfBirth shouldBe Some(LocalDate.of(1952, 11, 21))
      desSummaryF.dateOfDeath shouldBe None
      desSummaryF.earningsIncludedUpTo shouldBe Some(LocalDate.of(2014, 4, 5))
    }

    "return valid Summary from cache" in {
      when(mockSummaryRepo().findByNino(any())(any(), any())).thenReturn(Future.successful(Some(testSummaryModel)))
      val desSummaryF = connector.getSummary(generateNino())(HeaderCarrier())
      await(desSummaryF) shouldBe testSummaryModel
    }

    "return valid NIRecord response" in {
      when(mockNIRecordRepo().findByNino(any())(any(), any())).thenReturn(Future.successful(Some(niRecord)))
      val desNIRecordF = connector.getNationalInsuranceRecord(generateNino())(HeaderCarrier())
      await(desNIRecordF) shouldBe niRecord
    }

    "return valid NIRecord from cache" in {
      when(mockNIRecordRepo().findByNino(any())(any(), any())).thenReturn(Future.successful(Some(niRecord)))
      val desNIRecordF = connector.getNationalInsuranceRecord(generateNino())(HeaderCarrier())
      await(desNIRecordF) shouldBe niRecord
    }

    "return valid Liabilities response" in {
      when(mockLiabilitiesRepo().findByNino(any())(any(), any())).thenReturn(Future.successful(Some(liabilities)))
      val desLiabilitiesF = connector.getLiabilities(generateNino())(HeaderCarrier())
      await(desLiabilitiesF) shouldBe liabilities
    }

    "return valid Liabilities from cache" in {
      when(mockLiabilitiesRepo().findByNino(any())(any(), any())).thenReturn(Future.successful(Some(liabilities)))
      val desLiabilitiesF = connector.getLiabilities(generateNino())(HeaderCarrier())
      await(desLiabilitiesF) shouldBe liabilities
    }
  }

  "Des connector - headers" should {

    val nino = generateNino()
    val authValue = Random.alphanumeric.take(20).mkString
    val headerCarrier = HeaderCarrier(authorization = Some(Authorization(authValue)), requestId = Some(RequestId("requestId")))

    "be present for summary" in {

      when(mockMetrics.startTimer(any())).thenReturn(mock[Timer.Context])
      when(mockSummaryRepo().findByNino(any())(any(), any())).thenReturn(Future.successful(None))
      server.stubFor(get(urlEqualTo(s"/individuals/${nino.withoutSuffix}/pensions/summary"))
        .willReturn(ok(Json.stringify(testSummaryRecordJson))))

      await(connector.getSummary(nino)(headerCarrier))

      server.verify(getRequestedFor(urlEqualTo(s"/individuals/${nino.withoutSuffix}/pensions/summary"))
        .withHeader(HeaderNames.authorisation, equalTo(appConfig.authorization))
        .withHeader("Originator-Id", equalTo("DA_PF"))
        .withHeader("Environment", equalTo(appConfig.desEnvironment))
        .withHeader("CorrelationId", matching("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}"))
        .withHeader(HeaderNames.xRequestId, equalTo("requestId"))
      )
    }

    "be present for ni" in {

      when(mockMetrics.startTimer(any())).thenReturn(mock[Timer.Context])
      when(mockNIRecordRepo().findByNino(any())(any(), any())).thenReturn(Future.successful(None))
      server.stubFor(get(urlEqualTo(s"/individuals/${nino.withoutSuffix}/pensions/ni"))
        .willReturn(ok(Json.stringify(testNIRecordJson))))

      await(connector.getNationalInsuranceRecord(nino)(headerCarrier))

      server.verify(getRequestedFor(urlEqualTo(s"/individuals/${nino.withoutSuffix}/pensions/ni"))
        .withHeader(HeaderNames.authorisation, equalTo(appConfig.authorization))
        .withHeader("Originator-Id", equalTo("DA_PF"))
        .withHeader("Environment", equalTo(appConfig.desEnvironment))
        .withHeader("CorrelationId", matching("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}"))
        .withHeader(HeaderNames.xRequestId, equalTo("requestId"))
      )
    }

    "be present for liabilities" in {

      when(mockMetrics.startTimer(any())).thenReturn(mock[Timer.Context])
      when(mockLiabilitiesRepo().findByNino(any())(any(), any())).thenReturn(Future.successful(None))
      server.stubFor(get(urlEqualTo(s"/individuals/${nino.withoutSuffix}/pensions/liabilities"))
        .willReturn(ok(Json.stringify(testLiabilitiesJson))))

      await(connector.getLiabilities(nino)(headerCarrier))

      server.verify(getRequestedFor(urlEqualTo(s"/individuals/${nino.withoutSuffix}/pensions/liabilities"))
        .withHeader(HeaderNames.authorisation, equalTo(appConfig.authorization))
        .withHeader("Originator-Id", equalTo("DA_PF"))
        .withHeader("Environment", equalTo(appConfig.desEnvironment))
        .withHeader("CorrelationId", matching("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}"))
        .withHeader(HeaderNames.xRequestId, equalTo("requestId"))
      )
    }

  }
}
