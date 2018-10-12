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

  def getOrderBeforeMatchWithoutFee(state: OrderState, account: Account, feeAccount: Account) = OrderBeforeMatch(
    state = Option(state),
    orderAvailableAmount = state.availableAmountS().toString,
    accountAvailableAmount = account.min.toString,
    feeAvailableAmount = feeAccount.min.toString)

  def getOrderBeforeMatchWithFee(state: OrderState, feeAccount: Account): OrderBeforeMatch = {
    val availableFee = state.availableFee().min(feeAccount.min)

    val availableAmountS = if (availableFee.compare(state.availableAmountS()) > 0) {
      availableFee.-(state.availableAmountS())
    } else {
      BigInt(0)
    }

    OrderBeforeMatch(
      state = Option(state),
      orderAvailableAmount = state.availableAmountS().toString,
      accountAvailableAmount = availableAmountS.toString(),
      feeAvailableAmount = availableFee.toString())
  }

  def updateOrderBeforeMatchWithTrade(src: OrderBeforeMatch, dealtCancelAmount: BigInt): OrderBeforeMatch = {

  }

  def updateOrderBeforeMatchWithAccount(src: OrderBeforeMatch, account: Account): OrderBeforeMatch = {

  }

  def getOrderForMatch(ord: OrderBeforeMatch): OrderForMatch = {
    val orderType = if (ord.getState.dealtAndCancelAmountS.compare(BigInt(0)) == 0) {
      OrderForMatchType.ORDER_NEW
    } else if (fundInsufficient(ord)) {
      OrderForMatchType.ORDER_REM
    } else {
      OrderForMatchType.ORDER_UPDATE
    }

    val availableAmountS = ord.orderAvailableAmount.asBigInt.min(ord.accountAvailableAmount.asBigInt)

    OrderForMatch(
      rawOrder = ord.getState.rawOrder,
      feeAddress = ord.getState.getRawOrder.feeAddr.safe,
      availableAmountS = availableAmountS.toString,
      availableFee = ord.feeAvailableAmount,
      matchType = orderType)
  }

  def fundInsufficient(src: OrderBeforeMatch): Boolean = {
    if (src.accountAvailableAmount.asBigInt.compare(src.orderAvailableAmount.asBigInt) > 0) {
      dustAmountS(src.orderAvailableAmount.asBigInt)
    } else {
      true
    }
  }

  // todo
  def dustAmountS(availableAmount: BigInt): Boolean = {
    availableAmount.compare(BigInt(0)) <= 0
  }

}
