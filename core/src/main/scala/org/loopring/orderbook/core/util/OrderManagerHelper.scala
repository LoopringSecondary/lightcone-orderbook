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

  // 轮询分叉事件
  // 注意: 这两个function只负责计算OrderForMatch,具体的涉及到增删改查ordermap的地方由其他function负责相关逻辑
  // fee不是tokenS
  // orderForMatch.availableAmountS选取min(allowance, balance)与当前交易后剩余量中最小值
  // orderForMatch.availableFee选取min(allowance, balance)与当前交易后剩余fee中最小值
  def getOrderForMatchWithoutFee(state: OrderState, account: Account, feeAccount: Account): OrderForMatch

  // fee即tokenS
  // orderForMatch.availableAmountS与orderForMatch.availableFee共用同一组min(allowance, balance)
  // 优先供给fee,如果fee足够则补充amountS,如果fee不够
  def getOrderForMatchWithFee(state: OrderState, account: Account): OrderForMatch

  //  // 比较订单当前撮合信息与最新撮合信息，并决定该订单在撮合引擎的curd操作类型
  //  // 1.next.amountS >= prev.amountS, next.fee >= prev.fee update/new
  //  // 2.next.amountS >= prev.amountS, next.fee < prev.fee update/rem
  //  // 3.next.amountS < prev.amountS, next.fee >= prev.fee update/rem
  //  // 4.next.amountS < prev.amountS, next.fee < prev.fee update/rem
  //  def accessNotifyTypeWithOrderForMatch(prev: OrderForMatch, next: OrderForMatch): OrderForMatch

  // todo 应该根据marketCap计算订单状态,这里暂时替代下
  def isOrderFinished(availableAmount: BigInt): Boolean

}
