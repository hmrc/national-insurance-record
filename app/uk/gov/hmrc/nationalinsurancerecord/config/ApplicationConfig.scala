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

package uk.gov.hmrc.nationalinsurancerecord.config

import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class ApplicationConfig @Inject()(servicesConfig: ServicesConfig) {

  val citizenDetailsUrl = servicesConfig.baseUrl("citizen-details")
  val statePensionUrl = servicesConfig.baseUrl("state-pension")
  val desUrl = servicesConfig.baseUrl("des-hod")
  val proxyCacheUrl = servicesConfig.baseUrl("ni-and-sp-proxy-cache")
  val internalAuthUrl = servicesConfig.baseUrl("internal-auth")

  val responseCacheTTL = servicesConfig.getInt("mongodb.responseTTL")
  val responseCacheCollectionName = servicesConfig.getString("mongodb.collectionName")

  val authorization: String = s"Bearer ${servicesConfig.getConfString("des-hod.authorizationToken", "local")}"

  val desEnvironment: String = servicesConfig.getConfString("des-hod.environment", "local")

}
