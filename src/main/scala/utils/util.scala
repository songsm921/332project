package utils

import java.net.{DatagramSocket, InetAddress}
import scala.collection.mutable.ListBuffer
import scala.io.Source
import java.io.File
import java.io.PrintWriter
object util {
  def getMyIpAddress: String = {
    val socket = new DatagramSocket
    try {
      socket.connect(InetAddress.getByName("8.8.8.8"), 10002)
      socket.getLocalAddress.getHostAddress
    } finally if (socket != null) socket.close()
  }
  def parseArguments(args: Array[String]) = {
    val argList = ListBuffer(args: _ *)
    val masterInfo = argList.remove(0)
    val masterIP = masterInfo.split(":")(0)
    val masterPort = masterInfo.split(":")(1).toInt
    argList.remove(0) // remove -I
    val inputDirectoryList = ListBuffer[String]()
    while(argList.head != "-O"){
      inputDirectoryList.append(argList.remove(0))
    }
    argList.remove(0) // remove -O
    val outputPath = argList.remove(0)
    (masterIP, masterPort, inputDirectoryList, outputPath)
  }

  def splitFileper4MB(path: String,startLines: Int) = {
      val targetList = ListBuffer[String]()
      val lines = Source.fromFile(path).getLines().toList
      var isFinish = 0
      val destLines = startLines + 39999
      if(destLines >= lines.length){
        for(i <- startLines until lines.length){
          targetList.append(lines(i))
        }
        isFinish = 1
      }
      else{
        for (i <- startLines to destLines) {
          targetList.append(lines(i))
        }
        isFinish = 0
      }
      if(isFinish == 0){
        println("File : " + path + " completed " + destLines + " lines" + "/" + lines.length + "...")
        (targetList,destLines + 1)
      }
      else{
        println("File : " + path + " completed.")
        (targetList,-1)
      }

  }
  def copyOwnData(workerID: Int ,path: String, outputPath: String) = {
    val lines = Source.fromFile(path).getLines().toList
    new PrintWriter(outputPath + "fromMachine." + workerID.toString) {
      for (line <- lines) {
        write(line + "\r\n")
      }
      close
    }
  }

  def deleteTmpFiles(inputPath: String, outputPath: String) = {
    val inputDir = new File(inputPath)
    val outputDir = new File(outputPath)
    inputDir.listFiles.filter(_.getName.startsWith("toMachine")).foreach(_.delete)
    outputDir.listFiles.filter(_.getName.startsWith("fromMachine")).foreach(_.delete)
  }
}

object Phase extends Enumeration {
  val INITIAL = Value(0)
  val FRAGMENTATION = Value(1)
  val SORTING = Value(2)
  val SAMPLING = Value(3)
  val MERGING = Value(4)
  val BALANCING = Value(5)
  val TERMINATING = Value(6)
}

object workerPhase extends Enumeration{
  val INITIAL = Value(0)
  val FRAGMENTATION = Value(1)
  val SORTING = Value(2)
  val SAMPLING = Value(3)
  val MERGING = Value(4)
  val BALANCING = Value(5)
  val TERMINATING = Value(6)
}
