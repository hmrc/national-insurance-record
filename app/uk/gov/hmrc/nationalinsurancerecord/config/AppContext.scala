/*
 * Copyright 2017 HM Revenue & Customs
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

import play.api.Configuration
import play.api.Play._
import uk.gov.hmrc.play.config.ServicesConfig

trait AppContext {
  def appName: String
  def apiGatewayContext: String
  def access: Option[Configuration]
  def status: Option[String]
  def connectToHOD: Boolean
}

object AppContext extends AppContext with ServicesConfig {
  lazy val appName = current.configuration.getString("appName").getOrElse(throw new RuntimeException("appName is not configured"))
  lazy val apiGatewayContext = current.configuration.getString("api.gateway.context")
    .getOrElse(throw new RuntimeException("api.gateway.context is not configured"))
  lazy val access = current.configuration.getConfig("api.access")
  lazy val status = current.configuration.getString("api.status")
  lazy val connectToHOD = current.configuration.getBoolean("feature.connectToHOD").getOrElse(false)
}
