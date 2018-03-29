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

package uk.gov.hmrc.nationalinsurancerecord.domain.des

import org.joda.time.LocalDate
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.nationalinsurancerecord.domain.TaxYear

case class DesNITaxYear(
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
                         otherCredits: List[DesOtherCredits] = List()
                       ) {
  lazy val taxYear: String = TaxYear.getTaxYear(startTaxYear).taxYear
}

object DesNITaxYear {

  val readBigDecimal: JsPath => Reads[BigDecimal] = jsPath => jsPath.readNullable[BigDecimal].map(_.getOrElse(0))

  val readBigDecimalFromString: JsPath => Reads[BigDecimal] = jsPath => jsPath.readNullable[String].map(_.map(BigDecimal(_)).getOrElse(0))
  val writeStringFromBigDecimal: JsPath => OWrites[BigDecimal] = jsPath => jsPath.write[String].contramap[BigDecimal](_.toString)

  val readIntFromString: JsPath => Reads[Int] = jsPath => jsPath.readNullable[String].map(_.map(_.toInt).getOrElse(0))
  val writeStringFromInt: JsPath => OWrites[Int] = jsPath => jsPath.write[String].contramap[Int](_.toString)

  val readBooleanFromInt: JsPath => Reads[Boolean] = jsPath => jsPath.read[Int].map(_.equals(1))

  val readIntWithDefault: JsPath => Reads[Int] = jsPath => jsPath.readNullable[Int].map(_.getOrElse(0))
  val readBigDecimalWithDefault: JsPath => Reads[BigDecimal] = jsPath => jsPath.readNullable[BigDecimal].map(_.getOrElse(0))

  val writeIntFromBoolean: JsPath => OWrites[Boolean] = jsPath => jsPath.write[Int].contramap[Boolean] {
    case true => 1
    case _ => 0
  }

  val reads: Reads[DesNITaxYear] = (
        readIntFromString(__ \ "rattdTaxYear") and
        (__ \ "qualifying").read[Boolean] and
        (__ \ "underInvestigationFlag").read[Boolean] and
        (__ \ "payable").read[Boolean] and
        (__ \ "classThreePayable").read[BigDecimal] and
        (__ \ "classThreePayableBy").readNullable[LocalDate] and
        (__ \ "classThreePayableByPenalty").readNullable[LocalDate] and
          readBigDecimalWithDefault(__ \ "niEarningsEmployed") and
          readIntWithDefault(__ \ "niEarningsSelfEmployed") and
          readIntWithDefault(__ \ "niEarningsVoluntary") and
        (__ \ "otherCredits").read[List[DesOtherCredits]]
    )(DesNITaxYear.apply _)

  val writes: Writes[DesNITaxYear] = (
    writeStringFromInt(__ \ "rattdTaxYear") and
      (__ \ "qualifying").write[Boolean] and
      (__ \ "underInvestigationFlag").write[Boolean] and
      (__ \ "payable").write[Boolean] and
      (__ \ "classThreePayable").write[BigDecimal] and
      (__ \ "classThreePayableBy").writeNullable[LocalDate] and
      (__ \ "classThreePayableByPenalty").writeNullable[LocalDate] and
      (__ \ "niEarningsEmployed").write[BigDecimal] and
      (__ \ "niEarningsSelfEmployed").write[Int] and
      (__ \ "niEarningsVoluntary").write[Int] and
      (__ \ "otherCredits").write[List[DesOtherCredits]]
    )(unlift(DesNITaxYear.unapply))

  implicit val format: Format[DesNITaxYear] = Format(reads, writes)
}
