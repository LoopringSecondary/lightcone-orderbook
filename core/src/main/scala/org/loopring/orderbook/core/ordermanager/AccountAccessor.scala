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

import org.loopring.orderbook.lib.etypes._
import org.loopring.orderbook.proto.account._

import scala.collection.mutable

class AccountAccessor {

  // map[owner, map[token, Account]]
  var accountmap = mutable.HashMap.empty[String, mutable.HashMap[String, Account]]

  def get(owner: String, token: String): Account = {
    require(accountmap.contains(owner.safe))
    val map = accountmap.get(owner.safe)
    require(map.contains(token.safe))
    map.get(token.safe)
  }

  def addOrUpdate(owner: String, token: String, account: Account): Unit = this.synchronized {
    var map = accountmap.getOrElse(owner.safe, mutable.HashMap.empty[String, Account])
    map += token.safe -> account
    accountmap += owner.safe -> map
  }

  def del(owner: String, token: String): Unit = this.synchronized {
    require(accountmap.contains(owner.safe))
    var map = accountmap.getOrElse(owner.safe, mutable.HashMap.empty[String, Account])
    require(map.contains(token.safe))
    map -= token.safe
    if (map.size.equals(0)) {
      accountmap -= owner.safe
    }
  }
}
