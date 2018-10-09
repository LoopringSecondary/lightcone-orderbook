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
import org.loopring.orderbook.proto.order.{ Order, RawOrder }
import org.loopring.orderbook.lib.etypes._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class OrderWithStatus(order: Order, deferredTime: Long)

class OrderBook {
  var ordersOfPrice = mutable.TreeMap[Rational, Set[String]]()
  var orders = mutable.HashMap[String, OrderWithStatus]()
  var delayedMap = mutable.TreeMap[Rational, String]()
  var settlingOrders = Map[String, Int]()
  var validSinceOrders = Set[String]()

  def updateOrAddOrder(orderWithStatus: OrderWithStatus): Unit = {
    val now = System.currentTimeMillis() / 1e6
    val rawOrder = orderWithStatus.order.rawOrder.get
    if (rawOrder.validUntil <= now) {
      return
    }
    val sellPrice = Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt)
    if (rawOrder.validSince > now) {
      validSinceOrders.synchronized(validSinceOrders + rawOrder.hash.toLowerCase)
    } else if (rawOrder.validUntil <= now) {
      ordersOfPrice.synchronized {
        var orderHashes = ordersOfPrice.getOrElse(sellPrice, Set[String]())
        orderHashes = orderHashes + rawOrder.hash
        ordersOfPrice.put(sellPrice, orderHashes)
      }
    }
    orders.synchronized(orders.put(rawOrder.hash.toLowerCase, orderWithStatus))
  }

  def delOrder(rawOrder: RawOrder) = {
    orders.synchronized(orders.remove(rawOrder.hash.toLowerCase))
    val sellPrice = Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt)
    ordersOfPrice.synchronized {
      var orderHashes = ordersOfPrice.getOrElse(sellPrice, Set[String]())
      orderHashes = orderHashes.filter(_ != rawOrder.hash.toLowerCase)
      ordersOfPrice.put(sellPrice, orderHashes)
    }
  }

  def getOrder(hash: String) = {
    this.orders(hash.toLowerCase).order
  }

}

class OrderBookManager(tokenS: String, tokenB: String) extends Actor {
  var orderBook = new OrderBook()

  override def receive: Receive = {
    case order: Order ⇒
  }
}
