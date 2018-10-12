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

package org.loopring.orderbook.core.actors

import akka.actor.{ Actor, ActorRef }
import org.loopring.orderbook.lib.math.Rational
import org.loopring.orderbook.proto.order._
import org.loopring.orderbook.lib.etypes._
import org.loopring.orderbook.core.database.richproto._
import akka.pattern._
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.{ ExecutionContext, Future }

class PriceIndex() {
  var index = mutable.TreeMap[Rational, Set[String]]()

  def add(sellPrice: Rational, rawOrder: RawOrder) = {
    index.synchronized {
      var orderHashes = index.getOrElse(sellPrice, Set[String]())
      orderHashes = orderHashes + rawOrder.hash.toLowerCase
      index.put(sellPrice, orderHashes)
    }
  }

  def del(sellPrice: Rational, orderhash: String) = {
    index.synchronized {
      var orderHashes = index.getOrElse(sellPrice, Set[String]())
      orderHashes = orderHashes - orderhash.toLowerCase
      if (orderHashes.nonEmpty) {
        index.put(sellPrice, orderHashes)
      } else {
        index.remove(sellPrice)
      }
    }
  }

  def range(from: Rational, until: Rational) = {
    index.range(from, until).flatMap(_._2)
  }

}

/**
 * 保存订单
 * 所有订单放置在orders中，根据不同情况放入不同的index中
 * 查询时，根据index进行查询
 *
 * ordersWithPriceIdx 按照价格保存订单，保存的订单类型为：在有效期内的，包含delay的，不包含灰尘单
 */
class OrderBook {
  var orders = mutable.HashMap[String, OrderWithAvailableStatus]()

  //订单的获取，根据情况，按照index获取
  //按照价格保存订单，这里的订单是可以被撮合，保存的订单类型为：在有效期内的，包含defer的，不包含灰尘单
  var ordersWithPriceIdx = new PriceIndex()

  def addOrUpdateOrder(order: OrderWithAvailableStatus): Unit = {
    val rawOrder = order.rawOrder
    val orderhash = rawOrder.hash.toLowerCase
    val sellPrice = Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt)
    orders.synchronized(orders.put(orderhash, order))
    ordersWithPriceIdx.add(sellPrice, rawOrder)
  }

  def delOrder(rawOrder: RawOrder) = {
    val orderhash = rawOrder.hash.toLowerCase
    orders.synchronized(orders.remove(orderhash))
  }

  def getOrder(hash: String) = {
    this.orders(hash.toLowerCase)
  }

  def range(from: Rational, until: Rational) = {
    ordersWithPriceIdx.range(from, until).map(orders(_))
  }
}

class OrderWithAvailableStatus(order: OrderForMatch) {
  var rawOrder = order.getRawOrder
  var availableAmountS = order.availableAmountS.asRational
  var availableLrcFee = order.availableLrcFee.asRational

  def subAvailable(amountS: Rational, lrcFee: Rational): Boolean = {
    this.synchronized {
      //todo:可以进一步细化返回值
      if (this.availableAmountS < amountS || this.availableLrcFee < lrcFee) {
        false
      } else {
        this.availableAmountS = this.availableAmountS - amountS
        this.availableLrcFee = this.availableLrcFee - lrcFee
        true
      }
    }
  }

  def addAvailable(amountS: Rational, lrcFee: Rational): Boolean = {
    this.synchronized {
      this.availableAmountS = this.availableAmountS + amountS
      this.availableLrcFee = this.availableLrcFee + lrcFee
      true
    }
  }

  override def toString: String = {
    rawOrder.toString + "-" + availableAmountS.toString + "-" + availableLrcFee.toString
  }
}

