import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.tensorflow.util.Event

// 假设这些函数和类在 Scala 中有对应的实现
object EventIO {
  def makeEvent(tag: String, value: Double): Event = ???
  def makeEvent(tag: String, values: Array[Double]): Event = ???
  def createEventStream(fname: java.io.File): Unit = ???
  def appendEvents(fname: java.io.File, events: Seq[Event]): Unit = ???
  def slurpEvents(loader: Event => Unit, param: Boolean, fname: java.io.File): Seq[Any] = ???
}

class EventIOTest extends AnyFlatSpec with Matchers {
  "EventIO" should "create an Event for a double value" in {
    val event = EventIO.makeEvent("tagname", 0.8)
    event shouldBe a [Event]
  }

  it should "create an Event for histogram values" in {
    val arrayEvent = EventIO.makeEvent("tagname", (2 to 11).map(_.toDouble).toArray)
    arrayEvent shouldBe a [Event]

    val nestedArrayEvent = EventIO.makeEvent("tagname", Array((2 to 11).map(_.toDouble).toArray).flatten)
    nestedArrayEvent shouldBe a [Event]
  }

  "Tag and simple values" should "be correct" in {
    val fname = java.io.File.createTempFile("tfevents", ".out")
    val tag = "a/fdd2"
    EventIO.createEventStream(fname)
    val events = (0 until 10).map(i => EventIO.makeEvent(tag, i.toDouble))
    EventIO.appendEvents(fname, events)
    val resp = EventIO.slurpEvents(_ => (), true, fname)

    val tags = (0 until 10).map(i => resp(i + 1).asInstanceOf[Map[String, Any]]
      .get("summary").asInstanceOf[Map[String, Any]]
      .get("value").asInstanceOf[Seq[Map[String, Any]]](0)
      .get("tag").asInstanceOf[String])
    tags.forall(_ == tag) shouldBe true

    val simpleValues = (0 until 10).map(i => resp(i + 1).asInstanceOf[Map[String, Any]]
      .get("summary").asInstanceOf[Map[String, Any]]
      .get("value").asInstanceOf[Seq[Map[String, Any]]](0)
      .get("simple-value").asInstanceOf[Double])
    simpleValues shouldBe (0 until 10).map(_.toDouble)

    val steps = (1 until 10).map(i => resp(i + 1).asInstanceOf[Map[String, Any]]
      .get("step").asInstanceOf[Int])
    steps shouldBe (1 until 10)
  }

  "Histograms" should "have correct tags and bucket values" in {
    val fname = java.io.File.createTempFile("tfeventhist", ".out")
    val tag = "a/hist"
    EventIO.createEventStream(fname)
    val events = (0 until 10).map(i => {
      val values = (i until i + 10).map(_.toDouble).toArray
      EventIO.makeEvent(tag + i.toString, values)
    })
    EventIO.appendEvents(fname, events)
    val resp = EventIO.slurpEvents(_ => (), true, fname)

    val expectedTags = (0 until 10).map(i => tag + i.toString)
    val actualTags = (0 until 10).map(i => resp(i + 1).asInstanceOf[Map[String, Any]]
      .get("summary").asInstanceOf[Map[String, Any]]
      .get("value").asInstanceOf[Seq[Map[String, Any]]](0)
      .get("tag").asInstanceOf[String])
    actualTags shouldBe expectedTags

    val bucketValues = resp(1).asInstanceOf[Map[String, Any]]
      .get("summary").asInstanceOf[Map[String, Any]]
      .get("value").asInstanceOf[Seq[Map[String, Any]]](0)
      .get("histo").asInstanceOf[Map[String, Any]]
      .get("bucket").asInstanceOf[Seq[Double]]
    bucketValues.forall(_ == 1.0) shouldBe true
  }
}
