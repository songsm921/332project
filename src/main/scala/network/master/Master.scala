package network.master

import io.grpc.{Server, ServerBuilder}
import org.apache.logging.log4j.scala.Logging
import java.util.concurrent.CountDownLatch
import scala.concurrent.{ExecutionContext, Future}

import network.common.Util.getMyIpAddress
import distributed.distributed.{
  DistributedGrpc, ConnectionCheckRequest, ConnectionCheckResponse,
  DoneRequest, DoneResponse, IsSendingDone, PartitionedDataRequest, PartitionedData, PartitionedDataResponse,
  SampleRequest, SampleResponse
}

object Master{
  def main(args: Array[String]): Unit = {
    val numClient = args.headOption
    if (numClient.isEmpty) {
      return
    }

    val server = new Master(ExecutionContext.global, numClient.get.toInt)
    server.start()
    server.printEndpoint()
    server.blockUntilShutdown()
  }

  private val port = 50051
}

class WorkerClient(val id: Int, val ip: String) {
  override def toString: String = ip
}

class Master(executionContext: ExecutionContext, val numClient: Int) extends Logging { self =>
  private[this] var server: Server = null
  private val clientLatch: CountDownLatch = new CountDownLatch(numClient)
  var slaves: Vector[WorkerClient] = Vector.empty

  private def start(): Unit = {
    server = ServerBuilder.forPort(Master.port).addService(DistributedGrpc.bindService(new DistributedImpl, executionContext)).build.start
    //Add Service Implementation here
    //server = ServerBuilder.forPort(Master.port).addService(FragServiceGrpc.bindService(new FragImpl, executionContext)).build.addService(SortServiceGrpc.bindService(new SortImpl, executionContext)).build.start
    logger.info("Server numClient: " + self.numClient)
    logger.info("Server started, listening on " + Master.port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  private def printEndpoint(): Unit = {
    System.out.println(getMyIpAddress + ":" + Master.port)
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination(450, java.util.concurrent.TimeUnit.SECONDS)
    }
  }

  private def addNewSlave(ipAddress: String): Int = {
    this.synchronized {
      slaves foreach{ slave => if (slave.ip == ipAddress) return slave.id }
      this.slaves = this.slaves :+ new WorkerClient(this.slaves.length, ipAddress)
      if (this.slaves.length == this.numClient) printSlaveIpAddresses()
      slaves.last.id
    }
  }

  private def printSlaveIpAddresses(): Unit = {
    System.out.println(this.slaves.mkString(", "))
  }

  private class DistributedImpl extends DistributedGrpc.Distributed {
    override def connectionCheck(req: ConnectionCheckRequest) = {
      logger.info("sayHello from " + req.ipAddress)
      val _machineID = addNewSlave(req.ipAddress)
      clientLatch.await()
      val reply = ConnectionCheckResponse(machineID = _machineID)
      Future.successful(reply)
    }

    override def getSampleRange(request: SampleRequest): Future[SampleResponse] = ???

    override def requestPartitionedData(request: PartitionedDataRequest): Future[PartitionedDataResponse] = ???

    override def sendPartitionedData(request: PartitionedData): Future[IsSendingDone] = ???

    override def taskDoneReport(request: DoneRequest): Future[DoneResponse] = ???


  }

}
