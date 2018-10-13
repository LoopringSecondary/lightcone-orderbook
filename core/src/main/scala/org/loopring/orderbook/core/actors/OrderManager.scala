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
import org.loopring.orderbook.core.ordermanager.OrderManagerHelper
import org.loopring.orderbook.proto.account._
import org.loopring.orderbook.proto.deployment.OrderAmountFacilitatorSettings
import org.loopring.orderbook.proto.order._

import scala.concurrent.{ ExecutionContext, Future }

class OrderManager(
  orderBookManager: ActorRef,
  accountManager: ActorRef,
  helper: OrderManagerHelper)(
  implicit
  timeout: Timeout,
  ec: ExecutionContext) extends Actor {

  var market = ""

  override def receive: Receive = {
    case s: OrderAmountFacilitatorSettings => market = s.tokenS.toLowerCase

    case e: GatewayOrderEvent => onThisShard() {
      for {
        ord <- Future.successful(e.getState.getRawOrder)
        res <- accountManager ? GetTokenAndFeeAccountReq(ord.owner, ord.tokenS, ord.feeAddr)
        orderForMatch = res match {
          case r: GetTokenAndFeeAccountRes => helper.handleOrderNew(ord, r.getToken, r.getFee)
          case _ => throw new Exception("get user account failed")
        }
      } yield orderBookManager ! orderForMatch
    }

    case e: OrderUpdateEvent => onThisShard() {
      val orderForMatch = helper.handleOrderUpdate(e)
      orderForMatch match {
        case Some(x) => orderBookManager ! orderForMatch
        case _ => throw new Exception("update order failed")
      }
    }

    case e: BalanceChangedEvent => onThisShard() {
      val event = AccountChangedEvent(token = e.token, owner = e.owner, amount = e.currentAmount, isBalance = true)
      val seq = helper.handleAccountChanged(event)
      seq.map(orderBookManager ! _)
    }

    case e: AllowanceChangedEvent => onThisShard() {
      val event = AccountChangedEvent(token = e.token, owner = e.owner, amount = e.currentAmount, isBalance = false)
      val seq = helper.handleAccountChanged(event)
      seq.map(orderBookManager ! _)
    }

  }

  // todo: how to sharding(不能光通过tokenS来分片, lrcFee&2.0后续其他的fee也要考虑)
  // todo: 最好是通过owner来分片
  private def onThisShard()(op: => Any) = {
    //    if (token.toLowerCase.equals(market)) {
    //      val result = op
    //    }
    val result = op
  }

}
