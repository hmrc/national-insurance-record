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

package uk.gov.hmrc.nationalinsurancerecord.events

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.domain.Exclusion
import uk.gov.hmrc.play.http.HeaderCarrier

object NationalInsuranceRecordExclusion{
  def apply(nino: Nino, exclusionReasons: List[Exclusion.Exclusion])(implicit hc: HeaderCarrier): NationalInsuranceRecordExclusion =
    new NationalInsuranceRecordExclusion(nino, exclusionReasons)
}

class NationalInsuranceRecordExclusion(nino: Nino, exclusionReasons: List[Exclusion.Exclusion]) (implicit hc: HeaderCarrier)
  extends BusinessEvent("NationalInsuranceRecordExclusion", nino,
    Map(
      "reasons" -> exclusionReasons.map(_.toString).mkString(",")
    )
  )