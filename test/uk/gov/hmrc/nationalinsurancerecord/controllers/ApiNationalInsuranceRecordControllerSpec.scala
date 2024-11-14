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

import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.nationalinsurancerecord.connectors.StatePensionConnector
import uk.gov.hmrc.nationalinsurancerecord.controllers.actions.{ApiAuthAction, FakeApiAuthAction}//, FakePertaxAuthAction, PertaxAuthAction}
import uk.gov.hmrc.nationalinsurancerecord.controllers.nationalInsurance.{ApiNationalInsuranceRecordController, NationalInsuranceRecordController}
import uk.gov.hmrc.nationalinsurancerecord.services.NationalInsuranceRecordService

class ApiNationalInsuranceRecordControllerSpec extends NationalInsuranceRecordControllerSpec {

  override def nationalInsuranceRecordController: NationalInsuranceRecordController = app.injector.instanceOf[ApiNationalInsuranceRecordController]

  override val linkPath: String = "ni"

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[NationalInsuranceRecordService].toInstance(mockNationalInsuranceRecordService),
      bind[StatePensionConnector].toInstance(mockStatePensionConnector),
      bind[ApiAuthAction].to[FakeApiAuthAction]//,
//      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    )
    .build()
}
