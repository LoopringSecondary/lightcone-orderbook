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

package org.loopring.orderbook.lib

package object math {

  implicit class RichDouble(d: Double) {
    def rounded = {
      if (d < 1.0) scaled(12)
      else if (d > 1.0) scaled(8)
      else d
    }

    def scaled(s: Int) = BigDecimal(d).setScale(s, BigDecimal.RoundingMode.HALF_UP).toDouble

    def precised = scaled(4)
  }

}
