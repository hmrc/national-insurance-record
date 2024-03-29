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

package uk.gov.hmrc.nationalinsurancerecord

import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nationalinsurancerecord.util.UnitSpec

import scala.util.Random

trait NationalInsuranceRecordUnitSpec extends UnitSpec {
  private val ninoGenerator = new Generator(new Random())
  def generateNino(): Nino = ninoGenerator.nextNino
  def generateNinoWithPrefix(prefix: String): Nino = {
    require(prefix.length == 2)
    Nino(ninoGenerator.nextNino.toString().replaceFirst("[A-Z]{2}", prefix))
  }
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
}