/**
 * 接收到新订单之后，先向对手方向请求撮合，根据撮合结果，确定订单的状态是否为defer
 * 如果为defer，则不发给depth，否则发给depth
 *
 * 定时器:
 * 1、定时获取对手方向的最大价格，根据最大价格，将未defer的订单，发给对手方撮合，确定订单状态
 * 2、定时根据validsince和validuntil增加和删除订单
 * 3、定时检测defer的订单，过了defer时间，则发给对手方撮合，再确定状态
 *
 * 需要保存内容：
 * 1、订单的可用金额，包括settling和balance
 * 2、
 */
class OrderBookManager(
  tokenA: String,
  tokenB: String) {
  var tokenAOrderBook = new OrderBook()
  var tokenBOrderBook = new OrderBook()

  var tokenADepthPrice = Rational(5, 1000)
  var tokenBDepthPrice = Rational(1000, 5)
  var usedEth = Rational(35, 10000)
  var priceOfEth = Rational(5, 1000)

  /*
  1、被一个单子清空掉买单池
  2、暗池与深度里的，何时重新匹配
   */

  //决定是否匹配，并给出深度价格
  def matchOrderAndDecideDepthPrice(otherOrder: OrderWithAvailableStatus) = {
    val rawOrder = otherOrder.rawOrder
    val sellPrice = Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt)
    //    println(sellPrice)
    val orderbook = if (rawOrder.tokenS.equalsIgnoreCase(tokenA)) {
      tokenBOrderBook
    } else {
      tokenAOrderBook
    }
    //todo:int.max_value should be instead.
    val matchStatus = orderbook.range(Rational(1) / sellPrice, Rational(Integer.MAX_VALUE)).foldLeft {
      MatchStatus(
        remainedOtherOrder = otherOrder, rings = Seq[Ring](), sellPrice = Rational(0))
    } {
      (status, order) ⇒
        {
          if (status.remainedOtherOrder.availableAmountS > Rational(0)) {
            val ring = calReceived(status.remainedOtherOrder, order)

            var (otherFilledOrder, filledOrder) = if (ring.orders.head.rawOrder.hash.equalsIgnoreCase(status.remainedOtherOrder.rawOrder.hash)) {
              (ring.orders.head, ring.orders(1))
            } else {
              (ring.orders(1), ring.orders.head)
            }
            //todo：需要确定隐藏的价格，以及删除完全匹配的订单
            if (status.remainedOtherOrder.subAvailable(otherFilledOrder.filledAmountS, otherFilledOrder.lrcFee)) {
              if (order.subAvailable(filledOrder.filledAmountS, filledOrder.lrcFee)) {
                //及时删除
                status.copy(remainedOtherOrder = status.remainedOtherOrder, rings = status.rings :+ ring)
              } else {
                status.remainedOtherOrder.addAvailable(otherFilledOrder.filledAmountS, otherFilledOrder.lrcFee)
                status
              }
            } else {
              status
            }
          } else {
            status
          }
        }
    }

    println(matchStatus)
    //todo:
  }

  //计算收益
  def calReceived(order1: OrderWithAvailableStatus, order2: OrderWithAvailableStatus): Ring = {
    val rawOrder1 = order1.rawOrder
    val rawOrder2 = order2.rawOrder
    val availableAmountB1 = rawOrder1.getAvailableAmountB(order1.availableAmountS)
    val availableAmountB2 = rawOrder2.getAvailableAmountB(order2.availableAmountS)

    val ring = if (order1.availableAmountS > availableAmountB2) {
      var lrcFee1 = order1.availableLrcFee
      val lrcFee1Tmp = rawOrder1.lrcFee.asRational * availableAmountB2 / rawOrder1.amountS.asRational
      if (lrcFee1Tmp < lrcFee1) {
        lrcFee1 = lrcFee1Tmp
      }
      val filledOrder1 = FilledOrder(
        rawOrder = rawOrder1,
        filledAmountS = availableAmountB1,
        lrcFee = lrcFee1)
      val filledOrder2 = FilledOrder(
        rawOrder = rawOrder2,
        filledAmountS = rawOrder2.amountS.asRational * availableAmountB2 / rawOrder2.amountB.asRational,
        lrcFee = order2.availableLrcFee)

      Ring((lrcFee1 + order2.availableLrcFee) * priceOfEth - usedEth, Seq[FilledOrder](filledOrder1, filledOrder2))
    } else {
      var lrcFee2 = order2.availableLrcFee
      val lrcFee2Tmp = Rational(rawOrder2.lrcFee.asBigInt) * availableAmountB1 / Rational(rawOrder2.amountS.asBigInt)
      if (lrcFee2Tmp < lrcFee2) {
        lrcFee2 = lrcFee2Tmp
      }
      val filledOrder1 = FilledOrder(
        rawOrder = rawOrder1,
        filledAmountS = rawOrder1.amountS.asRational * availableAmountB1 / rawOrder1.amountB.asRational,
        lrcFee = order1.availableLrcFee)
      val filledOrder2 = FilledOrder(
        rawOrder = rawOrder2,
        filledAmountS = availableAmountB1,
        lrcFee = lrcFee2)
      Ring((order1.availableLrcFee + lrcFee2) * priceOfEth - usedEth, Seq[FilledOrder](filledOrder1, filledOrder2))
    }
    ring
  }

}

