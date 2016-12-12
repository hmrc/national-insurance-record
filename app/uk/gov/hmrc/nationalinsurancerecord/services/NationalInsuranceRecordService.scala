/*
 * Copyright 2016 HM Revenue & Customs
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

import org.joda.time.LocalDate
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.domain._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import play.api.Play.current
import uk.gov.hmrc.nationalinsurancerecord.util.EitherReads._

import scala.concurrent.Future

trait NationalInsuranceRecordService {
  def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[NationalInsuranceRecordExclusion, NationalInsuranceRecord]]
  def getTaxYear(nino: Nino, taxYear: String)(implicit hc: HeaderCarrier): Future[Either[NationalInsuranceRecordExclusion, NationalInsuranceTaxYear]]
}

object SandboxNationalInsuranceService extends NationalInsuranceRecordService {
  // scalastyle:off magic.number

  private val defaultResponsePath = "conf/resources/sandbox/EZ/"
  private val resourcePath = "conf/resources/sandbox/"

  private def getTaxYearFileFromPrefix(nino: Nino, taxYear: String): Either[NationalInsuranceRecordExclusion, NationalInsuranceTaxYear] = {
    val prefix = nino.toString.substring(0, 2)
    val taxYearPrefix = taxYear.substring(0,4)

    play.api.Play.getExistingFile(resourcePath + prefix + "/" + taxYearPrefix + ".json") match {
      case Some(file) => Json.parse(scala.io.Source.fromFile(file).mkString).as[Either[NationalInsuranceRecordExclusion,NationalInsuranceTaxYear]]
      case None => {
        Logger.info(s"Sandbox: Resource not found for $prefix, using default")
        Json.parse(scala.io.Source.fromFile( defaultResponsePath + taxYearPrefix + ".json").mkString).
          as[Either[NationalInsuranceRecordExclusion,NationalInsuranceTaxYear]]
      }
    }
  }

  private def getSummaryFileFromPrefix(nino: Nino): Either[NationalInsuranceRecordExclusion, NationalInsuranceRecord] = {
    val prefix = nino.toString.substring(0, 2)
    play.api.Play.getExistingFile(resourcePath + prefix + "/summary.json") match {
      case Some(file) => Json.parse(scala.io.Source.fromFile(file).mkString).
        as[Either[NationalInsuranceRecordExclusion,NationalInsuranceRecord]]
      case None => Logger.info(s"Sandbox: Resource not found for $prefix, using default");
        Json.parse(scala.io.Source.fromFile( defaultResponsePath + "/summary.json").
          mkString).as[Either[NationalInsuranceRecordExclusion,NationalInsuranceRecord]]
    }
  }

  override def getNationalInsuranceRecord(nino: Nino)(implicit hc: HeaderCarrier):
    Future[Either[NationalInsuranceRecordExclusion, NationalInsuranceRecord]] = Future.successful(getSummaryFileFromPrefix(nino))

  override def getTaxYear(nino: Nino, taxYear: String)(implicit hc: HeaderCarrier): Future[Either[NationalInsuranceRecordExclusion, NationalInsuranceTaxYear]] =
    Future.successful(getTaxYearFileFromPrefix(nino, taxYear))

}