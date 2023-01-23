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
  val bootstrapVersion = "7.12.0"
  val mongoVersion = "0.74.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % bootstrapVersion,
    "uk.gov.hmrc"       %% "domain"                    % "8.1.0-play-28",
    "uk.gov.hmrc"       %% "play-hmrc-api"             % "7.1.0-play-28",
    "uk.gov.hmrc"       %% "play-hal"                  % "3.2.0-play-28",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"        % mongoVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % bootstrapVersion,
    "org.pegdown"             % "pegdown"                 % "1.6.0",
    "com.typesafe.play"      %% "play-test"               % PlayVersion.current,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.1",
    "com.github.tomakehurst"  % "wiremock-jre8"           % "2.35.0",
    "org.mockito"             % "mockito-core"            % "4.11.0",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % mongoVersion
  ).map(_ % "test,it")

  val all: Seq[ModuleID] = compile ++ test

}