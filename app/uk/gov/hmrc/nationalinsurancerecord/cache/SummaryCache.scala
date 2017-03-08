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

package uk.gov.hmrc.nationalinsurancerecord.cache

import org.joda.time.DateTime
import play.api.libs.json.Json
import play.modules.reactivemongo.MongoDbConnection
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.nationalinsurancerecord.config.ApplicationConfig
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.nps.NpsSummary
import uk.gov.hmrc.nationalinsurancerecord.services.{CachingModel, CachingMongoService}
import scala.concurrent.ExecutionContext.Implicits.global

case class SummaryCache(key: String, response: NpsSummary, expiresAt: DateTime)
  extends CachingModel[SummaryCache, NpsSummary]

object SummaryCache {
  implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats
  implicit val idFormat = ReactiveMongoFormats.objectIdFormats
  implicit def formats = Json.format[SummaryCache]
}

object SummaryRepository extends MongoDbConnection {

  private lazy val cacheService = new CachingMongoService[SummaryCache, NpsSummary](
    SummaryCache.formats, SummaryCache.apply, APITypes.Summary, ApplicationConfig
  )

  def apply(): CachingMongoService[SummaryCache, NpsSummary] = cacheService
}
