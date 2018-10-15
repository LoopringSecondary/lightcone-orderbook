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

package org.loopring.orderbook.core

import org.loopring.orderbook.lib.math.Rational
import org.loopring.orderbook.proto.order.{ OrderForMatch, RawOrder }
import org.web3j.crypto.{ Hash ⇒ web3Hash, _ }
import org.web3j.utils.Numeric
import org.loopring.orderbook.lib.etypes._

package object richproto {

  val ethereumPrefix = "\u0019Ethereum Signed Message:\n"

  implicit class RichRawOrder(rawOrder: RawOrder) {
    def getAvailableAmountS(availableAmountB: Rational): Rational = {
      rawOrder.amountS.asRational * availableAmountB / rawOrder.amountB.asRational
    }

    def getAvailableAmountB(availableAmountS: Rational): Rational = {
      rawOrder.amountB.asRational * availableAmountS / rawOrder.amountS.asRational
    }
  }

  implicit class RichOrderForMatch(order: OrderForMatch) {
    def getAvailableAmountSAndLrcFee(availableAmountB: Rational): (Rational, Rational) = {
      (Rational(0), Rational(0))
    }
  }

}
