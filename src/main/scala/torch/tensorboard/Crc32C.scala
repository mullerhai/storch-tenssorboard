/* Copyright 2016 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/
package torch.tensorboard

import java.util.zip.Checksum

object Crc32C {
  private val MASK_DELTA = 0xa282ead8

  def maskedCrc32c(data: Array[Byte]): Int = maskedCrc32c(data, 0, data.length)

  def maskedCrc32c(data: Array[Byte], offset: Int, length: Int): Int = {
    val crc32c = new Crc32C
    crc32c.update(data, offset, length)
    crc32c.getMaskedValue
  }

  /**
   * Return a masked representation of crc.
   * <p/>
   * Motivation: it is problematic to compute the CRC of a string that
   * contains embedded CRCs.  Therefore we recommend that CRCs stored
   * somewhere (e.g., in files) should be masked before being stored.
   */
  def mask(crc: Int): Int = {
    // Rotate right by 15 bits and add a constant.
    ((crc >>> 15) | (crc << 17)) + MASK_DELTA
  }

  /**
   * Return the crc whose masked representation is masked_crc.
   */
  def unmask(maskedCrc: Int): Int = {
    val rot = maskedCrc - MASK_DELTA
    (rot >>> 17) | (rot << 15)
  }
}

class Crc32C extends Checksum {
  crc32C = new PureJavaCrc32C
  private var crc32C: PureJavaCrc32C = null

  def getMaskedValue: Int = Crc32C.mask(getIntValue)

  def getIntValue: Int = getValue.toInt

  override def update(b: Int): Unit = {
    crc32C.update(b)
  }

  override def update(b: Array[Byte], off: Int, len: Int): Unit = {
    crc32C.update(b, off, len)
  }

  override def getValue: Long = crc32C.getValue

  override def reset(): Unit = {
    crc32C.reset()
  }
}