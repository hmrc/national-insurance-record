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
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % "5.12.0",
    "uk.gov.hmrc" %% "domain"                    % "6.2.0-play-28",
    "uk.gov.hmrc" %% "play-hmrc-api"             % "6.4.0-play-28",
    "uk.gov.hmrc" %% "play-hal"                  % "3.0.0-play-28-SNAPSHOT",
    "uk.gov.hmrc" %% "simple-reactivemongo"      % "8.0.0-play-28",
    "com.typesafe.play" %% "play-json-joda" % "2.6.10"
  )

  val test: Seq[ModuleID] = Seq(
    "org.pegdown"             %  "pegdown"            % "1.6.0",
    "org.scalatestplus.play"  %% "scalatestplus-play" % "5.1.0",
    "com.typesafe.play"       %% "play-test"          % PlayVersion.current,
    "com.github.tomakehurst"  % "wiremock-jre8"      % "2.27.0",
    "org.mockito"             % "mockito-core"        % "2.24.5",
    "uk.gov.hmrc"             %% "reactivemongo-test" % "5.0.0-play-28",
    "com.vladsch.flexmark" % "flexmark-all" % "0.35.10"
  ).map(_ % "test,it")

  val all: Seq[ModuleID] = compile ++ test

}