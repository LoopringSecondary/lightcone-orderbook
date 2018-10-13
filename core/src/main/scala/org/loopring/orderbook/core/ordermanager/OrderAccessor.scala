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

package org.loopring.orderbook.core.ordermanager

import org.loopring.orderbook.proto.order._
import org.loopring.orderbook.lib.time.TimeProvider
import org.loopring.orderbook.lib.etypes._

import scala.collection.mutable

class OrderAccessor(timeProvider: TimeProvider) {

  // map[orderhash, orderState]
  var ordermap = mutable.HashMap.empty[String, OrderState]

  def getSingleOrder(orderhash: String): Option[OrderState] = this.synchronized {
    ordermap.get(safekey(orderhash))
  }

  def getSortedOrder(seq: Seq[String]): mutable.SortedMap[Long, OrderState] = this.synchronized {
    var sortedmap = mutable.SortedMap.empty[Long, OrderState]
    seq.map(x => {
      ordermap.get(x) match {
        case Some(o) => sortedmap += o.createdAt -> o
        case _ => throw new Exception("get order failed")
      }
    })
    sortedmap
  }

  def ord2State(ord: RawOrder): OrderState = {
    OrderState(
      rawOrder = Option(ord),
      dealtAmountS = zeroAmount.toString,
      dealtAmountB = zeroAmount.toString,
      cancelAmountS = zeroAmount.toString,
      cancelAmountB = zeroAmount.toString,
      createdAt = timeProvider.getTimeMillis)
  }

  def add(state: OrderState): Unit = this.synchronized {
    val orderhash = state.getRawOrder.hash
    ordermap += safekey(orderhash) -> state
  }

  def del(orderhash: String): Unit = this.synchronized {
    val key = safekey(orderhash)
    require(ordermap.contains(key))
    ordermap -= key
  }

  def update(state: OrderState): Unit = this.synchronized {
    val key = safekey(state.getRawOrder.hash)
    require(ordermap.contains(key))
    ordermap += key -> state
  }

  private def safekey(orderhash: String) = orderhash.safe
}
