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
import org.loopring.orderbook.lib.math._
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

  val numOfOrderBookToKeep = 500
  var market = SetMarket()
  val pageSize = 20

  var asks = SortedMap.empty[Double, Entry] // sell
  var bids = SortedMap.empty[Double, Entry] // buy

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

  getAskBidsFromOrderBookManager(0, true)
  getAskBidsFromOrderBookManager(0, false)

  override def receive: Receive = {
    case s: SetMarket => market = s

    case e: DepthUpdateEvent =>
      inThisMarket(e.tokenS, e.tokenB, market) {
        update(e)
      }

    case r: GetDepthReq =>
      inThisMarket(r.tokenS, r.tokenB, market) {
        require(precisedMap.contains(r.granularity))
        val a = assemble(r.granularity, true).values.toSeq.reverse.take(r.size)
        val b = assemble(r.granularity, false).values.toSeq.reverse.take(r.size)
        GetDepthRes(asks = a, bids = b)
      }
  }

  private def getAskBidsFromOrderBookManager(pageIndex: Int, isAsk: Boolean): Future[Unit] = for {
    res <- orderBookManager ? GetDepthOrderListReq(market.marketTokenAddr, market.exchangeTokenAddr, pageSize, pageIndex)
    _ = res match {
      case s: GetDepthOrderListRes =>
        s.list.map(update)
        if (s.nextPage > 0) {
          getAskBidsFromOrderBookManager(s.nextPage, isAsk)
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
