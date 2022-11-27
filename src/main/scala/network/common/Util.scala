package network.common

import java.net.{DatagramSocket, InetAddress}

object Util {
  def getMyIpAddress: String = {
    val socket = new DatagramSocket
    try {
      socket.connect(InetAddress.getByName("8.8.8.8"), 10002)
      socket.getLocalAddress.getHostAddress
    } finally if (socket != null) socket.close()
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