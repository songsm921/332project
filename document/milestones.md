# Milestones

### Milestone #0 : Test Function for each part
design unit test before implementation (edge case)
Design at least 2 unit tests before implementation, implement test function

### Milestone #1 : Setting up development environment
Selecting development OS
Scala version, JDK version
Setting github directory

### Milestone #2 : Generating Dataset
Understanding Gensort
Trying making sample dataset

### Milestone #3 : Server and Worker communication
Understanding gRPC Server and worker(Make document for other member)
running test Sending and Receiving sample data

### Milestone #4 : Dataset fragmentation
Divide single file with any size into designated sized files

### Milestone #5 : Sorting Data fragment
Sort any single file with key, and then extend to multiple files

### Milestone #6 : Partition Data fragment
Label data with range given by master


### Milestone #7 : Shuffling data fragment
Exchange data through master so that every machine has its own labeled data

### Milestone #8 : Merging data on each worker machine
Sort multiple files with arbitrary size in increasing order

### Milestone #9 : Balancing data on multiple worker machines
By additional communication, let every machine have similar data size
