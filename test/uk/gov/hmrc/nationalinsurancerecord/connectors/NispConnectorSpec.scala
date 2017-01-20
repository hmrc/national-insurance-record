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

import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.Json
import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.domain._
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier, HttpGet, HttpResponse}
import uk.gov.hmrc.nationalinsurancerecord.connectors.NispConnector.JsonValidationException

import scala.concurrent.Future

class NispConnectorSpec extends NationalInsuranceRecordUnitSpec with MockitoSugar with OneAppPerSuite {

  val testNispConnector = new NispConnector {
    override def nispBaseUrl: String = ""

    override val http: HttpGet = mock[HttpGet]
  }

  implicit val dummyHeaderCarrier = HeaderCarrier()

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


  private def generateTaxYear(taxYear: String, qualifying: Boolean): NationalInsuranceTaxYear = {
    if (qualifying) {
      dummyTaxYearQualifying.copy(taxYear = taxYear)
    } else {
      dummyTaxYearNonQualifying.copy(taxYear = taxYear)
    }
  }

  "getSummary" when {
    "there is exclusion JSON" should {
      "return Left(Exclusion)" in {
        when(testNispConnector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(
          200,
          Some(Json.parse(
            """
              |{
              |  "exclusionReasons": [
              |    "Dead"
              |  ]
              |}
            """.stripMargin
          ))
        )))

        await(testNispConnector.getSummary(generateNino())) shouldBe Left(ExclusionResponse(
          List(
            Exclusion.Dead
          )
        ))
      }
    }


