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

  var orderhashSet = mutable.Set[String]()
  var asks = SortedMap.empty[Double, Entry] // sell
  var bids = SortedMap.empty[Double, Entry] // buy

  override def receive: Receive = {
    case s: SetMarket => market = s

    case s: OrderAddEvent =>
      inThisMarket(s.getOrder.tokenS, s.getOrder.tokenB, market) {
        add(s)
      }

    case s: OrderDelEvent =>
      inThisMarket(s.getOrder.tokenS, s.getOrder.tokenB, market) {
        del(s)
      }

    case s: OrderUpdateEvent =>
      inThisMarket(s.getOrder.tokenS, s.getOrder.tokenB, market) {
        update(s)
      }
  }

  def initAsks() = {

  }

  def initBids = {

  }

  private def add(event: OrderAddEvent) = {
    val order = event.getOrder
    if (order.isAsk(market)) {

    } else {

    }
  }

  private def del(event: OrderDelEvent) = {
    val ord = event.getOrder
    ord.isAsk(market) match {
      case true => calculate(ord.getPrice.doubleValue(), ord.availableAmountS.asBigInt, true, false)
      case false => calculate(ord.getPrice.doubleValue(), ord.availableAmountB.asBigInt, false, false)
    }
  }

  private def update(event: OrderUpdateEvent) = {
    val ord = event.getOrder
    ord.isAsk(market) match {
      case true => calculate()
      case false =>
    }
  }

  private def calculate(orderhash: String, price: Double, amount: BigInt, isAsk: Boolean, isAdd: Boolean) = {
    var dest = if(isAsk) {
      asks
    } else {
      bids
    }

    val exist = dest.getOrElse(price, Entry(price, 0, BigInt(0).toString))
    val (calAmount, calSize) = if(isAdd) {
      (
        exist.amount.asBigInteger.add(amount.bigInteger),
        exist.size + 1
      )
    } else {
      (
        exist.amount.asBigInteger.subtract(amount.bigInteger),
        exist.size - 1
      )
    }

    orderhashSet.contains(orderhash.toLowerCase)

    if (calSize <= 0 || calAmount.compareTo(BigInt(0)) <= 0) {
      dest -= price
    } else {
      if (dest.size >= numOfOrderBookToKeep) dest = dest.drop(1)
      dest += price -> exist.copy(price, calSize, calAmount.toString)
    }

    if(isAsk) {
      asks = dest
    } else {
      bids = dest
    }
  }

}
