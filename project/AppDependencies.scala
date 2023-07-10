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
  val bootstrapVersion = "7.16.0"
  val mongoVersion = "1.3.0"
  val playVersion = "play-28"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %%  "bootstrap-backend-play-28"     % bootstrapVersion,
    "uk.gov.hmrc"       %%  "domain"                        % s"8.1.0-$playVersion",
    "uk.gov.hmrc"       %%  "play-hmrc-api"                 % s"7.1.0-$playVersion",
    "uk.gov.hmrc"       %%  "play-hal"                      % s"3.2.0-$playVersion",
    "uk.gov.hmrc"       %%  "mongo-feature-toggles-client"  % "0.2.0",
    "uk.gov.hmrc.mongo" %%  s"hmrc-mongo-$playVersion"      % mongoVersion,
    ehcache
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %%  s"bootstrap-test-$playVersion"    % bootstrapVersion,
    "org.pegdown"                    %  "pegdown"                         % "1.6.0",
    "com.typesafe.play"             %%  "play-test"                       % PlayVersion.current,
    "com.fasterxml.jackson.module"  %%  "jackson-module-scala"            % "2.14.1",
    "com.github.tomakehurst"         %  "wiremock-jre8"                   % "2.35.0",
    "org.mockito"                    %  "mockito-core"                    % "4.11.0",
    "uk.gov.hmrc.mongo"             %%  s"hmrc-mongo-test-$playVersion"   % mongoVersion
  ).map(_ % "test,it")

  private val silencerDependencies: Seq[ModuleID] = Seq(
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.9" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.9" % Provided cross CrossVersion.full
  )

  val all: Seq[ModuleID] = compile ++ test ++ silencerDependencies

}