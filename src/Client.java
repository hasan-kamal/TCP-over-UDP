/*

@author hasankamal
Implementing a reliable data transport protocol (called RDT; like TCP) over UDP

*/

import java.io.*;

public class Client{
	public static void main(String[] args){
		
		String hostAddr = args[0];
		int portNumber = Integer.parseInt(args[1]);

		RDTSocket clientSocket = new RDTSocket(hostAddr, portNumber);
		clientSocket.start(); //sends a SYN packet

		System.out.println("type \"quit\" to exit session");

		try{
			//take user data and send to server
			BufferedReader inputStdStream = new BufferedReader(new InputStreamReader(System.in));
			String userInput = inputStdStream.readLine();
			String sReceive;
			while(!userInput.equals("quit")){
				clientSocket.send(userInput, 0.5);
				String serverReply = clientSocket.receive();
				System.out.println("Client received: " + serverReply);
				userInput = inputStdStream.readLine();
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}

		clientSocket.close();
	}
}