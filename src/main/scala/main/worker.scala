package main
import utils.{Phase, util, workerPhase}
import network.workerClient
import network.MasterServer
import network.WorkerServer
import network.tempClient
import org.apache.logging.log4j.scala.Logging
import java.io._
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext

object worker {
  val workerPort = 18219
  def main(args: Array[String]): Unit = {
    val parameter = util.parseArguments(args)
    val masterIP = parameter._1
    val masterPort = parameter._2
    val inputDirectoryList = parameter._3
    val outputPath = parameter._4
    val client = new workerClient(masterIP,masterPort,outputPath)
    for (inputDirectory <- inputDirectoryList){
      val dir =  new File(inputDirectory)
      val files = dir.listFiles.filter(_.isFile).toList
      val inputAbsolutePath = files.map {x => x.toString}
      client.addInputAbsolutePath(inputAbsolutePath.to(ListBuffer))
    }
    try{
      client.connect2Server()
      client.startSort()
      client.sortEndMsg2Master()
      client.startSampling()
      client.samplingEndMsg2Master()
      client.startPartitioning(inputDirectoryList(0))
      client.partitioningEndMsg2Master()
      println("All workers enter SHUFFLE Phase")
      /*Start Shuffling*/
      for(i <- 0 until client.totalWorkerNum){
        val serverWorkerID = client.startShufflingMsg2Master(i)
        if(i == client.myWorkerNum){
          println("Worker "+ client.myWorkerNum + " is server")
          val workerserver = new WorkerServer(ExecutionContext.global,client.totalWorkerNum,workerPort+i,outputPath,client.myWorkerNum,inputDirectoryList(0))
          workerserver.start()
          var check = 1
          while(check == 1){
            if(workerserver.isShutdown == 1){
              check = 0
            }
            else{
              Thread.sleep(10)
            }
          }
          workerserver.stop()
          println("Workerserver terminated : " + "Worker " + client.myWorkerNum)

        }
        else{
          Thread.sleep(100)
          println("Worker " + client.myWorkerNum + " is temporary client")
          val client2client = new tempClient(client.workersIPList(i),workerPort + i,outputPath,client.myWorkerNum)
          var isSplitFinish = 0
          var startLines = 0
          while(isSplitFinish == 0){
            val content:(ListBuffer[String],Int) = util.splitFileper4MB(inputDirectoryList(0)+"toMachine."+i.toString,startLines)
            if(content._2 == -1){
              isSplitFinish = 1
            }
            else{
              startLines = content._2
            }

            client2client.Shuffle(content._1,isSplitFinish)
          }
          client2client.ShutdownWorkerServer()
          client2client.shutdown()
          println("Temporary Client terminated : " + "Worker " + client.myWorkerNum)
        }
      }
      println("End of Shuffling")
      utils.util.copyOwnData(client.myWorkerNum,inputDirectoryList(0)+"toMachine."+ client.myWorkerNum.toString,outputPath)
      println("Worker " + client.myWorkerNum + " starts Merge Phase")
      client.startMergeSort()
      val status = client.mergeSortEndMsg2Master()
      if(status == 1){
        println("Worker " + client.myWorkerNum + " deletes temporary files....")
        util.deleteTmpFiles(inputDirectoryList(0),outputPath)
        client.taskDoneMsg2Master()
      }
      else{
        println("Merge Sort Fail")
      }
    }
    catch {
      case e: Exception => println("Exception: " + e)
    }
    finally {
      println("All task is done. Worker " + client.myWorkerNum + " is terminated")
      client.shutdown()
    }
  }
}
