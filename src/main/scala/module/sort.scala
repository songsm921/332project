package module

import java.io._
import scala.jdk.CollectionConverters._
import scala.collection.mutable.ListBuffer
import scala.io.Source
class sort{
  def sortFile(path: String) = {
    val linesList = Source.fromFile(path).getLines().map(_.splitAt(10)).toList
    val sortedLinesList = linesList.sortBy(_._1)
    new PrintWriter(path) {
      for (line <- sortedLinesList) {
        write(line._1 + line._2 + "\r\n")
      }
      close
    }
  }
}
