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

package uk.gov.hmrc.nationalinsurancerecord.controllers.actions

import cats.data.EitherT
import play.api.Logging
import play.api.http.Status.{BAD_GATEWAY, LOCKED, NOT_FOUND, TOO_MANY_REQUESTS}
import uk.gov.hmrc.http.{HttpException, HttpResponse, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PertaxAuthParser @Inject() ()(implicit ec: ExecutionContext) extends Logging {
  def apply(
      request: Future[Either[UpstreamErrorResponse, HttpResponse]]
  ): EitherT[Future, UpstreamErrorResponse, PertaxAuthResponse] =
    EitherT(request.map {
      case Right(response) =>
        Right(response)
      case Left(error) =>
        if (error.statusCode >= 499 || error.statusCode == TOO_MANY_REQUESTS)
          logger.error(error.message)
        else if (error.statusCode == NOT_FOUND || error.statusCode == LOCKED)
          logger.info(error.message)
        else
          logger.error(error.message, error)
        Left(error)
    } recover {
      case ex: HttpException =>
        logger.error(ex.message)
        Left(UpstreamErrorResponse(ex.message, BAD_GATEWAY, BAD_GATEWAY))
      case ex =>
        throw ex
    }).map(_.json.as[PertaxAuthResponse])
}
