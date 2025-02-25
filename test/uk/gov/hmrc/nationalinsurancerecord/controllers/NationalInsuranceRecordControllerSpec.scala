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

package uk.gov.hmrc.nationalinsurancerecord.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.nationalinsurancerecord.connectors.{ApiStatePensionConnector, MdtpStatePensionConnector}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.controllers.nationalInsurance.NationalInsuranceRecordController
import uk.gov.hmrc.nationalinsurancerecord.domain._
import uk.gov.hmrc.nationalinsurancerecord.services.NationalInsuranceRecordService
import uk.gov.hmrc.nationalinsurancerecord.util.DateFormats.localDateFormat

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

trait NationalInsuranceRecordControllerSpec extends NationalInsuranceRecordUnitSpec with GuiceOneAppPerSuite with BeforeAndAfterEach with ScalaFutures {

  val mockApiStatePensionConnector: ApiStatePensionConnector = mock[ApiStatePensionConnector]
  val mockMdtpStatePensionConnector: MdtpStatePensionConnector = mock[MdtpStatePensionConnector]
  val emptyRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val emptyRequestWithHeader: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
  val mockNationalInsuranceRecordService: NationalInsuranceRecordService = mock[NationalInsuranceRecordService]
  val nino: Nino = generateNino()
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  def mockStatePensionController(returnVal: Option[HttpResponse]): OngoingStubbing[Future[Option[HttpResponse]]]

  mockStatePensionController(None)

  def nationalInsuranceRecordController: NationalInsuranceRecordController

