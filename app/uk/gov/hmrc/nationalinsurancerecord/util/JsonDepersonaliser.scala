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

package uk.gov.hmrc.nationalinsurancerecord.util

import play.api.libs.json._

import scala.collection.immutable
import scala.util.{Failure, Success, Try}

object JsonDepersonaliser {

  def depersonalise(json: JsValue): String =
    Try(Json.prettyPrint(depersonaliseValue(json))) match {
      case Success(s) =>
        s"Depersonalised JSON\n$s"
      case Failure(e) =>
        s"JSON could not be depersonalised\n${e.toString}"
    }

  def depersonaliseObject(obj: JsObject): JsObject = {

    val underlying: Map[String, JsValue] = (for {
      (key, value) <- obj.fields
    } yield {
      (key, depersonaliseValue(value))
    }).toMap

    JsObject(underlying)

  }

  def depersonaliseArray(array: JsArray): JsArray = {

    val value: collection.IndexedSeq[JsValue] = for {
      value <- array.value
    } yield {
      depersonaliseValue(value)
    }

    JsArray(value)

  }

  def depersonaliseValue(value: JsValue): JsValue =
    value match {
      case v: JsArray   => depersonaliseArray(v)
      case _: JsBoolean => JsBoolean(false)
      case v: JsNumber  => JsNumber(depersonaliseNumber(v.value))
      case v: JsObject  => depersonaliseObject(v)
      case v: JsString  => JsString(depersonaliseString(v.value))
      case JsNull       => JsNull
    }


  def depersonaliseString(string: String): String =
    string.replaceAll("[0-9]", "1").replaceAll("[a-zA-Z]", "a")

  def depersonaliseNumber(number: BigDecimal): BigDecimal =
    BigDecimal.apply(number.toString().replaceAll("[0-9]", "1"))

  def formatJsonErrors(errors: immutable.Seq[(JsPath, immutable.Seq[JsonValidationError])]): String =
    s"JSON Validation Error: ${
      errors
        .map(p => s"${p._1.toString()} - ${p._2.map(e => removeJson(e.message)).mkString(",")}")
        .mkString(" | ")
    }"

  private def removeJson(message: String): String =
    message.indexOf("{") match {
      case i if i != -1 =>
        s"${message.substring(0, i - 1)} [JSON removed]"
      case _ =>
        message
    }
}
