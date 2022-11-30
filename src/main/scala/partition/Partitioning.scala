package partition

import java.io.PrintWriter
import scala.collection.mutable.ListBuffer
import scala.io.Source

class Partitioning {

  private val instWriter = ListBuffer[PrintWriter]()

  def createWriterForTest(fileCount: Int) = {
    for (i <- 1 to fileCount) {
      instWriter.append(new PrintWriter("toMachine." + i.toString))
    }
  }

  def closeInstWriter() = {
    for (writer <- instWriter) {
      writer.close
    }
  }

  def partitionEachLine(path: String, rangeList: ListBuffer[(String, String)]) = {
    val lines = Source.fromFile(path).getLines().map(_.splitAt(10)).toList
    val partitionedLines: ListBuffer[(Int, String)] = ListBuffer()
    for (line <- lines) {
      var i = 0
      while (i < rangeList.length) {
        if (line._1 >= rangeList(i)._1 && line._1 < rangeList(i)._2) {
          partitionedLines.append((i, line._1 + line._2))
        }
        i = i + 1
      }
    }
    /*val printInst = new PrintWriter(path)
    for (line <- partitionedLines) {
      printInst.write((line._1 + 1).toString + " " + line._2 + "\r\n")
    }
    printInst.close*/
    for (i <- 0 until rangeList.length) {
      instWriter(i).append(partitionedLines.filter(_._1 == i).map(_._2 + "\r\n").mkString)
    }
  }

}
