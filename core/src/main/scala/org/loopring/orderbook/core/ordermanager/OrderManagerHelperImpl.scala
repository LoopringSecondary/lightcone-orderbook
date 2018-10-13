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

package org.loopring.orderbook.core.ordermanager

import org.loopring.orderbook.proto.order._
import org.loopring.orderbook.proto.account._
import org.loopring.orderbook.lib.etypes._

class OrderManagerHelperImpl(
  accountAccessor: AccountAccessor,
  orderAccessor: OrderAccessor,
  accountOrderIndexer: AccountOrderIndexer) extends OrderManagerHelper {

  def handleOrderNew(ord: RawOrder, account: Account, feeAccount: Account): OrderForMatch = {
    val state = orderAccessor.ord2State(ord)
    val orderBeforeMatch = assemble(state, account, feeAccount)
    val orderforMatch = convert(orderBeforeMatch)
    updateOrderAccount(orderBeforeMatch, orderforMatch.matchType)

    orderforMatch
  }

  def handleOrderUpdate(event: OrderUpdateEvent): Option[OrderForMatch] = {
    orderAccessor.getSingleOrder(event.orderHash) match {
      case Some(x) =>
        val dealtAmount = x.dealtAmountS.asBigInt.+(event.dealtAmountS.asBigInt)
        val cancelAmount = x.cancelAmountS.asBigInt.+(event.cancelAmountS.asBigInt)

        val state = x.copy(dealtAmountS = dealtAmount.toString, cancelAmountS = cancelAmount.toString)

        val account = accountAccessor.get(event.owner, state.getRawOrder.tokenS)
        val feeaccount = if (state.tokenNotFee) {
          accountAccessor.get(event.owner, state.getRawOrder.feeAddr)
        } else {
          account
        }

        val orderBeforeMatch = assemble(state, account, feeaccount)
        val orderForMatch = convert(orderBeforeMatch)
        updateOrderAccount(orderBeforeMatch, orderForMatch.matchType)

        Option(orderForMatch)
      case _ => Option.empty[OrderForMatch]
    }

  }

  def handleBalanceChanged(event: BalanceChangedEvent): Seq[OrderForMatch] = {
    val orderhashseq = accountOrderIndexer.getOrderhashList(event.owner, event.token)
    if (orderhashseq.isEmpty) {
      Seq()
    } else {
      val originAccount = accountAccessor.get(event.owner, event.token)
      var account = originAccount.copy(balance = event.currentAmount)
      val sortedmap = orderAccessor.getSortedOrder(orderhashseq)
      sortedmap.map(x => {
        val state = x._2
        val feeaccount = if (state.tokenNotFee) {
          accountAccessor.get(event.owner, state.getRawOrder.feeAddr)
        } else {
          account
        }

        val orderBeforeMatch = assemble(state, account, feeaccount)

        // 计算account.balance在对该订单交易后，下一个订单还可以使用的余额
        // account.balance - availableAmountS
        // if tokenIsFee account.balance - availableFee
        if (event.token.safe.equals(state.getRawOrder.tokenS.safe)) {
          val restbalance =
        } else {

        }

        // todo
        state.availableAmountS()

        account = account.copy()

        val orderForMatch = convert(orderBeforeMatch)
        updateOrderAccount(orderBeforeMatch, orderForMatch.matchType)
        orderForMatch
      }).toSeq
    }
  }

  def handleAllowanceChanged(event: AllowanceChangedEvent): Seq[OrderForMatch] = Seq()

  def updateOrderAccount(ord: OrderBeforeMatch, matchType: OrderForMatchType): Unit = {
    val state = ord.getState
    val rawOrder = ord.getState.getRawOrder
    val owner = rawOrder.owner
    val orderhash = rawOrder.hash
    val tokens = rawOrder.tokenS
    val tokenfee = rawOrder.feeAddr
    val account = Account(balance = ord.tokenSBalance, allowance = ord.tokenSAllowance)
    val feeaccount = Account(balance = ord.feeBalance, allowance = ord.feeAllowance)

    matchType match {
      case OrderForMatchType.ORDER_NEW => {
        orderAccessor.add(state)
        accountAccessor.addOrUpdate(owner, tokens, account)
        accountOrderIndexer.add(owner, tokens, orderhash)
        if (state.tokenNotFee) {
          accountAccessor.addOrUpdate(owner, tokenfee, feeaccount)
          accountOrderIndexer.add(owner, tokenfee, orderhash)
        }
      }

      case OrderForMatchType.ORDER_UPDATE =>
        orderAccessor.update(state)

      case OrderForMatchType.ORDER_REM => {
        orderAccessor.del(orderhash)
        accountOrderIndexer.del(owner, tokens, orderhash)
        if (accountOrderIndexer.size(owner, tokens).equals(0)) {
          accountAccessor.del(owner, tokens)
        }
        if (state.tokenNotFee) {
          accountOrderIndexer.del(owner, tokenfee, orderhash)
          if (accountOrderIndexer.size(owner, tokenfee).equals(0)) {
            accountAccessor.del(owner, tokenfee)
          }
        }
      }
    }
  }

  private def assemble(state: OrderState, account: Account, feeAccount: Account) = OrderBeforeMatch(
    state = Option(state),
    tokenSBalance = account.balance,
    tokenSAllowance = account.allowance,
    feeBalance = feeAccount.balance,
    feeAllowance = feeAccount.allowance)

  private def convert(ord: OrderBeforeMatch): OrderForMatch = {
    val state = ord.getState
    val orderType = if (state.dealtAndCancelAmountS.compare(BigInt(0)) == 0) {
      OrderForMatchType.ORDER_NEW
    } else if (fundInsufficient(ord.accountAvailableAmount, ord.orderAvailableAmount)) {
      OrderForMatchType.ORDER_REM
    } else {
      OrderForMatchType.ORDER_UPDATE
    }

    val availableAmountS = ord.orderAvailableAmount.min(ord.accountAvailableAmount)

    OrderForMatch(
      rawOrder = state.rawOrder,
      feeAddress = state.getRawOrder.feeAddr.safe,
      availableAmountS = availableAmountS.toString,
      availableFee = ord.feeAvailableAmount.toString,
      matchType = orderType)
  }

  // allowance/balance不足
  // @param accountAvailableAmount: BigInt 可用min(余额/授权)
  // @param orderAvailableAmount: BigInt 订单未成交量
  // @return boolean, true->不足, false->充足
  private def fundInsufficient(accountAvailableAmount: BigInt, orderAvailableAmount: BigInt): Boolean = {
    if (accountAvailableAmount.compare(orderAvailableAmount) > 0) {
      dustAmountS(orderAvailableAmount)
    } else {
      true
    }
  }

  // todo
  private def dustAmountS(availableAmount: BigInt): Boolean = {
    availableAmount.compare(BigInt(0)) <= 0
  }
}
