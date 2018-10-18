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

import org.loopring.orderbook.core.richproto._
import org.loopring.orderbook.lib.etypes._
import org.loopring.orderbook.lib.math.Rational
import org.loopring.orderbook.proto.order._

import scala.collection.mutable

case class FilledOrder(rawOrder: RawOrder, filledAmountS: Rational, lrcFee: Rational)

case class Ring(received: Rational, orders: Seq[FilledOrder])

case class OrderForDepth(sellPrice: Rational, hash: String, amount: Rational, size: Int)

/**
 * 一轮匹配之后，需要决定，订单的剩余金额、深度价格、影响到深度价格的所有价格对应的交易量和数量，订单缩减完成
 * 1、如果有剩余的金额，则需要隐藏部分订单
 * 2、如果没有剩余金额，则不用管
 * 3、匹配过程中需要同步更改lrcfee和availableamounts
 * 4、匹配完成后，更改所有影响到的sellprice给depth，影响到的depth可能是两个方向的，一旦吃掉最高价之后，也需要移动sellprice
 */
case class MatchStatus(remainedOrder: OrderWithAvailableStatus, rings: Seq[Ring], sellPrice: Rational)

/**
 * 新订单，首先进行撮合，然后选择sellprice来确定深度图的价格，然后缩减相应的订单剩余金额
 * 匹配之后，本轮的最大价格，根据该价格释放隐藏的深度
 */

/**
 * 保存订单
 * 所有订单放置在orders中，根据不同情况放入不同的index中
 * 查询时，根据index进行查询
 *
 * ordersWithPriceIdx 按照价格保存订单，保存的订单类型为：在有效期内的，包含delay的，不包含灰尘单
 */
class OrderBook {
  var orders = mutable.HashMap[String, OrderWithAvailableStatus]()
  orders.sizeHint(2000000)
  var depthPrice = Rational.MaxIntValue

  var depths = new Depths()

  //订单的获取，根据情况，按照index获取
  //按照价格保存订单，这里的订单是可以被撮合，保存的订单类型为：在有效期内的，包含defer的，不包含灰尘单
  var ordersWithPriceIdx = new PriceIndex()

  def addOrReplaceOrder(order: OrderWithAvailableStatus)(implicit dustEvaluator: DustEvaluator): Unit = {
    val rawOrder = order.rawOrder
    val orderhash = rawOrder.hash.toLowerCase
    orders.get(orderhash) match {
      case None if !dustEvaluator.isDust(order.availableAmountS) ⇒
        val depthEvent = DepthEvent(order.sellPrice, 1, order.availableAmountS.bigintValue())
        orders.synchronized(orders.put(orderhash, order))
        ordersWithPriceIdx.add(order.sellPrice, rawOrder)
        depths.updateDepth(depthEvent)
      case Some(order1) if dustEvaluator.isDust(order.availableAmountS) ⇒
        //更改为灰尘单,则删除该订单
        this.delOrder(orderhash)
      case Some(order1) ⇒
        val depthEvent = DepthEvent(order.sellPrice, 0, (order1.availableAmountS - order.availableAmountS).bigintValue())
        orders.synchronized(orders.put(orderhash, order))
        //        ordersWithPriceIdx.add(order.sellPrice, rawOrder)
        depths.updateDepth(depthEvent)
      case _ ⇒
    }
  }

  //同时会更改深度
  def updateOrder(
    order: OrderWithAvailableStatus,
    incrAmount: Rational,
    incrFee: Rational)(
    implicit
    dustEvaluator: DustEvaluator): Boolean = {
    if (dustEvaluator.isDust(order.availableAmountS + incrAmount)) {
      this.delOrder(order.rawOrder.hash)
    } else {
      if (order.updateAvailable(incrAmount, incrFee)) {
        val depthEvent = DepthEvent(order.sellPrice, 0, incrAmount.bigintValue())
        depths.updateDepth(depthEvent)
        true
      } else {
        false
      }
    }
  }

  def delOrder(orderhashArg: String): Boolean = {
    val orderhash = orderhashArg.toLowerCase
    orders.get(orderhash) match {
      case None ⇒
      case Some(order) ⇒
        orders.synchronized(orders.remove(orderhash))
        ordersWithPriceIdx.del(order.sellPrice, orderhash)
        val depthEvent = DepthEvent(order.sellPrice, -1, (Rational(-1) * order.availableAmountS).bigintValue())
        depths.updateDepth(depthEvent)
    }
    true
  }

  def rangeOrders(from: Rational, until: Rational) = {
    ordersWithPriceIdx.range(from, until).map(orders(_))
  }

  def getOrdersByPrice(sellPrice: Rational) = {
    ordersWithPriceIdx.get(sellPrice).map(orders(_))
  }

  def rangeSellPrice(from: Rational, until: Rational): Seq[Rational] = {
    var prices = ordersWithPriceIdx.index.range(Rational(0), until).keys.toSeq
    if (ordersWithPriceIdx.index.contains(until)) {
      prices :+ until
    } else {
      prices
    }
  }

