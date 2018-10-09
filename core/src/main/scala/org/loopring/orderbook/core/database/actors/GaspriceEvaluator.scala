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

import akka.actor.Actor
import org.loopring.orderbook.proto.gas._

import scala.util.Random

class GaspriceEvaluator extends Actor {

  var gasPrice = BigInt(0)

  val min = 4950000000L
  val max = 14052734625L

  override def receive: Receive = {
    case req: GasReq => GasRes().withValue(BigInt(getRandomGasPrice).toString)
    case set: SetGas => gasPrice = BigInt.apply(set.value)
  }

  def getRandomGasPrice = {
    val rnd = new Random
    min + rnd.nextInt(max - min + 1)
  }
}
