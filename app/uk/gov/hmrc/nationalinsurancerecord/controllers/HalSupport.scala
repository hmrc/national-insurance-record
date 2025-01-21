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

import play.api.hal.{Hal, HalLink, HalResource}
import play.api.libs.json.{JsObject, JsValue, Json, Writes}
import play.api.mvc.Result

import play.api.hal.halLinkWrites

trait HalSupport {

  private def halResource(jsValue: JsValue, links: Set[HalLink], embedded: Option[Vector[(String, Vector[HalResource])]] = None): HalResource = {

    val halState = Hal.state(jsValue)

    embedded match {
      case Some(embed) => Hal.hal(halState, links.toVector, embed)
      case None => links.foldLeft(halState)((res, link) => res ++ link)
    }
  }

  def halResourceSelfLink(value: JsValue, self: String, embedded: Option[Vector[(String, Vector[HalResource])]] = None): HalResource = {
    halResource(
      value,
      Set(HalLink("self", self)),
      embedded
    )
  }

  implicit val halResourceWrites: Writes[HalResource] = new Writes[HalResource] {
    def writes(hal: HalResource): JsValue = {
      val embedded = toEmbeddedJson(hal)
      val resource = if (hal.links.links.isEmpty) hal.state
      else Json.toJson(hal.links).as[JsObject] ++ hal.state
      if (embedded.fields.isEmpty) resource
      else resource + ("_embedded" -> embedded)
    }

    def toEmbeddedJson(hal: HalResource): JsObject = {
      hal.embedded match {
        case e if e.isEmpty => JsObject(Nil)
        case e => JsObject(e.map {
          case (link, resources) =>
            link -> Json.toJson(resources.map(r => Json.toJson(r)))
        })
      }
    }
  }

  // scalastyle:off method.name
  def Ok(hal: HalResource): Result =
    play.api.mvc.Results.Ok(Json.toJson(hal)).as("application/hal+json")

}
