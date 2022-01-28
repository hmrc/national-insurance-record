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

package uk.gov.hmrc.nationalinsurancerecord.domain

import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.domain.{SimpleName, SimpleObjectReads, SimpleObjectWrites, TaxIdentifier}

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

case class TaxYear(taxYear: String) extends TaxIdentifier with SimpleName {
  if(!TaxYear.isValid(taxYear)) throw new IllegalArgumentException

  override def value: String = taxYear
  override val name: String = "taxYear"

  val startYear = taxYear.split("-")(0)
  val endYear = (startYear.toInt + 1).toString
}

object TaxYear {

  implicit val taxYearWrite: Writes[TaxYear] = new SimpleObjectWrites[TaxYear](_.value)
  implicit val taxYearRead: Reads[TaxYear] = new SimpleObjectReads[TaxYear]("taxYear", TaxYear.apply)

  final val TaxYearRegex = """^(\d{4})-(\d{2})$"""

  val matchTaxYear: String => Option[Match] = new Regex(TaxYear.TaxYearRegex, "first", "second") findFirstMatchIn _

  def isValid(taxYearReference: String): Boolean = matchTaxYear(taxYearReference) exists {
    r => (r.group("first").toInt + 1) % 100 == r.group("second").toInt
  }

  def getTaxYear(startYear: Int): TaxYear = TaxYear(startYear.toString.concat("-").concat((startYear.toInt + 1).toString.substring(2)))

}
