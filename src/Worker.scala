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
    def samplingFile(path: String, sampleSizeKB: Int = 100)  = {
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
  class Patitioning{
    def partitionEachLine(path: String, rangeList: ListBuffer[(String,String)]) = {
      val lines = Source.fromFile(path).getLines().map(_.splitAt(10)).toList
      val partitionedLines : ListBuffer[(Int, String)] = ListBuffer()
      for (line <- lines) {
        var i = 0
        while (i < rangeList.length) {
          if (line._1 >= rangeList(i)._1 && line._1 <= rangeList(i)._2) {
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
      for(i <- 0 until rangeList.length) {
        val printInst = new PrintWriter("toMachine" + "." + (i + 1).toString)
        for (line <- partitionedLines) {
          if (line._1 == i) {
            printInst.write(line._2 + "\r\n")
          }
        }
        printInst.close
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
    val sampleElements = new ListBuffer[String]()
    for (i <- 1 to fileCount) {
      sampleElements.appendAll(instSort.samplingFile("partition." + i.toString))
    }
    val toMaster = sampleElements.sorted
    /* Until Network established, test by 1 machine (Worker = Master...)*/
    val countWorker = 4
    val rangeEachMachine = ListBuffer[(String,String)]()
    for(i <- 1 to countWorker){
      if(i == 1)
        rangeEachMachine.append(("          ",toMaster((toMaster.length/countWorker)*i-1)))
      else if(i == countWorker)
        rangeEachMachine.append((rangeEachMachine(i-2)._2,"~~~~~~~~~~"))
      else
        rangeEachMachine.append((rangeEachMachine(i-2)._2,toMaster((toMaster.length/countWorker)*i-1)))
    }
    for(ele <- rangeEachMachine){
      print(ele._1 + " " + ele._2 + "\n")
    }
    val instPartition = new Patitioning()
    for(i <- 1 to fileCount){
      instPartition.partitionEachLine("partition." + i.toString,rangeEachMachine)
    }
  }

}


// 192,fff -I path1 path2 .... -O Outputpath