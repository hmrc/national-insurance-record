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

package uk.gov.hmrc.nationalinsurancerecord.controllers.actions

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.nationalinsurancerecord.connectors.ApiStatePensionConnector
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.Future

class ApiCopeExclusionActionSpec extends CopeExclusionActionSpec {

  val mockStatePensionConnector: ApiStatePensionConnector = mock[ApiStatePensionConnector]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
       bind[ApiStatePensionConnector].toInstance(mockStatePensionConnector)
    )
    .build()

  override def beforeEach(): Unit = {
    reset(mockStatePensionConnector)
  }

  override def mockStatePensionConnector(nino: Nino, returnVal: Future[Option[HttpResponse]]): OngoingStubbing[Future[Option[HttpResponse]]] =
    when(mockStatePensionConnector.getCopeCase(meq(nino))(any())).thenReturn(returnVal)

  override def actionUnderTest: TestHarness = new TestHarness(app.injector.instanceOf[ApiCopeExclusionAction])
}
