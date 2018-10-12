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

package org.loopring.orderbook.core.actors

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import org.loopring.orderbook.core.util._
import org.loopring.orderbook.lib.etypes._
import org.loopring.orderbook.proto.account._
import org.loopring.orderbook.proto.deployment.OrderAmountFacilitatorSettings
import org.loopring.orderbook.proto.order._

import scala.collection.{ SortedMap, mutable }
import scala.concurrent.ExecutionContext

// 1.所有非终态订单存储到内存
// 2.接收balanceManager的balanceUpdate,allowanceUpdate事件并更新订单可用余额
// 3.发送信息给OrderBookManager
// 4.只管理订单tokenS

class OrderManager(
  orderBookManager: ActorRef,
  accountManager: ActorRef,
  helper: OrderManagerHelper)(
  implicit
  timeout: Timeout,
  ec: ExecutionContext) extends Actor {

  var market = ""

  // 对同一个用户地址的订单进行聚合，
  // hashmap[string, sortMap[create_time, orderBeforeMatch]]
  var ordermap = mutable.HashMap.empty[String, SortedMap[Long, OrderBeforeMatch]]
  override def receive: Receive = {
    case s: OrderAmountFacilitatorSettings => market = s.tokenS.toLowerCase
  }

  // todo save into ordermap
  def handleOrderNew(ord: OrderState, account: Account, feeAccount: Account): OrderForMatch = {
    val state = ord.copy(dealtAmountS = BigInt(0).toString, cancelAmountS = BigInt(0).toString())

    val orderBeforeMatch = OrderBeforeMatch(
      state = Option(state),
      tokenSBalance = account.balance.toString,
      tokenSAllowance = account.allowance.toString,
      feeBalance = feeAccount.balance.toString,
      feeAllowance = feeAccount.allowance.toString)

    helper.getOrderForMatch(orderBeforeMatch)
  }

  def handleOrderFill(ord: OrderBeforeMatch, dealtAmountS: BigInt): OrderForMatch = {
    val amount = ord.getState.dealtAmountS.asBigInt.+(dealtAmountS)
    val state = ord.getState.copy(dealtAmountS = amount.toString)
    helper.getOrderForMatch(ord.copy(state = Option(state)))
  }

  def handleOrderCancel(ord: OrderBeforeMatch, cancelAmountS: BigInt): OrderForMatch = {
    val amount = ord.getState.cancelAmountS.asBigInt.+(cancelAmountS)
    val state = ord.getState.copy(dealtAmountS = amount.toString)
    helper.getOrderForMatch(ord.copy(state = Option(state)))
  }

  def handleOrderCutoff(ord: OrderBeforeMatch) = OrderForMatch(
    rawOrder = ord.getState.rawOrder,
    feeAddress = ord.getState.getRawOrder.feeAddr.safe,
    availableAmountS = BigInt(0).toString(),
    availableFee = BigInt(0).toString(),
    matchType = OrderForMatchType.ORDER_REM)

  // todo
  def handleAccountUpdate(ordmap: SortedMap[Long, OrderBeforeMatch]): Seq[OrderForMatch] = Seq()

  // todo: how to sharding(不能光通过tokenS来分片, lrcFee&2.0后续其他的fee也要考虑)
  private def onThisActor()(op: => Any) = {
    //    if (token.toLowerCase.equals(market)) {
    //      val result = op
    //    }
    val result = op
  }

  private def key(owner: String, token: String) = owner.safe + "-" + token.safe

}
