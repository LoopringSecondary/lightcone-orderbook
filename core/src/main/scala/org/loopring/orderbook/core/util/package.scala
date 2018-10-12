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

import org.loopring.orderbook.lib.math.Rational
import org.loopring.orderbook.proto.order.OrderState
import org.loopring.orderbook.lib.etypes._
import org.loopring.orderbook.proto.account.Account

package object util {

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

    def tokenIsFee(): Boolean = {
      val ord = src.getRawOrder
      ord.feeAddr.safe.equals(ord.feeAddr.safe)
    }

  }
}
