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

package uk.gov.hmrc.nationalinsurancerecord.services

import com.mongodb.client.model.Updates
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.ReadPreference
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexModel, IndexOptions, Indexes}
import play.api.Logging
import play.api.libs.json.{Format, OFormat, Reads}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.nationalinsurancerecord.config.ApplicationConfig
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes.APITypes

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

trait CachingModel[A, B] {
  val key: String
  val response: B
  val expiresAt: DateTime
}

trait CachingService[A, B] {
  def findByNino(nino: Nino)(implicit formats: Reads[A], e: ExecutionContext): Future[Option[B]]
  def insertByNino(nino: Nino, response: B)(implicit formats: OFormat[A], e: ExecutionContext): Future[Boolean]
  val timeToLive: Int

  def metrics: MetricsService
}

class CachingMongoService[A <: CachingModel[A, B], B]
(mongo: MongoComponent, formats: Format[A], apply: (String, B, DateTime) => A, apiType: APITypes, appConfig: ApplicationConfig, metricsX: MetricsService)
(implicit e: ExecutionContext, ct: ClassTag[A])
  extends PlayMongoRepository[A](
    mongoComponent = mongo,
    collectionName = appConfig.responseCacheCollectionName,
    domainFormat = formats,
    indexes = Seq(
      IndexModel(Indexes.ascending("expiresAt"), IndexOptions().name("responseExpiry").unique(false)),
      IndexModel(Indexes.ascending("key"), IndexOptions().name("responseUniqueKey").unique(true))
    )
  ) with CachingService[A, B] with Logging {

  private def cacheKey(nino: Nino, api: APITypes) = s"$nino-$api"
  override val timeToLive = appConfig.responseCacheTTL

  override def findByNino(nino: Nino)(implicit formats: Reads[A], e: ExecutionContext): Future[Option[B]] = {
    val tryResult = Try {
      metrics.cacheRead()
      collection.withReadPreference(ReadPreference.primaryPreferred()).find(equal("key", cacheKey(nino, apiType))).toFuture()
    }

    tryResult match {
      case Success(success) => {
        success.map { results =>
          logger.debug(s"[$apiType][findByNino] : { cacheKey : ${cacheKey(nino, apiType)}, result: $results }")
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
          case e: Throwable => logger.warn(s"[$apiType][findByNino] : { cacheKey : ${cacheKey(nino, apiType)}, exception: ${e.getMessage} }", e); None
        }
      }
      case Failure(f) =>
        logger.debug(s"[$apiType][findByNino] : { cacheKey : ${cacheKey(nino, apiType)}, exception: ${f.getMessage} }")
        metrics.cacheReadNotFound()
        Future.successful(None)
    }
  }

  override def insertByNino(nino: Nino, response: B)
                           (implicit formats: OFormat[A], e: ExecutionContext): Future[Boolean] = {
    val query = equal("key", cacheKey(nino, apiType))
    val doc = Updates.combine(
      Updates.set("key", cacheKey(nino, apiType)),
      Updates.set("response", response),
      Updates.set("expiresAt", DateTime.now(DateTimeZone.UTC).plusSeconds(timeToLive))
    )

    collection.findOneAndUpdate(query, doc, FindOneAndUpdateOptions().upsert(true)).toFuture().map { result =>
//      logger.debug(s"[$apiType][insertByNino] : { cacheKey : ${cacheKey(nino, apiType)}, " +
//        s"request: $response, result: ${result.ok}, errors: ${result.errmsg} }")
      metrics.cacheWritten()
      true
    } recover {
      case e: Throwable => logger.warn(s"[$apiType][insertByNino] : cacheKey : ${cacheKey(nino, apiType)}, exception: ${e.getMessage} }", e); false
    }
  }

  override def metrics: MetricsService = metricsX
}
