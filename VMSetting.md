# How to Configure JDK 11 / Scala 2.13.0  - 2022.11.29

## 1. Download JDK 11 / Scala 2.13.0
In VM #05~#10(Already installed in VM #01,#03,#04), 
 - wget https://download.java.net/java/GA/jdk10/10/binaries/openjdk-10_linux-x64_bin.tar.gz 
 - wget https://downloads.lightbend.com/scala/2.13.0/scala-2.13.0.tgz
 - tar -xvf openjdk-11....tar.gz 
 - tar -xvf scala-2.13.0....tgz 
## 2. Check .bashrc
 - ls -al
 - vim .bashrc
## 3. Add below lines in .bashrc above export PATH = ...sbt.bin
 - export PATH=/home/indigo/scala-2.13.0/bin:$PATH:~/.sbt.bin
 - export PATH=/home/indigo/bin:$PATH:~/.sbt.bin
 - **IMPORTANT!! : MUST ADD LINES ABOVE "export PATH = ...sbt.bin"**
## 4. Save .bashrc and apply it
 - source .bashrc
## 5. Check installed properly
 -  java -verison
 -  javac -version
 -  scala -version
 -  scalac -version

**If you don't know how to configure it, see VM#01's .bashrc and structure.**
