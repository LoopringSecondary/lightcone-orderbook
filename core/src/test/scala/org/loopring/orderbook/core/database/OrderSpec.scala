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

package org.loopring.orderbook.core.database

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }
import org.scalatest._
import org.loopring.orderbook.core.base._

import scala.concurrent.Await

class OrderSpec() extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  info("execute cmd [sbt core/'testOnly *ExtractorSpec'] test extractor worker")

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "getSingleOrder" should {
    info("execute cmd [sbt core/'testOnly *OrderSpec -- -z getSingleOrder'] to debug getSingleOrder")

    val orderhash = ""
    val resultFuture = database.orders.getOrder(orderhash)

    Await.result(resultFuture, timeout.duration) match {
      case Some(order) => info(order.toProtoString)
      case _ => info("cann't find any order")
    }

  }
}
