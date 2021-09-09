/*
 * Copyright 2021 HM Revenue & Customs
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

import com.google.inject.Inject
import play.api.Configuration


class AppContext @Inject()(configuration: Configuration) {
  lazy val appName = configuration.getOptional[String]("appName").getOrElse(throw new RuntimeException("appName is not configured"))
  lazy val apiGatewayContext = configuration.getOptional[String]("api.gateway.context")
    .getOrElse(throw new RuntimeException("api.gateway.context is not configured"))
  lazy val access = configuration.getOptional[Configuration]("api.access")
  lazy val status = configuration.getOptional[String]("api.status")
}
