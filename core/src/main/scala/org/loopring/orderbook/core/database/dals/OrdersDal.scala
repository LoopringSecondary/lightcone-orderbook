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

package org.loopring.orderbook.core.database.dals

import org.loopring.orderbook.core.database.OrderDatabase
import org.loopring.orderbook.core.database.base._
import org.loopring.orderbook.core.database.tables._
import org.loopring.orderbook.proto.order._
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

case class QueryCondition(
  delegateAddress: String = "",
  owner: Option[String] = None,
  market: Option[String] = None,
  status: Seq[String] = Seq(),
  orderHashes: Seq[String] = Seq(),
  side: Option[String] = None)

trait OrdersDal extends BaseDalImpl[Orders, OrderState] {
  def getOrder(orderHash: String): Future[Option[OrderState]]

  def getOrders(condition: QueryCondition, skip: Int, take: Int): Future[Seq[OrderState]]

  def count(condition: QueryCondition): Future[Int]

  def getOrders(condition: QueryCondition): Future[Seq[OrderState]]

  def getOrdersWithCount(condition: QueryCondition, skip: Int, take: Int): (Future[Seq[OrderState]], Future[Int])

  def saveOrder(order: OrderState): Future[Int]

  def unwrapCondition(condition: QueryCondition): Query[Orders, OrderState, Seq]
}

class OrdersDalImpl(val module: OrderDatabase) extends OrdersDal {
  val query = ordersQ

  override def update(row: OrderState): Future[Int] = {
    db.run(query.filter(_.id === row.id).update(row))
  }

  override def update(rows: Seq[OrderState]): Future[Unit] = {
    db.run(DBIO.seq(rows.map(r ⇒ query.filter(_.id === r.id).update(r)): _*))
  }

  def saveOrder(order: OrderState): Future[Int] = module.db.run(query += order)

  def getOrder(orderHash: String): Future[Option[OrderState]] = {
    findByFilter(_.orderHash === orderHash).map(_.headOption)
  }

  def getOrders(condition: QueryCondition, skip: Int, take: Int): Future[Seq[OrderState]] = {

    db.run(unwrapCondition(condition)
      .drop(skip)
      .take(take)
      .result)
  }

  def count(condition: QueryCondition): Future[Int] = db.run(unwrapCondition(condition).length.result)

  // 用来查询所有相关订单， 没有分页参数，主要用来做软取消，慎用
  def getOrders(condition: QueryCondition): Future[Seq[OrderState]] = {
    db.run(unwrapCondition(condition)
      .result)
  }

  def getOrdersWithCount(condition: QueryCondition, skip: Int, take: Int): (Future[Seq[OrderState]], Future[Int]) = {

    val action = unwrapCondition(condition)
      .drop(skip)
      .take(take)

    (db.run(action.drop(skip).take(take).result), db.run(action.length.result))
  }

  override def unwrapCondition(condition: QueryCondition): Query[Orders, OrderState, Seq] = {

    query
      .filter { o ⇒
        condition.owner.map(o.owner === _).getOrElse(true: Rep[Boolean])
      }
      .filter { o ⇒
        condition.side.map(o.side === _).getOrElse(true: Rep[Boolean])
      }
      .filter { o ⇒
        condition.market.map(o.market === _).getOrElse(true: Rep[Boolean])
      }
      .filter(_.status inSet condition.status)
      .filter(_.orderHash inSet condition.orderHashes)
      .sortBy(_.createdAt.desc)
  }
}
