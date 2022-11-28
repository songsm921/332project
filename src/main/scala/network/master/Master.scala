package network.master

import io.grpc.{Server, ServerBuilder}
import org.apache.logging.log4j.scala.Logging
import java.util.concurrent.CountDownLatch
import scala.concurrent.{ExecutionContext, Future}

import network.common.Util.getMyIpAddress
import fragment.fragment.
{FragServiceGrpc,
  FragRequest,
  FragReply}
import sorting.sorting.
{SortServiceGrpc,
  SortRequest,
  SortReply}

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
  private val messageLatch: CountDownLatch = new CountDownLatch(numClient)
  var slaves: Vector[WorkerClient] = Vector.empty
  var msgStacker = ""
  var temp = 1;

  private def start(): Unit = {
    server = ServerBuilder.forPort(Master.port).addService(FragServiceGrpc.bindService(new FragImpl, executionContext)).build.start
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

  private def addNewSlave(ipAddress: String): Unit = {
    this.synchronized {
      slaves foreach { slave => if (slave.ip == ipAddress) return }
      this.slaves = this.slaves :+ new WorkerClient(this.slaves.length, ipAddress)
      if (this.slaves.length == this.numClient) printSlaveIpAddresses()
    }
  }

  private def printSlaveIpAddresses(): Unit = {
    System.out.println(this.slaves.mkString(", "))
  }

  private class FragImpl extends FragServiceGrpc.FragService {
    override def sayHello(req: FragRequest) = {
      logger.info("sayHello from " + req.name)
      messageLatch.countDown()
      msgStacker += f"${temp}: add ${req.name} "
      messageLatch.await()
      clientLatch.countDown()
      addNewSlave(req.name)
      clientLatch.await()
      val reply = FragReply(message = msgStacker)
      Future.successful(reply)
    }
  }

  private class SortImpl extends SortServiceGrpc.SortService {
    override def sortSayHello(req: SortRequest) = {
      logger.info("sayHello from " + req.name)
      clientLatch.countDown()
      addNewSlave(req.name)
      clientLatch.await()
      val reply = SortReply(message = "Hello")
      Future.successful(reply)
    }
  }

}
