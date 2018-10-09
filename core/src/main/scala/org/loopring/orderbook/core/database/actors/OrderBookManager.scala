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

import akka.actor.{Actor, ActorRef}
import org.loopring.orderbook.lib.math.Rational
import org.loopring.orderbook.proto.order._
import org.loopring.orderbook.lib.etypes._
import akka.pattern._
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

case class OrderWithStatus(order: Order, deferredTime: Long)

class PriceIndex {
  var index = mutable.TreeMap[Rational, Set[String]]()

  def add(sellPrice:Rational, orderhash:String) = {

  }

  def del(sellPrice:Rational, orderhash:String) = {

  }
}

class DelayIndex {
  var index = mutable.TreeMap[String, Int]()

  def add(orderhash:String, delayTime:Int) = {

  }

  def del(orderhash:String) = {

  }
}

class OrderAvailable {
  var SettlingAmount = mutable.HashMap[String, BigInt]()
  var Balances = mutable.HashMap[String, BigInt]()

  def add(orderhash:String, delayTime:Int) = {

  }

  def del(orderhash:String) = {

  }

}

class ValidIndex {
  val validSinceIndex = mutable.TreeMap[Long, Set[String]]()
  val validUntilIndex = mutable.TreeMap[Long, Set[String]]()

  def add(validSince:Long, orderhash:String) = {

  }

  def del(validSince:Long, orderhash:String) = {

  }
}

/**
  * 保存订单
  * 所有订单放置在orders中，根据不同情况放入不同的index中
  * 查询时，根据index进行查询
  *
  * ordersWithPriceIdx 按照价格保存订单，保存的订单类型为：在有效期内的，包含delay的，不包含灰尘单
  * ordersWithDelayIdx 保存订单的延迟时间，
  * ordersWithAvailable 保存订单的可用金额，包括settling和balance的金额
  * ordersWithValidIdx 保存有效时间，未在有效时间的和将要到期的订单，便于定时器添加和删除
  */

class OrderBook {
  var orders = mutable.HashMap[String, OrderWithStatus]()
  //订单的获取，根据情况，按照index获取
//  按照价格保存订单，保存的订单类型为：在有效期内的，包含delay的，不包含灰尘单
  var ordersWithPriceIdx = new PriceIndex()
  //保存订单的延迟时间，
  var ordersWithDelayIdx = new DelayIndex()
  //保存订单的可用金额，包括settling和balance的金额
  var ordersWithAvailable = new OrderAvailable()
  //保存有效时间，未在有效时间的和将要到期的订单，便于定时器添加和删除
  var ordersWithValidIdx = new ValidIndex()

  def addOrder(orderWithStatus: OrderWithStatus): Unit = {
    val now = System.currentTimeMillis() / 1e6
    val rawOrder = orderWithStatus.order.rawOrder.get
    val orderhash = rawOrder.hash.toLowerCase
    //当前已过期，不处理
    if (rawOrder.validUntil <= now) {
      return
    }
    //未过期，根据情况，放入各个index中
    orders.synchronized(orders.put(orderhash, orderWithStatus))

//    val sellPrice = Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt)
//    if (rawOrder.validSince > now) {
//      ordersWithValidSinceIdx.synchronized(ordersWithValidSinceIdx + orderhash)
//    } else if (rawOrder.validUntil <= now) {
//      ordersWithPriceIdx.synchronized {
//        var orderHashes = ordersWithPriceIdx.getOrElse(sellPrice, Set[String]())
//        orderHashes = orderHashes + rawOrder.hash
//        ordersWithPriceIdx.put(sellPrice, orderHashes)
//      }
//    }

  }

  def delOrder(rawOrder: RawOrder) = {
    val orderhash = rawOrder.hash.toLowerCase
    orders.synchronized(orders.remove(orderhash))
//    delAllIndex(rawOrder)

  }

