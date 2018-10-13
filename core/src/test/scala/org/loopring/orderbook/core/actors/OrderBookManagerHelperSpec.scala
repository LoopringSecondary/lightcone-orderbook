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

import org.loopring.orderbook.core.util.{OrderBookManagerHelperImpl, OrderWithAvailableStatus}
import org.loopring.orderbook.lib.math.Rational
import org.loopring.orderbook.proto.order.{OrderForMatch, RawOrder}
import org.scalatest.FlatSpec

import scala.collection.mutable
import org.loopring.orderbook.lib.etypes._


class OrderBookManagerHelperSpec extends FlatSpec {
  //  /**
  //    *
  //    * 测试：深度单方向的，depthprice的变化
  //    * 1、有撮合一个的
  //    * 2、撮合过都有剩余的
  //    * 3、撮合过无剩余的
  //    *
  //    */
  implicit val dustEvaluator = new DustEvaluator()
  val manager = new OrderBookManagerHelperImpl("a", "b")
  var orders = mutable.Set[OrderWithAvailableStatus]()

  //单方向的深度
  //  "one direction depth" should "depth price" in {
  //    val startTime = System.currentTimeMillis()
  //    var orders = mutable.Set[OrderWithAvailableStatus]()
  //    val random = new util.Random()
  //    (1 until 20).foreach {
  //      i ⇒ {
  //        val amountS = random.nextInt(100).abs + 1
  //        val amountB = random.nextInt(1000).abs + 1
  //        val order = new OrderWithAvailableStatus(OrderForMatch(
  //          rawOrder = Some(RawOrder(tokenS = "a", tokenB = "b", amountS = amountS.toString, amountB = amountB.toString, fee = "100", hash = "hash-" + i)),
  //          availableFee = "3",
  //          availableAmountS = amountS.toString))
  //        orders.add(order)
  //      }
  //    }
  //    orders foreach manager.matchOrderAndSetDepthPrice
  //
  //    val prices = orders.map {
  //      o ⇒ Rational(o.rawOrder.amountS.asBigInt, o.rawOrder.amountB.asBigInt)
  //    }.toSet
  //
  //
  //    val depths = manager.tokenAOrderBook.depths
  //    println("depths:", depths)
  //    assert(depths.depthsWithSpecPrices.size == prices.size)
  //
  //    prices map {
  //      price ⇒
  //        val (sumAmount, size) = orders.filter {
  //          o ⇒ price == Rational(o.rawOrder.amountS.asBigInt, o.rawOrder.amountB.asBigInt)
  //        }.foldLeft((BigInt(0), 0)) {
  //          (sum, o) ⇒ (sum._1 + o.rawOrder.amountS.asBigInt, sum._2 + 1)
  //        }
  //        val depth = depths.depthsWithSpecPrices(price)
  //        assert(depth.amount == sumAmount && depth.size == size)
  //    }
  //
  //  }

