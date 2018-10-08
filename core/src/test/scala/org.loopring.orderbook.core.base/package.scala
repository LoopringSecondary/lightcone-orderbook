/*
 * Copyright 2018 Loopring Foundation
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

package org.loopring.orderbook.core

import java.util.concurrent.ForkJoinPool

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.loopring.orderbook.core.database.MysqlOrderDatabase
import org.loopring.orderbook.lib.time.LocalSystemTimeProvider
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext
import scala.concurrent.Promise
import scala.concurrent.duration._

package object base {
  implicit val system = ActorSystem()

  implicit val timeout = Timeout(10 seconds)

  val config = ConfigFactory.defaultApplication()

  val httpFlow = Http().cachedHostConnectionPool[Promise[HttpResponse]](
    host = config.getString("ethereum.host"),
    port = config.getInt("ethereum.port"))

  val timeProvider = new LocalSystemTimeProvider
  val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("db.default", config)
  println(dbConfig.config.atKey("db.url"))
  val context = ExecutionContext.fromExecutor(ForkJoinPool.commonPool())
  val database = new MysqlOrderDatabase(dbConfig, timeProvider, context)

  database.generateDDL()
}
