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

import scala.collection.mutable

// 整个订单管理模块，需要根据账户(owner, token)寻找订单，也需要根据订单owner寻找账户
// 针对根据账户建立订单索引

class AccountOrderIndexer {

  // map["owner.safe-token.safe", Set[orderhash]]
  var idxmap = mutable.HashMap.empty[String, mutable.Set[String]]

  def getOrderhashList(owner: String, token: String): Seq[String] = {
    idxmap.getOrElse(safeKey(owner, token), mutable.Set.empty[String]).toSeq
  }

  def add(owner: String, token: String, orderhash: String): Unit = this.synchronized {
    val key = safeKey(owner, token)
    var s = idxmap.getOrElse(key, mutable.Set.empty[String])
    s += safeValue(orderhash)
    idxmap += key -> s
  }

  def del(owner: String, token: String, orderhash: String): Unit = this.synchronized {
    val key = safeKey(owner, token)
    val value = safeValue(orderhash)

    require(idxmap.contains(key))
    var s = idxmap.getOrElse(key, mutable.Set.empty[String])
    require(s.contains(value))
    s -= value
    if (s.size.equals(0)) {
      idxmap -= key
    }
  }

  def size(owner: String, token: String): Int = this.synchronized {
    val key = safeKey(owner, token)
    idxmap.getOrElse(key, mutable.Set.empty[String]).size
  }

  private def safeKey(owner: String, token: String) = owner.safe + "-" + token.safe
  private def safeValue(orderhash: String) = orderhash.safe
}
