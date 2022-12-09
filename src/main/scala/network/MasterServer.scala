package network

import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}
import java.util.concurrent.CountDownLatch
import io.grpc.{Server, ServerBuilder}
import utils.util.getMyIpAddress
import scala.collection.mutable.ListBuffer
import generalnet.generalNet.{Connect2ServerRequest, Connect2ServerResponse,SortEndMsg2MasterRequest,SortEndMsg2MasterResponse, GeneralnetGrpc,
  SamplingEndMsg2MasterRequest, SamplingEndMsg2MasterResponse, PartitioningEndMsg2MasterRequest, PartitioningEndMsg2MasterResponse,
  StartShufflingMsg2MasterRequest,StartShufflingMsg2MasterResponse,MergeSortEndMsg2MasterRequest,MergeSortEndMsg2MasterResponse,
  TaskDoneMsg2MasterRequest,TaskDoneMsg2MasterResponse}

object MasterServer{
  var numFinishGetSamples = 0
  var nextShuffleServerID = 0
  val totalSampleList = ListBuffer[String]()
}
class MasterServer(executionContext: ExecutionContext, val numClient: Int, val Port: Int) extends Logging {
  self =>
  private[this] var server: Server = null
  private val clientLatch: CountDownLatch = new CountDownLatch(numClient)
  private val sortLatch: CountDownLatch = new CountDownLatch(numClient)
  private val sampleLatch: CountDownLatch = new CountDownLatch(numClient)
  private val partitionLatch: CountDownLatch = new CountDownLatch(numClient)
  private val shuffleLatch: ListBuffer[CountDownLatch] =  new ListBuffer[CountDownLatch]()
  private val mergeLatch: CountDownLatch = new CountDownLatch(numClient)
  private val taskLatch: CountDownLatch = new CountDownLatch(numClient)
  private val workerIPList : ListBuffer[String] = ListBuffer[String]()
  def start() = {
    server = ServerBuilder.forPort(Port).addService(GeneralnetGrpc.bindService(new GeneralnetImpl, executionContext)).build.start
    logger.info("Server numClient: " + self.numClient)
    logger.info("Server started, listening on " + Port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
    for(i<-0 until numClient){
      shuffleLatch.append(new CountDownLatch(numClient))
    }
  }

  def printEndpoint(): Unit = {
    System.out.println(getMyIpAddress + ":" + Port)
  }
  def stop() = {
    if (server != null) {
      server.shutdown()
    }
  }
  def blockUntilShutdown() = {
    if (server != null) {
      server.awaitTermination()
    }
  }
  private class GeneralnetImpl extends GeneralnetGrpc.Generalnet {
    override def connect2Server(request: Connect2ServerRequest): Future[Connect2ServerResponse] = {
      val _workerID_ = workerIPList.length
      workerIPList.append(request.workerIpAddress)
      println(request.workerIpAddress)
      clientLatch.countDown()
      clientLatch.await()
      val response = Connect2ServerResponse(workerID = _workerID_,workerNum = numClient,workerIPList = workerIPList.toList)
      Future.successful(response)
    }

    override def sortEndMsg2Master(request: SortEndMsg2MasterRequest): Future[SortEndMsg2MasterResponse] = {
      sortLatch.countDown()
      sortLatch.await()
      val response = SortEndMsg2MasterResponse(startNext = 1)
      Future.successful(response)
    }

    override def samplingEndMsg2Master(request: SamplingEndMsg2MasterRequest): Future[SamplingEndMsg2MasterResponse] = {
      while(MasterServer.numFinishGetSamples < numClient){
       if(MasterServer.numFinishGetSamples == request.workerID){
         MasterServer.totalSampleList.appendAll(request.samples)
         MasterServer.numFinishGetSamples += 1
         sampleLatch.countDown()
       }
       else{
          Thread.sleep(100)
       }
      }
      sampleLatch.await()
      val sortedSampleList = MasterServer.totalSampleList.sorted
      val rangeEachMachine : ListBuffer[(String,String)] = ListBuffer[(String,String)]()
      val firstRange = " " * 10
      val lastRange = "~" * 10
      for(i<-1 to numClient){
        if(i == 1){
          rangeEachMachine.append((firstRange,sortedSampleList((sortedSampleList.length/numClient) * i - 1)))
        }
        else if(i == numClient){
          rangeEachMachine.append((rangeEachMachine(i-2)._2,lastRange))
        }
        else{
          rangeEachMachine.append((rangeEachMachine(i-2)._2,sortedSampleList((sortedSampleList.length/numClient) * i - 1)))
        }
      }
      val _rangeSequence = rangeEachMachine.toList.flatten{case (a,b)=>List(a,b)}
      val response = SamplingEndMsg2MasterResponse(totalSamples = _rangeSequence)
      Future.successful(response)
    }

    override def partitioningEndMsg2Master(request: PartitioningEndMsg2MasterRequest): Future[PartitioningEndMsg2MasterResponse] = {
      partitionLatch.countDown()
      partitionLatch.await()
      val response = PartitioningEndMsg2MasterResponse(startNext = 1)
      Future.successful(response)
    }
    override def startShufflingMsg2Master(request: StartShufflingMsg2MasterRequest): Future[StartShufflingMsg2MasterResponse] = {
      shuffleLatch(request.workerID).countDown()
      shuffleLatch(request.workerID).await()
      val response = StartShufflingMsg2MasterResponse(nextServerWorkerID = MasterServer.nextShuffleServerID)
      Future.successful(response)
    }

    override def mergeSortEndMsg2Master(request: MergeSortEndMsg2MasterRequest): Future[MergeSortEndMsg2MasterResponse] = {
      mergeLatch.countDown()
      mergeLatch.await()
      val response = MergeSortEndMsg2MasterResponse(status = 1)
      Future.successful(response)
    }

    override def taskDoneMsg2Master(request: TaskDoneMsg2MasterRequest): Future[TaskDoneMsg2MasterResponse] = {
      taskLatch.countDown()
      taskLatch.await()
      val response = TaskDoneMsg2MasterResponse(status = 1)
      server.shutdown()
      Future.successful(response)
    }
  }
}