  def getOrder(hash: String) = {
    this.orders(hash.toLowerCase).order
  }

//  def delAllIndex(rawOrder: RawOrder) = {
//    val orderhash = rawOrder.hash.toLowerCase
//    ordersWithSettling.synchronized(ordersWithSettling.remove(orderhash))
//    ordersWithValidSinceIdx.synchronized{
//      var orderHashes = ordersWithValidSinceIdx.getOrElse(rawOrder.validSince, Set[String]())
//      orderHashes = orderHashes.filter(_ != orderhash)
//      ordersWithValidSinceIdx.put(rawOrder.validSince, orderHashes)
//    }
//
//    val sellPrice = Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt)
//    ordersWithPriceIdx.synchronized {
//      var orderHashes = ordersWithPriceIdx.getOrElse(sellPrice, Set[String]())
//      orderHashes = orderHashes.filter(_ != orderhash)
//      ordersWithPriceIdx.put(sellPrice, orderHashes)
//    }
//  }
//
//  def delDelayIdx(orderhash:String) = {
//    ordersWithDelayIdx.synchronized(ordersWithDelayIdx.remove(orderhash))
//  }
//  def delDelayIdx(orderhash:String) = {
//    ordersWithDelayIdx.synchronized(ordersWithDelayIdx.remove(orderhash))
//  }
//  def delDelayIdx(orderhash:String) = {
//    ordersWithDelayIdx.synchronized(ordersWithDelayIdx.remove(orderhash))
//  }
//  def delDelayIdx(orderhash:String) = {
//    ordersWithDelayIdx.synchronized(ordersWithDelayIdx.remove(orderhash))
//  }
//
//
//  def addToIndex(orderWithStatus: OrderWithStatus) = {
//    val rawOrder = orderWithStatus.order.getRawOrder
//    val orderhash = rawOrder.hash.toLowerCase
//    ordersWithDelayIdx.synchronized(ordersWithDelayIdx.remove(orderhash))
//    ordersWithSettling.synchronized(ordersWithSettling.remove(orderhash))
//    ordersWithValidSinceIdx.synchronized{
//      var orderHashes = ordersWithValidSinceIdx.getOrElse(rawOrder.validSince, Set[String]())
//      orderHashes = orderHashes.filter(_ != orderhash)
//      ordersWithValidSinceIdx.put(rawOrder.validSince, orderHashes)
//    }
//
//    val sellPrice = Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt)
//    ordersWithPriceIdx.synchronized {
//      var orderHashes = ordersWithPriceIdx.getOrElse(sellPrice, Set[String]())
//      orderHashes = orderHashes.filter(_ != orderhash)
//      ordersWithPriceIdx.put(sellPrice, orderHashes)
//    }
//  }
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
  *
  *
  */
class OrderBookManager(
  tokenS: String,
  tokenB: String,
  counterpartyActor: ActorRef,
  depthActor:ActorRef)(implicit
  ec: ExecutionContext,
  timeout: Timeout) extends RepeatedJobActor {
  var orderBook = new OrderBook()

  override def receive: Receive = {
    case _ ⇒
//    case orderForMatch: OrderForMatch ⇒
//      //需要遍历匹配的订单
//      val rawOrder = orderForMatch.rawOrder.get
//      val sellPrice = Rational(rawOrder.amountS.asBigInt, rawOrder.amountB.asBigInt)
//      //todo:test
//      val candidates = orderBook.ordersWithPriceIdx.range(Rational(0,1), sellPrice).flatMap{
//        a ⇒ a._2
//      }.map{
//        orderHash ⇒ {
//          val order = orderBook.getOrder(orderHash)
//          val volume = Rational(0,1)
//          val lrcFee = Rational(rawOrder.lrcFee.asBigInt) * Rational(rawOrder.amountS.asBigInt)/volume +
//            Rational(order.getRawOrder.lrcFee.asBigInt) * Rational(order.getRawOrder.amountS.asBigInt)/volume
//          (lrcFee, order.getRawOrder, rawOrder)
//        }
//      }.filter(_._1 > Rational(0))
//      if (candidates.nonEmpty) {
//
//      } else {
//
//      }
//
//    case order: Order ⇒ for {
//      res ← counterpartyActor ? order
//    } yield {
//      res match {
//        case deferred:OrderDeferred ⇒
//          val order = orderBook.getOrder(deferred.hash.toLowerCase())
//          val orderWithStatus = OrderWithStatus(order=order, deferredTime=deferred.deferredTime)
//          orderBook.updateOrAddOrder(orderWithStatus)
//      }
//    }
  }

  override def handleRepeatedJob(): Future[Unit] = ???

}
