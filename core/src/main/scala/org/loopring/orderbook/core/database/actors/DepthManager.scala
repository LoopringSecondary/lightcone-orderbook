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

    event.isAsk(market) match {
      case true => if (event.size <= 0 || num.compare(BigInt(0)) <= 0) {
        asks -= price
      } else {
        if (asks.size >= numOfOrderBookToKeep) asks.drop(1)
        asks += price -> entry
      }

      case false => if (event.size <= 0 || num.compare(BigInt(0)) <= 0) {
        bids -= price
      } else {
        if (bids.size >= numOfOrderBookToKeep) bids.drop(1)
        bids += price -> entry
      }
    }
  }

}
