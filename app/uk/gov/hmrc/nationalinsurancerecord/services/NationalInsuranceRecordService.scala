/*
 * Copyright 2016 HM Revenue & Customs
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

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.domain.{NationalInsuranceRecord, NationalInsuranceRecordExclusion}
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.concurrent.Future

trait NationalInsuranceRecordService {
  def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[NationalInsuranceRecordExclusion, NationalInsuranceRecord]]
}

object SandboxNationalInsuranceService extends NationalInsuranceRecordService {
  private val dummyRecord: NationalInsuranceRecord = NationalInsuranceRecord(
    // scalastyle:off magic.number
    qualifyingYears = 28,
    qualifyingYearsPriorTo1975 = 2,
    nonQualifyingYears = 10,
    numberOfGaps = 10,
    numberOfGapsPayable = 4,
    dateOfEntry =  new LocalDate(2014, 4, 5),
    homeResponsibilitiesProtection = false
  )

  override def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier):
    Future[Either[NationalInsuranceRecordExclusion, NationalInsuranceRecord]] = Future.successful(Right(dummyRecord))
}