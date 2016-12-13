/*
 * Copyright 2016 HM Revenue & Customs
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
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.connectors.CustomAuditConnector
import uk.gov.hmrc.nationalinsurancerecord.domain.{NationalInsuranceRecord, NationalInsuranceRecordExclusion, NationalInsuranceTaxYear, TaxYearSummary}
import uk.gov.hmrc.nationalinsurancerecord.services.{NationalInsuranceRecordService, SandboxNationalInsuranceService}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.AuditEvent
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.play.test.WithFakeApplication
import play.api.test.Helpers._

import scala.concurrent.Future

class NationalInsuranceRecordControllerSpec extends NationalInsuranceRecordUnitSpec with WithFakeApplication {

  val nino: Nino = generateNinoWithPrefix("EZ")

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

  private val dummyTaxYearDefault: NationalInsuranceTaxYear = NationalInsuranceTaxYear(
    taxYear =  "2010-11",
    qualifying = true,
    classOneContributions =  1149.98,
    classTwoCredits =  0,
    classThreeCredits =  0,
    otherCredits =  0,
    classThreePayable = 0,
    classThreePayableBy = None,
    classThreePayableByPenalty = None,
    payable = false,
    underInvestigation =  false
  )

  private val dummyTaxYearForEY: NationalInsuranceTaxYear = NationalInsuranceTaxYear(
    taxYear =  "2010-11",
    qualifying = true,
    classOneContributions =  1149.98,
    classTwoCredits =  0,
    classThreeCredits =  0,
    otherCredits =  0,
    classThreePayable = 0,
    classThreePayableBy = Some(new LocalDate(2017,4,5)),
    classThreePayableByPenalty = None,
    payable = false,
    underInvestigation =  false
  )

  "get" should {

    val testNIRecordController = testNationalInsuranceRecordController(SandboxNationalInsuranceService)

    "return NI Tax Year status code 406 when the headers are invalid" in {
      val response = testNIRecordController.getTaxYear(nino,"2010-11")(emptyRequest)
      status(response) shouldBe 406
      contentAsJson(response) shouldBe Json.parse("""{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}""")
    }

    "return NI Tax Year status code 200 with a Response" in {
      val response = testNIRecordController.getTaxYear(nino, "2010-11")(emptyRequestWithHeader)
      status(response) shouldBe 200
      val json = contentAsJson(response)
      (json \ "classThreePayableByPenalty").as[LocalDate] shouldBe new LocalDate(2023,4,5)
      (json \ "qualifying").as[Boolean] shouldBe false
      (json \ "underInvestigation").as[Boolean] shouldBe false
      (json \ "payable").as[Boolean] shouldBe false
      (json \ "taxYear").as[String] shouldBe "2010-11"
      (json \ "classThreePayable").as[BigDecimal] shouldBe 0
      (json \ "classOneContributions").as[BigDecimal] shouldBe 1149.98
      (json \ "otherCredits").as[Int] shouldBe 0
      (json \ "classTwoCredits").as[Int] shouldBe 0
      (json \ "classThreePayableBy") shouldBe JsDefined(JsNull)
      (json \ "classThreeCredits").as[Int] shouldBe 0
      (json \ "_links" \ "self" \ "href").as[String] shouldBe s"/test/ni/$nino/taxyear/2010-11"
    }

    "return code NISummary status code 406 when the headers are invalid" in {
      val response = testNIRecordController.getSummary(nino)(emptyRequest)
      status(response) shouldBe 406
      contentAsJson(response) shouldBe Json.parse("""{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}""")
    }

    "return code 200 with a valid NationalInsuranceRecord" in {
      val responseSummary = testNIRecordController.getSummary(nino)(emptyRequestWithHeader)
      status(responseSummary) shouldBe 200
      val json = contentAsJson(responseSummary)

      (json \ "qualifyingYears").as[Int] shouldBe 36
      (json \ "qualifyingYearsPriorTo1975").as[Int] shouldBe 5
      (json \ "numberOfGaps").as[Int] shouldBe 10
      (json \ "numberOfGapsPayable").as[Int] shouldBe 4
      (json \ "dateOfEntry").as[LocalDate] shouldBe new LocalDate(1969,8,1)
      (json \ "homeResponsibilitiesProtection").as[Boolean] shouldBe false
      (json \ "_links" \ "self" \ "href").as[String] shouldBe s"/test/ni/$nino"
      ((json \ "_embedded" \ "taxYears") (0) \ "_links" \ "self" \ "href").as[String] shouldBe s"/test/ni/$nino/taxyear/2015-16"
      ((json \ "_embedded" \ "taxYears") (0) \ "taxYear").as[String] shouldBe s"2015-16"
      ((json \ "_embedded" \ "taxYears") (0) \ "qualifying").as[Boolean] shouldBe true
      (json \ "_embedded" \ "taxYears").as[JsArray].value.length shouldBe 41

    }

    "return 403 with an error message for an MCI exclusion" in {
      val responseExclusionMCI = testNIRecordController.getSummary(generateNinoWithPrefix("MC"))(emptyRequestWithHeader)
      status(responseExclusionMCI) shouldBe 403
      contentAsJson(responseExclusionMCI) shouldBe Json.parse(
        """{"code":"EXCLUSION_MANUAL_CORRESPONDENCE","message":
          |"The customer cannot access the service, they should contact HMRC"}""".stripMargin)
    }

    "return 403 with an error message for Dead exclusion" in {
      val responseExclusionDead = testNIRecordController.getSummary(generateNinoWithPrefix("YN"))(emptyRequestWithHeader)
      status(responseExclusionDead) shouldBe 403
      contentAsJson(responseExclusionDead) shouldBe Json.parse(
        """{"code":"EXCLUSION_DEAD","message":
          |"The customer needs to contact the National Insurance helpline"}""".stripMargin)
    }

    "return 403 with an error message for Married Women Reduced Rate exclusion" in {
      val responseExclusionMWRRE = testNIRecordController.getSummary(generateNinoWithPrefix("MW"))(emptyRequestWithHeader)
      status(responseExclusionMWRRE) shouldBe 403
      contentAsJson(responseExclusionMWRRE) shouldBe Json.parse(
        """{"code":"EXCLUSION_MARRIED_WOMENS_REDUCED_RATE","message":
          |"The customer needs to contact the National Insurance helpline"}""".stripMargin)
    }

    "return 403 with an error message for Isle of Man exclusion" in {
      val responseExclusionIOM = testNIRecordController.getSummary(generateNinoWithPrefix("MA"))(emptyRequestWithHeader)
      status(responseExclusionIOM) shouldBe 403
      contentAsJson(responseExclusionIOM) shouldBe Json.parse(
        """{"code":"EXCLUSION_ISLE_OF_MAN","message":
          |"The customer needs to contact the National Insurance helpline"}""".stripMargin)
    }

  }

}
