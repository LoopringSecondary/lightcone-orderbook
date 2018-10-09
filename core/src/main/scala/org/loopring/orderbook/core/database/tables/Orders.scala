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

package org.loopring.orderbook.core.database.tables

import org.loopring.orderbook.core.database.base._
import org.loopring.orderbook.proto.order._
import slick.jdbc.MySQLProfile.api._

class Orders(tag: Tag) extends BaseTable[Order](tag, "ORDERS") {
  def owner = column[String]("owner", O.SqlType("VARCHAR(64)"))
  def authAddress = column[String]("auth_address", O.SqlType("VARCHAR(64)"))
  def privateKey = column[String]("private_key", O.SqlType("VARCHAR(128)"))
  def walletAddress = column[String]("wallet_address", O.SqlType("VARCHAR(64)"))
  def orderHash = column[String]("order_hash", O.SqlType("VARCHAR(128)"))
  def tokenS = column[String]("token_s", O.SqlType("VARCHAR(64)"))
  def tokenB = column[String]("token_b", O.SqlType("VARCHAR(64)"))
  def amountS = column[String]("amount_s", O.SqlType("VARCHAR(64)"))
  def amountB = column[String]("amount_b", O.SqlType("VARCHAR(64)"))
  def lrcFee = column[String]("lrc_fee", O.SqlType("VARCHAR(64)"))
  def buyNoMoreThanAmountB = column[Boolean]("buy_no_more_than_amount_b")
  def marginSplitPercentage = column[Int]("margin_split_percentage", O.SqlType("TINYINT(4)"))
  def dealtAmountS = column[String]("dealt_amount_s", O.SqlType("VARCHAR(64)"))
  def dealtAmountB = column[String]("dealt_amount_b", O.SqlType("VARCHAR(64)"))
  def delayCause = column[String]("delay_cause", O.SqlType("VARCHAR(64)"))
  def status = column[String]("status", O.SqlType("VARCHAR(64)"))
  def market = column[String]("market", O.SqlType("VARCHAR(32)"))
  def side = column[String]("side", O.SqlType("VARCHAR(32)"))
  def price = column[Double]("price", O.SqlType("DECIMAL(28,16)"))

  def * = (
    id,
    rawOrderProjection,
    dealtAmountS,
    dealtAmountB,
    delayCause,
    status,
    market,
    side,
    price,
    createdAt,
    updatedAt) <> (extendTupled, unwrapOption)

  def rawOrderProjection = (
    tokenS,
    tokenB,
    amountS,
    amountB,
    lrcFee,
    buyNoMoreThanAmountB,
    marginSplitPercentage,
    owner,
    walletAddress,
    authAddress,
    privateKey,
    orderHash) <> (
      (RawOrder.apply _).tupled,
      RawOrder.unapply)

  private def extendTupled = (i: Tuple11[Long, RawOrder, String, String, String, String, String, String, Double, Long, Long]) ⇒
    Order.apply(
      i._1,
      Some(i._2),
      i._3,
      i._4,
      i._5,
      OrderStatus.fromName(i._6).getOrElse(OrderStatus.ORDER_STATUS_UNKNOWN),
      i._7,
      i._8,
      i._9,
      i._10,
      i._11)

  private def unwrapOption(order: Order) = {
    val unapplyOrder = Order.unapply(order).get
    Some((
      unapplyOrder._1,
      unapplyOrder._2.get,
      unapplyOrder._3,
      unapplyOrder._4,
      unapplyOrder._5,
      unapplyOrder._6.name,
      unapplyOrder._7,
      unapplyOrder._8,
      unapplyOrder._9,
      unapplyOrder._10,
      unapplyOrder._11))
  }

  def idx = index("idx_order_hash", orderHash, unique = true)
}
