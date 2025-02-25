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
import uk.gov.hmrc.nationalinsurancerecord.connectors.MdtpStatePensionConnector
import uk.gov.hmrc.nationalinsurancerecord.controllers.actions.{FakeMdtpAuthAction, FakePertaxAuthAction, MdtpAuthAction, PertaxAuthAction}
import uk.gov.hmrc.nationalinsurancerecord.controllers.nationalInsurance.{MdtpNationalInsuranceRecordController, NationalInsuranceRecordController}
import uk.gov.hmrc.nationalinsurancerecord.services.NationalInsuranceRecordService

import scala.concurrent.Future

class MdtpNationalInsuranceRecordControllerSpec extends NationalInsuranceRecordControllerSpec {

  override val linkPath: String = "ni/mdtp"

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[NationalInsuranceRecordService].toInstance(mockNationalInsuranceRecordService),
      bind[MdtpStatePensionConnector].toInstance(mockMdtpStatePensionConnector),
      bind[MdtpAuthAction].to[FakeMdtpAuthAction],
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    )
    .build()

  override def beforeEach(): Unit = {
    reset(mockNationalInsuranceRecordService)
    reset(mockMdtpStatePensionConnector)
    mockStatePensionController(None)
  }

  override def nationalInsuranceRecordController: NationalInsuranceRecordController = app.injector.instanceOf[MdtpNationalInsuranceRecordController]

  override def mockStatePensionController(returnVal: Option[HttpResponse]): OngoingStubbing[Future[Option[HttpResponse]]] =
    when(mockMdtpStatePensionConnector.getCopeCase(any())(any())).thenReturn(Future.successful(returnVal))
}
