package merge

import java.io.PrintWriter
import scala.collection.mutable.ListBuffer

class Merge {

  private val dataList = ListBuffer[(String, String)]()
  private val sortedDataList = ListBuffer[(String, String)]()

  def mergeDataPrototype(dataListFromMachine: ListBuffer[String]) = {
    dataList.append((dataListFromMachine.splitAt(10)._1.mkString, dataListFromMachine.splitAt(10)._2.mkString))
  }

  def sortMergedData(): ListBuffer[(String, String)] = {
    sortedDataList.appendAll(dataList.sortBy(_._1))
  }

  def writeMergedData(outputPath: String) = {
    val printInst = new PrintWriter(outputPath)
    for (line <- dataList) {
      printInst.write(line._1 + line._2 + "\r\n")
    }
    printInst.close
  }

}
