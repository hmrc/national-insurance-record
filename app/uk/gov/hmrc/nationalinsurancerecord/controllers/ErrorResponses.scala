/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.api.controllers.ErrorResponse

object ErrorResponses {
  val CODE_INVALID_NINO = "ERROR_NINO_INVALID"
  val CODE_INVALID_TAXYEAR = "CODE_TAXYEAR_INVALID"
  val CODE_MANUAL_CORRESPONDENCE = "EXCLUSION_MANUAL_CORRESPONDENCE"
  val CODE_DEAD = "EXCLUSION_DEAD"
  val CODE_ISLE_OF_MAN = "EXCLUSION_ISLE_OF_MAN"
  val CODE_MARRIED_WOMENS_REDUCED_RATE = "EXCLUSION_MARRIED_WOMENS_REDUCED_RATE"

  object ErrorNinoInvalid extends ErrorResponse(400, CODE_INVALID_NINO, "The provided NINO is not valid")
  object ErrorTaxYearInvalid extends ErrorResponse(400, CODE_INVALID_TAXYEAR, "The provided TAX YEAR is not valid")
  object ExclusionManualCorrespondence extends ErrorResponse(403, CODE_MANUAL_CORRESPONDENCE, "The customer cannot access the service, they should contact HMRC")
  object ExclusionDead extends ErrorResponse(403, CODE_DEAD, "The customer needs to contact the National Insurance helpline")
  object ExclusionIsleOfMan extends ErrorResponse(403, CODE_ISLE_OF_MAN, "The customer needs to contact the National Insurance helpline")
  object ExclusionMarriedWomenReducedRate extends ErrorResponse(403, CODE_MARRIED_WOMENS_REDUCED_RATE, "The customer needs to contact the National Insurance helpline")
}
