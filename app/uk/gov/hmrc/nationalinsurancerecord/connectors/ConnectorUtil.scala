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

package uk.gov.hmrc.nationalinsurancerecord.connectors

import com.google.inject.Inject
import play.api.Logging
import play.api.http.Status.BAD_GATEWAY
import play.api.libs.json.{JsPath, JsonValidationError, Reads}
import uk.gov.hmrc.http.{HttpException, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.nationalinsurancerecord.domain.des.DesError
import uk.gov.hmrc.nationalinsurancerecord.util.JsonDepersonaliser.{depersonalise, formatJsonErrors}

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class ConnectorUtil @Inject() (
  implicit val executionContext: ExecutionContext
) extends Logging {

  def handleConnectorResponse[A](
    futureResponse: Future[Either[UpstreamErrorResponse, HttpResponse]],
    jsonParseError: String
  )(
    implicit reads: Reads[A]
  ): Future[Either[DesError, A]] = {
    futureResponse map {
      case Right(response) =>
        response.json.validate[A].fold(
          errs => {
            val formattedErrors: String =
              formatJsonErrors(errs.asInstanceOf[immutable.Seq[(JsPath, immutable.Seq[JsonValidationError])]])

            Left(DesError.JsonValidationError(
              s"Unable to de-serialise $jsonParseError: $formattedErrors\n${depersonalise(response.json)}"
            ))
          },
          valid =>
            Right(valid)
        )
      case Left(error) => Left(DesError.HttpError(error))
    } recover {
      case error: HttpException =>
        Left(DesError.HttpError(UpstreamErrorResponse(error.message, BAD_GATEWAY)))
      case error =>
        Left(DesError.OtherError(error))
    }
  }
}
