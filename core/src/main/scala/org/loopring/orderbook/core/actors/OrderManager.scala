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
import scala.concurrent.{ ExecutionContext, Future }

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

  // todo 对同一个用户地址的订单进行聚合，订单频率是否需要控制，create_time最小单位是ms/s?
  // hashmap[string, sortMap[create_time, orderBeforeMatch]]
  var ordermap = mutable.HashMap.empty[String, SortedMap[Long, OrderBeforeMatch]]
  override def receive: Receive = {
    case s: OrderAmountFacilitatorSettings => market = s.tokenS.toLowerCase

    case e: GatewayOrderEvent =>
      for {
        ord <- Future.successful(e.getState.getRawOrder)
        res <- accountManager ? GetTokenAndFeeAccountReq(ord.owner, ord.tokenS, ord.feeAddr)
        orderBeforeMatch = res match {
          case r: GetTokenAndFeeAccountRes => handleOrderNew(e.getState, r.token.get, r.fee.get)
          case _ => throw new Exception("get user account failed")
        }
        orderForMatch = helper.getOrderForMatch(orderBeforeMatch)
        _ = updateOrderMap(orderBeforeMatch, orderForMatch.matchType)
      } yield orderBookManager ! orderForMatch

    case e: OrderUpdateEvent => onThisActor() {

    }

    case e: BalanceChangedEvent => onThisActor() {

    }

    case e: AllowanceChangedEvent => onThisActor() {

    }
  }

  // todo save into ordermap
  def handleOrderNew(ord: OrderState, account: Account, feeAccount: Account) = OrderBeforeMatch(
    state = Option(ord.copy(dealtAmountS = BigInt(0).toString, cancelAmountS = BigInt(0).toString())),
    tokenSBalance = account.balance.toString,
    tokenSAllowance = account.allowance.toString,
    feeBalance = feeAccount.balance.toString,
    feeAllowance = feeAccount.allowance.toString)

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

  def handleAccountUpdate(ordmap: SortedMap[Long, OrderBeforeMatch], amount: BigInt, isBalance: Boolean): Seq[OrderForMatch] = {
    ordmap.map(x => {
      val state = x._2.getState
      val orderBeforeMatch = if (isBalance && state.tokenIsFee()) {
        x._2.copy(tokenSBalance = amount.toString, feeBalance = amount.toString)
      } else if (isBalance && state.tokenNotFee) {
        x._2.copy(tokenSBalance = amount.toString)
      } else if (!isBalance && state.tokenIsFee()) {
        x._2.copy(tokenSAllowance = amount.toString, feeAllowance = amount.toString)
      } else {
        x._2.copy(tokenSAllowance = amount.toString)
      }
      helper.getOrderForMatch(orderBeforeMatch)
    }).toSeq
  }

  def updateOrderMap(ord: OrderBeforeMatch, typ: OrderForMatchType): Unit =
    this.synchronized {
      val state = ord.getState
      val rawOrder = ord.getState.getRawOrder
      val key = getKey(rawOrder.owner, rawOrder.tokenS)

      typ match {
        case OrderForMatchType.ORDER_NEW =>
          var map = this.ordermap.getOrElse(key, SortedMap.empty[Long, OrderBeforeMatch])
          map += state.createdAt -> ord
          this.ordermap += key -> map

        case OrderForMatchType.ORDER_UPDATE =>
          require(this.ordermap.contains(key))
          var map = this.ordermap.getOrElse(key, SortedMap.empty[Long, OrderBeforeMatch])
          require(map.contains(state.createdAt))
          map += state.createdAt -> ord
          this.ordermap += key -> map

        case OrderForMatchType.ORDER_REM =>
          require(this.ordermap.contains(key))
          var map = this.ordermap.getOrElse(key, SortedMap.empty[Long, OrderBeforeMatch])
          require(map.contains(state.createdAt))
          map -= state.createdAt
          if (map.size.equals(0)) {
            this.ordermap -= key
          } else {
            this.ordermap += key -> map
          }
      }
    }

  // todo: how to sharding(不能光通过tokenS来分片, lrcFee&2.0后续其他的fee也要考虑)
  private def onThisActor()(op: => Any) = {
    //    if (token.toLowerCase.equals(market)) {
    //      val result = op
    //    }
    val result = op
  }

  private def getKey(owner: String, token: String) = owner.safe + "-" + token.safe

}
