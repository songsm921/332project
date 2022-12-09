package module
import java.io._
import java.io.PrintWriter
import scala.collection.mutable.ListBuffer
import scala.io.Source
class merge{
  //Implement K-way Merge sort
  def mergeSort(outputPath : String, totalWorkerNum : Int, workerID : Int) = {
    new PrintWriter(outputPath + "Result." + workerID.toString) {
      for(i <- 0 until totalWorkerNum){
        val lines = Source.fromFile(outputPath + "fromMachine." + i.toString).getLines()
        for (line <- lines) {
          write(line + "\r\n")
    }
      }
      close
    }
    val lines = Source.fromFile(outputPath + "Result." + workerID.toString).getLines().map(_.splitAt(10)).toList
    val sortedLinesList = lines.sortBy(_._1)
    new PrintWriter(outputPath + "Result." + workerID.toString) {
      for (line <- sortedLinesList) {
        write(line._1 + line._2 + "\r\n")
      }
      close
    }
  }

}