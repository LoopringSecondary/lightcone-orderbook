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

package org.loopring.orderbook.core.database

import org.loopring.orderbook.proto.deployment.{ DepthManagerSettings, MarketConfig }
import org.loopring.orderbook.proto.depth.DepthUpdateEvent
import org.loopring.orderbook.proto.order.{ OrderBeforeMatch, OrderForMatch, OrderState }
import org.loopring.orderbook.lib.math.Rational
import org.loopring.orderbook.lib.etypes._
import org.loopring.orderbook.proto.account.Account

package object actors {

  def inThisMarket(tokenS: String, tokenB: String, market: MarketConfig)(op: => Any) = {
    val ts = tokenS.toLowerCase
    val tb = tokenB.toLowerCase
    val ms = market.marketToken.toLowerCase
    val mb = market.exchangeToken.toLowerCase

    if ((ts.equals(ms) && tb.equals(mb)) || (ts.equals(mb) && tb.equals(ms))) {
      val result = op
    }
  }

  implicit class RichAddress(src: String) {

    def safe = src.toLowerCase
  }

  implicit class RichAccount(src: Account) {

    def min: BigInt = {
      if (src.allowance.asBigInt.compare(src.balance.asBigInt) < 0) {
        src.allowance.asBigInt
      } else {
        src.balance.asBigInt
      }
    }

    def max: BigInt = {
      if (src.allowance.asBigInt.compare(src.balance.asBigInt) < 0) {
        src.balance.asBigInt
      } else {
        src.allowance.asBigInt
      }
    }
  }

  implicit class RichOrderState(src: OrderState) {

    def availableAmountS(): BigInt = {
      val rawOrder = src.getRawOrder
      val totalAmountS = rawOrder.amountS.asBigInt
      val dealtAmountS = src.dealtAmountS.asBigInt.bigInteger
      val cancelAmountS = src.cancelAmountS.asBigInt.bigInteger
      val dealtAndCancelAmount = dealtAmountS.add(cancelAmountS)

      if (totalAmountS.compare(dealtAndCancelAmount) > 0) {
        BigInt(totalAmountS.bigInteger.subtract(dealtAndCancelAmount))
      } else {
        BigInt(0)
      }
    }

    def availableFee(): BigInt = {
      val rawOrder = src.getRawOrder
      val rawFee = rawOrder.fee.asRational
      val fillRate = src.dealtAmountS.asRational / Rational(rawOrder.amountS.asBigInt)
      val dealtFee = fillRate * Rational(rawOrder.fee.asBigInt)

      if (rawFee.compare(dealtFee) > 0) {
        (rawFee - dealtFee).bigintValue()
      } else {
        BigInt(0)
      }
    }

  }

  implicit class RichDepthUpdateEvent(src: DepthUpdateEvent) {

    def getPrice: Rational = {
      Rational.apply(src.amountS.asBigInt, src.amountB.asBigInt)
    }

    // lrc-weth sell lrc, buy weth, ask == sell
    def isAsk(market: MarketConfig): Boolean = {
      src.tokenS.toLowerCase.equals(market.exchangeToken.toLowerCase)
    }

  }

  implicit class RichOrderForMatch(src: OrderForMatch) {

    def tokenIsFee: Boolean = if (src.getRawOrder.tokenS.safe.equals(src.feeAddress)) {
      true
    } else {
      false
    }
  }

}
