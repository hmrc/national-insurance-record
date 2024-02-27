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

import play.api.http.Status._
import play.mvc.Http.Status.NOT_ACCEPTABLE
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

object ErrorResponses {
  val CODE_INVALID_NINO = "ERROR_NINO_INVALID"
  val CODE_INVALID_TAXYEAR = "CODE_TAXYEAR_INVALID"
  val CODE_MANUAL_CORRESPONDENCE = "EXCLUSION_MANUAL_CORRESPONDENCE"
  val CODE_DEAD = "EXCLUSION_DEAD"
  val CODE_ISLE_OF_MAN = "EXCLUSION_ISLE_OF_MAN"
  val CODE_MARRIED_WOMENS_REDUCED_RATE = "EXCLUSION_MARRIED_WOMENS_REDUCED_RATE"

  def ErrorNinoInvalid = ErrorResponse(BAD_REQUEST, "The provided NINO is not valid", Some(CODE_INVALID_NINO))
  def ErrorTaxYearInvalid = ErrorResponse(BAD_REQUEST, "The provided TAX YEAR is not valid", Some(CODE_INVALID_TAXYEAR))
  def ExclusionManualCorrespondence = ErrorResponse(FORBIDDEN, "The customer cannot access the service, they should contact HMRC", Some(CODE_MANUAL_CORRESPONDENCE))
  def ExclusionDead  = ErrorResponse(FORBIDDEN, "The customer needs to contact the National Insurance helpline", Some(CODE_DEAD))
  def ExclusionIsleOfMan = ErrorResponse(FORBIDDEN, "The customer needs to contact the National Insurance helpline", Some(CODE_ISLE_OF_MAN))
  def ExclusionMarriedWomenReducedRate = ErrorResponse(FORBIDDEN, "The customer needs to contact the National Insurance helpline", Some(CODE_MARRIED_WOMENS_REDUCED_RATE))
  def ErrorNotFound = ErrorResponse(NOT_FOUND, "Resource was not found", Some("NOT_FOUND"))
  def ErrorInternalServerError = ErrorResponse(INTERNAL_SERVER_ERROR, "Internal server error", Some("INTERNAL_SERVER_ERROR"))
  def ErrorGenericBadRequest(msg: String = "Bad Request") = ErrorResponse(BAD_REQUEST, msg, Some("BAD_REQUEST"))
  def ErrorAcceptHeaderInvalid = ErrorResponse(NOT_ACCEPTABLE, "The accept header is missing or invalid", Some("ACCEPT_HEADER_INVALID"))



}
