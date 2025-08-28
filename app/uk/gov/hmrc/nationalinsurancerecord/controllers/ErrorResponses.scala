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

import play.api.http.Status.*
import play.mvc.Http.Status.NOT_ACCEPTABLE
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

object ErrorResponses {
  val CODE_INVALID_NINO = "ERROR_NINO_INVALID"
  val CODE_INVALID_TAXYEAR = "CODE_TAXYEAR_INVALID"
  
  private val CODE_MANUAL_CORRESPONDENCE = "EXCLUSION_MANUAL_CORRESPONDENCE"
  private val CODE_DEAD = "EXCLUSION_DEAD"
  private val CODE_ISLE_OF_MAN = "EXCLUSION_ISLE_OF_MAN"
  private val CODE_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR"
  private val CODE_ACCEPT_HEADER_INVALID = "ACCEPT_HEADER_INVALID"
  private val CODE_BAD_REQUEST = "BAD_REQUEST"

  def ExclusionManualCorrespondence: ErrorResponse =
    ErrorResponse(FORBIDDEN, "The customer cannot access the service, they should contact HMRC", Some(CODE_MANUAL_CORRESPONDENCE))

  def ExclusionDead: ErrorResponse =
    ErrorResponse(FORBIDDEN, "The customer needs to contact the National Insurance helpline", Some(CODE_DEAD))

  def ExclusionIsleOfMan: ErrorResponse =
    ErrorResponse(FORBIDDEN, "The customer needs to contact the National Insurance helpline", Some(CODE_ISLE_OF_MAN))

  def ErrorNotFound: ErrorResponse =
    ErrorResponse(NOT_FOUND, "Resource was not found", Some("NOT_FOUND"))

  def ErrorInternalServerError: ErrorResponse =
    ErrorResponse(INTERNAL_SERVER_ERROR, "Internal server error", Some(CODE_INTERNAL_SERVER_ERROR))

  def ErrorGenericBadRequest(msg: String = "Bad Request"): ErrorResponse =
    ErrorResponse(BAD_REQUEST, msg, Some(CODE_BAD_REQUEST))

  def ErrorAcceptHeaderInvalid: ErrorResponse =
    ErrorResponse(NOT_ACCEPTABLE, "The accept header is missing or invalid", Some(CODE_ACCEPT_HEADER_INVALID))

}
