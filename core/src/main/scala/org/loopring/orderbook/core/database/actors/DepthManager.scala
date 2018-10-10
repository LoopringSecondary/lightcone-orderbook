/*

  Copyright 2017 Loopring Project Ltd (Loopring Foundation).

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

*/

package org.loopring.orderbook.core.database.actors

import akka.actor.Actor
import org.loopring.orderbook.lib.math.Rational
import org.loopring.orderbook.lib.etypes._
import org.loopring.orderbook.proto.depth._

import scala.collection.mutable
import scala.collection.SortedMap

// 依赖orderbook
// 初始化: 从orderBook获取priceIndex
class DepthManager extends Actor {

  val numOfOrderBookToKeep = 100000
  var market = SetMarket()

  var asks = SortedMap.empty[Double, Entry] // sell
  var bids = SortedMap.empty[Double, Entry] // buy

  override def receive: Receive = {
    case s: SetMarket => market = s

    case s: DepthUpdateEvent =>
      inThisMarket(s.tokenS, s.tokenB, market) {
        s.isAsk(market) match {
          case true =>
          case false =>
        }

      }
  }

  def initAsks() = {

  }

  def initBids = {

  }

}
