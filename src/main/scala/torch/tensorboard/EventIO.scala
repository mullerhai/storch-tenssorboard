package torch.tensorboard

import java.io.{DataInputStream, DataOutputStream, File, InputStream, OutputStream}
import java.util.Date
import scala.collection.mutable.ListBuffer
import org.tensorflow.framework.{Summary, HistogramProto}
import org.tensorflow.util.Event
import org.tensorflow.hadoop.util.TFRecordReader
import org.tensorflow.hadoop.util.TFRecordWriter

// 假设 Histogram 相关逻辑在这个对象里实现
object Histogram {
  def makeHistogram(values: Seq[Double]): Map[String, Any] = {
    // 这里需要实现 makeHistogram 具体逻辑
    Map.empty
  }
}

// 状态类，替代 Clojure 的原子操作
class State {
  private var wallTime: Double = 0
  private var tags: Map[String, Map[String, Int]] = Map.empty

  def getTime: Double = new Date().getTime.toDouble / 1000

  def reset(): Unit = {
    wallTime = getTime
    tags = Map.empty
  }

  def updateTagState(tag: String): Unit = {
    tags.get(tag) match {
      case Some(tagState) =>
        val newStep = tagState("step") + 1
        tags = tags.updated(tag, tagState.updated("step", newStep))
      case None =>
        tags = tags.updated(tag, Map("step" -> 0))
    }
  }

  def getTagState(tag: String): (Int, Double) = {
    updateTagState(tag)
    val step = tags(tag)("step")
    val currentWallTime = getTime
    (step, currentWallTime - wallTime)
  }
}

object EventIO {
  private val state = new State()

  def slurpEvents(filePath: String): Seq[Event] = slurpEvents(true, filePath)

  def slurpEvents(compressed: Boolean, filePath: String): Seq[Event] = {
    slurpEvents((bytes: Array[Byte]) => Event.parseFrom(bytes), compressed, filePath)
  }

  def slurpEvents(loadFn: Array[Byte] => Event, compressed: Boolean, filePath: String): Seq[Event] = {
    val file = new File(filePath)
    val inputStream = new DataInputStream(fileInputStream(file))
    try {
      val tfr = new TFRecordReader(inputStream, compressed)
      val result = new ListBuffer[Event]()
      var res: Array[Byte] = tfr.read()
      while (res != null) {
        result += loadFn(res)
        res = tfr.read()
      }
      result.toSeq
    } finally {
      inputStream.close()
    }
  }

  private def fileInputStream(file: File): InputStream = {
    java.io.FileInputStream(file)
  }

  private def startEvent(): Event = {
    val wallTime = state.getTime
    Event.newBuilder()
      .setWallTime(wallTime)
      .setFileVersion("brain.Event:2")
      .build()
  }

  def appendEvents(filePath: String, events: Seq[Event]): Unit = {
    val file = new File(filePath)
    val outputStream = new DataOutputStream(fileOutputStream(file, true))
    try {
      val tw = new TFRecordWriter(outputStream)
      events.foreach(event => tw.write(event.toByteArray))
    } finally {
      outputStream.close()
    }
  }

  private def fileOutputStream(file: File, append: Boolean): OutputStream = {
    java.io.FileOutputStream(file, append)
  }

  def createEventStream(filePath: String): Unit = {
    state.reset()
    val file = new File(filePath)
    if (file.exists()) file.delete()
    appendEvents(filePath, Seq(startEvent()))
  }

  private def summary(tag: String, summary: Summary): Event = {
    val (step, wallTime) = state.getTagState(tag)
    Event.newBuilder()
      .setSummary(summary)
      .setStep(step)
      .setWallTime(wallTime)
      .build()
  }

  def makeEvent(tag: String, v: Double): Event = {
    val v1 = v match {
      case Double.PositiveInfinity => Float.MaxValue
      case Double.NegativeInfinity => Float.MinValue
      case _ => v.toFloat
    }
    val sum1 = Summary.newBuilder()
      .addValue(
        Summary.Value.newBuilder()
          .setTag(tag)
          .setSimpleValue(v1)
          .build()
      )
      .build()
    summary(tag, sum1)
  }

  private def buildHist(tag: String, values: Seq[Double]): Summary = {
    val histData = Histogram.makeHistogram(values)
    val min = histData("min").asInstanceOf[Double]
    val max = histData("max").asInstanceOf[Double]
    val num = histData("num").asInstanceOf[Long]
    val sum = histData("sum").asInstanceOf[Double]
    val sumSquares = histData("sum-squares").asInstanceOf[Double]
    val bucket = histData("bucket").asInstanceOf[Seq[Long]]
    val bucketLimit = histData("bucket-limit").asInstanceOf[Seq[Double]]

    Summary.newBuilder()
      .addValue(
        Summary.Value.newBuilder()
          .setTag(tag)
          .setHisto(
            HistogramProto.newBuilder()
              .setMin(min)
              .setMax(max)
              .setNum(num)
              .setSum(sum)
              .setSumSquares(sumSquares)
              .addAllBucket(bucket)
              .addAllBucketLimit(bucketLimit)
              .build()
          )
          .build()
      )
      .build()
  }

  def makeEvent(tag: String, values: Array[Double]): Event = {
    val sum1 = buildHist(tag, values.toSeq)
    summary(tag, sum1)
  }

  def makeEvent(tag: String, values: Seq[Seq[Double]]): Event = {
    val flatValues = values.flatten
    val sum1 = buildHist(tag, flatValues)
    summary(tag, sum1)
  }
}
