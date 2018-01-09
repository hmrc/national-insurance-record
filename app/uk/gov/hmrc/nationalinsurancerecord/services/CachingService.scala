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

package uk.gov.hmrc.nationalinsurancerecord.services

import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.libs.json.{Format, Json, OFormat, Reads}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nationalinsurancerecord.domain.APITypes.APITypes
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{DefaultDB, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.nationalinsurancerecord.config.ApplicationConfig

import scala.concurrent.{ExecutionContext, Future}
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
(formats: Format[A], apply: (String, B, DateTime) => A, apiType: APITypes, appConfig: ApplicationConfig, metricsX: MetricsService)
(implicit mongo: () => DefaultDB, m: Manifest[A], e: ExecutionContext)
  extends ReactiveRepository[A, BSONObjectID]("responses", mongo, formats)
    with CachingService[A, B] {

  val ttlfieldName = "expiresAt"
  val ttlIndexName = "responseExpiry"
  val expireAfterSeconds = "expireAfterSeconds"
  override val timeToLive = appConfig.responseCacheTTL

  Logger.info(s"Document expiresAt will be set to $timeToLive")
  createIndex(ttlfieldName, ttlIndexName, Some(0), uniqueField = false)

  val uniqueFieldName = "key"
  val uniqueIndexName = "responseUniqueKey"
  val unique = "unique"
  createIndex(uniqueFieldName, uniqueIndexName, None, uniqueField = true)

  private def createIndex(field: String, indexName: String, ttl: Option[Int], uniqueField: Boolean)(implicit e: ExecutionContext): Future[Boolean] = {

    val ttlOption = ttl.fold(BSONDocument())(time => BSONDocument(expireAfterSeconds -> time))
    val options = if(uniqueField) ttlOption.add(unique -> true) else ttlOption

    collection.indexesManager.ensure(Index(Seq((field, IndexType.Ascending)), Some(indexName),
      options = options)) map {
      result => {
        // $COVERAGE-OFF$
        Logger.debug(s"set [$indexName] with ttl $ttl and unique $uniqueField -> result : $result")
        if(result) Logger.info(s"Successfully created $indexName")
        // $COVERAGE-ON$
        result
      }
    } recover {
      // $COVERAGE-OFF$
      case ex => Logger.error(s"Failed to set $indexName index", ex)
        false
      // $COVERAGE-ON$
    }
  }

  private def cacheKey(nino: Nino, api: APITypes) = s"$nino-$api"


  override def findByNino(nino: Nino)(implicit formats: Reads[A], e: ExecutionContext): Future[Option[B]] = {
    val tryResult = Try {
      metrics.cacheRead()
      collection.find(Json.obj("key" -> cacheKey(nino, apiType))).cursor[A](ReadPreference.primary).collect[List]()
    }

    tryResult match {
      case Success(success) => {
        success.map { results =>
          Logger.debug(s"[$apiType][findByNino] : { cacheKey : ${cacheKey(nino, apiType)}, result: $results }")
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
          case e: Throwable => Logger.warn(s"[$apiType][findByNino] : { cacheKey : ${cacheKey(nino, apiType)}, exception: ${e.getMessage} }", e); None
        }
      }
      case Failure(f) =>
        Logger.debug(s"[$apiType][findByNino] : { cacheKey : ${cacheKey(nino, apiType)}, exception: ${f.getMessage} }")
        metrics.cacheReadNotFound()
        Future.successful(None)
    }
  }

  override def insertByNino(nino: Nino, response: B)
                           (implicit formats: OFormat[A], e: ExecutionContext): Future[Boolean] = {
    val query = Json.obj("key" -> cacheKey(nino, apiType))
    val doc = apply(cacheKey(nino, apiType), response, DateTime.now(DateTimeZone.UTC).plusSeconds(timeToLive))

    collection.update(query, doc, upsert = true).map { result =>
      Logger.debug(s"[$apiType][insertByNino] : { cacheKey : ${cacheKey(nino, apiType)}, " +
        s"request: $response, result: ${result.ok}, errors: ${result.errmsg} }")
      metrics.cacheWritten()
      result.errmsg.foreach(msg => Logger.warn(s"[$apiType][insertByNino] : { cacheKey : ${cacheKey(nino, apiType)}, results: $msg }"))
      result.ok
    } recover {
      case e: Throwable => Logger.warn(s"[$apiType][insertByNino] : cacheKey : ${cacheKey(nino, apiType)}, exception: ${e.getMessage} }", e); false
    }
  }

  override def metrics: MetricsService = metricsX
}
