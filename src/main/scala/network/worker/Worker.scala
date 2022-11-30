package network.worker

import distributed.distributed.DistributedGrpc.{DistributedBlockingClient, DistributedBlockingStub}
import org.apache.logging.log4j.scala.Logging
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}
import java.util.concurrent.TimeUnit
import scala.concurrent.{Future, Promise}
import network.common.Util.getMyIpAddress
import network.common.Phase
import distributed.distributed.{
  DistributedGrpc, ConnectionCheckRequest, ConnectionCheckResponse,
  DoneRequest, DoneResponse, IsSendingDone, PartitionedDataRequest, PartitionedData, PartitionedDataResponse,
  SampleRequest, SampleResponse
}
import scala.concurrent.Promise


object Worker {
  def apply(host: String, port: Int): Worker = {
    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().asInstanceOf[ManagedChannelBuilder[_]].build
//    val blockingStub = FragServiceGrpc.blockingStub(channel)
    val stub = DistributedGrpc.blockingStub(channel)
    new Worker(channel, stub)
  }

  def main(args: Array[String]): Unit = {
    var currentPhase = Phase.INITIAL
    val masterEndpoint = args.headOption
    if (masterEndpoint.isEmpty) {
      System.out.println("Master ip:port argument is empty.")
      System.exit(1)
    }
    val splitedEndpoint = masterEndpoint.get.split(':')
    val client = Worker(splitedEndpoint(0), splitedEndpoint(1).toInt)
    try {
      //add what slave should do here
      client.connectionCheck()
      // FIX: Fix phase
      currentPhase = Phase.TERMINATING
    } finally {
      client.shutdown()
    }
  }
}

class Worker private(
                      private val channel: ManagedChannel,
                      private val blockingStub: DistributedGrpc.DistributedBlockingStub
                    ) extends Logging {
  var machineID = -1;

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def connectionCheck(): Unit = {
    val request = ConnectionCheckRequest(ipAddress = getMyIpAddress)
    try {
      val response = blockingStub.connectionCheck(request)
      machineID = response.machineID
      logger.info("SendMessage: " + response.machineID)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.warn(s"RPC failed: ${e.getStatus}")
    }
    //response
  }

  def getSampleRange(request: SampleRequest): Future[SampleResponse] = ???

  def requestPartitionedData(request: PartitionedDataRequest): Future[PartitionedDataResponse] = ???

  def sendPartitionedData(request: PartitionedData): Future[IsSendingDone] = ???

  def taskDoneReport(request: DoneRequest): Future[DoneResponse] = ???
}