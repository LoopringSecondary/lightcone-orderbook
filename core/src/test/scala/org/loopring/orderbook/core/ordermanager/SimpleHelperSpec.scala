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

package org.loopring.orderbook.core.actors

import org.loopring.orderbook.core.ordermanager._
import org.scalatest.FlatSpec
import org.loopring.orderbook.lib.time._
import org.loopring.orderbook.proto.account.Account
import org.loopring.orderbook.proto.order.RawOrder

class SimpleHelperSpec extends FlatSpec {

  info("execute cmd [sbt core/'testOnly *SimpleHelperSpec -- -z gatewayOrder'] to debug new order")

  val timeProvider = new LocalSystemTimeProvider()
  val accountAccessor = new AccountAccessor()
  val orderAccessor = new OrderAccessor(timeProvider)
  val accountOrderIndexer = new AccountOrderIndexer()

  val helper = new OrderManagerHelperImpl(accountAccessor, orderAccessor, accountOrderIndexer)

  "gatewayOrder" should "add new order" in {
    val rawOrder = RawOrder()
    val account = Account()
    val feeAccount = Account()

    helper.handleOrderNew(rawOrder, account, feeAccount)
  }

}
