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

import akka.actor.{ Actor, ActorRef }
import org.loopring.orderbook.lib.math.Rational
import org.loopring.orderbook.proto.order._
import org.loopring.orderbook.lib.etypes._
import akka.pattern._
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.{ ExecutionContext, Future }

case class OrderWithStatus(order: Order, deferredTime: Long, filledAmountS: BigInt = BigInt(0), filledAmountB: BigInt = BigInt(0))

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
}

class DeferredIndex {
  var index = mutable.TreeMap[String, Long]()

  def add(orderhash: String, delayTime: Long) = {
    index.synchronized {
      if (delayTime > 0) {
        index.put(orderhash.toLowerCase(), delayTime)
      } else {
        index.remove(orderhash.toLowerCase())
      }
    }
  }

  def del(orderhash: String) = {
    index.synchronized {
      index.remove(orderhash.toLowerCase())
    }
  }
}

class OrderAvailable {
  //todo:optimize it
  var settlingAmount = mutable.HashMap[String, BigInt]()
  var balances = mutable.HashMap[String, BigInt]()

  def updateBalance(address: String, balance: BigInt) = {
    val addr = address.toLowerCase()
    balances.synchronized(balances.put(addr, balance))
  }

  //买入和卖出
  def addSettlingAmount(orderhash: String, amountS: BigInt, amountB: BigInt) = {
    val hash = orderhash.toLowerCase()
    settlingAmount.synchronized {
      var preAmountS = settlingAmount.getOrElse(hash, BigInt(0))
      settlingAmount.put(hash, preAmountS + amountS)
    }
  }

  //可能需要根据ringhash删除
  def delSettlingAmount(orderhash: String, amount: BigInt) = {
    val hash = orderhash.toLowerCase()
    settlingAmount.synchronized {
      var preAmount = settlingAmount.getOrElse(hash, BigInt(0))
      if (preAmount > amount) {
        settlingAmount.put(hash, preAmount - amount)
      } else {
        settlingAmount.remove(hash)
      }
    }
  }

}

class ValidIndex {
  val validSinceIndex = mutable.TreeMap[Long, Set[String]]()
  val validUntilIndex = mutable.TreeMap[Long, Set[String]]()

  def add(rawOrder: RawOrder) = {
    val orderhash = rawOrder.hash.toLowerCase()
    val now = System.currentTimeMillis() / 1e6
    if (rawOrder.validSince > now) {
      validSinceIndex.synchronized {
        var hashes = validSinceIndex.getOrElse(rawOrder.validSince, Set[String]())
        hashes = hashes + orderhash
        validSinceIndex.put(rawOrder.validSince, hashes)
      }
    } else if (rawOrder.validUntil > now) {
      validUntilIndex.synchronized {
        var hashes = validUntilIndex.getOrElse(rawOrder.validUntil, Set[String]())
        hashes = hashes + orderhash
        validUntilIndex.put(rawOrder.validUntil, hashes)
      }
    }
  }

  def del(rawOrder: RawOrder) = {
    val orderhash = rawOrder.hash.toLowerCase()
    validSinceIndex.synchronized {
      var hashes = validSinceIndex.getOrElse(rawOrder.validSince, Set[String]())
      if (hashes.nonEmpty) {
        hashes = hashes - orderhash
        if (hashes.nonEmpty) {
          validSinceIndex.put(rawOrder.validSince, hashes)
        } else {
          validSinceIndex.remove(rawOrder.validSince)
        }
      }
    }
    validUntilIndex.synchronized {
      var hashes = validUntilIndex.getOrElse(rawOrder.validUntil, Set[String]())
      if (hashes.nonEmpty) {
        hashes = hashes - orderhash
        if (hashes.nonEmpty) {
          validUntilIndex.put(rawOrder.validUntil, hashes)
        } else {
          validUntilIndex.remove(rawOrder.validUntil)
        }
      }
    }

  }
}
//灰尘单
class DustIndex(dustEvaluator: DustEvaluator) {
  val index = mutable.Set[String]()

  def add(order: Order) = {
    val orderhash = order.getRawOrder.hash.toLowerCase
    if (dustEvaluator.isDust()) {
      index.synchronized(index.add(orderhash))
    }
  }

  def del(orderhash: String) = {
    index.synchronized(index.remove(orderhash))
  }
}

class DustEvaluator {
  var usedEth = BigInt(1)
  var priceOfEth = Rational(1)
  var dustValue = BigInt(0)
  var finishValue = BigInt(0)

  //根据可用金额而不是订单本身的金额计算
  def isDust(): Boolean = {
    false
  }

  //需要根据订单本身的金额，而不是可用金额
  def isFinished(): Boolean = {
    false
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
  var orders = mutable.HashMap[String, Order]()

  //订单的获取，根据情况，按照index获取
  //按照价格保存订单，这里的订单是可以被撮合，保存的订单类型为：在有效期内的，包含defer的，不包含灰尘单
  var ordersWithPriceIdx = new PriceIndex()
  def addOrUpdateOrder(orderWithStatus: OrderWithStatus): Unit = {
    val now = System.currentTimeMillis() / 1e6
    val rawOrder = orderWithStatus.order.getRawOrder
    val orderhash = rawOrder.hash.toLowerCase
    orders.synchronized(orders.put(orderhash, orderWithStatus.order))
  }

  def delOrder(rawOrder: RawOrder) = {
    val orderhash = rawOrder.hash.toLowerCase
    orders.synchronized(orders.remove(orderhash))
  }

  def getOrder(hash: String) = {
    this.orders(hash.toLowerCase)
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
  tokenB: String,
  depthActor: ActorRef)(implicit
  ec: ExecutionContext,
  timeout: Timeout) extends RepeatedJobActor {
  var tokenAOrderBook = new OrderBook()
  var tokenBOrderBook = new OrderBook()

  var tokenADepthPrice = Rational(0)
  var tokenBDepthPrice = Rational(0)

  override def receive: Receive = {
    case order: OrderForMatch ⇒
  }

  override def handleRepeatedJob(): Future[Unit] = ???

}

