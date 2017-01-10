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

package uk.gov.hmrc.nationalinsurancerecord.controllers

import org.joda.time.LocalDate
import play.api.libs.json._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.connectors.CustomAuditConnector
import uk.gov.hmrc.nationalinsurancerecord.domain._
import uk.gov.hmrc.nationalinsurancerecord.services.{NationalInsuranceRecordService, SandboxNationalInsuranceService}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.AuditEvent
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.play.test.WithFakeApplication
import play.api.test.Helpers.{contentAsJson, _}

import scala.concurrent.Future

class NationalInsuranceRecordControllerSpec extends NationalInsuranceRecordUnitSpec with WithFakeApplication {

  val emptyRequest = FakeRequest()
  val emptyRequestWithHeader = FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")

  val mockAuditConnector = new CustomAuditConnector {
    override lazy val auditConnector: AuditConnector = ???

    override def sendEvent(event: AuditEvent)(implicit hc: HeaderCarrier): Unit = {}
  }

  def testNationalInsuranceRecordController(niRecordService: NationalInsuranceRecordService): NationalInsuranceRecordController
  = new NationalInsuranceRecordController {
    override val nationalInsuranceRecordService: NationalInsuranceRecordService = niRecordService
    override val app: String = "Test National Insurance Record"
    override val context: String = "test"
    override val customAuditConnector: CustomAuditConnector = mockAuditConnector
  }

  private val dummyRecord: NationalInsuranceRecord = NationalInsuranceRecord(
    // scalastyle:off magic.number
    qualifyingYears = 36,
    qualifyingYearsPriorTo1975 = 5,
    numberOfGaps = 10,
    numberOfGapsPayable = 4,
    dateOfEntry = new LocalDate(1969, 8, 1),
    homeResponsibilitiesProtection = false,
    List(
      TaxYearSummary("2015-16", true),
      TaxYearSummary("2014-15", true),
      TaxYearSummary("2013-14", true),
      TaxYearSummary("2012-13", false),
      TaxYearSummary("2011-12", false),
      TaxYearSummary("2010-11", false),
      TaxYearSummary("2009-10", true),
      TaxYearSummary("2008-09", false),
      TaxYearSummary("2007-08", true),
      TaxYearSummary("2006-07", true),
      TaxYearSummary("2005-06", true),
      TaxYearSummary("2004-05", true),
      TaxYearSummary("2003-04", true),
      TaxYearSummary("2002-03", true),
      TaxYearSummary("2001-02", false),
      TaxYearSummary("2000-01", true),
      TaxYearSummary("1999-00", true),
      TaxYearSummary("1998-99", true),
      TaxYearSummary("1997-98", true),
      TaxYearSummary("1996-97", false),
      TaxYearSummary("1995-96", false),
      TaxYearSummary("1994-95", true),
      TaxYearSummary("1993-94", true),
      TaxYearSummary("1992-93", true),
      TaxYearSummary("1991-92", true),
      TaxYearSummary("1990-91", true),
      TaxYearSummary("1989-90", true),
      TaxYearSummary("1988-89", true),
      TaxYearSummary("1987-88", true),
      TaxYearSummary("1986-87", false),
      TaxYearSummary("1985-86", false),
      TaxYearSummary("1984-85", true),
      TaxYearSummary("1983-84", false),
      TaxYearSummary("1982-83", true),
      TaxYearSummary("1981-82", true),
      TaxYearSummary("1980-81", true),
      TaxYearSummary("1979-80", true),
      TaxYearSummary("1978-79", true),
      TaxYearSummary("1977-78", true),
      TaxYearSummary("1976-77", true),
      TaxYearSummary("1975-76", true)
    )
  )

  private val dummyTaxYearQualifying: NationalInsuranceTaxYear = NationalInsuranceTaxYear(
    taxYear = "2010-11",
    qualifying = true,
    classOneContributions = 1149.98,
    classTwoCredits = 0,
    classThreeCredits = 0,
    otherCredits = 0,
    classThreePayable = 0,
    classThreePayableBy = None,
    classThreePayableByPenalty = None,
    payable = false,
    underInvestigation = false
  )

