# TCP-over-UDP

### Introduction
- This is an implementation of a custom [TCP](https://en.wikipedia.org/wiki/Transmission_Control_Protocol)-like reliable data transport protocol over [UDP](https://en.wikipedia.org/wiki/User_Datagram_Protocol)
- We name this protocol `RDT` (for  **R**eliable  **D**ata  **T**ransport Protocol)
- Following functionalities have been added over UDP to make it reliable:
	1. Single and cumulative `ACK`nowledgement
	2. Sequence numbering
	3. Re-transmission
	4. Detecting duplicates and discarding them
	5. Congestion/flow control
- Just for demonstration purposes, the server has been set to reply back with an uppercased version of the string it receives from client

### Repository structure
-  `src/` contains the source code
	- `src/RDTPacket.java` describes structure of a `RDT` packet
    - `src/RDTSocket.java` and `src/RDTServerSocket.java` contain client socket and server socket implementations of `RDT`, respectively
    - `src/Client.java` and `src/Server.java` contain client and server applications, respectively (both use `RDT` to communicate)
- `compile.sh` bash shell-script can be used to compile required source files

### Build
- First compile by `cd`-ing into the root of this repository and then running command  `./compile.sh`
- To start the server,
	- Execute `java -classpath bin Server <port_no>` where `port_no` is the port number at which you want to start the server
	- For example, execute `java -classpath bin Server 1024` if server is to be started at port number _1024_
- To start client,
	- Execute `java -classpath bin Client <ip_addr> <port_no>` where `ip_addr`, `port_no` are IP address of server and port number, respectively
	- For example, execute `java -classpath bin Client 192.168.1.15 1024` is server's IP is _192.168.1.15_ and port number is _1024_

### Example run
1. Client first sends string `Hello` followed by `Is this working alright?`
2. Consequently, server replies first with `HELLO` followed by `IS THIS WORKING ALRIGHT?`
3. Client output
	```bash
	java -classpath bin Client 192.168.1.15 1024
	type "quit" to exit session
	Hello
	--windowSize = 5
	--dropping packet seq=#0
	--timeout packet seq=#0, retransmitted this packet
	--exiting slow start phase
	--cwnd decreased from 5.000000 to 2.500000
	--received ack with ackNumber=#5, rwnd=9
	--windowSize = 9
	--sending packet seq=#5
	--received ack with ackNumber=#6, rwnd=9

	--received packet seq=#0
	--received 'HELLO', delivered to upper layer
	--acked with ackNumber=#5, rwnd=9
	--received packet seq=#5
	--received '
	', delivered to upper layer
	--acked with ackNumber=#6, rwnd=9
	Client received: HELLO

	Is this working alright?
	--windowSize = 5
	--dropping packet seq=#0
	--timeout packet seq=#0, retransmitted this packet
	--exiting slow start phase
	--cwnd decreased from 5.000000 to 2.500000
	--received ack with ackNumber=#5, rwnd=9
	--windowSize = 9
	--sending packet seq=#5
	--received ack with ackNumber=#10, rwnd=9
	--windowSize = 9
	--sending packet seq=#10
	--received ack with ackNumber=#15, rwnd=9
	--windowSize = 9
	--dropping packet seq=#15
	--timeout packet seq=#15, retransmitted this packet
	--cwnd decreased from 12.500000 to 6.250000
	--received ack with ackNumber=#20, rwnd=9
	--windowSize = 6
	--dropping packet seq=#20
	--timeout packet seq=#20, retransmitted this packet
	--cwnd decreased from 6.250000 to 3.125000
	--received ack with ackNumber=#25, rwnd=9

	--received packet seq=#0
	--received 'IS TH', delivered to upper layer
	--acked with ackNumber=#5, rwnd=9
	--received packet seq=#5
	--received 'IS WO', delivered to upper layer
	--acked with ackNumber=#10, rwnd=9
	--received packet seq=#10
	--received 'RKING', delivered to upper layer
	--acked with ackNumber=#15, rwnd=9
	--received packet seq=#15
	--received ' ALRI', delivered to upper layer
	--acked with ackNumber=#20, rwnd=9
	--received packet seq=#20
	--received 'GHT?
	', delivered to upper layer
	--acked with ackNumber=#25, rwnd=9
	Client received: IS THIS WORKING ALRIGHT?
	```
4. Server output
	```bash
	java -classpath bin Server 1024
	accepted connection from 192.168.1.15 at 50233
	--received packet seq=#0
	--received 'Hello', delivered to upper layer
	--acked with ackNumber=#5, rwnd=9
	--received packet seq=#5
	--received '
	', delivered to upper layer
	--acked with ackNumber=#6, rwnd=9
	Server received: Hello

	--windowSize = 5
	--dropping packet seq=#0
	--timeout packet seq=#0, retransmitted this packet
	--exiting slow start phase
	--cwnd decreased from 5.000000 to 2.500000
	--received ack with ackNumber=#5, rwnd=9
	--windowSize = 5
	--sending packet seq=#5
	--received ack with ackNumber=#6, rwnd=9

	--received packet seq=#0
	--received 'Is th', delivered to upper layer
	--acked with ackNumber=#5, rwnd=9
	--received packet seq=#5
	--received 'is wo', delivered to upper layer
	--acked with ackNumber=#10, rwnd=9
	--received packet seq=#10
	--received 'rking', delivered to upper layer
	--acked with ackNumber=#15, rwnd=9
	--received packet seq=#15
	--received ' alri', delivered to upper layer
	--acked with ackNumber=#20, rwnd=9
	--received packet seq=#20
	--received 'ght?
	', delivered to upper layer
	--acked with ackNumber=#25, rwnd=9
	Server received: Is this working alright?

	--windowSize = 5
	--sending packet seq=#0
	--received ack with ackNumber=#5, rwnd=9
	--windowSize = 9
	--sending packet seq=#5
	--received ack with ackNumber=#10, rwnd=9
	--windowSize = 9
	--dropping packet seq=#10
	--timeout packet seq=#10, retransmitted this packet
	--exiting slow start phase
	--cwnd decreased from 15.000000 to 7.500000
	--received ack with ackNumber=#15, rwnd=9
	--windowSize = 7
	--sending packet seq=#15
	--received ack with ackNumber=#20, rwnd=9
	--windowSize = 7
	--dropping packet seq=#20
	--timeout packet seq=#20, retransmitted this packet
	--cwnd decreased from 7.500000 to 3.750000
	--received ack with ackNumber=#25, rwnd=9
	```

