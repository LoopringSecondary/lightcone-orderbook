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

package org.loopring.orderbook.core.util

import org.loopring.orderbook.proto.order._
import org.loopring.orderbook.lib.etypes._

class OrderManagerHelpImpl()
  extends OrderManagerHelper {

  def getOrderForMatch(ord: OrderBeforeMatch): OrderForMatch = {
    val state = ord.getState
    val orderType = if (state.dealtAndCancelAmountS.compare(BigInt(0)) == 0) {
      OrderForMatchType.ORDER_NEW
    } else if (fundInsufficient(ord.accountAvailableAmount, ord.orderAvailableAmount)) {
      OrderForMatchType.ORDER_REM
    } else {
      OrderForMatchType.ORDER_UPDATE
    }

    val availableAmountS = ord.orderAvailableAmount.min(ord.accountAvailableAmount)

    OrderForMatch(
      rawOrder = state.rawOrder,
      feeAddress = state.getRawOrder.feeAddr.safe,
      availableAmountS = availableAmountS.toString,
      availableFee = ord.feeAvailableAmount.toString,
      matchType = orderType)
  }

  // allowance/balance不足
  // @param accountAvailableAmount: BigInt 可用min(余额/授权)
  // @param orderAvailableAmount: BigInt 订单未成交量
  // @return boolean, true->不足, false->充足
  def fundInsufficient(accountAvailableAmount: BigInt, orderAvailableAmount: BigInt): Boolean = {
    if (accountAvailableAmount.compare(orderAvailableAmount) > 0) {
      dustAmountS(orderAvailableAmount)
    } else {
      true
    }
  }

  // todo
  def dustAmountS(availableAmount: BigInt): Boolean = {
    availableAmount.compare(BigInt(0)) <= 0
  }

}
