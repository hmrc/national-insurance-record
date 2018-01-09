/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.nationalinsurancerecord.domain.nps

import org.joda.time.LocalDate
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.nationalinsurancerecord.domain.TaxYear

case class NpsNITaxYear(
                         startTaxYear: Int = 0,
                         qualifying: Boolean = false,
                         underInvestigation: Boolean = false,
                         payable: Boolean = false,
                         classThreePayable: BigDecimal = 0,
                         classThreePayableBy: Option[LocalDate] = None,
                         classThreePayableByPenalty: Option[LocalDate] = None,
                         classOneContribution: BigDecimal = 0,
                         classTwoCredits: Int = 0,
                         classThreeCredits: Int = 0,
                         otherCredits: List[NpsOtherCredits] = List()
                       ) {
  lazy val taxYear: String = TaxYear.getTaxYear(startTaxYear).taxYear
}

object NpsNITaxYear {

  val readBigDecimal: JsPath => Reads[BigDecimal] = jsPath => jsPath.readNullable[BigDecimal].map(_.getOrElse(0))

  val readBigDecimalFromString: JsPath => Reads[BigDecimal] = jsPath => jsPath.readNullable[String].map(_.map(BigDecimal(_)).getOrElse(0))
  val writeStringFromBigDecimal: JsPath => OWrites[BigDecimal] = jsPath => jsPath.write[String].contramap[BigDecimal](_.toString)

  val readIntFromString: JsPath => Reads[Int] = jsPath => jsPath.readNullable[String].map(_.map(_.toInt).getOrElse(0))
  val writeStringFromInt: JsPath => OWrites[Int] = jsPath => jsPath.write[String].contramap[Int](_.toString)

  val readBooleanFromInt: JsPath => Reads[Boolean] = jsPath => jsPath.read[Int].map(_.equals(1))
  val writeIntFromBoolean: JsPath => OWrites[Boolean] = jsPath => jsPath.write[Int].contramap[Boolean] {
    case true => 1
    case _ => 0
  }

  val reads: Reads[NpsNITaxYear] = (
        (__ \ "rattd_tax_year").read[Int] and
        readBooleanFromInt(__ \ "qualifying") and
        readBooleanFromInt(__ \ "under_investigation_flag") and
        readBooleanFromInt(__ \ "payable") and
        readBigDecimal(__ \ "class_three_payable") and
        (__ \ "class_three_payable_by").readNullable[LocalDate] and
        (__ \ "class_three_payable_by_penalty").readNullable[LocalDate] and
        readBigDecimalFromString(__ \ "ni_earnings_employed") and
        readIntFromString(__ \ "ni_earnings_self_employed") and
        readIntFromString(__ \ "ni_earnings_voluntary") and
        (__ \ "npsLothcred").read[List[NpsOtherCredits]]
    )(NpsNITaxYear.apply _)

  val writes: Writes[NpsNITaxYear] = (
      (__ \ "rattd_tax_year").write[Int] and
      writeIntFromBoolean(__ \ "qualifying") and
      writeIntFromBoolean(__ \ "under_investigation_flag") and
      writeIntFromBoolean(__ \ "payable") and
      (__ \ "class_three_payable").write[BigDecimal] and
      (__ \ "class_three_payable_by").writeNullable[LocalDate] and
      (__ \ "class_three_payable_by_penalty").writeNullable[LocalDate] and
        writeStringFromBigDecimal(__ \ "ni_earnings_employed") and
      writeStringFromInt(__ \ "ni_earnings_self_employed") and
      writeStringFromInt(__ \ "ni_earnings_voluntary") and
      (__ \ "npsLothcred").write[List[NpsOtherCredits]]
    )(unlift(NpsNITaxYear.unapply))

  implicit val format: Format[NpsNITaxYear] = Format(reads, writes)
}
