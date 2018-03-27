/*
 * Copyright 2018 HM Revenue & Customs
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

import org.joda.time.DateTime
import play.api.libs.json.Json
import play.modules.reactivemongo.MongoDbConnection
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.nationalinsurancerecord.config.ApplicationConfig
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.des.DesSummary
import uk.gov.hmrc.nationalinsurancerecord.services.{CachingModel, CachingMongoService, MetricsService}

import scala.concurrent.ExecutionContext.Implicits.global

case class DesSummaryCache(key: String, response: DesSummary, expiresAt: DateTime)
  extends CachingModel[DesSummaryCache, DesSummary]

object DesSummaryCache {
  implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats
  implicit val idFormat = ReactiveMongoFormats.objectIdFormats
  implicit def formats = Json.format[DesSummaryCache]
}

object DesSummaryRepository extends MongoDbConnection {

  private lazy val cacheService = new CachingMongoService[DesSummaryCache, DesSummary](
    DesSummaryCache.formats, DesSummaryCache.apply, APITypes.Summary, ApplicationConfig, MetricsService
  )

  def apply(): CachingMongoService[DesSummaryCache, DesSummary] = cacheService
}
