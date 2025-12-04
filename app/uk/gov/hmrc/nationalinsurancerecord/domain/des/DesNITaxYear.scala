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

package uk.gov.hmrc.nationalinsurancerecord.domain.des

import play.api.libs.functional.syntax._
import uk.gov.hmrc.nationalinsurancerecord.util.DateFormats.localDateFormat
import play.api.libs.json._
import uk.gov.hmrc.nationalinsurancerecord.domain.TaxYear

import java.time.LocalDate

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
  val readIntFromString: JsPath => Reads[Int] =
    jsPath =>
      jsPath.readNullable[String].map(_.map(_.toInt).getOrElse(0))

  val writeStringFromInt: JsPath => OWrites[Int] =
    jsPath =>
      jsPath.write[String].contramap[Int](_.toString)

  val readIntWithDefault: JsPath => Reads[Int] =
    jsPath =>
      jsPath.readNullable[Int].map(_.getOrElse(0))
  val readBigDecimalWithDefault: JsPath => Reads[BigDecimal] =
    jsPath =>
      jsPath.readNullable[BigDecimal].map(_.getOrElse(0))

  val readBooleanWithDefault: JsPath => Reads[Boolean] =
    jsPath =>
      jsPath.readNullable[Boolean].map(_.getOrElse(false))

  val readNullableList:JsPath => Reads[List[DesOtherCredits]] =
    jsPath =>
      jsPath.readNullable[List[DesOtherCredits]].map(_.getOrElse(List.empty))

  val reads: Reads[DesNITaxYear] = (
        readIntFromString(__ \ "rattdTaxYear") and
        readBooleanWithDefault(__ \ "qualifying") and
        readBooleanWithDefault(__ \ "underInvestigationFlag") and
        readBooleanWithDefault(__ \ "payable") and
        readBigDecimalWithDefault(__ \ "classThreePayable") and
        (__ \ "classThreePayableBy").readNullable[LocalDate]                                                                                                                                                               and
        (__ \ "classThreePayableByPenalty").readNullable[LocalDate] and
        readBigDecimalWithDefault(__ \ "niEarningsEmployed") and
        readIntWithDefault(__ \ "niEarningsSelfEmployed") and
        readIntWithDefault(__ \ "niEarningsVoluntary") and
        readNullableList(__ \ "otherCredits")
    )(DesNITaxYear.apply)

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
    )(o => Tuple.fromProductTyped(o))

  implicit val format: Format[DesNITaxYear] = Format(reads, writes)
}
