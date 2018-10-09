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

package org.loopring.orderbook.core.database.actors

import akka.actor.Actor
import org.loopring.orderbook.lib.math.Rational

import scala.collection.mutable

// 依赖orderbook
// 初始化: 从orderBook获取priceIndex
class DepthManager extends Actor {

  var asks = mutable.TreeMap[Rational, String]()
  var bids = mutable.TreeMap[Rational, String]()

  override def receive: Receive = ???

}