  //匹配一个订单的
  "one match" should "match one order" in {
    val startTime = System.currentTimeMillis()
    (1 until 2).foreach {
      i ⇒ {
        val order = new OrderWithAvailableStatus(OrderForMatch(
          rawOrder = Some(RawOrder(tokenS = "a", tokenB = "b", amountS = "10", amountB = "100", fee = "100", hash = "hash-" + i)),
          availableFee = "3",
          availableAmountS = "10"))
        if (i % 100 == 0) {
          println("time4:" + (System.currentTimeMillis() - startTime))
        }
        manager.matchOrderAndSetDepthPrice(order)
      }
    }

    val otherOrder1 = new OrderWithAvailableStatus(
      OrderForMatch(
        rawOrder = Some(RawOrder(tokenS = "b", tokenB = "a", amountS = "200", amountB = "20", fee = "10", hash = "otherhash1")),
        availableFee = "3",
        availableAmountS = "200"))
    println("time2222:" + (System.currentTimeMillis() - startTime))

    val ring1 = manager.matchOrderAndSetDepthPrice(otherOrder1)
    println(manager.tokenAOrderBook.depthPrice, manager.tokenBOrderBook.depthPrice)
    Thread.sleep(100)
    println(manager.tokenAOrderBook.depths, manager.tokenBOrderBook.depths)
    //    println(manager.tokenAOrderBook.ordersWithPriceIdx, manager.tokenBOrderBook.ordersWithPriceIdx)
  }
  //  "test1" should "depth price" in {
  //    val startTime = System.currentTimeMillis()
  //    implicit val dustEvaluator = new DustEvaluator()
  //
  //    val manager = new OrderBookManagerHelperImpl("a", "b")
  //    //    val order1 = new OrderWithAvailableStatus(OrderForMatch(
  //    //      rawOrder = Some(RawOrder(tokenS = "a", tokenB = "b", amountS = "10", amountB = "100", fee = "100", hash = "hash1")),
  //    //      availableFee = "3",
  //    //      availableAmountS = "10"))
  //    val order2 = new OrderWithAvailableStatus(OrderForMatch(
  //      rawOrder = Some(RawOrder(tokenS = "a", tokenB = "b", amountS = "5", amountB = "100", fee = "100", hash = "hash2")),
  //      availableFee = "3",
  //      availableAmountS = "10"))
  //    val order3 = new OrderWithAvailableStatus(OrderForMatch(
  //      rawOrder = Some(RawOrder(tokenS = "a", tokenB = "b", amountS = "5", amountB = "100", fee = "100", hash = "hash3")),
  //      availableFee = "3",
  //      availableAmountS = "10"))
  //    println("time11:" + (System.currentTimeMillis() - startTime))
  //    (1 until 200).foreach {
  //      i ⇒ {
  //        val order = new OrderWithAvailableStatus(OrderForMatch(
  //          rawOrder = Some(RawOrder(tokenS = "a", tokenB = "b", amountS = "10", amountB = "100", fee = "100", hash = "hash-" + i)),
  //          availableFee = "3",
  //          availableAmountS = "10"))
  //        if (i % 100 == 0) {
  //          println("time4:" + (System.currentTimeMillis() - startTime))
  //        }
  //        manager.matchOrderAndSetDepthPrice(order)
  //        //        println("time3:" + (System.currentTimeMillis() - startTime))
  //
  //      }
  //    }
  //    //    manager.matchOrderAndDecideDepthPrice(order1)
  //    //    manager.matchOrderAndDecideDepthPrice(order2)
  //    manager.matchOrderAndSetDepthPrice(order3)
  //    //    println(manager.tokenAOrderBook.orders, manager.tokenAOrderBook.ordersWithPriceIdx.index)
  //    //    val orders = manager.tokenAOrderBook.rangeOrders(Rational(0, 1), Rational(6, 100))
  //    //    println(orders, orders.size)
  //
  //    val otherOrder1 = new OrderWithAvailableStatus(
  //      OrderForMatch(
  //        rawOrder = Some(RawOrder(tokenS = "b", tokenB = "a", amountS = "200", amountB = "20", fee = "10", hash = "otherhash1")),
  //        availableFee = "3",
  //        availableAmountS = "100"))
  //    val otherOrder2 = new OrderWithAvailableStatus(
  //      OrderForMatch(
  //        rawOrder = Some(RawOrder(tokenS = "b", tokenB = "a", amountS = "300", amountB = "200", fee = "10", hash = "otherhash2")),
  //        availableFee = "3",
  //        availableAmountS = "100"))
  //    //    (1 until 10000).foreach {
  //    //      i ⇒ {
  //    //        val otherOrder1 = new OrderWithAvailableStatus(
  //    //          OrderForMatch(
  //    //            rawOrder = Some(RawOrder(tokenS = "b", tokenB = "a", amountS = "200", amountB = "20", lrcFee = "10", hash = "otherhash1")),
  //    //            availableLrcFee = "3",
  //    //            availableAmountS = "100"))
  //    //        manager.matchOrderAndDecideDepthPrice(otherOrder1)
  //    //      }
  //    //    }
  //    println("time2222:" + (System.currentTimeMillis() - startTime))
  //
  //    val ring1 = manager.matchOrderAndSetDepthPrice(otherOrder1)
  //    //    val ring2 = manager.matchOrderAndDecideDepthPrice(otherOrder2)
  //    //    println(ring)
  //    //    println(manager.tokenAOrderBook.orders, manager.tokenAOrderBook.ordersWithPriceIdx.index)
  //    println("time:" + (System.currentTimeMillis() - startTime))
  //    println(manager.tokenAOrderBook.depthPrice, manager.tokenBOrderBook.depthPrice)
  //    Thread.sleep(100)
  //    println(manager.tokenAOrderBook.depths, manager.tokenBOrderBook.depths)
  //    //    println(manager.tokenAOrderBook.ordersWithPriceIdx, manager.tokenBOrderBook.ordersWithPriceIdx)
  //    assert("a" == "d")
  //  }


}
