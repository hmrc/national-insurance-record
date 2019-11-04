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
    "uk.gov.hmrc" %% "microservice-bootstrap" % "10.4.0",
    "uk.gov.hmrc" %% "domain"                 % "5.6.0-play-25",
    "uk.gov.hmrc" %% "play-hmrc-api"          % "3.4.0-play-25",
    "uk.gov.hmrc" %% "play-hal"               % "1.8.0-play-25",
    "uk.gov.hmrc" %% "play-reactivemongo"     % "6.4.0",
    "uk.gov.hmrc" %% "simple-reactivemongo"   % "7.14.0-play-25",
    "uk.gov.hmrc" %% "auth-client"            % "2.31.0-play-25"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "hmrctest"           % "3.6.0-play-25",
    "org.pegdown"             %  "pegdown"            % "1.6.0",
    "com.typesafe.play"       %% "play-test"          % PlayVersion.current,
    "org.scalatestplus.play"  %% "scalatestplus-play" % "2.0.1",
    "org.mockito"             %  "mockito-all"        % "1.10.19",
    "uk.gov.hmrc"             %% "reactivemongo-test" % "4.9.0-play-25"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test

}