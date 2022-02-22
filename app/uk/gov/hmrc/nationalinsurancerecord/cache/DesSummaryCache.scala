/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.nationalinsurancerecord.cache

import com.google.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.{MongoFormats, MongoJodaFormats}
import uk.gov.hmrc.nationalinsurancerecord.config.ApplicationConfig
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.des.DesSummary
import uk.gov.hmrc.nationalinsurancerecord.services.{CachingModel, CachingMongoService, MetricsService}

import scala.concurrent.ExecutionContext.Implicits.global

case class DesSummaryCache(key: String, response: DesSummary, expiresAt: DateTime)
  extends CachingModel[DesSummaryCache, DesSummary]

object DesSummaryCache {
  implicit val dateFormat = MongoJodaFormats.dateTimeFormat
  implicit val idFormat = MongoFormats.objectIdFormat
  implicit def formats = Json.format[DesSummaryCache]
}

//TODO: look to extend CachingMongoService?
@Singleton
class DesSummaryRepository @Inject()(mongoComponent: MongoComponent,
                                     metricsService: MetricsService,
                                     applicationConfig: ApplicationConfig) {

//  implicit val db: () => DefaultDB = reactiveMongoComponent.mongoConnector.db

  private lazy val cacheService = new CachingMongoService[DesSummaryCache, DesSummary](
    mongoComponent: MongoComponent, DesSummaryCache.formats, DesSummaryCache.apply, APITypes.Summary, applicationConfig, metricsService
  )

  def apply(): CachingMongoService[DesSummaryCache, DesSummary] = cacheService
}