  private val dummyTaxYearNonQualifying: NationalInsuranceTaxYear = NationalInsuranceTaxYear(
    taxYear = "2009-10",
    qualifying = false,
    classOneContributions = 0,
    classTwoCredits = 12,
    classThreeCredits = 0,
    otherCredits = 10,
    classThreePayable = 325.14,
    classThreePayableBy = Some(new LocalDate(2019, 4, 5)),
    classThreePayableByPenalty = Some(new LocalDate(2023, 4, 5)),
    payable = true,
    underInvestigation = false
  )

  "getSummary" when {

    def generateSummaryResponse(serviceResult: Either[ExclusionResponse, NationalInsuranceRecord], nino: Nino = generateNino()) =
      testNationalInsuranceRecordController(new NationalInsuranceRecordService {
        override def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[ExclusionResponse, NationalInsuranceRecord]] = Future.successful(serviceResult)

        override def getTaxYear(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Either[ExclusionResponse, NationalInsuranceTaxYear]] = ???
      }).getSummary(nino)(emptyRequestWithHeader)

    "the request headers are invalid" should {
      "return status code 406" in {
        val response = testNationalInsuranceRecordController(new NationalInsuranceRecordService {
          override def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[ExclusionResponse, NationalInsuranceRecord]] = ???

          override def getTaxYear(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Either[ExclusionResponse, NationalInsuranceTaxYear]] = ???
        }).getSummary(generateNino())(emptyRequest)
        status(response) shouldBe 406
        contentAsJson(response) shouldBe Json.parse("""{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}""")
      }
    }

    "there is a dead exclusion" should {

      val response = generateSummaryResponse(Left(ExclusionResponse(List(Exclusion.Dead))))

      "return status 403" in {
        status(response) shouldBe 403
      }

      "return the dead message" in {
        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_DEAD","message": "The customer needs to contact the National Insurance helpline"}"""
        )
      }

    }

    "there is a manual correspondence exclusion" should {

      val response = generateSummaryResponse(Left(ExclusionResponse(List(Exclusion.ManualCorrespondenceIndicator))))

      "return status 403" in {
        status(response) shouldBe 403
      }

      "return the mci message" in {
        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_MANUAL_CORRESPONDENCE","message": "The customer cannot access the service, they should contact HMRC"}"""
        )
      }

    }

    "there is a married women's reduced rate election exclusion" should {

      val response = generateSummaryResponse(Left(ExclusionResponse(List(Exclusion.MarriedWomenReducedRateElection))))

      "return status 403" in {
        status(response) shouldBe 403
      }

      "return the mwrre message" in {
        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_MARRIED_WOMENS_REDUCED_RATE","message": "The customer needs to contact the National Insurance helpline"}"""
        )
      }

    }

    "there is an Isle of Man exclusion" should {

      val response = generateSummaryResponse(Left(ExclusionResponse(List(Exclusion.IsleOfMan))))

      "return status 403" in {
        status(response) shouldBe 403
      }

      "return the IoM message" in {
        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_ISLE_OF_MAN","message": "The customer needs to contact the National Insurance helpline"}"""
        )
      }
    }

    "there is a a list of dead, MCI, MWRRE, IoM exclusions" should {

      val response = generateSummaryResponse(Left(ExclusionResponse(List(
        Exclusion.IsleOfMan,
        Exclusion.ManualCorrespondenceIndicator,
        Exclusion.Dead,
        Exclusion.MarriedWomenReducedRateElection
      ))))

      "return status 403" in {
        status(response) shouldBe 403
      }

      "return the dead message" in {
        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_DEAD","message": "The customer needs to contact the National Insurance helpline"}"""
        )
      }
    }

    "there is a a list of MCI, MWRRE, IoM exclusions" should {

      val response = generateSummaryResponse(Left(ExclusionResponse(List(
        Exclusion.IsleOfMan,
        Exclusion.ManualCorrespondenceIndicator,
        Exclusion.MarriedWomenReducedRateElection
      ))))

      "return status 403" in {
        status(response) shouldBe 403
      }

      "return the mci message" in {
        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_MANUAL_CORRESPONDENCE","message": "The customer cannot access the service, they should contact HMRC"}"""
        )
      }
    }

    "there is a a list of MWRRE and IoM exclusions" should {

      val response = generateSummaryResponse(Left(ExclusionResponse(List(
        Exclusion.IsleOfMan,
        Exclusion.MarriedWomenReducedRateElection
      ))))

      "return status 403" in {
        status(response) shouldBe 403
      }

      "return the mwrre message" in {
        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_MARRIED_WOMENS_REDUCED_RATE","message": "The customer needs to contact the National Insurance helpline"}"""
        )
      }

    }

    "there is a valid National Insurance Record" should {

      val testNino = generateNino()
      val responseSummary = generateSummaryResponse(Right(dummyRecord), testNino)
      val json = contentAsJson(responseSummary)

      "return 200" in {
        status(responseSummary) shouldBe 200
      }

      "have an Int called qualifyingYears which is 36" in {
        (json \ "qualifyingYears").as[Int] shouldBe 36
      }

      "have an Int called qualifyingYearsPriorTo1975 which is 5" in {
        (json \ "qualifyingYearsPriorTo1975").as[Int] shouldBe 5
      }
      "have an Int called numberOfGaps which is 10" in {
        (json \ "numberOfGaps").as[Int] shouldBe 10
      }

      "have an Int called numberOfGapsPayable which is 4" in {
        (json \ "numberOfGapsPayable").as[Int] shouldBe 4
      }
      "have a LocalDate called dateOfEntry which is 1/8/1969" in {
        (json \ "dateOfEntry").as[LocalDate] shouldBe  new LocalDate(1969, 8, 1)
      }

      "have a Boolean called homeResponsibilitiesProtection which is false" in {
        (json \ "homeResponsibilitiesProtection").as[Boolean] shouldBe  false
      }

      "have a list of 41 tax years" in {
        (json \ "_embedded" \ "taxYears").as[JsArray].value.length shouldBe 41
      }

      "have the first tax year be 2015-16" in {
        ((json \ "_embedded" \ "taxYears") \ 0 \ "taxYear").as[String] shouldBe "2015-16"
      }

      "have the first tax year be qualifying" in {
        ((json \ "_embedded" \ "taxYears") \ 0 \ "qualifying").as[Boolean] shouldBe true
      }

      "have the last tax year be 1975-76" in {
        ((json \ "_embedded" \ "taxYears") \ 40 \ "taxYear").as[String] shouldBe "1975-76"
      }

      "have the last tax year be qualifying" in {
        ((json \ "_embedded" \ "taxYears") \ 40 \ "qualifying").as[Boolean] shouldBe true
      }

      "have tax years we a link to their resource" in {
        ((json \ "_embedded" \ "taxYears") \ 0 \ "_links" \ "self" \ "href").as[String] shouldBe s"/test/ni/$testNino/taxyear/2015-16"
      }

      "have a link to itself" in {
        ((json \ "_links" \ "self") \ "href" ).as[String] shouldBe s"/test/ni/$testNino"
      }

    }
  }

  "getTaxYear" should {

    def generateTaxYearResponse(serviceResult: Either[ExclusionResponse, NationalInsuranceTaxYear], nino: Nino = generateNino(), taxYear: TaxYear = TaxYear("0000-01")) =
      testNationalInsuranceRecordController(new NationalInsuranceRecordService {
        override def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[ExclusionResponse, NationalInsuranceRecord]] = ???

        override def getTaxYear(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Either[ExclusionResponse, NationalInsuranceTaxYear]] = Future.successful(serviceResult)
      }).getTaxYear(nino, taxYear)(emptyRequestWithHeader)

    "the request headers are invalid" should {
      "return status code 406" in {
        val response = testNationalInsuranceRecordController(new NationalInsuranceRecordService {
          override def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[ExclusionResponse, NationalInsuranceRecord]] = ???

          override def getTaxYear(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Either[ExclusionResponse, NationalInsuranceTaxYear]] = ???
        }).getTaxYear(generateNino(), TaxYear("0000-01"))(emptyRequest)
        status(response) shouldBe 406
        contentAsJson(response) shouldBe Json.parse("""{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}""")
      }
    }

    "there is a dead exclusion" should {

      val response = generateTaxYearResponse(Left(ExclusionResponse(List(Exclusion.Dead))))

      "return status 403" in {
        status(response) shouldBe 403
      }

      "return the dead message" in {
        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_DEAD","message": "The customer needs to contact the National Insurance helpline"}"""
        )
      }

    }

    "there is a manual correspondence exclusion" should {

      val response = generateTaxYearResponse(Left(ExclusionResponse(List(Exclusion.ManualCorrespondenceIndicator))))

      "return status 403" in {
        status(response) shouldBe 403
      }

      "return the mci message" in {
        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_MANUAL_CORRESPONDENCE","message": "The customer cannot access the service, they should contact HMRC"}"""
        )
      }

    }

    "there is a married women's reduced rate election exclusion" should {

      val response = generateTaxYearResponse(Left(ExclusionResponse(List(Exclusion.MarriedWomenReducedRateElection))))

      "return status 403" in {
        status(response) shouldBe 403
      }

      "return the mwrre message" in {
        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_MARRIED_WOMENS_REDUCED_RATE","message": "The customer needs to contact the National Insurance helpline"}"""
        )
      }

    }

    "there is an Isle of Man exclusion" should {

      val response = generateTaxYearResponse(Left(ExclusionResponse(List(Exclusion.IsleOfMan))))

      "return status 403" in {
        status(response) shouldBe 403
      }

      "return the IoM message" in {
        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_ISLE_OF_MAN","message": "The customer needs to contact the National Insurance helpline"}"""
        )
      }
    }

    "there is a a list of dead, MCI, MWRRE, IoM exclusions" should {

      val response = generateTaxYearResponse(Left(ExclusionResponse(List(
        Exclusion.IsleOfMan,
        Exclusion.ManualCorrespondenceIndicator,
        Exclusion.Dead,
        Exclusion.MarriedWomenReducedRateElection
      ))))

      "return status 403" in {
        status(response) shouldBe 403
      }

      "return the dead message" in {
        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_DEAD","message": "The customer needs to contact the National Insurance helpline"}"""
        )
      }
    }

    "there is a a list of MCI, MWRRE, IoM exclusions" should {

      val response = generateTaxYearResponse(Left(ExclusionResponse(List(
        Exclusion.IsleOfMan,
        Exclusion.ManualCorrespondenceIndicator,
        Exclusion.MarriedWomenReducedRateElection
      ))))

      "return status 403" in {
        status(response) shouldBe 403
      }

      "return the mci message" in {
        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_MANUAL_CORRESPONDENCE","message": "The customer cannot access the service, they should contact HMRC"}"""
        )
      }
    }

    "there is a a list of MWRRE and IoM exclusions" should {

      val response = generateTaxYearResponse(Left(ExclusionResponse(List(
        Exclusion.IsleOfMan,
        Exclusion.MarriedWomenReducedRateElection
      ))))

      "return status 403" in {
        status(response) shouldBe 403
      }

      "return the mwrre message" in {
        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_MARRIED_WOMENS_REDUCED_RATE","message": "The customer needs to contact the National Insurance helpline"}"""
        )
      }

    }

    "there is a valid Qualifying Tax Year" should {
      val testNino = generateNino()
      val response = generateTaxYearResponse(Right(dummyTaxYearQualifying), testNino, TaxYear(dummyTaxYearQualifying.taxYear))
      val json = contentAsJson(response)

      "return 200" in {
        status(response) shouldBe 200
      }

      "have a string called taxYear that is 2010-11" in {
        (json \ "taxYear").as[String] shouldBe "2010-11"
      }

      "have a boolean called qualifying that is true" in {
        (json \ "qualifying").as[Boolean] shouldBe true
      }

      "have a big decimal called classOneContributions that is 1149.98" in {
        (json \ "classOneContributions").as[BigDecimal] shouldBe 1149.98
      }

      "have an Int called classTwoCredits that is 0" in {
        (json \ "classTwoCredits").as[Int] shouldBe 0
      }

      "have an Int called classThreeCredits that is 0" in {
        (json \ "classThreeCredits").as[Int] shouldBe 0
      }

      "have an Int called otherCredits that is 0" in {
        (json \ "otherCredits").as[Int] shouldBe 0
      }

      "have an Int called classThreePayable that is 0" in {
        (json \ "classThreePayable").as[BigDecimal] shouldBe 0
      }

      "have a nullable local date called classThreePayableBy that is null" in {
        (json \ "classThreePayableBy") shouldBe JsDefined(JsNull)
      }

      "have a nullable local date called classThreePayableByPenalty that is null" in {
        (json \ "classThreePayableByPenalty") shouldBe JsDefined(JsNull)
      }

      "have a Boolean called payable that is false" in {
        (json \ "payable").as[Boolean] shouldBe false
      }

      "have a Boolean called underInvestigation that is false" in {
        (json \ "underInvestigation").as[Boolean] shouldBe false
      }

      "have a link to itself" in {
        ((json \ "_links" \ "self") \ "href" ).as[String] shouldBe s"/test/ni/$testNino/taxyear/2010-11"
      }
    }

    "there is a valid Non-Qualifying Tax Year" should {
      val testNino = generateNino()
      val response = generateTaxYearResponse(Right(dummyTaxYearNonQualifying), testNino, TaxYear(dummyTaxYearNonQualifying.taxYear))
      val json = contentAsJson(response)

      "return 200" in {
        status(response) shouldBe 200
      }

      "have a string called taxYear that is 2009-10" in {
        (json \ "taxYear").as[String] shouldBe "2009-10"
      }

      "have a boolean called qualifying that is false" in {
        (json \ "qualifying").as[Boolean] shouldBe false
      }

      "have a big decimal called classOneContributions that is 0" in {
        (json \ "classOneContributions").as[BigDecimal] shouldBe 0
      }

      "have an Int called classTwoCredits that is 12" in {
        (json \ "classTwoCredits").as[Int] shouldBe 12
      }

      "have an Int called classThreeCredits that is 0" in {
        (json \ "classThreeCredits").as[Int] shouldBe 0
      }

      "have an Int called otherCredits that is 10" in {
        (json \ "otherCredits").as[Int] shouldBe 10
      }

      "have an Int called classThreePayable that is 325.14" in {
        (json \ "classThreePayable").as[BigDecimal] shouldBe 325.14
      }

      "have a nullable local date called classThreePayableBy that is 5/4/2019" in {
        (json \ "classThreePayableBy").as[LocalDate] shouldBe new LocalDate(2019, 4, 5)
      }

      "have a nullable local date called classThreePayableByPenalty that is 5/4/2023" in {
        (json \ "classThreePayableByPenalty").as[LocalDate] shouldBe new LocalDate(2023, 4, 5)
      }

      "have a Boolean called payable that is true" in {
        (json \ "payable").as[Boolean] shouldBe true
      }

      "have a Boolean called underInvestigation that is false" in {
        (json \ "underInvestigation").as[Boolean] shouldBe false
      }

      "have a link to itself" in {
        ((json \ "_links" \ "self") \ "href" ).as[String] shouldBe s"/test/ni/$testNino/taxyear/2009-10"
      }
    }

  }

}
