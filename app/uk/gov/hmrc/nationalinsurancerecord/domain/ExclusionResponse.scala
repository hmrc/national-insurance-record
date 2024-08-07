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

package uk.gov.hmrc.nationalinsurancerecord.domain

import play.api.libs.json._

object Exclusion extends Enumeration {
  type Exclusion = Value
  val IsleOfMan = Value
  val Dead = Value
  val ManualCorrespondenceIndicator = Value

  implicit val formats: Format[Exclusion] = new Format[Exclusion] {
    def reads(json: JsValue): JsResult[Exclusion] = JsSuccess(Exclusion.withName(json.as[String]))
    def writes(exclusion: Exclusion): JsValue = JsString(exclusion.toString)
  }
}

case class ExclusionResponse(exclusionReasons: List[Exclusion.Exclusion])

object ExclusionResponse {
  implicit val formats: OFormat[ExclusionResponse] = Json.format[ExclusionResponse]
}
