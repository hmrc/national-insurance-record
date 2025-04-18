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

package uk.gov.hmrc.nationalinsurancerecord.services

import org.mongodb.scala.ReadPreference
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{FindOneAndReplaceOptions, IndexModel, IndexOptions, Indexes}
import play.api.Logging
import play.api.libs.json.{Format, OFormat, Reads}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.nationalinsurancerecord.config.ApplicationConfig
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes.APITypes

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

import org.mongodb.scala.gridfs.ObservableFuture

trait CachingModel[A, B] {
  val key: String
  val response: B
  val expiresAt: Instant
}

trait CachingService[A, B] {
  def findByNino(nino: Nino)(implicit formats: Reads[A], e: ExecutionContext): Future[Option[B]]
  def insertByNino(nino: Nino, response: B)(implicit formatA: OFormat[A], e: ExecutionContext): Future[Boolean]
  val timeToLive: Int

  def metrics: MetricsService
}
@Singleton
class CachingMongoService[A <: CachingModel[A, B], B](
                                                       mongo: MongoComponent,
                                                       formats: Format[A],
                                                       apply: (String, B, Instant) => A,
                                                       apiType: APITypes,
                                                       appConfig: ApplicationConfig,
                                                       metricsX: MetricsService
                                                     )(
                                                       implicit ec: ExecutionContext,
                                                       ct: ClassTag[A]
                                                     ) extends PlayMongoRepository[A](
  mongoComponent = mongo,
  collectionName = appConfig.responseCacheCollectionName,
  domainFormat = formats,
  replaceIndexes = true,
  indexes = Seq(
    IndexModel(
      Indexes.ascending("expiresAt"),
      IndexOptions()
        .name("responseExpiry")
        .unique(false)
        .expireAfter(60, TimeUnit.MINUTES)
    ),
    IndexModel(
      Indexes.ascending("key"),
      IndexOptions()
        .name("responseUniqueKey")
        .unique(true)
        .expireAfter(60, TimeUnit.MINUTES)
    )
  )
) with CachingService[A, B] with Logging {

  private def cacheKey(nino: Nino, api: APITypes) = s"$nino-$api"
  override val timeToLive: Int = appConfig.responseCacheTTL

  override def findByNino(nino: Nino)(implicit formats: Reads[A], ec: ExecutionContext): Future[Option[B]] = {
    val tryResult = Try {
      metrics.cacheRead()
      collection.withReadPreference(ReadPreference.primaryPreferred()).find(equal("key", cacheKey(nino, apiType))).toFuture()
    }

    tryResult match {
      case Success(success) =>
        success.map { results =>
          logger.info(s"[$apiType][findByNino] : { cacheKey : ${cacheKey(nino, apiType)}, result: $results }")
          val response = results.headOption.map(_.response)
          response match {
            case Some(data) =>
              metrics.cacheReadFound()
              Some(data)
            case None =>
              metrics.cacheReadNotFound()
              None
          }
        } recover {
          case e: Throwable =>
            logger.warn(s"[$apiType][findByNino] : { cacheKey : ${cacheKey(nino, apiType)}, exception: ${e.getMessage} }", e)
            None
        }
      case Failure(f) =>
        logger.info(s"[$apiType][findByNino] : { cacheKey : ${cacheKey(nino, apiType)}, exception: ${f.getMessage} }")
        metrics.cacheReadNotFound()
        Future.successful(None)
    }
  }

  override def insertByNino(nino: Nino, response: B)
                           (implicit formatA: OFormat[A], ec: ExecutionContext): Future[Boolean] = {
    val query = equal("key", cacheKey(nino, apiType))

    val doc = apply(cacheKey(nino, apiType), response, Instant.now().plusSeconds(timeToLive))

    collection.findOneAndReplace(query, doc, FindOneAndReplaceOptions().upsert(true)).toFuture().map { _ =>
      metrics.cacheWritten()
      true
    } recover {
      case e: Throwable =>
        logger.warn(s"[$apiType][insertByNino] : cacheKey : ${cacheKey(nino, apiType)}, exception: ${e.getMessage} }", e)
        false
    }
  }

  override def metrics: MetricsService = metricsX
}