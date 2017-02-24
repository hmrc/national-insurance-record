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

package uk.gov.hmrc.nationalinsurancerecord.domain


import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec

class TaxYearSpec extends UnitSpec with Matchers {
  // scalastyle:off magic.number

  val validTaxYears = Seq("2014-15", "2013-14", "2016-17", "2019-20", "2099-00")

  val invalidTaxYears = Seq("2014", "201314", "2016-1X", "A2014-15", "2015-17", "2013-18")

  "isValid" should {
    validTaxYears.foreach {
      ty => s"return true for tax year $ty" in {
        TaxYear.isValid(ty) shouldBe true
      }
    }

    invalidTaxYears.foreach {
      ty => s"return false for tax year $ty" in {
        TaxYear.isValid(ty) shouldBe false
      }
    }
  }

  "TaxYear from starTaxYear" should {
    "return TaxYear(2014-15) for 2014" in {
      TaxYear.getTaxYear(2014) shouldBe "2014-15"
    }
    "return TaxYear(2018-19) for 2018" in {
      TaxYear.getTaxYear(2018) shouldBe "2018-19"
    }
    "return TaxYear(1999-00) for 1999" in {
      TaxYear.getTaxYear(1999) shouldBe "1999-00"
    }
  }

  "TaxYear constructor" should {
    validTaxYears.foreach {
      ty => s"create a taxYear for a valid argument '$ty'" in {
        TaxYear("2014-15").taxYear == ty
      }
    }

    invalidTaxYears.foreach {
      ty => s"throw an IllegalArgumentException for an invalid argument '$ty'" in {
        an[IllegalArgumentException] should be thrownBy TaxYear(ty)
      }
    }

    "a valid TaxYear" should {
      "be transformed and startYr should be 2014" in {
        TaxYear("2014-15").startYear shouldBe "2014"
      }
    }

    "a valid TaxYear" should {
      "be transformed and startYear should be 2015" in {
        TaxYear("2015-16").startYear shouldBe "2015"
      }
    }

  }


}
