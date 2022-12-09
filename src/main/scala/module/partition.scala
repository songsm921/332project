package module

import java.io.PrintWriter
import scala.collection.mutable.ListBuffer
import scala.io.Source

class partition{
  private val instWriter = ListBuffer[PrintWriter]()

  def createWriterForTest(numWorker: Int,tempFilePath: String) = {
    for (i<-0 until numWorker){
      instWriter.append(new PrintWriter(tempFilePath +"toMachine." + i.toString))
    }
  }

  def closeInstWriter() = {
    for (writer <- instWriter) {
      writer.close
    }
  }

  def partitionEachLine(path: String, rangeList: Array[String]) = {
    val lines = Source.fromFile(path).getLines().map(_.splitAt(10)).toList
    val partitionedLines: ListBuffer[(Int, String)] = ListBuffer()
    val _rangeList_ = rangeList
    for (line <- lines) {
      var idx = 0
      while ((2*idx) < _rangeList_.length) {
        if (line._1 >= _rangeList_(2*idx) && line._1 < _rangeList_(2*idx + 1)) {
          partitionedLines.append((idx, line._1 + line._2))
        }
        idx = idx + 1
      }
      if(line._1 == "~~~~~~~~~~"){
        partitionedLines.append((_rangeList_.length / 2 - 1, line._1 + line._2))
      }
    }
    for (i <- 0 until instWriter.length) {
      instWriter(i).append(partitionedLines.filter(_._1 == i).map(_._2 + "\r\n").mkString)
    }
  }
}
