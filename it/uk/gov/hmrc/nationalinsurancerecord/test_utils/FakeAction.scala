package uk.gov.hmrc.nationalinsurancerecord.test_utils

import play.api.mvc.{ActionBuilder, ActionFilter, BodyParser, Request, Result}

import scala.concurrent.{ExecutionContext, Future}

class FakeAction[B](implicit ec: ExecutionContext) extends ActionBuilder[Request, B] with ActionFilter[Request] {
  override def parser: BodyParser[B] = ???
  override protected def executionContext: ExecutionContext = ec

  override protected def filter[C](request: Request[C]): Future[Option[Result]] = Future.successful(None)
}