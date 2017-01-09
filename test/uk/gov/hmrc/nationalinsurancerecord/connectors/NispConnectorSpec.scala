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
import uk.gov.hmrc.nationalinsurancerecord.domain.{Exclusion, ExclusionResponse, NationalInsuranceRecord, TaxYearSummary}
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier, HttpGet, HttpResponse}
import uk.gov.hmrc.nationalinsurancerecord.connectors.NispConnector.JsonValidationException

import scala.concurrent.Future

class NispConnectorSpec extends NationalInsuranceRecordUnitSpec with MockitoSugar with OneAppPerSuite {

  val testNispConnector = new NispConnector {
    override def nispBaseUrl: String = ""
    override val http: HttpGet = mock[HttpGet]
  }

  implicit val dummyHeaderCarrier = HeaderCarrier()

  "NispConnector" should {
    "getSummary" should {
      "return Left(Exclusion) when there is Exclusion JSON" in {
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

      "return Right(StatePension) when there is StatePensionJson" in {
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
           |  "taxYears": [
           |    {
           |      "taxYear": "1975-76",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1976-77",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1977-78",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1978-79",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1979-80",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1980-81",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1981-82",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1982-83",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1983-84",
           |      "qualifying": false
           |    },
           |    {
           |      "taxYear": "1984-85",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1985-86",
           |      "qualifying": false
           |    },
           |    {
           |      "taxYear": "1986-87",
           |      "qualifying": false
           |    },
           |    {
           |      "taxYear": "1987-88",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1988-89",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1989-90",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1990-91",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1991-92",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1992-93",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1993-94",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1994-95",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1995-96",
           |      "qualifying": false
           |    },
           |    {
           |      "taxYear": "1996-97",
           |      "qualifying": false
           |    },
           |    {
           |      "taxYear": "1997-98",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1998-99",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "1999-00",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "2000-01",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "2001-02",
           |      "qualifying": false
           |    },
           |    {
           |      "taxYear": "2002-03",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "2003-04",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "2004-05",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "2005-06",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "2006-07",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "2007-08",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "2008-09",
           |      "qualifying": false
           |    },
           |    {
           |      "taxYear": "2009-10",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "2010-11",
           |      "qualifying": false
           |    },
           |    {
           |      "taxYear": "2011-12",
           |      "qualifying": false
           |    },
           |    {
           |      "taxYear": "2012-13",
           |      "qualifying": false
           |    },
           |    {
           |      "taxYear": "2013-14",
           |      "qualifying": true
           |    },
           |    {
           |      "taxYear": "2014-15",
           |      "qualifying": true
           |    }
           |  ]
           |}
            """.stripMargin
          ))
        )))

        def generateTaxYear(year: String, qualifying: Boolean): TaxYearSummary = TaxYearSummary(year, qualifying)

        val responseF = testNispConnector.getSummary(generateNino())
        await(testNispConnector.getSummary(generateNino())) shouldBe Right(NationalInsuranceRecord(
          qualifyingYears = 36,
          qualifyingYearsPriorTo1975 = 5,
          numberOfGaps = 10,
          numberOfGapsPayable = 4,
          dateOfEntry = new LocalDate(1969, 8, 1),
          homeResponsibilitiesProtection = false,
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

      "return a failed future when it cannot parse to Either an exclusion or statement and report validation errors" in {
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

      "return a failed future when there is an http error and pass on the exception" in {
        when(testNispConnector.http.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new BadRequestException("You want me to do what?")))
        ScalaFutures.whenReady(testNispConnector.getSummary(generateNino()).failed) { ex =>
          ex shouldBe a[BadRequestException]
        }
      }
    }
  }
}

