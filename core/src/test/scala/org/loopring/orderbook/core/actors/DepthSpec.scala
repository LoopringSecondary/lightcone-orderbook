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

import org.loopring.orderbook.lib.etypes._
import org.loopring.orderbook.lib.math._
import org.loopring.orderbook.proto.depth._
import org.scalatest.FlatSpec

import scala.collection.SortedMap

class DepthSpec extends FlatSpec {
  info("execute cmd [sbt core/'testOnly *DepthSpec -- -z assemble'] to debug assemble")

  var asks = SortedMap.empty[Double, Entry]

  asks += 101.02 -> Entry(101.02, 1, BigInt(1).toString)
  asks += 103.02 -> Entry(103.02, 1, BigInt(1).toString)
  asks += 108.02 -> Entry(108.02, 1, BigInt(1).toString)
  asks += 0.0000001 -> Entry(0.0000001, 1, BigInt(1).toString)
  asks += 0.00053297 -> Entry(0.00053297, 1, BigInt(1).toString)
  asks += 0.000550 -> Entry(0.000550, 1, BigInt(10).toString)
  asks += 0.00053998 -> Entry(0.00053998, 1, BigInt(1).toString)
  asks += 0.000557 -> Entry(0.000557, 1, BigInt(1).toString)
  asks += 0.000551 -> Entry(0.000551, 1, BigInt(15).toString)

  var bids = SortedMap.empty[Double, Entry]

  val precisedMap = Map[Double, Int](
    0.1d -> 1,
    0.01d -> 2,
    0.001d -> 3,
    0.0001d -> 4,
    0.00001d -> 5,
    0.000001d -> 6,
    0.0000001d -> 7,
    0.00000001d -> 8,
    0.000000001d -> 9,
    0.0000000001d -> 10,
  )

  "assemble" should "show depth" in {
    var g = 0.00001d
    info(s"-----------------${g.toString}------------------")
    assemble(g, true).map(e => info(e._2.toString))

    g = 0.0001d
    info(s"-----------------${g.toString}------------------")
    assemble(g, true).map(e => info(e._2.toString))

    g = 1
    info(s"-----------------${g.toString}------------------")
    assemble(g, true).map(e => info(e._2.toString))

    g = 10
    info(s"-----------------${g.toString}------------------")
    assemble(g, true).map(e => info(e._2.toString))
  }

  private def assemble(granularity: Double, isAsk: Boolean): SortedMap[Double, Entry] = {
    var src = if (isAsk) asks else bids
    var dest = SortedMap.empty[Double, Entry]

    src.map { a =>
      val price = middlePrice(a._1, granularity)
      val entry = dest.getOrElse(price, Entry(price, 0, BigInt(0).toString()))
      val size = entry.size + a._2.size
      val amount = BigInt(entry.amount).bigInteger.add(BigInt(a._2.amount).bigInteger)

      dest += price -> entry.copy(price, size, amount.toString)
    }
    dest
  }

  private def middlePrice(price: Double, granularity: Double) = {
    if (price <= granularity) {
      granularity
    } else {
      val p = precisedMap.getOrElse(granularity, 1)
      ((price / granularity).round * granularity).scaled(p)
    }
  }
}
