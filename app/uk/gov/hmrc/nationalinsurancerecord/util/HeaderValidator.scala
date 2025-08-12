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

import play.api.mvc.*
import play.api.mvc.Results.Status
import play.mvc.Http.Status.NOT_ACCEPTABLE
import uk.gov.hmrc.nationalinsurancerecord.controllers.ErrorResponses.ErrorAcceptHeaderInvalid

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex
import scala.util.matching.Regex.Match

trait HeaderValidator {
  outer =>

  val validateVersion:     String => Boolean = _ == "1.0"
  val validateContentType: String => Boolean = _ == "json"

  val matchHeader: String => Option[Match] =
    new Regex("""^application/vnd[.]{1}hmrc[.]{1}(.*?)[+]{1}(.*)$""", "version", "contenttype").findFirstMatchIn(_)

  val acceptHeaderValidationRules: Option[String] => Boolean =
    _.flatMap(a =>
      matchHeader(a).map(res => validateContentType(res.group("contenttype")) && validateVersion(res.group("version")))
    ).getOrElse(false)

  def parser: BodyParser[AnyContent]

  def validateAccept(rules: Option[String] => Boolean): ActionBuilder[Request, AnyContent] = new ActionBuilder[Request, AnyContent] {

    def invokeBlock[A](
                        request: Request[A],
                        block:   (Request[A]) => Future[Result]
                      ): Future[Result] =
      if (rules(request.headers.get("Accept"))) block(request)
      else Future.successful(Status(NOT_ACCEPTABLE)(ErrorResponseUtils.convertToJson(ErrorAcceptHeaderInvalid)))

    override def parser: BodyParser[AnyContent] = outer.parser

    override protected def executionContext: ExecutionContext = outer.executionContext
  }

  protected def executionContext: ExecutionContext
}
