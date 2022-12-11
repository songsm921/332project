# Indigo gRPC document

2022-11-24 최초 작성.

`sbt 1.8.0, jdk 11, scala 2.13.0`

### STEP 1. 각종 디펜던시 추가

project/scalapb.sbt

```scala
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.3")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.11"
```

build.sbt에 gRPC 및 log4j 관련 Dependency를 추가한다.

```scala
Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
  "org.apache.logging.log4j" % "log4j-api" % "2.19.0",
  "org.apache.logging.log4j" % "log4j-core" % "2.19.0",
  "org.apache.logging.log4j" %% "log4j-api-scala" % "12.0",
  "org.apache.logging.log4j" % "log4j-scala" % "11.0"
)
```

### STEP 2. 프로젝트 파일 구조



### STEP 3. proto 파일 작성 후 컴파일

기능 별로 나누어 프로토파일을 만들고 서비스 Declaration. 이때 파일 위쪽에 package fragment; 같이 경로 지정해 줘야 나중에 타겟 폴더에서 나눠져서 컴파일되기 때문에 같은 이름의 메소드가 충돌하지 않는다.

또한 다른 프로토파일이어도 서비스명, rpc명, 파라미터, 리턴 타입 이름은 웬만하면 안겹치게...

### STEP 4. Master 작성

Master에 import 할 것들

```scala
import io.grpc.{Server, ServerBuilder}
import org.apache.logging.log4j.scala.Logging
import java.util.concurrent.CountDownLatch
import scala.concurrent.{ExecutionContext, Future}

import network.common.Util.getMyIpAddress
import fragment.fragment.
{FragGreeterGrpc,
  HelloRequest,
  HelloReply,
  HelloRequest2,
  HelloReply2}
import sorting.sorting.
{SortGreeterGrpc,
  HelloRequest,
  HelloReply} //generate 된 코드들 중 proto제외한 파일
```

Master 클래스 start() 메소드에 서비스 추가

```scala
private def start(): Unit = {
    //Add Service Implementation here
    server = ServerBuilder.forPort(Master.port)
      .addService(FragServiceGrpc.bindService(new FragImpl, executionContext)).build
      .addService(SortServiceGrpc.bindService(new SortImpl, executionContext)).build.start
    logger.info("Server numClient: " + self.numClient)
    logger.info("Server started, listening on " + Master.port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }
```

Master 클래스 아래쪽에 각 서비스 메소드들의 구체적 구현

```scala
private class FragImpl extends FragServiceGrpc.FragService {
    override def sayHello(req: FragRequest) = {
      logger.info("sayHello from " + req.name)
      clientLatch.countDown()
      addNewSlave(req.name)
      clientLatch.await()
      val reply = FragReply(message = "Hello")
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
```

### STEP 5. Worker 작성

Worker에 import 할 것들

```scala
import org.apache.logging.log4j.scala.Logging
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}
import java.util.concurrent.TimeUnit

import network.common.Util.getMyIpAddress
import fragment.fragment.FragServiceGrpc.FragServiceBlockingStub
import sorting.sorting.SortServiceGrpc.SortServiceBlockingStub

import fragment.fragment.
{FragServiceGrpc,
  FragRequest,
  FragReply}
import sorting.sorting.
{SortServiceGrpc,
  SortRequest,
  SortReply}
```

Worker 클래스에 worker 가 써야하는 메소드들을 Implementation한다. 이때 서버로부터 받아오는 정보는 BlockingStub에서 받아오면 된다.

### 함수 어떻게 추가해?

- Master.scala > Master class > ServiceImple class ⇒ 클라이언트로부터 인풋이 왔을 때 아웃풋 지정. Future[Respond] 형태로 리턴

```scala
override def sortSayHello(req: SortRequest) = {
      logger.info("sayHello from " + req.name)
      clientLatch.countDown()
      addNewSlave(req.name)
      clientLatch.await()
      val reply = SortReply(message = "Hello")
      Future.successful(reply)
    }
```

- Worker.scala > Worker class ⇒

```scala
def SayHello(): Unit = {
    val request = FragRequest(name = getMyIpAddress)
    try {
      val response = blockingStub.sayHello(request)
      logger.info("SendMessage: " + response.message)
    }
```

### Repeated 데이터 타입 사용

List 또는 Seq 타입의 데이터를 보내고자 한다면 proto 타입에서 repeated 형식으로 지정하고 사용.

```scala
message SamplingEndMsg2MasterRequest {
  int32 workerID = 1;
  repeated string samples = 2;
}

message SamplingEndMsg2MasterResponse {
  repeated string totalSamples = 1;
}
```

### Discussion

1. **만약 서비스가 여러개라면 서버와 클라이언트 구현할 때 아래 코드를 어떻게 확장할 것인가.**

```scala
class Worker private(
                      private val channel: ManagedChannel,
                      private val blockingStub: FragServiceBlockingStub
                    ) extends Logging {

//여기서 worker를 새로 할당할때 FragServiceBlockingStub을 파라미터로 전달한다.
//그럼 다른 서비스의 stub를 전달하려면? 서비스마다 종류가 다른 워커를 선언해서 만들어 줘야하는 걸까?

```

```scala
class Master(executionContext: ExecutionContext, val numClient: Int) extends Logging { self =>
  private[this] var server: Server = null
  private val clientLatch: CountDownLatch = new CountDownLatch(numClient)
  var slaves: Vector[WorkerClient] = Vector.empty

  private def start(): Unit = {
    server = ServerBuilder.forPort(Master.port).addService(FragServiceGrpc.bindService(new FragImpl, executionContext)).build.start

// 여기서도 서버를 빌드할 때 FragService 서비스를 addService한다고 한다. 하나의 서버의 여러 서비스를 어떻게 추가해줘야 할까?
```

⇒ 생각해보면 굳이 기능별로 프로토파일 나눌 필요는 없을거같다. rpc도 많아봐야 10개 이내일것 같고.

1. **클라이언트 여러개가 정해진 일을 다 수행해야 서버가 다음 작업을 지시해야 하잖아. ⇒ LatchCountdown() 사용. 모든 thread가 할 일을 마치면 countdown하고 await()에서 기다리다가 마지막 thread가 끝나면 0이 되면서 await()가 풀리는 방식으로 synchronization 구현**

1. **rpc 구체적인 디자인. 어느 페이스에서 어느 rpc를 쓸것인가. 어떤 리퀘스트 타입과 어떤 리플라이 타입을 가질 것인가.**