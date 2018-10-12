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

trait OrderManagerHelper {

  // 根据orderBeforeMatch获取orderForMatch
  def getOrderForMatch(ord: OrderBeforeMatch): OrderForMatch

  // 判断订单余额/授权是否充足
  def fundInsufficient(accountAvailableAmount: BigInt, orderAvailableAmount: BigInt): Boolean

  // todo 应该根据marketCap计算订单状态,这里暂时替代下
  def dustAmountS(availableAmount: BigInt): Boolean

}
