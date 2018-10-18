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

import scala.collection.mutable

case class Depth(size: Int, amount: BigInt)

case class DepthEvent(sellPrice: Rational, incrSize: Int, incrAmount: BigInt)

class Depths {
  var depthChanges = mutable.Queue[Depth]()
  var depthsWithSpecPrices = mutable.TreeMap[Rational, Depth]()
  var depthsWithPrices = mutable.TreeMap[Rational, Depth]()

  def updateDepth(evt: DepthEvent) = {
    depthsWithSpecPrices.synchronized {
      val depth1 = depthsWithSpecPrices.getOrElse(evt.sellPrice, Depth(0, BigInt(0)))
      if (evt.incrAmount.signum >= 0 || (evt.incrAmount.abs < depth1.amount && evt.incrSize.abs < depth1.size)) {
        depthsWithSpecPrices.put(evt.sellPrice, depth1.copy(size = depth1.size + evt.incrSize, amount = depth1.amount + evt.incrAmount))
      } else {
        depthsWithSpecPrices.remove(evt.sellPrice)
      }
    }
  }

  def setDepth(sellPrice: Rational, depth: Depth) = {
    depthsWithSpecPrices.synchronized(depthsWithSpecPrices.put(sellPrice, depth))
  }

  override def toString: String = {
    depthsWithSpecPrices.toString()
  }
}
