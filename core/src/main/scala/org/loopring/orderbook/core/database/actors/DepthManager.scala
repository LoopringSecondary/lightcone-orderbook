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

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import org.loopring.orderbook.lib.etypes._
import org.loopring.orderbook.proto.depth._

import scala.collection.SortedMap
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

// 依赖orderbook
// 初始化: 从orderBook获取priceIndex
class DepthManager(orderBookManager: ActorRef)(
  implicit
  timeout: Timeout,
  ec: ExecutionContext) extends Actor {

  val numOfOrderBookToKeep = 100000
  var market = SetMarket()
  val pageSize = 20

  var asks = SortedMap.empty[Double, Entry] // sell
  var bids = SortedMap.empty[Double, Entry] // buy

  setAskBids(0, true)
  setAskBids(0, false)

  override def receive: Receive = {
    case s: SetMarket => market = s

    case e: DepthUpdateEvent =>
      inThisMarket(e.tokenS, e.tokenB, market) {
        update(e)
      }
  }

  private def setAskBids(pageIndex: Int, isAsk: Boolean): Future[Unit] = for {
    res <- orderBookManager ? GetDepthOrderListReq(market.marketTokenAddr, market.exchangeTokenAddr, pageSize, pageIndex)
    _ = res match {
      case s: GetDepthOrderListRes =>
        s.list.map(update(_))
        if (s.nextPage > 0) {
          setAskBids(s.nextPage, isAsk)
        }
      case _ =>
    }
  } yield null

  private def update(event: DepthUpdateEvent) = {
    val price = event.getPrice.toDouble
    val entry = Entry(price, event.size, event.amount)
    val num = event.amount.asBigInt
    val isAsk = event.isAsk(market)
    var dest = if (isAsk) asks else bids

    if (event.size <= 0 || num.compare(BigInt(0)) <= 0) {
      dest -= price
    } else {
      if (dest.size >= numOfOrderBookToKeep) dest.drop(1)
      dest += price -> entry
    }

    if (isAsk) asks = dest else bids = dest
  }

  private def sort(granularity: Double, isAsk: Boolean) = {
    var src = if (isAsk) asks else bids
    var dest = SortedMap.empty[Double, Entry]

    src.map { a =>
      val entry = if (dest.size < 1) {
        Entry(a._1, 0, BigInt(0).toString)
      } else {
        val temp = dest.dropWhile(_._1 < a._1 + granularity)
        if (temp.size > 0) {
          temp.head._2
        } else {
          Entry(a._1, 0, BigInt(0).toString)
        }
      }

      val tmpPrice = middlePrice(entry.price, granularity)
      val newPrice = priceConvertUp(tmpPrice)
      val newEntry = dest.getOrElse(newPrice, OrderBook.Entry(newPrice, 0, 0))

      dest += newPrice -> newEntry.copy(size = newEntry.size + a._2.size, count = newEntry.count + a._2.count)
    }
    dest
  }

  private def middlePrice(price: Double, granularity: Double) = {
    val s = (granularity - granularity.floor).toString.size
    val high = price + granularity / 2
    ((high / granularity).round * granularity).scaled(s)
  }

}
