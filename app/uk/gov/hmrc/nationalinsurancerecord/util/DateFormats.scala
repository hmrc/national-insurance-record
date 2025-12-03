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

package uk.gov.hmrc.nationalinsurancerecord.util

import play.api.libs.json.{Format, JsError, JsPath, JsResult, JsString, JsSuccess, JsValue, Reads, Writes}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try

object DateFormats {

  val localDatePattern = DateTimeFormatter.ISO_LOCAL_DATE

  implicit def localDateReads: Reads[LocalDate] = new Reads[LocalDate] {
    override def reads(json: JsValue): JsResult[LocalDate] =
      Try(JsSuccess(LocalDate.parse(json.as[String], localDatePattern), JsPath)).getOrElse(JsError())
  }

  implicit def localDateWrites: Writes[LocalDate] = new Writes[LocalDate] {
    def writes(localDate: LocalDate): JsValue = JsString(localDate.format(localDatePattern))
  }

  implicit def localDateFormat: Format[LocalDate] = Format(localDateReads, localDateWrites)
}
