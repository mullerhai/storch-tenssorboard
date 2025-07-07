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

import java.io.{DataInput, EOFException, IOException}
import java.nio.{ByteBuffer, ByteOrder}

class TFRecordReader(private val input: DataInput, private val crcCheck: Boolean) {
  @throws[IOException]
  def read: Array[Byte] = {
    /**
     * TFRecord format:
     * uint64 length
     * uint32 masked_crc32_of_length
     * byte   data[length]
     * uint32 masked_crc32_of_data
     */
    val lenBytes = new Array[Byte](8)
    try
      // Only catch EOF here, other case means corrupted file
      input.readFully(lenBytes)
    catch {
      case eof: EOFException =>
        return null // return null means EOF
    }
    val len = fromInt64LE(lenBytes)
    // Verify length crc32
    if (!crcCheck) input.skipBytes(4)
    else {
      val lenCrc32Bytes = new Array[Byte](4)
      input.readFully(lenCrc32Bytes)
      val lenCrc32 = fromInt32LE(lenCrc32Bytes)
      if (lenCrc32 != Crc32C.maskedCrc32c(lenBytes)) throw new IOException("Length header crc32 checking failed: " + lenCrc32 + " != " + Crc32C.maskedCrc32c(lenBytes) + ", length = " + len)
    }
    if (len > Integer.MAX_VALUE) throw new IOException("Record size exceeds max value of int32: " + len)
    val data = new Array[Byte](len.intValue)
    input.readFully(data)
    // Verify data crc32
    if (!crcCheck) input.skipBytes(4)
    else {
      val dataCrc32Bytes = new Array[Byte](4)
      input.readFully(dataCrc32Bytes)
      val dataCrc32 = fromInt32LE(dataCrc32Bytes)
      if (dataCrc32 != Crc32C.maskedCrc32c(data)) throw new IOException("Data crc32 checking failed: " + dataCrc32 + " != " + Crc32C.maskedCrc32c(data))
    }
    data
  }

  private def fromInt64LE(data: Array[Byte]) = {
    assert(data.length == 8)
    val bb = ByteBuffer.wrap(data)
    bb.order(ByteOrder.LITTLE_ENDIAN)
    bb.getLong
  }

  private def fromInt32LE(data: Array[Byte]) = {
    assert(data.length == 4)
    val bb = ByteBuffer.wrap(data)
    bb.order(ByteOrder.LITTLE_ENDIAN)
    bb.getInt
  }
}