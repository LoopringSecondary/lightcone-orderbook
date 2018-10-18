package org.loopring.orderbook.core.util

import org.loopring.orderbook.lib.math.Rational
import org.loopring.orderbook.proto.order.RawOrder

import scala.collection.mutable

class PriceIndex() {
  var index = mutable.TreeMap[Rational, mutable.LinkedHashSet[String]]()

  def add(sellPrice: Rational, rawOrder: RawOrder) = {
    index.synchronized {
      var orderHashes = index.getOrElse(sellPrice, mutable.LinkedHashSet[String]())
      orderHashes = orderHashes + rawOrder.hash.toLowerCase
      index.put(sellPrice, orderHashes)
    }
  }

  def del(sellPrice: Rational, orderhash: String) = {
    index.synchronized {
      var orderHashes = index.getOrElse(sellPrice, mutable.LinkedHashSet[String]())
      orderHashes = orderHashes - orderhash.toLowerCase
      if (orderHashes.nonEmpty) {
        index.put(sellPrice, orderHashes)
      } else {
        index.remove(sellPrice)
      }
    }
  }

  def range(from: Rational, until: Rational) = {
    index.range(from, until).flatMap(_._2)
  }

  def get(sellPrice: Rational) = {
    index.getOrElse(sellPrice, Set[String]())
  }

}