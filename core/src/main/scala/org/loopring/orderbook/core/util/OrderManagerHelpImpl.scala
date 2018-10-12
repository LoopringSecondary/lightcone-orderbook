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

import org.loopring.orderbook.proto.account.Account
import org.loopring.orderbook.proto.order._
import org.loopring.orderbook.lib.etypes._

class OrderManagerHelpImpl()
  extends OrderManagerHelper {

  def handleNewOrder(): OrderForMatch = null

  def handleOrderFill(): OrderForMatch = null

  def handleOrderCancel(): OrderForMatch = null

  def getOrderForMatchWithoutFee(state: OrderState, account: Account, feeAccount: Account): OrderForMatch = {
    var matchType = OrderForMatchType.ORDER_UPDATE
    val orderAvailableAmountS = state.availableAmountS()
    val accountAvailableAmount = account.min
    val availableAmountS = if (accountAvailableAmount.compare(orderAvailableAmountS) > 0) {
      orderAvailableAmountS
    } else {
      accountAvailableAmount
    }

    val orderAvailableFee = state.availableFee()
    val accountAvailableFee = feeAccount.min
    val availableFee = if (accountAvailableFee.compare(orderAvailableFee) > 0) {
      orderAvailableFee
    } else {
      accountAvailableFee
    }

    OrderForMatch(
      rawOrder = state.rawOrder,
      feeAddress = state.getRawOrder.feeAddr.safe,
      availableAmountS = availableAmountS.toString,
      availableFee = availableFee.toString(),
      matchType = matchType)
  }

  def getOrderForMatchWithFee(state: OrderState, account: Account): OrderForMatch = {
    var matchType = OrderForMatchType.ORDER_UPDATE
    val orderAvailableFee = state.availableFee()
    val accountAvailableFee = account.min
    val availableFee = if (accountAvailableFee.compare(orderAvailableFee) > 0) {
      orderAvailableFee
    } else {
      accountAvailableFee
    }

    val orderAvailableAmountS = state.availableAmountS()
    val accountAvailableAmount = account.min
    val availableAmountS = if (accountAvailableAmount.compare(orderAvailableAmountS) > 0) {
      orderAvailableAmountS
    } else {
      accountAvailableAmount
    }

    OrderForMatch(
      rawOrder = state.rawOrder,
      feeAddress = state.getRawOrder.feeAddr.safe,
      availableAmountS = availableAmountS.toString,
      availableFee = availableFee.toString(),
      matchType = matchType)
  }

  def isOrderFinished(availableAmount: BigInt): Boolean = {
    availableAmount.compare(BigInt(0)) <= 0
  }

}
