import java.io._
import scala.jdk.CollectionConverters._
import scala.collection.mutable.ListBuffer
import scala.io.Source
object Worker{
  class IO(masterIP: String,outputPath: String){
    def getMasterIP(): String = masterIP
    def getOuputPath(): String = outputPath
  }
  class Fragmentation{
    private var fileCount = 0
    private val splitSizeMB = 32
    private val sizePerLineMB = 0.0001 // 10KB
    private val fileNamePrefix = "partition"
    def getFileCount(): Int = fileCount
    def splitFile(path: String, sizeMB: Double = splitSizeMB): Unit = {
      val lineCount = Source.fromFile(path).getLines().size
      val rawFileSize = lineCount * sizePerLineMB
      val linePerSplitedFile = sizeMB / sizePerLineMB
      val lines = ListBuffer[String]()
      var i = 0
      if (rawFileSize < sizeMB) {
        for (line <- Source.fromFile(path).getLines()) {
          lines.append(line)
        }
        fileCount = fileCount + 1
        new PrintWriter(fileNamePrefix + "." +(fileCount).toString) {
          while (!lines.isEmpty) {
            write(lines.remove(0) + "\r\n")
          }
          close
        }
      }
      else {
        for (line <- Source.fromFile(path).getLines()) {
          lines.append(line)
          i = i + 1
          if (i % linePerSplitedFile == 0 && i != 0) {
            fileCount = fileCount + 1
            new PrintWriter(fileNamePrefix + "." + (fileCount).toString) {
              while (!lines.isEmpty) {
                write(lines.remove(0) + "\r\n")
              }
              close
            }
          }

        }
        if (!lines.isEmpty) {
          fileCount = fileCount + 1
          new PrintWriter(fileNamePrefix + "." + fileCount) {
            while (!lines.isEmpty) {
              write(lines.remove(0) + "\r\n")
            }
            close
          }
        }
      }
    }
  }
  class Sort{
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
  }

  def main(args: Array[String]) : Unit = {
    val argList = ListBuffer(args: _ *)
    val instFragment = new Fragmentation()
    val instSort = new Sort()
    val masterIP = argList.remove(0)
    argList.remove(0)
    while(argList.head != "-O"){
      instFragment.splitFile(argList.remove(0))
    }
    argList.remove(0)
    val outputPath = argList.remove(0)
    val instIO = new IO(masterIP,outputPath)
    print(instIO.getMasterIP())
    print(instIO.getOuputPath())
    val fileCount = instFragment.getFileCount()
    for(i <- 1 to fileCount){
      instSort.sortFile("partition." + i.toString)
    }
  }

}


// 192,fff -I path1 path2 .... -O Outputpath