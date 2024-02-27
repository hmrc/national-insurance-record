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
  val bootstrapVersion = "8.4.0"
  val mongoVersion = "1.7.0"
  val playVersion = "play-30"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %%  s"bootstrap-backend-$playVersion"                   % bootstrapVersion,
    "uk.gov.hmrc"       %%  s"domain-$playVersion"                              % "9.0.0",
    "uk.gov.hmrc"       %%  s"play-hal-$playVersion"                            % "4.0.0",
    "uk.gov.hmrc"       %%  s"mongo-feature-toggles-client-$playVersion"        % "1.1.0",
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %%  s"bootstrap-test-$playVersion"    % bootstrapVersion,
    "org.pegdown"                    %  "pegdown"                         % "1.6.0",
    "org.playframework"             %% "play-test"                        % "3.0.1",
    "com.github.tomakehurst"        %  "wiremock"                         % "2.27.2",
    "org.mockito"                    %  "mockito-core"                    % "5.10.0",
    "uk.gov.hmrc.mongo"             %%  s"hmrc-mongo-test-$playVersion"   % mongoVersion,
  ).map(_ % "test,it")

  private val silencerDependencies: Seq[ModuleID] = Seq(
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.15" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.15" % Provided cross CrossVersion.full
  )

  val all: Seq[ModuleID] = compile ++ test ++ silencerDependencies

}