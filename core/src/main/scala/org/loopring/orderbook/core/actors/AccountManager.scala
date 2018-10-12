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

import akka.actor.Actor
import org.loopring.orderbook.proto.account._
import org.loopring.orderbook.lib.etypes._

import scala.collection.mutable

class AccountManager extends Actor {

  // map[owner, map[token_address, Account]]
  var accountmap = mutable.HashMap.empty[String, mutable.HashMap[String, Account]]

  override def receive: Receive = {
    case a: AllowanceChangedEvent =>
      val account = Account(owner = a.owner.safe, token = a.token.safe, allowance = a.currentAmount)
      val tokenmap = accountmap.getOrElse(a.owner.safe, mutable.HashMap.empty[String, Account])
      val exsit = tokenmap.getOrElse(a.token.safe, account)
      tokenmap += a.token.safe -> exsit.copy(allowance = a.currentAmount)
      accountmap += a.owner.safe -> tokenmap

    case a: BalanceChangedEvent =>
      val account = Account(owner = a.owner.safe, token = a.token.safe, balance = a.currentAmount)
      val tokenmap = accountmap.getOrElse(a.owner.safe, mutable.HashMap.empty[String, Account])
      val exist = tokenmap.getOrElse(a.token.safe, account)
      tokenmap += a.token.safe -> exist.copy(balance = a.currentAmount)
      accountmap += a.owner.safe -> tokenmap

    case r: GetAllowanceAndBalanceReq =>
      require(accountmap.contains(r.owner.safe))
      val tokenmap = accountmap.get(r.owner.safe)
      require(tokenmap.contains(r.tokenAddr.safe))
      val account = tokenmap.get(r.tokenAddr.safe)
      require(account.balance.nonEmpty)
      require(account.allowance.nonEmpty)
      GetAllowanceAndBalanceRes().withAccount(account)
  }

}
