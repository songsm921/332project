package sorting

import java.io._
import scala.jdk.CollectionConverters._
import scala.collection.mutable.ListBuffer
import scala.io.Source

class Sort {

  def sortFile(path: String) = {
    val lines = Source.fromFile(path).getLines().map(_.splitAt(10)).toList
    val sortedLines = lines.sortBy(_._1)
    new PrintWriter(path) {
      for (line <- sortedLines) {
        write(line._1 + line._2 + "\r\n")
      }
      close
    }
  }

  def samplingFile(path: String, sampleSizeKB: Int = 100) = {
    val linesList = Source.fromFile(path).getLines().map(_.splitAt(10)).toList
    val keyArray = linesList.map(_._1).toArray
    val sampleSize = sampleSizeKB / 10
    val sampleArray = new Array[String](sampleSize)
    for (i <- 0 until sampleSize) {
      sampleArray(i) = keyArray(scala.util.Random.nextInt(keyArray.length))
    }
    val sampleList = sampleArray.toList
    sampleList

  }

}
