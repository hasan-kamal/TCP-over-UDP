/*

@author hasankamal
Implementing a reliable data transport protocol (called RDT; like TCP) over UDP

*/

import java.net.*;

public class RDTServerSocket{
	
	private DatagramSocket udpSocket = null; //underlying unreliable UDP socket
	private int portNumber_;

	public RDTServerSocket(int portNumber){
		try{
			this.udpSocket = new DatagramSocket(portNumber);
		}catch(Exception ex){
			System.out.println("RDTServerSocket creation failed");
			ex.printStackTrace();
		}
		this.portNumber_ = portNumber;
	}

	//waits initially, then connects with the client
	public RDTSocket accept(){
		byte[] buf = new byte[RDTPacket.MSS + RDTPacket.HEADER_LENGTH];
		DatagramPacket packet = null;
		RDTPacket rdtPacket = null;

		try{
			do{
				packet = new DatagramPacket(buf, buf.length);
				this.udpSocket.receive(packet);
				rdtPacket = new RDTPacket(packet);
			}while(rdtPacket.getSYN() == 0);
		}catch(Exception ex){
			System.out.println("RDTServerSocket accept() error");
			ex.printStackTrace();
		}

		InetAddress clientAddress = packet.getAddress(); //client's address
		int clientPort = packet.getPort();
		System.out.println("accepted connection from " + clientAddress.getHostAddress() + " at " + clientPort);
		return new RDTSocket(clientAddress.getHostAddress(), clientPort, udpSocket);
	}

}