package network
import io.grpc.{Server, ServerBuilder}

import scala.concurrent.{ExecutionContext, Future}
import org.apache.logging.log4j.scala.Logging
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}
import java.util.concurrent.CountDownLatch
import java.io._
import scala.jdk.CollectionConverters._
import scala.collection.mutable.ListBuffer
import scala.io.Source
import java.util.concurrent.TimeUnit
import scala.concurrent.{Future, Promise}
import utils.{util, Phase, workerPhase}
import generalnet.generalNet.{GeneralnetGrpc,Connect2ServerRequest,Connect2ServerResponse,SortEndMsg2MasterRequest,SortEndMsg2MasterResponse,
  SamplingEndMsg2MasterRequest, SamplingEndMsg2MasterResponse, PartitioningEndMsg2MasterRequest, PartitioningEndMsg2MasterResponse,
  StartShufflingMsg2MasterRequest,StartShufflingMsg2MasterResponse,MergeSortEndMsg2MasterRequest,MergeSortEndMsg2MasterResponse,
  TaskDoneMsg2MasterRequest,TaskDoneMsg2MasterResponse}
import shuffling.shuffling.{ShufflingGrpc,ShuffleRequest,ShuffleResponse,ShutdownWorkerServerRequest,ShutdownWorkerServerResponse}
import module.{sort,sample,partition,merge}

class workerClient(host: String, port: Int, outputAbsoluteDir : String) extends Logging {
  val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().asInstanceOf[ManagedChannelBuilder[_]].build
  val stub = GeneralnetGrpc.blockingStub(channel)
  val inputAbsolutePath = ListBuffer[String]()
  var totalWorkerNum = -1
  var myWorkerNum = -1
  val workersIPList = ListBuffer[String]()
  var samplesList2Master = ListBuffer[String]()
  var partitionRanges = Array[String]()
  var myPartitionRange = Array[String]("noRangeYet", "noRangeYet")
  def shutdown() = {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
  }

  def addInputAbsolutePath(Path : ListBuffer[String]) = {
    inputAbsolutePath.appendAll(Path)
  }

  def connect2Server(): Unit = {
    val request = Connect2ServerRequest(workerIpAddress = util.getMyIpAddress)
    try{
      val response = stub.connect2Server(request)
      totalWorkerNum = response.workerNum
      myWorkerNum = response.workerID
      workersIPList.appendAll(response.workerIPList)
    }
    catch
      {
        case e: StatusRuntimeException =>
          logger.warn(s"RPC failed: ${e.getStatus}")
          return
      }
  }

  def startSort() = {
  val sortInstance = new sort()
  for(path <- inputAbsolutePath){
    sortInstance.sortFile(path)
    }
  }
  def sortEndMsg2Master(): Unit = {
    val request = SortEndMsg2MasterRequest(workerID = myWorkerNum)
    try{
      val response = stub.sortEndMsg2Master(request)
    }
    catch
      {
        case e: StatusRuntimeException =>
          logger.warn(s"RPC failed: ${e.getStatus}")
          return
      }
  }

  def startSampling()   = {
    val sampleInstance  = new sample()
    samplesList2Master = sampleInstance.sampleFile(inputAbsolutePath)
  }

  def samplingEndMsg2Master(): Unit = {
    val samples2Master = samplesList2Master.toList.sorted
    val request = SamplingEndMsg2MasterRequest(workerID = myWorkerNum, samples = samples2Master)
    try {
      val response = stub.samplingEndMsg2Master(request)
      partitionRanges = partitionRanges.concat(response.totalSamples.toArray)
      myPartitionRange(0) = partitionRanges(2 * myWorkerNum)
      myPartitionRange(1) = partitionRanges(2 * myWorkerNum + 1)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.warn(s"RPC failed: ${e.getStatus}")
        return
    }
  }

  def startPartitioning(firstInputPath : String): Unit = {
    val partitionInstance = new partition()
    partitionInstance.createWriterForTest(totalWorkerNum,firstInputPath)
    for(path <- inputAbsolutePath){
      partitionInstance.partitionEachLine(path, partitionRanges)
    }
    partitionInstance.closeInstWriter()
  }

  def partitioningEndMsg2Master(): Unit = {
    val request = PartitioningEndMsg2MasterRequest(workerID = myWorkerNum)
    try{
      val response = stub.partitioningEndMsg2Master(request)
    }
    catch
      {
        case e: StatusRuntimeException =>
          logger.warn(s"RPC failed: ${e.getStatus}")
          return
      }
  }

  def startShufflingMsg2Master(i: Int): Int = {
    val request = StartShufflingMsg2MasterRequest(workerID = i)
    try{
      val response = stub.startShufflingMsg2Master(request)
      response.nextServerWorkerID
    }
    catch
      {
        case e: StatusRuntimeException =>
          logger.warn(s"RPC failed: ${e.getStatus}")
          -1
      }
  }
  def startMergeSort(): Unit = {
    val mergesortInstance = new merge()
    mergesortInstance.mergeSort(outputAbsoluteDir, totalWorkerNum,myWorkerNum)
  }
  def mergeSortEndMsg2Master(): Int = {
    val request = MergeSortEndMsg2MasterRequest(workerID = myWorkerNum)
    try{
      val response = stub.mergeSortEndMsg2Master(request)
      response.status
    }
    catch
      {
        case e: StatusRuntimeException =>
          logger.warn(s"RPC failed: ${e.getStatus}")
          return -1
      }
  }
  def taskDoneMsg2Master(): Int = {
    val request = TaskDoneMsg2MasterRequest(workerID = myWorkerNum)
    try{
      val response = stub.taskDoneMsg2Master(request)
      response.status
    }
    catch
      {
        case e: StatusRuntimeException =>
          logger.warn(s"RPC failed: ${e.getStatus}")
          return -1
      }
  }


}
