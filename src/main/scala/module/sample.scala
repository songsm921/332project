package module
import java.io._
import scala.jdk.CollectionConverters._
import scala.collection.mutable.ListBuffer
import scala.io.Source
class sample{
  val sampleKeyNum = 10
  def sampleFile(paths: ListBuffer[String]) = {
    val samples = ListBuffer[String]()
    for(path <- paths)
    {
      val linesList = Source.fromFile(path).getLines().map(_.splitAt(10)).toList
      val keyArray = linesList.map(_._1).toArray
      for (i <- 0 until sampleKeyNum) {
        samples.append(keyArray(scala.util.Random.nextInt(keyArray.length)))
      }
    }
    val finalSamples = ListBuffer[String]()
    for (i<-0 until sampleKeyNum){
      finalSamples.append(samples(scala.util.Random.nextInt(samples.length)))
    }
    finalSamples
  }
}
