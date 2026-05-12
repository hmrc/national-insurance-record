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

package uk.gov.hmrc.nationalinsurancerecord.services

import java.time.{LocalDate, LocalDateTime, ZoneId}

trait TaxYearResolver {

  lazy val now: () => LocalDateTime

  def currentTaxYear: Int = taxYearFor(now().toLocalDate)

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