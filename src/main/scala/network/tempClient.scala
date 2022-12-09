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
  StartShufflingMsg2MasterRequest,StartShufflingMsg2MasterResponse}
import shuffling.shuffling.{ShufflingGrpc,ShuffleRequest,ShuffleResponse,ShutdownWorkerServerRequest,ShutdownWorkerServerResponse}
import module.{sort,sample,partition}

class tempClient(host: String, port: Int, outputAbsoluteDir : String, myWorkerNum : Int) extends Logging {
  val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().asInstanceOf[ManagedChannelBuilder[_]].build
  val stub = ShufflingGrpc.blockingStub(channel)
  val inputAbsolutePath = ListBuffer[String]()
  var totalWorkerNum = -1
  val workersIPList = ListBuffer[String]()
  var samplesList2Master = ListBuffer[String]()
  var partitionRanges = Array[String]()
  var myPartitionRange = Array[String]("noRangeYet", "noRangeYet")
  def shutdown() = {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
  }


  def Shuffle(data: ListBuffer[String],sendFinish: Int): Int = {
    val request = ShuffleRequest(datas = data.toList,myWorkerNum)
    try{
      val response = stub.shuffle(request)
      return response.sendTerminate
    }
    catch
    {
      case e: StatusRuntimeException =>
        logger.warn(s"RPC failed: ${e.getStatus}")
        return -1
    }
  }
  def ShutdownWorkerServer(): Unit = {
    val request = ShutdownWorkerServerRequest(workerID = myWorkerNum)
    try{
      val response = stub.shutdownWorkerServer(request)
    }
    catch
    {
      case e: StatusRuntimeException =>
        logger.warn(s"RPC failed: ${e.getStatus}")
        return
    }
  }
}

