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

trait OrderManagerHelper {

  // fee不是tokenS
  def getOrderBeforeMatchWithoutFee(state: OrderState, account: Account, feeAccount: Account): OrderBeforeMatch

  // fee即tokenS
  // 优先供给fee,如果fee足够则补充amountS,如果fee不够
  def getOrderBeforeMatchWithFee(state: OrderState, feeAccount: Account): OrderBeforeMatch

  // 在账户余额不变情况下更新
  def updateOrderBeforeMatchWithTrade(src: OrderBeforeMatch, dealtCancelAmount: BigInt): OrderBeforeMatch

  // 在成交情况不变情况下更新
  def updateOrderBeforeMatchWithAccount(src: OrderBeforeMatch, account: Account): OrderBeforeMatch

  def getOrderForMatch(ord: OrderBeforeMatch): OrderForMatch

  def fundInsufficient(src: OrderBeforeMatch): Boolean

  // todo 应该根据marketCap计算订单状态,这里暂时替代下
  def dustAmountS(availableAmount: BigInt): Boolean

}
