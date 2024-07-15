/*
 * Copyright 2022 HM Revenue & Customs
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

import org.apache.pekko.Done
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.cache.AsyncCacheApi
import uk.gov.hmrc.domain.{Generator, Nino}
import utils.WireMockHelper

import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Random

trait IntegrationBaseSpec extends AnyWordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with WireMockHelper {

  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  def generateNino: Nino = new Generator(new Random).nextNino

  def generateUUId: UUID = UUID.randomUUID()

  def mock[T](implicit ev: ClassTag[T]): T =
    Mockito.mock(ev.runtimeClass.asInstanceOf[Class[T]])

  def mock[T](answer: Answer[Object])(implicit ev: ClassTag[T]): T =
    Mockito.mock(ev.runtimeClass.asInstanceOf[Class[T]], answer)

  // A cache that doesn't cache
  val mockCacheApi: AsyncCacheApi = new AsyncCacheApi {
    override def set(key: String, value: Any, expiration: Duration): Future[Done] = ???

    override def remove(key: String): Future[Done] = Future.successful(Done)

    override def getOrElseUpdate[A](key: String, expiration: Duration)(orElse: => Future[A])
                                   (implicit evidence$1: ClassTag[A]): Future[A] = orElse

    override def get[T](key: String)(implicit evidence$2: ClassTag[T]): Future[Option[T]] = ???

    override def removeAll(): Future[Done] = ???
  }
}