    "when there is valid json" should {

      "return Right(NationalInsuranceRecord)" in {
        when(testNispConnector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(
          200,
          Some(Json.parse(
            """
            {
           |  "qualifyingYears": 36,
           |  "qualifyingYearsPriorTo1975": 5,
           |  "numberOfGaps": 10,
           |  "numberOfGapsPayable": 4,
           |  "dateOfEntry": "1969-08-01",
           |  "homeResponsibilitiesProtection": false,
           |  "earningsIncludedUpTo": "2015-04-05",
           |  "taxYears": [
           |    {
           |      "taxYear": "1975-76",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1976-77",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1977-78",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1978-79",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1979-80",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1980-81",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1981-82",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1982-83",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1983-84",
           |      "qualifying": false,
           |      "classOneContributions": 0,
           |      "classTwoCredits": 12,
           |      "classThreeCredits": 0,
           |      "otherCredits": 10,
           |      "classThreePayable": 325.14,
           |      "classThreePayableBy": "2019-04-05",
           |      "classThreePayableByPenalty": "2023-04-05",
           |      "payable": true,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1984-85",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1985-86",
           |      "qualifying": false,
           |      "classOneContributions": 0,
           |      "classTwoCredits": 12,
           |      "classThreeCredits": 0,
           |      "otherCredits": 10,
           |      "classThreePayable": 325.14,
           |      "classThreePayableBy": "2019-04-05",
           |      "classThreePayableByPenalty": "2023-04-05",
           |      "payable": true,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1986-87",
           |      "qualifying": false,
           |      "classOneContributions": 0,
           |      "classTwoCredits": 12,
           |      "classThreeCredits": 0,
           |      "otherCredits": 10,
           |      "classThreePayable": 325.14,
           |      "classThreePayableBy": "2019-04-05",
           |      "classThreePayableByPenalty": "2023-04-05",
           |      "payable": true,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1987-88",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1988-89",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1989-90",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1990-91",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1991-92",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1992-93",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1993-94",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1994-95",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1995-96",
           |      "qualifying": false,
           |      "classOneContributions": 0,
           |      "classTwoCredits": 12,
           |      "classThreeCredits": 0,
           |      "otherCredits": 10,
           |      "classThreePayable": 325.14,
           |      "classThreePayableBy": "2019-04-05",
           |      "classThreePayableByPenalty": "2023-04-05",
           |      "payable": true,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1996-97",
           |      "qualifying": false,
           |      "classOneContributions": 0,
           |      "classTwoCredits": 12,
           |      "classThreeCredits": 0,
           |      "otherCredits": 10,
           |      "classThreePayable": 325.14,
           |      "classThreePayableBy": "2019-04-05",
           |      "classThreePayableByPenalty": "2023-04-05",
           |      "payable": true,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1997-98",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1998-99",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "1999-00",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "2000-01",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "2001-02",
           |      "qualifying": false,
           |      "classOneContributions": 0,
           |      "classTwoCredits": 12,
           |      "classThreeCredits": 0,
           |      "otherCredits": 10,
           |      "classThreePayable": 325.14,
           |      "classThreePayableBy": "2019-04-05",
           |      "classThreePayableByPenalty": "2023-04-05",
           |      "payable": true,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "2002-03",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "2003-04",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "2004-05",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "2005-06",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "2006-07",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "2007-08",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "2008-09",
           |      "qualifying": false,
           |      "classOneContributions": 0,
           |      "classTwoCredits": 12,
           |      "classThreeCredits": 0,
           |      "otherCredits": 10,
           |      "classThreePayable": 325.14,
           |      "classThreePayableBy": "2019-04-05",
           |      "classThreePayableByPenalty": "2023-04-05",
           |      "payable": true,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "2009-10",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "2010-11",
           |      "qualifying": false,
           |      "classOneContributions": 0,
           |      "classTwoCredits": 12,
           |      "classThreeCredits": 0,
           |      "otherCredits": 10,
           |      "classThreePayable": 325.14,
           |      "classThreePayableBy": "2019-04-05",
           |      "classThreePayableByPenalty": "2023-04-05",
           |      "payable": true,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "2011-12",
           |      "qualifying": false,
           |      "classOneContributions": 0,
           |      "classTwoCredits": 12,
           |      "classThreeCredits": 0,
           |      "otherCredits": 10,
           |      "classThreePayable": 325.14,
           |      "classThreePayableBy": "2019-04-05",
           |      "classThreePayableByPenalty": "2023-04-05",
           |      "payable": true,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "2012-13",
           |      "qualifying": false,
           |      "classOneContributions": 0,
           |      "classTwoCredits": 12,
           |      "classThreeCredits": 0,
           |      "otherCredits": 10,
           |      "classThreePayable": 325.14,
           |      "classThreePayableBy": "2019-04-05",
           |      "classThreePayableByPenalty": "2023-04-05",
           |      "payable": true,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "2013-14",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    },
           |    {
           |      "taxYear": "2014-15",
           |      "qualifying": true,
           |      "classOneContributions": 1149.98,
           |      "classTwoCredits": 0,
           |      "classThreeCredits": 0,
           |      "otherCredits": 0,
           |      "classThreePayable": 0,
           |      "classThreePayableBy": null,
           |      "classThreePayableByPenalty": null,
           |      "payable": false,
           |      "underInvestigation": false
           |    }
           |  ]
           |}
           |""".stripMargin
          ))
        )))

        val responseF = testNispConnector.getSummary(generateNino())
        await(testNispConnector.getSummary(generateNino())) shouldBe Right(NationalInsuranceRecord(
          qualifyingYears = 36,
          qualifyingYearsPriorTo1975 = 5,
          numberOfGaps = 10,
          numberOfGapsPayable = 4,
          dateOfEntry = new LocalDate(1969, 8, 1),
          homeResponsibilitiesProtection = false,
          earningsIncludedUpTo = new LocalDate(2015, 4, 5),
          taxYears = List(
            generateTaxYear("1975-76", true),
            generateTaxYear("1976-77", true),
            generateTaxYear("1977-78", true),
            generateTaxYear("1978-79", true),
            generateTaxYear("1979-80", true),
            generateTaxYear("1980-81", true),
            generateTaxYear("1981-82", true),
            generateTaxYear("1982-83", true),
            generateTaxYear("1983-84", false),
            generateTaxYear("1984-85", true),
            generateTaxYear("1985-86", false),
            generateTaxYear("1986-87", false),
            generateTaxYear("1987-88", true),
            generateTaxYear("1988-89", true),
            generateTaxYear("1989-90", true),
            generateTaxYear("1990-91", true),
            generateTaxYear("1991-92", true),
            generateTaxYear("1992-93", true),
            generateTaxYear("1993-94", true),
            generateTaxYear("1994-95", true),
            generateTaxYear("1995-96", false),
            generateTaxYear("1996-97", false),
            generateTaxYear("1997-98", true),
            generateTaxYear("1998-99", true),
            generateTaxYear("1999-00", true),
            generateTaxYear("2000-01", true),
            generateTaxYear("2001-02", false),
            generateTaxYear("2002-03", true),
            generateTaxYear("2003-04", true),
            generateTaxYear("2004-05", true),
            generateTaxYear("2005-06", true),
            generateTaxYear("2006-07", true),
            generateTaxYear("2007-08", true),
            generateTaxYear("2008-09", false),
            generateTaxYear("2009-10", true),
            generateTaxYear("2010-11", false),
            generateTaxYear("2011-12", false),
            generateTaxYear("2012-13", false),
            generateTaxYear("2013-14", true),
            generateTaxYear("2014-15", true)
          )
        ))
      }

    }

    "when it cannot parse to Either an exclusion or ni record" should {

      "fail and report validation errors" in {
        when(testNispConnector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(
          200,
          Some(Json.parse(
            """
              |{
              |  "earningsIncludedUpTo": "2015-04-05",
              |  "amounts": {
              |    "protectedPayment": false,
              |    "iamcorruptbutvalid": true
              |   }
              |}
            """.stripMargin
          ))
        )))

        ScalaFutures.whenReady(testNispConnector.getSummary(generateNino()).failed) { ex =>
          ex shouldBe a[JsonValidationException]
          ex.getMessage.contains("JSON Validation Error:") shouldBe true
        }
      }
    }

    "when there is an http error" should {
      "return the failed future and pass the http exception" in {
        when(testNispConnector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new BadRequestException("You want me to do what?")))
        ScalaFutures.whenReady(testNispConnector.getSummary(generateNino()).failed) { ex =>
          ex shouldBe a[BadRequestException]
        }
      }
    }
  }

  "getTaxYear" when {
    "there is exclusion JSON" should {
      "return Left(Exclusion)" in {
        when(testNispConnector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(
          200,
          Some(Json.parse(
            """
              |{
              |  "exclusionReasons": [
              |    "Dead"
              |  ]
              |}
            """.stripMargin
          ))
        )))

        await(testNispConnector.getTaxYear(generateNino(), TaxYear("1999-00"))) shouldBe Left(ExclusionResponse(
          List(
            Exclusion.Dead
          )
        ))
      }
    }


    "there is valid json" should {

      "return Right(NationalInsuranceRecordTaxYear)" in {
        when(testNispConnector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(
          200,
          Some(Json.parse(
            """
            {
 |            "taxYear": "2010-11",
 |            "qualifying": false,
 |            "classOneContributions": 1149.98,
 |            "classTwoCredits": 0,
 |            "classThreeCredits": 0,
 |            "otherCredits": 0,
 |            "classThreePayable": 0,
 |            "classThreePayableBy": null,
 |            "classThreePayableByPenalty": "2023-04-05",
 |            "payable": false,
 |            "underInvestigation": false
            }
            """.stripMargin
          ))
        )))

        val responseF = testNispConnector.getSummary(generateNino())
        await(testNispConnector.getTaxYear(generateNino(), TaxYear("1999-00"))) shouldBe Right(NationalInsuranceTaxYear(
          taxYear = "2010-11",
          qualifying = false,
          classOneContributions = 1149.98,
          classTwoCredits = 0,
          classThreeCredits = 0,
          otherCredits = 0,
          classThreePayable = 0,
          classThreePayableBy = None,
          classThreePayableByPenalty = Some(new LocalDate(2023, 4, 5)),
          payable = false,
          underInvestigation = false
        ))
      }

    }

    "it cannot parse to Either an exclusion or ni record" should {

      "fail and report validation errors" in {
        when(testNispConnector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(
          200,
          Some(Json.parse(
            """
              |{
              | "taxYear": "2010-11",
              | "qualifying": false,
              | "classOneContributionsYO": 1149.98
              |}
            """.stripMargin
          ))
        )))

        ScalaFutures.whenReady(testNispConnector.getTaxYear(generateNino(), TaxYear("1999-00")).failed) { ex =>
          ex shouldBe a[JsonValidationException]
          ex.getMessage.contains("JSON Validation Error:") shouldBe true
        }
      }
    }

    "when there is an http error" should {
      "return the failed future and pass the http exception" in {
        when(testNispConnector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new BadRequestException("You want me to do what?")))
        ScalaFutures.whenReady(testNispConnector.getTaxYear(generateNino(), TaxYear("1999-00")).failed) { ex =>
          ex shouldBe a[BadRequestException]
        }
      }
    }
  }

}

