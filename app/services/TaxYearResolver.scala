/*
 * Copyright 2019 HM Revenue & Customs
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

/*
 * Copyright 2019 HM Revenue & Customs
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

import uk.gov.hmrc.time.DateTimeUtils

trait TaxYearResolver {

  import org.joda.time.{Interval, LocalDate, DateTimeZone, DateTime}

  lazy val now: () => DateTime = ???

  private val ukTime : DateTimeZone = DateTimeZone.forID("Europe/London")

  def taxYearFor(dateToResolve: LocalDate): Int = {
    val year = dateToResolve.year.get

    if (dateToResolve.isBefore(new LocalDate(year, 4, 6)))
      year - 1
    else
      year
  }

  def fallsInThisTaxYear(currentDate: LocalDate): Boolean = {
    val earliestDateForCurrentTaxYear = new LocalDate(taxYearFor(now().toLocalDate), 4, 6)
    earliestDateForCurrentTaxYear.isBefore(currentDate) || earliestDateForCurrentTaxYear.isEqual(currentDate)
  }

  def currentTaxYear: Int = taxYearFor(new LocalDate(now(), ukTime))

  def currentTaxYearYearsRange = currentTaxYear to currentTaxYear + 1

  def startOfTaxYear(year: Int) = new LocalDate(year, 4, 6)

  def endOfTaxYear(year: Int) = new LocalDate(year + 1, 4, 5)

  def endOfLastTaxYear = endOfTaxYear(currentTaxYear - 1)

  def startOfCurrentTaxYear = startOfTaxYear(currentTaxYear)

  def endOfCurrentTaxYear = endOfTaxYear(currentTaxYear)

  def startOfNextTaxYear = startOfTaxYear(currentTaxYear + 1)

  def taxYearInterval = new Interval(startOfCurrentTaxYear.toDateTimeAtStartOfDay(ukTime),
    startOfNextTaxYear.toDateTimeAtStartOfDay(ukTime))
}

object TaxYearResolver extends TaxYearResolver {
  override lazy val now = () => DateTimeUtils.now
}