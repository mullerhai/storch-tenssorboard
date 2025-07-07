import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

// 假设这些函数在 Scala 中有对应的实现
object Histogram {
  def makeHistogram(values: Seq[Double]): Map[String, Any] = ???
  def insertionIndex(arr1: Array[Double], arr2: Seq[Double]): Seq[Int] = ???
}

class HistTest extends AnyFlatSpec with Matchers {
  "makeHistogram" should "produce valid bucket limits" in {
    val values = (2 to 11).map(_.toDouble)
    val h = Histogram.makeHistogram(values)
    val expectedBuckets = Seq(
      2.1628190051144287, 3.1665833053880363, 4.214722379471477,
      5.099814079160488, 6.1707750357841915, 7.466637793298873,
      8.213301572628762, 9.034631729891638, 10.931904393168884,
      12.025094832485772
    )

    val bucketLimit = h.get("bucket-limit").asInstanceOf[Seq[Double]]
    val diffs = bucketLimit.zip(expectedBuckets).map { case (a, b) => a - b }
    diffs.forall(_ == 0.0) shouldBe true

    val bucket = h.get("bucket").asInstanceOf[Seq[Double]]
    bucket.forall(_ == 1.0) shouldBe true
  }

  "insertionIndex" should "produce valid insertion indices" in {
    val arr1 = (2 to 11).map(_.toDouble).toArray
    val arr2 = (2 to 11).map(_.toDouble)
    val result1 = Histogram.insertionIndex(arr1, arr2)
    result1 shouldBe (0 to 9).toSeq

    val arr3 = (2.1 to 12.0 by 1.0).map(_.toDouble)
    val result2 = Histogram.insertionIndex(arr1, arr3)
    result2 shouldBe (1 to 10).toSeq
  }
}
