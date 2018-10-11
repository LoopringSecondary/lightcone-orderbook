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

import akka.actor._
import akka.util.Timeout
import org.loopring.orderbook.lib.etypes._
import org.loopring.orderbook.proto.order._
import org.loopring.orderbook.proto.deployment.OrderAmountFacilitatorSettings
import org.loopring.orderbook.proto.account._

import scala.collection.{SortedMap, mutable}
import scala.concurrent.ExecutionContext

// 1.所有非终态订单存储到内存
// 2.接收balanceManager的balanceUpdate,allowanceUpdate事件并更新订单可用余额
// 3.发送信息给OrderBookManager
// 4.只管理订单tokenS

class OrderManager(orderBookManager: ActorRef)(
  implicit
  timeout: Timeout,
  ec: ExecutionContext) extends Actor {

  var market = ""

  // 对同一个用户地址的订单进行聚合，
  // hashmap[string, sortMap[create_time, orderBeforeMatch]]
  var ordermap = mutable.HashMap.empty[String, SortedMap[Long, OrderBeforeMatch]]
  override def receive: Receive = {
    case s: OrderAmountFacilitatorSettings => market = s.tokenS.toLowerCase

    case o: OrderState => addNewOrder(o)

    case u: OrderUpdateEvent =>

    case e: AllowanceChangedEvent =>
      onThisActor() {
        ordermap.get(e.token.safe) match {
          case Some(ord) =>
            orderBookManager ! notify(ord, e)
          case _ =>
        }
      }

    case e: BalanceChangedEvent =>
      onThisActor() {
        ordermap.get(e.token.safe) match {
          case Some(ord) =>
            orderBookManager ! notify(ord, e)
          case _ =>
        }
      }
  }

  // orderState已从链上获取成交量取消量
  // todo 从accountManager获取balance&allowance
  private def addNewOrder(order: OrderState) = {
    if (ordermap.contains(order.getRawOrder.owner.safe)) {

    } else {
      var sortedMap = SortedMap.empty[Long, OrderBeforeMatch]
      val orderBeforeMatch = OrderBeforeMatch()
    }
  }

  private def notify(ord: OrderBeforeMatch, event: AllowanceChangedEvent): OrderMatchNotifyEvent = {
    val available = ord.getMatch.availableAmountS.asBigInt
    val allowance = ord.allowance.asBigInt
    val mord = ord.getMatch.copy(availableAmountS = event.currentAmount, )

    if (allowance.compare(available) < 0) {
      OrderMatchNotifyEvent().withMatch(ord.getMatch)
    } else {
      OrderMatchNotifyEvent()
    }
  }

  private def notify(ord: OrderBeforeMatch, event: BalanceChangedEvent): OrderMatchNotifyEvent = {

  }

  // 优先保证lrcfee
  // 1.订单处于完成状态则type为rem
  // 2.订单dealt&cancel都为0则type为new
  // 3.订单fill时为sub
  // 4.balance增加时，如果fee不够则补全OrderForMatch.fee, 剩余的如果超过当前available量则补全
  private def calculate(orderBeforeMatch: OrderBeforeMatch): OrderMatchNotifyEvent = {
  }

  // todo: how to sharding(不能光通过tokenS来分片, lrcFee&2.0后续其他的fee也要考虑)
  private def onThisActor()(op: => Any) = {
    //    if (token.toLowerCase.equals(market)) {
    //      val result = op
    //    }
    val result = op
  }

  private def afterDealtAndCancelAmount(orderBeforeMatch: OrderBeforeMatch): BigInt = {
    val rawOrder = orderBeforeMatch.getMatch.getRawOrder
    val totalAmountS = rawOrder.amountS.asBigInt
    val dealtAmountS = orderBeforeMatch.dealtAmountS.asBigInt.bigInteger
    val cancelAmountS = orderBeforeMatch.cancelAmountS.asBigInt.bigInteger
    val dealtAndCancelAmount = dealtAmountS.add(cancelAmountS)

    if (totalAmountS.compare(dealtAndCancelAmount) > 0) {
      BigInt(totalAmountS.bigInteger.subtract(dealtAndCancelAmount))
    } else {
      BigInt(0)
    }
  }

  // todo 应该根据marketCap计算订单状态,这里暂时替代下
  private def isOrderFinished(availableAmount: BigInt): Boolean = {
    availableAmount.compare(BigInt(0)) <= 0
  }
}
