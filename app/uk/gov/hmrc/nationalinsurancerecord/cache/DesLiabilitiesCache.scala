/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.Play
import play.api.libs.json.Json
import play.modules.reactivemongo.{MongoDbConnection, ReactiveMongoComponent}
import reactivemongo.api.DefaultDB
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.nationalinsurancerecord.config.ApplicationConfig
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.des.DesLiabilities
import uk.gov.hmrc.nationalinsurancerecord.services.{CachingModel, CachingMongoService, MetricsService}

import scala.concurrent.ExecutionContext.Implicits.global

case class DesLiabilitiesCache(key: String, response: DesLiabilities, expiresAt: DateTime)
  extends CachingModel[DesLiabilitiesCache, DesLiabilities]

object DesLiabilitiesCache {
  implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats
  implicit val idFormat = ReactiveMongoFormats.objectIdFormats
  implicit def formats = Json.format[DesLiabilitiesCache]
}

@Singleton
class DesLiabilitiesRepository @Inject()(implicit val reactiveMongoComponent: ReactiveMongoComponent, metricsService: MetricsService) {

  implicit val db: () => DefaultDB = reactiveMongoComponent.mongoConnector.db

  private lazy val cacheService = new CachingMongoService[DesLiabilitiesCache, DesLiabilities](
    DesLiabilitiesCache.formats, DesLiabilitiesCache.apply, APITypes.Liabilities, ApplicationConfig, metricsService
  )

  def apply(): CachingMongoService[DesLiabilitiesCache, DesLiabilities] = cacheService
}
