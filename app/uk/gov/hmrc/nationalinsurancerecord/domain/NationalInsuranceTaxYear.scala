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

package uk.gov.hmrc.nationalinsurancerecord.domain

import play.api.libs.functional.syntax._
import uk.gov.hmrc.nationalinsurancerecord.util.DateFormats._
import play.api.libs.json.{Json, Reads, Writes, _}

import java.time.LocalDate

case class NationalInsuranceTaxYear(
                    taxYear: String,
                    qualifying: Boolean,
                    classOneContributions: BigDecimal,
                    classTwoCredits: Int,
                    classThreeCredits: Int,
                    otherCredits: Int,
                    classThreePayable: BigDecimal,
                    classThreePayableBy: Option[LocalDate],
                    classThreePayableByPenalty: Option[LocalDate],
                    payable: Boolean,
                    underInvestigation: Boolean
                  ){
}

object NationalInsuranceTaxYear {

  implicit val reads: Reads[NationalInsuranceTaxYear] = Json.reads[NationalInsuranceTaxYear]
  implicit val writes: Writes[NationalInsuranceTaxYear] = (
    ( JsPath \ "taxYear").write[String] and
    ( JsPath \ "qualifying").write[Boolean] and
    ( JsPath \ "classOneContributions").write[BigDecimal] and
    ( JsPath \ "classTwoCredits").write[Int] and
    ( JsPath \ "classThreeCredits").write[Int] and
    ( JsPath \ "otherCredits").write[Int] and
    ( JsPath \ "classThreePayable").write[BigDecimal] and
    ( JsPath \ "classThreePayableBy").write[Option[LocalDate]] and
    ( JsPath \ Symbol("classThreePayableByPenalty")).write[Option[LocalDate]] and
    ( JsPath \ "payable").write[Boolean] and
    ( JsPath \ "underInvestigation").write[Boolean]
    )(unlift(NationalInsuranceTaxYear.unapply))
}
