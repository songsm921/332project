package main
import utils.{Phase, util, workerPhase}
import network.MasterServer

import scala.concurrent.ExecutionContext
object master {
  val masterPort = 18218
  def main(args: Array[String]): Unit = {
    val numClient = args.headOption
    if (numClient.isEmpty) {
      return
    }
    val server = new MasterServer(ExecutionContext.global,numClient.get.toInt,masterPort)
    server.start()
    server.printEndpoint()
    server.blockUntilShutdown()
    server.stop()
  }
}
