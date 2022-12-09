package network
import org.apache.logging.log4j.scala.Logging

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import java.util.concurrent.CountDownLatch
import io.grpc.{Server, ServerBuilder}
import shuffling.shuffling.{ShuffleRequest, ShuffleResponse, ShufflingGrpc, ShutdownWorkerServerRequest,ShutdownWorkerServerResponse}
import utils.util
import java.io.PrintWriter
import scala.io.Source

/* Worker Server의 역할은 data를 받는 역할!! => 즉, 이 서버가 열리면 이 서버에 담겨야하는 data들이 모여드는 것!!! */
class WorkerServer(executionContext: ExecutionContext, val numClient: Int, val Port: Int, val outputPath: String
                  ,val workerServerID: Int, val path: String) extends Logging {
  self =>
  private[this] var server: Server = null
  private val clientLatch: CountDownLatch = new CountDownLatch(numClient-1)
  private val workerIpList : ListBuffer[String] = ListBuffer[String]()
  private val printInstance : ListBuffer[PrintWriter] = ListBuffer[PrintWriter]()
  var isShutdown = 0
  def start() = {
    server = ServerBuilder.forPort(Port).addService(ShufflingGrpc.bindService(new ShufflingImpl, executionContext)).build.start
    for(i <- 0 until numClient){
      printInstance.append(new PrintWriter(outputPath + "fromMachine." + i))
    }
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
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
  private class ShufflingImpl extends ShufflingGrpc.Shuffling {
    override def shuffle(request: ShuffleRequest): Future[ShuffleResponse] = {
      val toFile = ListBuffer[String]()
      for (ele <- request.datas){
        toFile.append(ele)
      }
      val workerID = request.fromWorkerID
      for(line<- toFile){
        printInstance(workerID).write(line + "\r\n")
      }
      val response = ShuffleResponse(sendTerminate = 0)
      Future.successful(response)
    }

  override def shutdownWorkerServer(request: ShutdownWorkerServerRequest)= {
    if(request.workerID < numClient && request.workerID >= 0){
      printInstance(request.workerID).close()
      clientLatch.countDown()
    }
    clientLatch.await()
    printInstance(workerServerID).close
    isShutdown = 1
    val response = ShutdownWorkerServerResponse(shutdown = isShutdown)
    Future.successful(response)
  }
  }


}
