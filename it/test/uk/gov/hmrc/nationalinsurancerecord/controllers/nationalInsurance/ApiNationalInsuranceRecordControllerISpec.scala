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

package uk.gov.hmrc.nationalinsurancerecord.controllers.nationalInsurance

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Application
import play.api.cache.AsyncCacheApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.nationalinsurancerecord.controllers.actions.ApiCopeExclusionAction
import uk.gov.hmrc.nationalinsurancerecord.test_utils.FakeAction

class ApiNationalInsuranceRecordControllerISpec extends NationalInsuranceRecordControllerISpec {
  override def classPrefix: String = "Api"
  override def controllerUrl(nino: Nino): String = s"/ni/$nino"

  private val mockCopeExclusionAction: ApiCopeExclusionAction =
    mock[ApiCopeExclusionAction]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[ApiCopeExclusionAction].to(mockCopeExclusionAction),
      bind[AsyncCacheApi].toInstance(mockCacheApi),
      bind[FeatureFlagService].toInstance(mockFeatureFlagService)
    )
    .configure(
      wiremockConfig
    ).build()

  when(mockCopeExclusionAction.filterCopeExclusions(any()))
    .thenReturn(new FakeAction[AnyContent]())

}
