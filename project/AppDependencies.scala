/*
 * Copyright 2015 HM Revenue & Customs
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

import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % "7.8.0",
    "uk.gov.hmrc"       %% "domain"                    % "8.1.0-play-28",
    "uk.gov.hmrc"       %% "play-hmrc-api"             % "7.0.0-play-28",
    "uk.gov.hmrc"       %% "play-hal"                  % "3.1.0-play-28",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"        % "0.68.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % "7.8.0",
    "org.pegdown"             % "pegdown"                 % "1.6.0",
    "com.typesafe.play"      %% "play-test"               % PlayVersion.current,
    "com.github.tomakehurst"  % "wiremock-jre8"           % "2.27.2",
    "org.mockito"             % "mockito-core"            % "4.6.1",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % "0.68.0"
  ).map(_ % "test,it")

  private val silencerDependencies: Seq[ModuleID] = Seq(
    compilerPlugin(
      "com.github.ghik" % "silencer-plugin" % "1.7.9" cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % "1.7.9" % Provided cross CrossVersion.full
  )

  val all: Seq[ModuleID] = compile ++ test ++ silencerDependencies

}