  val linkPath: String

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
    classThreePayableBy = Some(LocalDate.of(2019, 4, 5)),
    classThreePayableByPenalty = Some(LocalDate.of(2023, 4, 5)),
    payable = true,
    underInvestigation = false
  )


  private def generateTaxYear(taxYear: String, qualifying: Boolean): NationalInsuranceTaxYear = {
    if (qualifying) {
      dummyTaxYearQualifying.copy(taxYear = taxYear)
    } else {
      dummyTaxYearNonQualifying.copy(taxYear = taxYear)
    }
  }

  private val dummyRecord: NationalInsuranceRecord = NationalInsuranceRecord(
    // scalastyle:off magic.number
    qualifyingYears = 36,
    qualifyingYearsPriorTo1975 = 5,
    numberOfGaps = 10,
    numberOfGapsPayable = 4,
    dateOfEntry = Some(LocalDate.of(1969, 8, 1)),
    homeResponsibilitiesProtection = false,
    earningsIncludedUpTo = LocalDate.of(2016, 4, 5),
    List(
      generateTaxYear("2015-16", true),
      generateTaxYear("2014-15", true),
      generateTaxYear("2013-14", true),
      generateTaxYear("2012-13", false),
      generateTaxYear("2011-12", false),
      generateTaxYear("2010-11", false),
      generateTaxYear("2009-10", true),
      generateTaxYear("2008-09", false),
      generateTaxYear("2007-08", true),
      generateTaxYear("2006-07", true),
      generateTaxYear("2005-06", true),
      generateTaxYear("2004-05", true),
      generateTaxYear("2003-04", true),
      generateTaxYear("2002-03", true),
      generateTaxYear("2001-02", false),
      generateTaxYear("2000-01", true),
      generateTaxYear("1999-00", true),
      generateTaxYear("1998-99", true),
      generateTaxYear("1997-98", true),
      generateTaxYear("1996-97", false),
      generateTaxYear("1995-96", false),
      generateTaxYear("1994-95", true),
      generateTaxYear("1993-94", true),
      generateTaxYear("1992-93", true),
      generateTaxYear("1991-92", true),
      generateTaxYear("1990-91", true),
      generateTaxYear("1989-90", true),
      generateTaxYear("1988-89", true),
      generateTaxYear("1987-88", true),
      generateTaxYear("1986-87", false),
      generateTaxYear("1985-86", false),
      generateTaxYear("1984-85", true),
      generateTaxYear("1983-84", false),
      generateTaxYear("1982-83", true),
      generateTaxYear("1981-82", true),
      generateTaxYear("1980-81", true),
      generateTaxYear("1979-80", true),
      generateTaxYear("1978-79", true),
      generateTaxYear("1977-78", true),
      generateTaxYear("1976-77", true),
      generateTaxYear("1975-76", true)
    ),
    reducedRateElection = false
  )

  private val dummyRecordSingle: NationalInsuranceRecord = NationalInsuranceRecord(
    // scalastyle:off magic.number
    qualifyingYears = 1,
    qualifyingYearsPriorTo1975 = 0,
    numberOfGaps = 0,
    numberOfGapsPayable = 0,
    dateOfEntry = Some(LocalDate.of(2021, 6, 24)),
    homeResponsibilitiesProtection = false,
    earningsIncludedUpTo = LocalDate.of(2022, 4, 5),
    List(
      generateTaxYear("2021-22", true)
    ),
    reducedRateElection = false
  )

  "getSummary" when {

    "the request headers are invalid" must {
      "return status code 406" in {

        val response = nationalInsuranceRecordController.getSummary(nino)(emptyRequest)
        status(response) shouldBe 406

        contentAsJson(response) shouldBe Json.parse("""{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}""")
      }
    }

    "there is a dead exclusion" must {
      val deadExclusion = NationalInsuranceRecordResult(Left(ExclusionResponse(List(Exclusion.Dead))))

      "return status 403" in {
        when(mockNationalInsuranceRecordService.getNationalInsuranceRecord(any())(any())).
          thenReturn(Future.successful(Right(deadExclusion)))
        val response = nationalInsuranceRecordController.getSummary(nino)(emptyRequestWithHeader)
        status(response) shouldBe 403
      }

      "return the dead message" in {
        when(mockNationalInsuranceRecordService.getNationalInsuranceRecord(any())(any())).
          thenReturn(Future.successful(Right(deadExclusion)))
        val response = nationalInsuranceRecordController.getSummary(nino)(emptyRequestWithHeader)
        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_DEAD","message": "The customer needs to contact the National Insurance helpline"}"""
        )
      }

    }

    "there is a manual correspondence exclusion" must {
      val manualCorrespondenceExclusion = NationalInsuranceRecordResult(Left(ExclusionResponse(List(Exclusion.ManualCorrespondenceIndicator))))

      "return status 403" in {
        when(mockNationalInsuranceRecordService.getNationalInsuranceRecord(any())(any())).
          thenReturn(Future.successful(Right(manualCorrespondenceExclusion)))
        val response = nationalInsuranceRecordController.getSummary(nino)(emptyRequestWithHeader)
        status(response) shouldBe 403

      }

      "return the mci message" in {
        when(mockNationalInsuranceRecordService.getNationalInsuranceRecord(any())(any()))
          .thenReturn(Future.successful(Right(manualCorrespondenceExclusion)))
        val response = nationalInsuranceRecordController.getSummary(nino)(emptyRequestWithHeader)

        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_MANUAL_CORRESPONDENCE","message": "The customer cannot access the service, they should contact HMRC"}"""
        )
      }

    }

    "there is an Isle of Man exclusion" must {
      val isleOfManExclusion = NationalInsuranceRecordResult(Left(ExclusionResponse(List(Exclusion.IsleOfMan))))

      "return status 403" in {
        when(mockNationalInsuranceRecordService.getNationalInsuranceRecord(any())(any())).
          thenReturn(Future.successful(Right(isleOfManExclusion)))
        val response = nationalInsuranceRecordController.getSummary(nino)(emptyRequestWithHeader)
        status(response) shouldBe 403
      }

      "return the IoM message" in {
        when(mockNationalInsuranceRecordService.getNationalInsuranceRecord(any())(any())).
          thenReturn(Future.successful(Right(isleOfManExclusion)))
        val response = nationalInsuranceRecordController.getSummary(nino)(emptyRequestWithHeader)

        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_ISLE_OF_MAN","message": "The customer needs to contact the National Insurance helpline"}"""
        )
      }
    }

    "there is a cope exclusion" must {

      val message = """{ "message": "COPE_FAILURE" }"""
      val copeResponse = HttpResponse(403, message)

      "return status 403" in {

        mockStatePensionController(Some(copeResponse))
        val response = nationalInsuranceRecordController.getSummary(nino)(emptyRequestWithHeader)

        status(response) shouldBe FORBIDDEN
      }

      "return message from state pension" in {

        mockStatePensionController(Some(copeResponse))
        val response = nationalInsuranceRecordController.getSummary(nino)(emptyRequestWithHeader)

        contentAsJson(response) shouldBe Json.parse(message)
      }
    }

    "there is a list of dead, MCI, IoM exclusions" must {
      val exclusions = NationalInsuranceRecordResult(Left(ExclusionResponse(List(
        Exclusion.IsleOfMan,
        Exclusion.ManualCorrespondenceIndicator,
        Exclusion.Dead
      ))))

      "return status 403" in {
        when(mockNationalInsuranceRecordService.getNationalInsuranceRecord(any())(any())).thenReturn(Future.successful(Right(exclusions)))
        val response = nationalInsuranceRecordController.getSummary(nino)(emptyRequestWithHeader)
        status(response) shouldBe 403
      }

      "return the dead message" in {
        when(mockNationalInsuranceRecordService.getNationalInsuranceRecord(any())(any())).thenReturn(Future.successful(Right(exclusions)))
        val response = nationalInsuranceRecordController.getSummary(nino)(emptyRequestWithHeader)

        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_DEAD","message": "The customer needs to contact the National Insurance helpline"}"""
        )
      }
    }

    "there is a list of MCI, IoM exclusions" must {
      val exclusions = NationalInsuranceRecordResult(Left(ExclusionResponse(List(
        Exclusion.IsleOfMan,
        Exclusion.ManualCorrespondenceIndicator
      ))))

      "return status 403" in {
        when(mockNationalInsuranceRecordService.getNationalInsuranceRecord(any())(any())).thenReturn(Future.successful(Right(exclusions)))
        val response = nationalInsuranceRecordController.getSummary(nino)(emptyRequestWithHeader)
        status(response) shouldBe 403
      }

      "return the mci message" in {

        when(mockNationalInsuranceRecordService.getNationalInsuranceRecord(any())(any())).thenReturn(Future.successful(Right(exclusions)))
        val response = nationalInsuranceRecordController.getSummary(nino)(emptyRequestWithHeader)

        contentAsJson(response) shouldBe Json.parse(
          """{"code":"EXCLUSION_MANUAL_CORRESPONDENCE","message": "The customer cannot access the service, they should contact HMRC"}"""
        )
      }
    }

    "there is a valid National Insurance Record with one item" must {

      when(mockNationalInsuranceRecordService.getNationalInsuranceRecord(any())(any())).
        thenReturn(Future.successful(Right(NationalInsuranceRecordResult(Right(dummyRecordSingle)))))
      val response = nationalInsuranceRecordController.getSummary(nino)(emptyRequestWithHeader)
      val json = contentAsJson(response)

      "return an array of taxYears" in {
        (json \ "_embedded").get shouldBe Json.parse(s"""{"taxYears":[{"_links":{"self":{"href":"/national-insurance-record/$linkPath/$nino/taxyear/2021-22"}},"taxYear":"2021-22","qualifying":true,"classOneContributions":1149.98,"classTwoCredits":0,"classThreeCredits":0,"otherCredits":0,"classThreePayable":0,"classThreePayableBy":null,"classThreePayableByPenalty":null,"payable":false,"underInvestigation":false}]}""")
      }
    }


    "there is a valid National Insurance Record" must {

      when(mockNationalInsuranceRecordService.getNationalInsuranceRecord(any())(any()))
        .thenReturn(Future.successful(Right(NationalInsuranceRecordResult(Right(dummyRecord)))))
      val response = nationalInsuranceRecordController.getSummary(nino)(emptyRequestWithHeader)
      val json = contentAsJson(response)

      "return 200" in {
        status(response) shouldBe 200
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
        (json \ "dateOfEntry").as[LocalDate] shouldBe LocalDate.of(1969, 8, 1)
      }

      "have a Boolean called homeResponsibilitiesProtection which is false" in {
        (json \ "homeResponsibilitiesProtection").as[Boolean] shouldBe false
      }

      "have a Boolean called reducedRateElection which is false" in {
        (json \ "reducedRateElection").as[Boolean] shouldBe false
      }

      "have a LocalDate called earningsIncludedUpTo which is 5/4/2016" in {
        (json \ "earningsIncludedUpTo").as[LocalDate] shouldBe LocalDate.of(2016, 4, 5)
      }

      "have a list of 41 tax years" in {
        (json \ "_embedded" \ "taxYears").as[JsArray].value.length shouldBe 41
      }

      "the first tax year" must {

        "have a string called taxYear which is 2015-16" in {
          ((json \ "_embedded" \ "taxYears") \ 0 \ "taxYear").as[String] shouldBe "2015-16"
        }

        "have a Boolean called qualifying which is true" in {
          ((json \ "_embedded" \ "taxYears") \ 0 \ "qualifying").as[Boolean] shouldBe true
        }

        "have a big decimal called classOneContributions that is 1149.98" in {
          ((json \ "_embedded" \ "taxYears") \ 0 \ "classOneContributions").as[BigDecimal] shouldBe 1149.98
        }

        "have an Int called classTwoCredits that is 0" in {
          ((json \ "_embedded" \ "taxYears") \ 0 \ "classTwoCredits").as[Int] shouldBe 0
        }

        "have an Int called classThreeCredits that is 0" in {
          ((json \ "_embedded" \ "taxYears") \ 0 \ "classThreeCredits").as[Int] shouldBe 0
        }

        "have an Int called otherCredits that is 0" in {
          ((json \ "_embedded" \ "taxYears") \ 0 \ "otherCredits").as[Int] shouldBe 0
        }

        "have an Int called classThreePayable that is 0" in {
          ((json \ "_embedded" \ "taxYears") \ 0 \ "classThreePayable").as[BigDecimal] shouldBe 0
        }

        "have a nullable local date called classThreePayableBy that is null" in {
          ((json \ "_embedded" \ "taxYears") \ 0 \ "classThreePayableBy") shouldBe JsDefined(JsNull)
        }

        "have a nullable local date called classThreePayableByPenalty that is null" in {
          ((json \ "_embedded" \ "taxYears") \ 0 \ "classThreePayableByPenalty") shouldBe JsDefined(JsNull)
        }

        "have a Boolean called payable that is false" in {
          ((json \ "_embedded" \ "taxYears") \ 0 \ "payable").as[Boolean] shouldBe false
        }

        "have a Boolean called underInvestigation that is false" in {
          ((json \ "_embedded" \ "taxYears") \ 0 \ "underInvestigation").as[Boolean] shouldBe false
        }

        "have a link to it's resource" in {
          ((json \ "_embedded" \ "taxYears") \ 0 \ "_links" \ "self" \ "href").as[String] shouldBe s"/national-insurance-record/$linkPath/$nino/taxyear/2015-16"
        }
      }

      "the last tax year" must {
        "have a string called taxYear which is 1975-76" in {
          ((json \ "_embedded" \ "taxYears") \ 40 \ "taxYear").as[String] shouldBe "1975-76"
        }

        "have a Boolean called qualifying which is true" in {
          ((json \ "_embedded" \ "taxYears") \ 40 \ "qualifying").as[Boolean] shouldBe true
        }

        "have a big decimal called classOneContributions that is 1149.98" in {
          ((json \ "_embedded" \ "taxYears") \ 40 \ "classOneContributions").as[BigDecimal] shouldBe 1149.98
        }

        "have an Int called classTwoCredits that is 0" in {
          ((json \ "_embedded" \ "taxYears") \ 40 \ "classTwoCredits").as[Int] shouldBe 0
        }

        "have an Int called classThreeCredits that is 0" in {
          ((json \ "_embedded" \ "taxYears") \ 40 \ "classThreeCredits").as[Int] shouldBe 0
        }

        "have an Int called otherCredits that is 0" in {
          ((json \ "_embedded" \ "taxYears") \ 40 \ "otherCredits").as[Int] shouldBe 0
        }

        "have an Int called classThreePayable that is 0" in {
          ((json \ "_embedded" \ "taxYears") \ 40 \ "classThreePayable").as[BigDecimal] shouldBe 0
        }

        "have a nullable local date called classThreePayableBy that is null" in {
          ((json \ "_embedded" \ "taxYears") \ 40 \ "classThreePayableBy") shouldBe JsDefined(JsNull)
        }

        "have a nullable local date called classThreePayableByPenalty that is null" in {
          ((json \ "_embedded" \ "taxYears") \ 40 \ "classThreePayableByPenalty") shouldBe JsDefined(JsNull)
        }

        "have a Boolean called payable that is false" in {
          ((json \ "_embedded" \ "taxYears") \ 40 \ "payable").as[Boolean] shouldBe false
        }

        "have a Boolean called underInvestigation that is false" in {
          ((json \ "_embedded" \ "taxYears") \ 40 \ "underInvestigation").as[Boolean] shouldBe false
        }

        "have a link to it's resource" in {
          ((json \ "_embedded" \ "taxYears") \ 40 \ "_links" \ "self" \ "href").as[String] shouldBe s"/national-insurance-record/$linkPath/$nino/taxyear/1975-76"
        }
      }
      "a non qualifying year like 2012" must {
        "have a string called taxYear which is 2012-13" in {
          ((json \ "_embedded" \ "taxYears") \ 3 \ "taxYear").as[String] shouldBe "2012-13"
        }

        "have a Boolean called qualifying which is true" in {
          ((json \ "_embedded" \ "taxYears") \ 3 \ "qualifying").as[Boolean] shouldBe false
        }

        "have a big decimal called classOneContributions that is 1149.98" in {
          ((json \ "_embedded" \ "taxYears") \ 3 \ "classOneContributions").as[BigDecimal] shouldBe 0
        }

        "have an Int called classTwoCredits that is 0" in {
          ((json \ "_embedded" \ "taxYears") \ 3 \ "classTwoCredits").as[Int] shouldBe 12
        }

        "have an Int called classThreeCredits that is 0" in {
          ((json \ "_embedded" \ "taxYears") \ 3 \ "classThreeCredits").as[Int] shouldBe 0
        }

        "have an Int called otherCredits that is 0" in {
          ((json \ "_embedded" \ "taxYears") \ 3 \ "otherCredits").as[Int] shouldBe 10
        }

        "have a decimal called classThreePayable that is 0" in {
          ((json \ "_embedded" \ "taxYears") \ 3 \ "classThreePayable").as[BigDecimal] shouldBe 325.14
        }

        "have a nullable local date called classThreePayableBy that is null" in {
          ((json \ "_embedded" \ "taxYears") \ 3 \ "classThreePayableBy") shouldBe JsDefined(JsString("2019-04-05"))
        }

        "have a nullable local date called classThreePayableByPenalty that is null" in {
          ((json \ "_embedded" \ "taxYears") \ 3 \ "classThreePayableByPenalty") shouldBe JsDefined(JsString("2023-04-05"))
        }

        "have a Boolean called payable that is false" in {
          ((json \ "_embedded" \ "taxYears") \ 3 \ "payable").as[Boolean] shouldBe true
        }

        "have a Boolean called underInvestigation that is false" in {
          ((json \ "_embedded" \ "taxYears") \ 3 \ "underInvestigation").as[Boolean] shouldBe false
        }

        "have a link to it's resource" in {
          ((json \ "_embedded" \ "taxYears") \ 3 \ "_links" \ "self" \ "href").as[String] shouldBe s"/national-insurance-record/$linkPath/$nino/taxyear/2012-13"
        }
      }

      "have a link to itself" in {
        ((json \ "_links" \ "self") \ "href").as[String] shouldBe s"/national-insurance-record/$linkPath/$nino"
      }

      "the date of entry is nullable" must {
        "not return the date of entry field" in {

          when(mockNationalInsuranceRecordService.getNationalInsuranceRecord(any())(any()))
            .thenReturn(Future.successful(Right(NationalInsuranceRecordResult(Right(dummyRecord.copy(dateOfEntry = None))))))
          val response = nationalInsuranceRecordController.getSummary(nino)(emptyRequestWithHeader)
          val json = contentAsJson(response)

          (json \ "dateOfEntry") shouldBe an[JsUndefined]
        }
      }
    }
  }

  "getTaxYear" must {


    "the request headers are invalid" must {
      "return status code 406" in {
        val response = nationalInsuranceRecordController.getTaxYear(nino, TaxYear("0000-01"))(emptyRequest)
        status(response) shouldBe 406
        contentAsJson(response) shouldBe Json.parse("""{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}""")
      }
    }
  }

  "there is a dead exclusion" must {
    val deadExclusion = NationalInsuranceTaxYearResult(Left(ExclusionResponse(List(Exclusion.Dead))))

    "return status 403" in {
      when(mockNationalInsuranceRecordService.getTaxYear(any(), any())(any())).thenReturn(Future.successful(Right(deadExclusion)))
      val response = nationalInsuranceRecordController.getTaxYear(nino, TaxYear("0000-01"))(emptyRequestWithHeader)

      status(response) shouldBe 403
    }

    "return the dead message" in {
      when(mockNationalInsuranceRecordService.getTaxYear(any(), any())(any())).thenReturn(Future.successful(Right(deadExclusion)))
      val response = nationalInsuranceRecordController.getTaxYear(nino, TaxYear("0000-01"))(emptyRequestWithHeader)

      contentAsJson(response) shouldBe Json.parse(
        """{"code":"EXCLUSION_DEAD","message": "The customer needs to contact the National Insurance helpline"}"""
      )
    }
  }

  "there is a manual correspondence exclusion" must {
    val manualCorrespondenceExclusion = NationalInsuranceTaxYearResult(Left(ExclusionResponse(List(Exclusion.ManualCorrespondenceIndicator))))

    "return status 403" in {
      when(mockNationalInsuranceRecordService.getTaxYear(any(), any())(any())).
        thenReturn(Future.successful(Right(manualCorrespondenceExclusion)))
      val response = nationalInsuranceRecordController.getTaxYear(nino, TaxYear("0000-01"))(emptyRequestWithHeader)
      status(response) shouldBe 403
    }

    "return the mci message" in {
      when(mockNationalInsuranceRecordService.getTaxYear(any(), any())(any())).
        thenReturn(Future.successful(Right(manualCorrespondenceExclusion)))
      val response = nationalInsuranceRecordController.getTaxYear(nino, TaxYear("0000-01"))(emptyRequestWithHeader)
      contentAsJson(response) shouldBe Json.parse(
        """{"code":"EXCLUSION_MANUAL_CORRESPONDENCE","message": "The customer cannot access the service, they should contact HMRC"}"""
      )
    }

  }

  "there is an Isle of Man exclusion" must {
    val isleOfManExclusion = NationalInsuranceTaxYearResult(Left(ExclusionResponse(List(Exclusion.IsleOfMan))))

    "return status 403" in {
      when(mockNationalInsuranceRecordService.getTaxYear(any(), any())(any())).thenReturn(Future.successful(Right(isleOfManExclusion)))
      val response = nationalInsuranceRecordController.getTaxYear(nino, TaxYear("0000-01"))(emptyRequestWithHeader)
      status(response) shouldBe 403
    }

    "return the IoM message" in {
      when(mockNationalInsuranceRecordService.getTaxYear(any(), any())(any())).thenReturn(Future.successful(Right(isleOfManExclusion)))
      val response = nationalInsuranceRecordController.getTaxYear(nino, TaxYear("0000-01"))(emptyRequestWithHeader)
      contentAsJson(response) shouldBe Json.parse(
        """{"code":"EXCLUSION_ISLE_OF_MAN","message": "The customer needs to contact the National Insurance helpline"}"""
      )
    }
  }

  "there is a list of dead, MCI, IoM exclusions" must {
    val exclusions = NationalInsuranceTaxYearResult(Left(ExclusionResponse(List(
      Exclusion.IsleOfMan,
      Exclusion.ManualCorrespondenceIndicator,
      Exclusion.Dead
    ))))

    "return the dead message" in {

      when(mockNationalInsuranceRecordService.getTaxYear(any(), any())(any())).thenReturn(Future.successful(Right(exclusions)))
      val response = nationalInsuranceRecordController.getTaxYear(nino, TaxYear("0000-01"))(emptyRequestWithHeader)

      status(response) shouldBe 403
      contentAsJson(response) shouldBe Json.parse(
        """{"code":"EXCLUSION_DEAD","message": "The customer needs to contact the National Insurance helpline"}"""
      )
    }
  }

  "there is a list of MCI, IoM exclusions" must {
    val exclusions = NationalInsuranceTaxYearResult(Left(ExclusionResponse(List(
      Exclusion.IsleOfMan,
      Exclusion.ManualCorrespondenceIndicator
    ))))

    "return status 403" in {
      when(mockNationalInsuranceRecordService.getTaxYear(any(), any())(any())).thenReturn(Future.successful(Right(exclusions)))
      val response = nationalInsuranceRecordController.getTaxYear(nino, TaxYear("0000-01"))(emptyRequestWithHeader)
      status(response) shouldBe 403
    }

    "return the mci message" in {
      when(mockNationalInsuranceRecordService.getTaxYear(any(), any())(any())).thenReturn(Future.successful(Right(exclusions)))
      val response = nationalInsuranceRecordController.getTaxYear(nino, TaxYear("0000-01"))(emptyRequestWithHeader)
      contentAsJson(response) shouldBe Json.parse(
        """{"code":"EXCLUSION_MANUAL_CORRESPONDENCE","message": "The customer cannot access the service, they should contact HMRC"}"""
      )
    }
  }


  "there is a valid Qualifying Tax Year" must {
    when(mockNationalInsuranceRecordService.getTaxYear(any(), any())(any()))
      .thenReturn(Future.successful(Right(NationalInsuranceTaxYearResult(Right(dummyTaxYearQualifying)))))
    val response = nationalInsuranceRecordController.getTaxYear(nino, TaxYear(dummyTaxYearQualifying.taxYear))(emptyRequestWithHeader)

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
      ((json \ "_links" \ "self") \ "href").as[String] shouldBe s"/national-insurance-record/$linkPath/$nino/taxyear/2010-11"
    }
  }

  "there is a valid Non-Qualifying Tax Year" must {
    when(mockNationalInsuranceRecordService.getTaxYear(any(), any())(any()))
      .thenReturn(Future.successful(Right(NationalInsuranceTaxYearResult(Right(dummyTaxYearNonQualifying)))))
    val response = nationalInsuranceRecordController.getTaxYear(nino, TaxYear(dummyTaxYearNonQualifying.taxYear))(emptyRequestWithHeader)
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
      (json \ "classThreePayableBy").as[LocalDate] shouldBe LocalDate.of(2019, 4, 5)
    }

    "have a nullable local date called classThreePayableByPenalty that is 5/4/2023" in {
      (json \ "classThreePayableByPenalty").as[LocalDate] shouldBe LocalDate.of(2023, 4, 5)
    }

    "have a Boolean called payable that is true" in {
      (json \ "payable").as[Boolean] shouldBe true
    }

    "have a Boolean called underInvestigation that is false" in {
      (json \ "underInvestigation").as[Boolean] shouldBe false
    }

    "have a link to itself" in {
      ((json \ "_links" \ "self") \ "href").as[String] shouldBe s"/national-insurance-record/$linkPath/$nino/taxyear/2009-10"
    }
  }
}
