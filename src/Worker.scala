import java.io._
import scala.jdk.CollectionConverters._
import scala.collection.mutable.ListBuffer
import scala.io.Source
object fileIO{
  val splitSizeMB = 32
  val oneLineSize = 0.0001 /*100 MegaByte*/
  val filename = "split"
  var fileCount = 0
  def readFileEachLine(path: String): Unit ={
    for(line <- Source.fromFile(path).getLines()){
      println(line)
    }
  }
  def writeFile(path: String) = {
    new PrintWriter(path) {
      write("Hello World!")
      close

    }
  }
  /*def splitFile(path: String,count: Int) = {
    val lineCount = Source.fromFile(path).getLines().size
    val iterCount = lineCount / count
    val filename = "split"
    val lines = ListBuffer[String]()
    var i = 0
    for(line <- Source.fromFile(path).getLines()){
      lines.append(line)
      i = i + 1
      if(i % iterCount ==0 && i!=0){
        new PrintWriter(filename + ((i / iterCount)-1).toString){
          while(!lines.isEmpty){
            write(lines.remove(0))
            write("\n")
            //lines.remove(0)
          }
          close
        }
      }
    }
    if(!lines.isEmpty){
      new PrintWriter(filename + ((i/iterCount)).toString){
        while(!lines.isEmpty){
          write(lines.remove(0))
          write("\n")
        }
        close
      }
    }
  }*/
  /* default = 32MB*/
  def splitFile(path: String,sizeMB : Double = splitSizeMB) = {
    val lineCount = Source.fromFile(path).getLines().size
    val rawFileSize = lineCount * oneLineSize
    val linePerSplitedFile = sizeMB / oneLineSize
    val lines = ListBuffer[String]()
    var i = 0
    if(rawFileSize < sizeMB){
      for(line <- Source.fromFile(path).getLines()){
        lines.append(line)
        }
      fileCount = fileCount + 1
      new PrintWriter(filename + (fileCount).toString) {
        while (!lines.isEmpty) {
          write(lines.remove(0)+"\n")
        }
        close
      }
    }
    else{
      for(line <- Source.fromFile(path).getLines()){
        lines.append(line)
        i = i + 1
        if(i % linePerSplitedFile ==0 && i!=0){
          fileCount = fileCount + 1
          new PrintWriter(filename + (fileCount).toString){
            while(!lines.isEmpty){
              write(lines.remove(0)+"\n")
            }
            close
          }
        }

      }
      if(!lines.isEmpty){
        fileCount = fileCount + 1
        new PrintWriter(filename + fileCount){
          while(!lines.isEmpty){
            write(lines.remove(0)+"\n")
          }
          close
        }
      }
    }

  }
  def main(args: Array[String]) : Unit = {
    for(arg<-args) {
      splitFile(arg)
    }
  }

}