case class FilledOrder(rawOrder: RawOrder, filledAmountS: Rational, lrcFee: Rational)

case class Ring(received: Rational, orders: Seq[FilledOrder])

/**
 * 一轮匹配之后，需要决定，订单的剩余金额、深度价格、影响到深度价格的所有价格对应的交易量和数量，订单缩减完成
 * 1、如果有剩余的金额，则需要隐藏部分订单
 * 2、如果没有剩余金额，则不用管
 * 3、匹配过程中需要同步更改lrcfee和availableamounts
 * 4、匹配完成后，更改所有影响到的sellprice给depth，影响到的depth可能是两个方向的，一旦吃掉最高价之后，也需要移动sellprice
 */
case class MatchStatus(remainedOtherOrder: OrderWithAvailableStatus, rings: Seq[Ring], sellPrice: Rational)

/**
 * 新订单，首先进行撮合，然后选择sellprice来确定深度图的价格，然后缩减相应的订单剩余金额
 * 匹配之后，本轮的最大价格，根据该价格释放隐藏的深度
 */

object Main {
  def main(args: Array[String]): Unit = {
    val manager = new OrderBookManager("a", "b")
    val order1 = new OrderWithAvailableStatus(OrderForMatch(
      rawOrder = Some(RawOrder(tokenS = "a", tokenB = "b", amountS = "10", amountB = "100", lrcFee = "100", hash = "hash1")),
      availableLrcFee = "3",
      availableAmountS = "6"))
    val order2 = new OrderWithAvailableStatus(
      OrderForMatch(
        rawOrder = Some(RawOrder(tokenS = "a", tokenB = "b", amountS = "5", amountB = "100", lrcFee = "20", hash = "hash2")),
        availableAmountS = "5",
        availableLrcFee = "10"))
    manager.tokenAOrderBook.addOrUpdateOrder(order1)
    manager.tokenAOrderBook.addOrUpdateOrder(order2)

    println(manager.tokenAOrderBook.orders, manager.tokenAOrderBook.ordersWithPriceIdx.index)
    val orders = manager.tokenAOrderBook.range(Rational(0, 1), Rational(6, 100))
    println(orders, orders.size)

    val otherOrder1 = new OrderWithAvailableStatus(
      OrderForMatch(
        rawOrder = Some(RawOrder(tokenS = "b", tokenB = "a", amountS = "100", amountB = "10", lrcFee = "10", hash = "otherhash1")),
        availableLrcFee = "3",
        availableAmountS = "7"))
    val ring = manager.matchOrderAndDecideDepthPrice(otherOrder1)
    println(ring)
    println(manager.tokenAOrderBook.orders, manager.tokenAOrderBook.ordersWithPriceIdx.index)
    println(otherOrder1)
  }
}

