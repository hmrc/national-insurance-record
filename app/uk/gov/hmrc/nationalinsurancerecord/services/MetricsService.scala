/*
 * Copyright 2021 HM Revenue & Customs
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

import com.codahale.metrics.Counter
import com.codahale.metrics.Timer.Context
import com.google.inject.Inject
import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.Exclusion.Exclusion
import uk.gov.hmrc.nationalinsurancerecord.domain.{APITypes, Exclusion}

class MetricsService @Inject()(metrics: Metrics){

  val registry = metrics.defaultRegistry

  val incrementCounters = Map(
    APITypes.Summary -> registry.counter("spsummary-counter"),
    APITypes.NIRecord -> registry.counter("nirecord-counter"),
    APITypes.Liabilities -> registry.counter("liabilities-counter")
  )

  val timers = Map(
    APITypes.Summary -> registry.timer("summary-response-timer"),
    APITypes.NIRecord -> registry.timer("nirecord-response-timer"),
    APITypes.Liabilities -> registry.timer("liabilities-response-timer")
  )

  val failedCounters = Map(
    APITypes.Summary -> registry.counter("summary-failed-counter"),
    APITypes.NIRecord -> registry.counter("nirecord-failed-counter"),
    APITypes.Liabilities -> registry.counter("liabilities-failed-counter")
  )

  def startTimer(api: APITypes): Context = timers(api).time()
  def incrementFailedCounter(api: APITypes): Unit = failedCounters(api).inc()
  def incrementCounter(api: APITypes): Unit = incrementCounters(api).inc()

  val gapsMeter = registry.histogram("gaps")
  val payableGapsMeter = registry.histogram("payable-gaps")
  val pre75YearsMeter = registry.histogram("pre75-years")
  val qualifyingYearsMeter = registry.histogram("qualifying-years")

  def niRecord(gaps: Int, payableGaps: Int, pre75Years: Int, qualifyingYears: Int): Unit = {
    gapsMeter.update(gaps)
    payableGapsMeter.update(payableGaps)
    pre75YearsMeter.update(pre75Years)
    qualifyingYearsMeter.update(qualifyingYears)
  }

  val exclusionMeters: Map[Exclusion, Counter] = Map(
    Exclusion.Dead -> registry.counter("exclusion-dead"),
    Exclusion.IsleOfMan -> registry.counter("exclusion-isle-of-man"),
    Exclusion.ManualCorrespondenceIndicator -> registry.counter("exclusion-manual-correspondence")
  )

  def exclusion(exclusion: Exclusion): Unit = exclusionMeters(exclusion).inc()

  def cacheRead(): Unit = registry.meter("cache-read").mark
  def cacheReadFound(): Unit = registry.meter("cache-read-found").mark
  def cacheReadNotFound(): Unit = registry.meter("cache-read-not-found").mark
  def cacheWritten(): Unit = registry.meter("cache-written").mark
  def startCitizenDetailsTimer(): Context = registry.timer("citizen-details-timer").time()
}
