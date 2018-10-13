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

package org.loopring.orderbook.core.ordermanager

import org.loopring.orderbook.proto.account.{ Account, AllowanceChangedEvent, BalanceChangedEvent }
import org.loopring.orderbook.proto.order.{ OrderBeforeMatch, OrderForMatch, OrderUpdateEvent, RawOrder }

trait OrderManagerHelper {

  def handleOrderNew(ord: RawOrder, account: Account, feeAccount: Account): OrderForMatch
  def handleOrderUpdate(event: OrderUpdateEvent): Option[OrderForMatch]
  def handleBalanceChanged(event: BalanceChangedEvent): Seq[OrderForMatch]
  def handleAllowanceChanged(event: AllowanceChangedEvent): Seq[OrderForMatch]
}
