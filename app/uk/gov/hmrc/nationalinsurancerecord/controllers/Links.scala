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

package uk.gov.hmrc.nationalinsurancerecord.controllers

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.domain.TaxYear

trait Links {

  val context: String
  def endpointSummaryUrl(nino: Nino): String
  def endpointTaxYearUrl(nino: Nino, taxYear: TaxYear): String

  private def createLink(endpointUrl: String) = if(context.isEmpty) endpointUrl else s"/$context$endpointUrl"

  def nationalInsuranceRecordHref(nino: Nino): String =
      createLink(endpointSummaryUrl(nino))

  def nationalInsuranceTaxYearHref(nino: Nino, taxYear: TaxYear): String =
    createLink(endpointTaxYearUrl(nino,taxYear))
}
