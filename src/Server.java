/*

@author hasankamal
Implementing a reliable data transport protocol (called RDT; like TCP) over UDP

*/

import java.io.*;

public class Server{
	public static void main(String[] args){
		
		int portNumber = Integer.parseInt(args[0]);
		RDTServerSocket serverSocket = new RDTServerSocket(portNumber);
		RDTSocket clientSocket = serverSocket.accept(); //wait and connect with the client

		String data = clientSocket.receive(); //server receives first for demonstration
		while(data != null){ //receive() call blocks until a message is available, returns null if connection terminated
			System.out.println("Server received: " + data);
			clientSocket.send(data.toUpperCase(), 0.5); //send some data back to show full duplex capability
			data = clientSocket.receive();
		}

		System.out.println("connection terminated");
	}
}