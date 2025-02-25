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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.nationalinsurancerecord.connectors.ApiStatePensionConnector
import uk.gov.hmrc.nationalinsurancerecord.controllers.actions.{ApiAuthAction, FakeApiAuthAction, FakePertaxAuthAction, PertaxAuthAction}
import uk.gov.hmrc.nationalinsurancerecord.controllers.nationalInsurance.{ApiNationalInsuranceRecordController, NationalInsuranceRecordController}
import uk.gov.hmrc.nationalinsurancerecord.services.NationalInsuranceRecordService

import scala.concurrent.Future

class ApiNationalInsuranceRecordControllerSpec extends NationalInsuranceRecordControllerSpec {

  override val linkPath: String = "ni"

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[NationalInsuranceRecordService].toInstance(mockNationalInsuranceRecordService),
      bind[ApiStatePensionConnector].toInstance(mockApiStatePensionConnector),
      bind[ApiAuthAction].to[FakeApiAuthAction],
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    )
    .build()

  override def beforeEach(): Unit = {
    reset(mockNationalInsuranceRecordService)
    reset(mockApiStatePensionConnector)
    mockStatePensionController(None)
  }

  override def nationalInsuranceRecordController: NationalInsuranceRecordController = app.injector.instanceOf[ApiNationalInsuranceRecordController]

  override def mockStatePensionController(returnVal: Option[HttpResponse]): OngoingStubbing[Future[Option[HttpResponse]]] =
    when(mockApiStatePensionConnector.getCopeCase(any())(any())).thenReturn(Future.successful(returnVal))
}
