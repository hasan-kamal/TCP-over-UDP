/*

@author hasankamal
Implementing a reliable data transport protocol (called RDT; like TCP) over UDP

*/

import java.net.*;

public class RDTPacket{
	
	//global constants
	public static final int MSS = 5; //maximum amount of payload data(bytes) in one RDT packet
	public static final int HEADER_LENGTH = 100;

	//speficic to this RDTPacket
	private int sourcePortNumber_;
	private int destPortNumber_;
	private int sequenceNumber_;
	private int ackNumber_;
	private int SYN_ = 0;
	private int FIN_ = 0;
	private int rwnd_;
	private String data_;

	public RDTPacket(int sourcePortNumber, int destPortNumber, int sequenceNumber, int ackNumber, int SYN, int FIN, int rwnd, String data){
		this.sourcePortNumber_  = sourcePortNumber;
		this.destPortNumber_ = destPortNumber;
		this.sequenceNumber_ = sequenceNumber;
		this.ackNumber_ = ackNumber;
		this.SYN_ = SYN;
		this.FIN_ = FIN;
		this.rwnd_ = rwnd;
		this.data_ = data;
	}

	public RDTPacket(DatagramPacket packet){
		String contents = new String(packet.getData(), 0, packet.getLength());
		String[] sep = contents.split(";");

		this.sourcePortNumber_ = Integer.parseInt(sep[0]);
		this.destPortNumber_ = Integer.parseInt(sep[1]);
		this.sequenceNumber_ = Integer.parseInt(sep[2]);
		this.ackNumber_ = Integer.parseInt(sep[3]);
		this.SYN_ = Integer.parseInt(sep[4]);
		this.FIN_ = Integer.parseInt(sep[5]);
		this.rwnd_ = Integer.parseInt(sep[6]);
		this.data_ = sep[7];
	}

	public DatagramPacket toUDPPacket(InetAddress destAddress, int destPortNumber){
		String output = String.format("%d;%d;%d;%d;%d;%d;%d;%s", sourcePortNumber_, destPortNumber_, sequenceNumber_, ackNumber_, SYN_, FIN_, rwnd_, data_);
		byte[] buf = output.getBytes();
		DatagramPacket packet = new DatagramPacket(buf, buf.length, destAddress, destPortNumber);
		return packet;
	}

	public int getSYN(){
		return this.SYN_;
	}

	public String getData(){
		return this.data_;
	}

	public int getSequenceNumber(){
		return sequenceNumber_;
	}

	public int getAckNumber(){
		return ackNumber_;
	}

	public int getFIN(){
		return FIN_;
	}

	public int getRwnd(){
		return rwnd_;
	}
}