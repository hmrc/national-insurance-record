/*
 * Copyright 2024 HM Revenue & Customs
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
import org.bson.types.ObjectId
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.{MongoFormats, MongoJavatimeFormats}
import uk.gov.hmrc.nationalinsurancerecord.config.ApplicationConfig
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes
import uk.gov.hmrc.nationalinsurancerecord.domain.des.DesSummary
import uk.gov.hmrc.nationalinsurancerecord.services.{CachingModel, CachingMongoService, MetricsService}

import java.time.Instant
import scala.concurrent.ExecutionContext

case class DesSummaryCache(
  key: String,
  response: DesSummary,
  expiresAt: Instant
) extends CachingModel[DesSummaryCache, DesSummary]

object DesSummaryCache {
  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit val idFormat: Format[ObjectId] = MongoFormats.objectIdFormat

  implicit def formats: OFormat[DesSummaryCache] = Json.format[DesSummaryCache]
}

//TODO: look to extend CachingMongoService?
@Singleton
class DesSummaryRepository @Inject()(
  mongoComponent: MongoComponent,
  metricsService: MetricsService,
  applicationConfig: ApplicationConfig,
  implicit val executionContext: ExecutionContext
) {

  private val cacheService = new CachingMongoService[DesSummaryCache, DesSummary](
    mongo = mongoComponent,
    formats = DesSummaryCache.formats,
    apply = DesSummaryCache.apply,
    apiType = APITypes.Summary,
    appConfig = applicationConfig,
    metricsX = metricsService
  )

  def apply(): CachingMongoService[DesSummaryCache, DesSummary] = cacheService
}
