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

package org.loopring.orderbook.core.util

import org.loopring.orderbook.lib.math.Rational

trait OrderBookManagerHelper {
  //添加订单并确定深度价格
  def matchOrderAndSetDepthPrice(order: OrderWithAvailableStatus): MatchStatus
  //更新token的价格
  def updateTokenPricesOfEth(prices: Map[String, Rational])
  //执行合约需要使用的eth，主要是gasprice的价格一起的变化
  def updateGasPrice(gasPrice: BigInt)
  //重新匹配隐藏部分的订单
  def rematchHidedOrders()
  //
  def blockedRing(ringhash:String)
}
