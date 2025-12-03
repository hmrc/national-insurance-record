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

package uk.gov.hmrc.nationalinsurancerecord.test_utils

import play.api.mvc.{ActionBuilder, ActionFilter, BodyParser, Request, Result}

import scala.concurrent.{ExecutionContext, Future}

class FakeAction[B](implicit ec: ExecutionContext) extends ActionBuilder[Request, B] with ActionFilter[Request] {
  override def parser: BodyParser[B] = ???
  override protected def executionContext: ExecutionContext = ec

  override protected def filter[C](request: Request[C]): Future[Option[Result]] = Future.successful(None)
}