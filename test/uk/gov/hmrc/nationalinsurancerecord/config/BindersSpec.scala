/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.nationalinsurancerecord.config

import uk.gov.hmrc.nationalinsurancerecord.NationalInsuranceRecordUnitSpec
import uk.gov.hmrc.nationalinsurancerecord.domain.TaxYear

class BindersSpec extends NationalInsuranceRecordUnitSpec {

  "nino.bind" must{

    "return Right with a NINO instance for a valid NINO string" in {
      val nino = generateNino()

      val result = Binders.ninoBinder.bind("nino", nino.nino)
      result shouldEqual Right(nino)
    }

    "return Left for an invalid NINO string" in {
      val nino = "invalid"

      val result = Binders.ninoBinder.bind("nino", nino)
      result shouldEqual Left("ERROR_NINO_INVALID")
    }
  }

  "taxYear.bind" must{

    "return Right with a TaxYear instance for a valid TaxYear string" in {
      val taxYear = "1979-80"

      val result = Binders.taxYearBinder.bind("taxYear", taxYear)
      result shouldEqual Right(TaxYear(taxYear))
    }

    "return Left for an invalid TaxYear string" in {
      val taxYear = "1979"

      val result = Binders.taxYearBinder.bind("taxYear", taxYear)
      result shouldEqual Left("CODE_TAXYEAR_INVALID")
    }

  }

}
