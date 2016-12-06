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
import uk.gov.hmrc.nationalinsurancerecord.domain.{NationalInsuranceRecord, NationalInsuranceRecordExclusion, TaxYearSummary}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait NationalInsuranceRecordService {
  def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[NationalInsuranceRecordExclusion, NationalInsuranceRecord]]
}

object SandboxNationalInsuranceService extends NationalInsuranceRecordService {
  private val dummyRecord: NationalInsuranceRecord = NationalInsuranceRecord(
    // scalastyle:off magic.number
    qualifyingYears = 36,
    qualifyingYearsPriorTo1975 = 5,
    numberOfGaps = 10,
    numberOfGapsPayable = 4,
    dateOfEntry =  new LocalDate(1969, 8, 1),
    homeResponsibilitiesProtection = false,
    List(
      TaxYearSummary("2015-16", true),
      TaxYearSummary("2014-15", true),
      TaxYearSummary("2013-14", true),
      TaxYearSummary("2012-13", false),
      TaxYearSummary("2011-12", false),
      TaxYearSummary("2010-11", false),
      TaxYearSummary("2009-10", true),
      TaxYearSummary("2008-09", false),
      TaxYearSummary("2007-08", true),
      TaxYearSummary("2006-07", true),
      TaxYearSummary("2005-06", true),
      TaxYearSummary("2004-05", true),
      TaxYearSummary("2003-04", true),
      TaxYearSummary("2002-03", true),
      TaxYearSummary("2001-02", false),
      TaxYearSummary("2000-01", true),
      TaxYearSummary("1999-00", true),
      TaxYearSummary("1998-99", true),
      TaxYearSummary("1997-98", true),
      TaxYearSummary("1996-97", false),
      TaxYearSummary("1995-96", false),
      TaxYearSummary("1994-95", true),
      TaxYearSummary("1993-94", true),
      TaxYearSummary("1992-93", true),
      TaxYearSummary("1991-92", true),
      TaxYearSummary("1990-91", true),
      TaxYearSummary("1989-90", true),
      TaxYearSummary("1988-89", true),
      TaxYearSummary("1987-88", true),
      TaxYearSummary("1986-87", false),
      TaxYearSummary("1985-86", false),
      TaxYearSummary("1984-85", true),
      TaxYearSummary("1983-84", false),
      TaxYearSummary("1982-83", true),
      TaxYearSummary("1981-82", true),
      TaxYearSummary("1980-81", true),
      TaxYearSummary("1979-80", true),
      TaxYearSummary("1978-79", true),
      TaxYearSummary("1977-78", true),
      TaxYearSummary("1976-77", true),
      TaxYearSummary("1975-76", true)

    )
  )

  override def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier):
    Future[Either[NationalInsuranceRecordExclusion, NationalInsuranceRecord]] = Future.successful(Right(dummyRecord))
}