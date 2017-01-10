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

package uk.gov.hmrc.nationalinsurancerecord.services

import play.api.Logger
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.domain._
import uk.gov.hmrc.play.http.HeaderCarrier
import play.api.Play.current
import uk.gov.hmrc.nationalinsurancerecord.util.EitherReads._

import scala.concurrent.Future
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

trait NationalInsuranceRecordService {
  def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[ExclusionResponse, NationalInsuranceRecord]]

  def getTaxYear(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[Either[ExclusionResponse, NationalInsuranceTaxYear]]
}

object SandboxNationalInsuranceService extends NationalInsuranceRecordService {
  // scalastyle:off magic.number

  private val defaultResponsePath = "conf/resources/sandbox/default/"
  private val resourcePath = "conf/resources/sandbox/"

  private def getTaxYearFileFromPrefix(nino: Nino, taxYear: TaxYear): Either[ExclusionResponse, NationalInsuranceTaxYear] = {
    val prefix = nino.toString.substring(0, 2)
    val taxYearPrefix = taxYear.startYear
    val path = resourcePath + prefix + "/" + taxYearPrefix + ".json"
    val defaultPath = defaultResponsePath + taxYearPrefix + ".json"
    loadResourceFileOrDefault[Either[ExclusionResponse, NationalInsuranceTaxYear]](path, defaultPath)
  }

  private def getSummaryFileFromPrefix(nino: Nino): Either[ExclusionResponse, NationalInsuranceRecord] = {
    val prefix = nino.toString.substring(0, 2)
    val path = resourcePath + prefix + "/summary.json"
    val defaultPath = defaultResponsePath + "summary.json"
    loadResourceFileOrDefault[Either[ExclusionResponse, NationalInsuranceRecord]](path, defaultPath)

  }

  private def loadResourceFileOrDefault[A](path: String, defaultPath: String)(implicit formats: Reads[A]): A = {
    play.api.Play.getExistingFile(path) match {
      case Some(file) => Json.parse(scala.io.Source.fromFile(file).mkString).
        as[A]
      case None =>
        Logger.info(s"Sandbox: Resource not found for $path using default")
        play.api.Play.getExistingFile(defaultPath) match {
          case Some(file) => Json.parse(scala.io.Source.fromFile(file).mkString).
            as[A]
          case None =>
            throw new RuntimeException("Can't find default data!")
        }
    }
  }

  private def checkForExclusions(nino: Nino): Option[ExclusionResponse] = {
    val prefix = nino.toString().take(2)
    prefix match {
      case "MA" => Some(ExclusionResponse(List(Exclusion.IsleOfMan)))
      case "MW" => Some(ExclusionResponse(List(Exclusion.MarriedWomenReducedRateElection)))
      case "YN" => Some(ExclusionResponse(List(Exclusion.Dead)))
      case "MC" => Some(ExclusionResponse(List(Exclusion.ManualCorrespondenceIndicator)))
      case _ => None
    }
  }

  override def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[ExclusionResponse, NationalInsuranceRecord]] =
    checkForExclusions(nino) match {
      case Some(exclusionResponse) => Future.successful(Left(exclusionResponse))
      case None => Future(getSummaryFileFromPrefix(nino))
    }

  override def getTaxYear(nino: Nino, taxYear: TaxYear)(implicit hc: HeaderCarrier):
  Future[Either[ExclusionResponse, NationalInsuranceTaxYear]] =
    checkForExclusions(nino) match {
      case Some(exclusionResponse) => Future.successful(Left(exclusionResponse))
      case None => Future(getTaxYearFileFromPrefix(nino, taxYear))
    }
}
