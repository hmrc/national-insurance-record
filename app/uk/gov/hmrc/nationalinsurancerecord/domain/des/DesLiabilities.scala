/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.nationalinsurancerecord.domain.des

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class DesLiabilities(liabilities: List[DesLiability])

object DesLiabilities {

  val readNullableList:JsPath => Reads[List[DesLiability]] =
    jsPath => jsPath.readNullable[List[DesLiability]].map(_.getOrElse(List.empty).filter(_.liabilityType.isDefined))

  val reads: Reads[DesLiabilities] = {
    readNullableList(__ \ "liabilities").map(DesLiabilities.apply)
  }
  val writes: Writes[DesLiabilities] = {
    (__ \ "liabilities").write[List[DesLiability]].contramap(_.liabilities)
  }
  implicit val formats: Format[DesLiabilities] = Format(reads, writes)
}
