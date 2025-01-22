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

package services

import java.time.{LocalDate, LocalDateTime, ZoneId}

trait TaxYearResolver {

  lazy val now: () => LocalDateTime = ???

  val earliestDateForCurrentTaxYear = LocalDate.of(taxYearFor(now().toLocalDate), 4, 6)

  def currentTaxYear: Int = taxYearFor(now().toLocalDate)

  def endOfCurrentTaxYear: LocalDate = endOfTaxYear(currentTaxYear)

  def endOfLastTaxYear: LocalDate = endOfTaxYear(currentTaxYear - 1)

  def endOfTaxYear(year: Int): LocalDate = LocalDate.of(year + 1, 4, 5)

  def fallsInThisTaxYear(currentDate: LocalDate): Boolean = {
    earliestDateForCurrentTaxYear.isBefore(currentDate) || earliestDateForCurrentTaxYear.isEqual(currentDate)
  }

  def startOfCurrentTaxYear: LocalDate = startOfTaxYear(currentTaxYear)

  def startOfNextTaxYear: LocalDate = startOfTaxYear(currentTaxYear + 1)

  def startOfTaxYear(year: Int): LocalDate = LocalDate.of(year, 4, 6)

  def taxYearFor(dateToResolve: LocalDate): Int = {
    val year = dateToResolve.getYear

    if (dateToResolve.isBefore(LocalDate.of(year, 4, 6)))
      year - 1
    else
      year
  }
}

object TaxYearResolver extends TaxYearResolver {
  private val ukTime : ZoneId = ZoneId.of("Europe/London")

  override lazy val now: () => LocalDateTime = () => LocalDateTime.now(ukTime)
}