# **2022 Software Design Distributed Sorting Project**
## Team Indigo

### **Before you Run**
1. For operating running command properly, you need to make directory for input files and output files.<br>
If you want make 2 input directory, it's free to naming, but make 2 input directory in advance. 
- ex) input1, input2, output => make directory named input1, input2 output in advance.
2. Please make clean your input directory except your input files. Also, make your output directory clean. <br> 
**i.e No files in the output directory.**
3. I recommend you to memorize input directory and output directory in **ABSOLUTE PATH**.
4. Our master Server uses port 18218. Hence, you should fix port number by 18218.
### **How to Run?**
1. Set your current directory to the root directory of the project.
2. Run the following command.
``` 
For Master : sbt "run (NUMBER OF WORKERS)"
For Worker : sbt "run (MASTER IP ADDRESS:18218) -I (INPUT ABSOLUTE DIRECTORY 1) (INPUT ABSOLUTE DIRECTORY 2) ... -O (OUTPUT DIRECTORY)"
```
- Please make your directory ends with '/'. ex) /home/user/input1 **/** & /home/user/output **/**
1. After compiling, you should select the mode of master or worker depending on what your machine role is.
2. If program run properly, you can see the following result.
```
=> In input directory : ONLY input files
=> In output directory : Result files which name ends with .i (i is the worker's ID)
```
### **Some Tips and TroubleShooting for our Teammates.**
1. If you make an interrupt during your test, you should clean your input directory and output directory.(We don't support cleaning temporary files when we make interrupt, yet.) <br >For convinience, I made some shell script in advance which can clean your input directory and output directory. <br>
```
In bash shell: type the command 
$ sh /home/indigo/remove.sh && sh /home/indigo/removeOutput.sh
```
 - It consists a simple bash command and regular expressions. So, if you want change path, you can open the shell script using vim and modify the path properly.
2. When you stuck with problem which can't open Master Server, check if some program is using port which is predefined. <br>
```
netstat -ntlp | grep :18218
Check if there is a process using port 18218.... If so, check PID and kill the process.
kill -9 PID
RUN MASTER SERVER AGAIN.
```