  def nextLittlePrice(until: Rational): Rational = {
    if (ordersWithPriceIdx.index.contains(until)) {
      until
    } else {
      var prices = ordersWithPriceIdx.index.range(Rational(0), until).keys
      if (prices.isEmpty) {
        until
      } else {
        prices.max
      }
    }
  }

  def hideOrdersBellowSellPrice(sellPrice: Rational) = {
    this.synchronized {
      this.depthPrice = this.nextLittlePrice(sellPrice)
    }
  }

}

class OrderWithAvailableStatus(order: OrderForMatch) {
  var rawOrder = order.getRawOrder
  var availableAmountS = order.availableAmountS.asRational
  var availableAmountB = order.getRawOrder.getAvailableAmountB(availableAmountS)

  //todo:
  var availableFee = order.availableFee.asRational
  var sellPrice = Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt)

  private def subAvailable(amountS: Rational, fee: Rational): Boolean = {
    this.synchronized {
      //todo:可以进一步细化返回值
      if (this.availableAmountS < amountS || this.availableFee < fee) {
        false
      } else {
        this.availableAmountS = this.availableAmountS - amountS
        this.availableAmountB = this.rawOrder.getAvailableAmountB(this.availableAmountS)
        this.availableFee = this.availableFee - fee
        true
      }
    }
  }

  private def addAvailable(amountS: Rational, fee: Rational): Boolean = {
    this.synchronized {
      this.availableAmountS = this.availableAmountS + amountS
      this.availableAmountB = this.rawOrder.getAvailableAmountB(this.availableAmountS)
      this.availableFee = this.availableFee + fee
      true
    }
  }

  def updateAvailable(amountS: Rational, fee: Rational): Boolean = {
    if (amountS.signum < 0) {
      this.subAvailable(amountS.abs(), fee.abs())
    } else {
      this.addAvailable(amountS, fee)
    }
  }

  override def toString: String = {
    rawOrder.toString + ", availableAmountS:" + availableAmountS.toString + ", availableLrcFee:" + availableFee.toString
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
class OrderBookManagerHelperImpl(
  tokenA: String,
  tokenB: String)(
  implicit
  dustEvaluator: DustEvaluator,
  matchedCacher: MatchedCacher) extends OrderBookManagerHelper {
  var tokenAOrderBook = new OrderBook()
  var tokenBOrderBook = new OrderBook()

  //执行合约需要的eth
  var usedEth = Rational(35, 10000)
  //token->eth 的价格
  var tokenPricesOfEth = Map[String, Rational]()

  /*
  1、被一个单子清空掉买单池
  2、暗池与深度里的，何时重新匹配
   */

  override def updateTokenPricesOfEth(prices: Map[String, Rational]): Unit = this.tokenPricesOfEth = prices

  override def updateGasPrice(gasPrice: BigInt): Unit = this.usedEth = Rational(gasPrice) //todo:

  override def rematchHidedOrders(): Unit = ???

  override def blockedRing(ringhash: String): Unit = ???

  //决定是否匹配，并给出深度价格
  override def matchOrderAndSetDepthPrice(order: OrderWithAvailableStatus): MatchStatus = {
    //todo:cache暂未实现,实现后放开
    //    matchedCacher.getCacheInfo(order.rawOrder.hash) match {
    //      case None ⇒
    //      case Some(filledOrder) ⇒
    //        order.availableAmountS = order.availableAmountS - filledOrder.filledAmountS
    //        order.availableAmountB = order.rawOrder.getAvailableAmountB(order.availableAmountS)
    //    }

    val (otherOrderbook, orderbook) = if (order.rawOrder.tokenS.equalsIgnoreCase(tokenA)) {
      (tokenBOrderBook, tokenAOrderBook)
    } else {
      (tokenAOrderBook, tokenBOrderBook)
    }
    //todo:int.max_value should be instead.
    val matchStatus =
      //    MatchStatus(
      //      remainedOrder = order, rings = Seq[Ring](), sellPrice = Rational(0))
      otherOrderbook.rangeOrders(Rational(1) / order.sellPrice, Rational.MaxIntValue).foldLeft {
        MatchStatus(
          remainedOrder = order, rings = Seq[Ring](), sellPrice = Rational(0))
      } {
        (status, otherOrder) ⇒
          {
            if (!dustEvaluator.isDust(status.remainedOrder.availableAmountS)) {
              val ring = calReceived(status.remainedOrder, otherOrder)
              var (filledOrder, otherFilledOrder) =
                if (ring.orders.head.rawOrder.hash.equalsIgnoreCase(status.remainedOrder.rawOrder.hash)) {
                  (ring.orders.head, ring.orders(1))
                } else {
                  (ring.orders(1), ring.orders.head)
                }
              //todo：需要确定隐藏的价格，以及删除完全匹配的订单
              if (status.remainedOrder.updateAvailable(filledOrder.filledAmountS.negValue(), filledOrder.lrcFee.negValue())) {
                if (otherOrderbook.updateOrder(otherOrder, otherFilledOrder.filledAmountS.negValue(), otherFilledOrder.lrcFee.negValue())) {
                  if (dustEvaluator.isDust(otherOrder.availableAmountS)) {
                    otherOrderbook.delOrder(otherOrder.rawOrder.hash)
                  }
                  //                else {
                  //                  otherDepthEvents.add(DepthEvent(otherOrder.sellPrice, 0, otherFilledOrder.filledAmountS.negValue().bigintValue()))
                  //                }
                  status.copy(remainedOrder = status.remainedOrder, rings = status.rings :+ ring)
                } else {
                  status.remainedOrder.updateAvailable(filledOrder.filledAmountS, filledOrder.lrcFee)
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

    /**
     * otherOrder 根据匹配完成之后的可用余额，则判断是否有隐藏的订单，然后判断当前的深度价格变化，决定otherorderbook的深度变化
     */
    if (!dustEvaluator.isDust(order.availableAmountS)) {
      orderbook.addOrReplaceOrder(order)
    }
    this.synchronized {
      var depthPrice = orderbook.depthPrice
      var otherDepthPrice = otherOrderbook.depthPrice
      //订单在交叉部分，则放入到otherorderbook的深度里
      //todo:更改orderbook的depthprice
      /**
       * 首先确定orderbook的depthprice，再确定otherorderbook的depthprice
       */
      //则需要进行比较决定隐藏的订单, 应该只比较最高价格的
      //      var otherMayHidePrice = otherDepthPrice
      val prices = otherOrderbook.rangeSellPrice(Rational(1) / order.sellPrice, depthPrice)
      //决定orderbook的depth，全部没有了，则为maxprice
      //否则每个price都遍历，已经没有无订单的price了，每个price都会有订单
      var (hide, otherMayHidePrice) =
        prices.foldLeft((false, otherDepthPrice)) {
          (res, price) ⇒
            {
              var thisHideFlag = res._1
              var otherPrice = res._2
              if (!thisHideFlag) {
                otherOrderbook.getOrdersByPrice(price).foreach {
                  otherOrder ⇒
                    val avaiableAmountB = otherOrder.availableAmountB
                    if (avaiableAmountB > order.availableAmountS) {
                      otherPrice = price
                      thisHideFlag = true
                    }
                }
              }
              (thisHideFlag, otherPrice)
            }
        }
      otherOrderbook.hideOrdersBellowSellPrice(otherMayHidePrice)
      val newOtherDepthPrice = otherOrderbook.depthPrice
      val newDepthPrice = if (Rational.MaxIntValue == newOtherDepthPrice) Rational.MaxIntValue else Rational(1) / newOtherDepthPrice
      orderbook.hideOrdersBellowSellPrice(newDepthPrice)

    }

    //todo:cache暂未实现
    //    matchStatus.rings foreach {
    //      ring ⇒ ring.orders foreach matchedCacher.addCache
    //    }
    matchStatus

  }

  //计算收益
  private def calReceived(order1: OrderWithAvailableStatus, order2: OrderWithAvailableStatus): Ring = {
    val rawOrder1 = order1.rawOrder
    val rawOrder2 = order2.rawOrder
    val availableAmountB1 = order1.availableAmountB
    val availableAmountB2 = order2.availableAmountB

    val ring = if (order1.availableAmountS > availableAmountB2) {
      var fee1 = order1.availableFee
      val fee1Tmp = rawOrder1.fee.asRational * availableAmountB2 / rawOrder1.amountS.asRational
      if (fee1Tmp < fee1) {
        fee1 = fee1Tmp
      }
      val filledOrder1 = FilledOrder(
        rawOrder = rawOrder1,
        filledAmountS = availableAmountB2,
        lrcFee = fee1)
      val filledOrder2 = FilledOrder(
        rawOrder = rawOrder2,
        filledAmountS = order2.availableAmountS,
        lrcFee = order2.availableFee)
      Ring(fee1 * tokenPricesOfEth(rawOrder1.feeAddr) + order2.availableFee * tokenPricesOfEth(rawOrder2.feeAddr) - usedEth, Seq[FilledOrder](filledOrder1, filledOrder2))
    } else {
      var fee2 = order2.availableFee
      val fee2Tmp = Rational(rawOrder2.fee.asBigInt) * availableAmountB2 / Rational(rawOrder2.amountS.asBigInt)
      if (fee2Tmp < fee2) {
        fee2 = fee2Tmp
      }
      val filledOrder1 = FilledOrder(
        rawOrder = rawOrder1,
        filledAmountS = order1.availableAmountS,
        lrcFee = order1.availableFee)
      val filledOrder2 = FilledOrder(
        rawOrder = rawOrder2,
        filledAmountS = availableAmountB1,
        lrcFee = fee2)
      Ring(order1.availableFee * tokenPricesOfEth(rawOrder1.feeAddr) + fee2 * tokenPricesOfEth(rawOrder2.feeAddr) - usedEth, Seq[FilledOrder](filledOrder1, filledOrder2))
    }
    ring
  }

}

