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
package org.loopring.orderbook.core

import org.loopring.orderbook.lib.etypes._
import org.loopring.orderbook.lib.math.Rational
import org.loopring.orderbook.proto.account._
import org.loopring.orderbook.proto.order._

package object ordermanager {

  def zeroAmount: BigInt = BigInt(0)

  def safeSub(a1: BigInt, a2: BigInt): BigInt = {
    if (a1.compare(a2) > 0) {
      a1.-(a2)
    } else {
      BigInt(0)
    }
  }

  def safeSub(a1: Rational, a2: Rational): BigInt = {
    if (a1.compare(a2) > 0) {
      (a1 - a2).bigintValue()
    } else {
      BigInt(0)
    }
  }

  implicit class RichAccount(src: Account) {
    def min: BigInt = src.allowance.asBigInt.min(src.balance.asBigInt)

    def max: BigInt = src.allowance.asBigInt.max(src.balance.asBigInt)
  }

  implicit class RichAccountChangedEvent(event: AccountChangedEvent) {

    def toAccount(origin: Account): Account = {
      if (event.isBalance) {
        origin.copy(balance = event.amount)
      } else {
        origin.copy(allowance = event.amount)
      }
    }

  }

  implicit class RichOrderState(src: OrderState) {

    def dealtAndCancelAmountS: BigInt = src.dealtAmountS.asBigInt.+(src.cancelAmountS.asBigInt)

    def dealtAndCancelAmountS(dealCancelAmount: BigInt): BigInt = {
      src.dealtAndCancelAmountS.+(dealCancelAmount)
    }

    // 待成交amount
    def availableAmountS(): BigInt = {
      val rawOrder = src.getRawOrder
      val totalAmountS = rawOrder.amountS.asBigInt
      val dealtAmountS = src.dealtAmountS.asBigInt
      val cancelAmountS = src.cancelAmountS.asBigInt
      val dealtAndCancelAmount = dealtAmountS.+(cancelAmountS)

      safeSub(totalAmountS, dealtAndCancelAmount)
    }

    def availableAmountS(dealtCancelAmount: BigInt): BigInt = {
      safeSub(src.availableAmountS(), dealtCancelAmount)
    }

    // 待成交fee
    def availableFee(): BigInt = {
      val rawOrder = src.getRawOrder
      val rawFee = rawOrder.fee.asRational
      val fillRate = src.dealtAmountS.asRational / Rational(rawOrder.amountS.asBigInt)
      val dealtFee = fillRate * Rational(rawOrder.fee.asBigInt)

      safeSub(rawFee, dealtFee)
    }

    def tokenIsFee(): Boolean = {
      val ord = src.getRawOrder
      ord.feeAddr.safe.equals(ord.feeAddr.safe)
    }

    def tokenNotFee: Boolean = !src.tokenIsFee()
  }

  implicit class RichOrderBeforeMatch(src: OrderBeforeMatch) {

    def minTokenAccount: BigInt = {
      src.tokenSBalance.asBigInt.min(src.tokenSAllowance.asBigInt)
    }

    def minFeeAccount: BigInt = {
      src.feeBalance.asBigInt.min(src.feeAllowance.asBigInt)
    }

    def orderAvailableAmount: BigInt = {
      src.getState.availableAmountS()
    }

    // fee允许为0
    def feeAvailableAmount: BigInt = if (src.getState.tokenIsFee()) {
      src.getState.availableFee().min(src.minFeeAccount)
    } else {
      src.minFeeAccount
    }

    def accountAvailableAmount: BigInt = if (src.getState.tokenIsFee()) {
      val state = src.getState
      val account = src.minFeeAccount
      val available = src.feeAvailableAmount
      val amounts = state.availableAmountS()

      amounts.min(safeSub(account, available))
    } else {
      src.minTokenAccount
    }
  }
}
