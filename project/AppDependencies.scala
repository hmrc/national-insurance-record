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

import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {
  private val bootstrapVersion = "9.5.0"
  private val playVersion = "play-30"
  private val hmrcMongoVersion = "2.2.0"

  val compile: Seq[ModuleID] = Seq(
    caffeine,
    ws,
    "uk.gov.hmrc"       %%  s"bootstrap-backend-$playVersion"                   % bootstrapVersion,
    "uk.gov.hmrc"       %%  s"domain-$playVersion"                              % "10.0.0",
    "uk.gov.hmrc"       %%  s"play-hal-$playVersion"                            % "4.1.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion"                           % hmrcMongoVersion,
    "org.typelevel"     %% "cats-core"                                          % "2.12.0",
    "uk.gov.hmrc"       %% s"internal-auth-client-$playVersion"                 % "3.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %%  s"bootstrap-test-$playVersion"    % bootstrapVersion,
    "org.pegdown"                    %  "pegdown"                         % "1.6.0",
    "uk.gov.hmrc.mongo"             %%  s"hmrc-mongo-test-$playVersion"   % hmrcMongoVersion,
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test

}