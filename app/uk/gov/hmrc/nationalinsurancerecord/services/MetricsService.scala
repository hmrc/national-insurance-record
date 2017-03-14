/*
 * Copyright 2017 HM Revenue & Customs
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

import com.codahale.metrics.{Counter, Timer}
import com.codahale.metrics.Timer.Context
import uk.gov.hmrc.nationalinsurancerecord.domain.{APITypes, Exclusion}
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.Exclusion.Exclusion
import uk.gov.hmrc.play.graphite.MicroserviceMetrics

trait MetricsService {
  def startTimer(api: APITypes): Timer.Context
  def incrementFailedCounter(api: APITypes): Unit
  def incrementCounter(api: APITypes): Unit
  def niRecord(gaps: Int, payableGaps: Int, pre75Years: Int, qualifyingYears: Int): Unit
  def exclusion(exclusion: Exclusion): Unit
  def cacheRead()
  def cacheReadFound()
  def cacheReadNotFound()
  def cacheWritten()
  def startCitizenDetailsTimer(): Timer.Context
}

object MetricsService extends MetricsService with MicroserviceMetrics{

  val incrementCounters = Map(
    APITypes.Summary -> metrics.defaultRegistry.counter("spsummary-counter"),
    APITypes.NIRecord -> metrics.defaultRegistry.counter("nirecord-counter"),
    APITypes.Liabilities -> metrics.defaultRegistry.counter("liabilities-counter")
  )

  val timers = Map(
    APITypes.Summary -> metrics.defaultRegistry.timer("summary-response-timer"),
    APITypes.NIRecord -> metrics.defaultRegistry.timer("nirecord-response-timer"),
    APITypes.Liabilities -> metrics.defaultRegistry.timer("liabilities-response-timer")
  )

  val failedCounters = Map(
    APITypes.Summary -> metrics.defaultRegistry.counter("summary-failed-counter"),
    APITypes.NIRecord -> metrics.defaultRegistry.counter("nirecord-failed-counter"),
    APITypes.Liabilities -> metrics.defaultRegistry.counter("liabilities-failed-counter")
  )

  override def startTimer(api: APITypes): Context = timers(api).time()
  override def incrementFailedCounter(api: APITypes): Unit = failedCounters(api).inc()
  override def incrementCounter(api: APITypes): Unit = incrementCounters(api).inc()

  val gapsMeter = metrics.defaultRegistry.histogram("gaps")
  val payableGapsMeter = metrics.defaultRegistry.histogram("payable-gaps")
  val pre75YearsMeter = metrics.defaultRegistry.histogram("pre75-years")
  val qualifyingYearsMeter = metrics.defaultRegistry.histogram("qualifying-years")

  override def niRecord(gaps: Int, payableGaps: Int, pre75Years: Int, qualifyingYears: Int): Unit = {
    gapsMeter.update(gaps)
    payableGapsMeter.update(payableGaps)
    pre75YearsMeter.update(pre75Years)
    qualifyingYearsMeter.update(qualifyingYears)
  }

  val exclusionMeters: Map[Exclusion, Counter] = Map(
    Exclusion.MarriedWomenReducedRateElection -> metrics.defaultRegistry.counter("exclusion-mwrre"),
    Exclusion.Dead -> metrics.defaultRegistry.counter("exclusion-dead"),
    Exclusion.IsleOfMan -> metrics.defaultRegistry.counter("exclusion-isle-of-man"),
    Exclusion.ManualCorrespondenceIndicator -> metrics.defaultRegistry.counter("exclusion-manual-correspondence")
  )

  override def exclusion(exclusion: Exclusion): Unit = exclusionMeters(exclusion).inc()

  override def cacheRead(): Unit = metrics.defaultRegistry.meter("cache-read").mark
  override def cacheReadFound(): Unit = metrics.defaultRegistry.meter("cache-read-found").mark
  override def cacheReadNotFound(): Unit = metrics.defaultRegistry.meter("cache-read-not-found").mark
  override def cacheWritten(): Unit = metrics.defaultRegistry.meter("cache-written").mark
  override def startCitizenDetailsTimer(): Context = metrics.defaultRegistry.timer("citizen-details-timer").time()
}
