/*

@author hasankamal
Implementing a reliable data transport protocol (called RDT; like TCP) over UDP

*/

import java.net.*;
import java.lang.Math;
import java.util.*;

public class RDTSocket{

	public static final int RECEIVER_BUFFER_SIZE = 9; //in bytes, change it here to demonstrate; make it small (like 2*RDTPacket.MSS) to explicitly show flow control properties, RDT becomes slow then though but correct still
	public static final int TIMEOUT_INTERVAL_MILLISECONDS = 4000; //set large to be observable

	private InetAddress destAddress_;
	private int destPortNumber_;
	private DatagramSocket udpSocket = null;
	
	//sender-specific state
	private Map<Integer, Boolean> ackMap = null;
	private int rwnd;
	private Timer timer;
	private double cwnd;
	private boolean isSlowStartPhase;
	
	//receiver-specific state
	private char[] bufferData = new char[RECEIVER_BUFFER_SIZE];

	public RDTSocket(String destAddress, int destPortNumber){
		try{
			this.destAddress_ = InetAddress.getByName(destAddress);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		this.destPortNumber_ = destPortNumber;
		
		try{
			this.udpSocket = new DatagramSocket();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

	public RDTSocket(String destAddress, int destPortNumber, DatagramSocket oldSocket){
		try{
			this.destAddress_ = InetAddress.getByName(destAddress);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		this.destPortNumber_ = destPortNumber;
		
		try{
			this.udpSocket = oldSocket;
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

	public void start(){
		//send a SYN packet as a request to initiate connection
		RDTPacket packet = new RDTPacket(0, this.destPortNumber_, 0, 0, 1, 0, this.rwnd, "no data");
		DatagramPacket packetToSend = packet.toUDPPacket(this.destAddress_, this.destPortNumber_);
		try{
			this.udpSocket.send(packetToSend);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

	public void send(String data, double probDropPacket){

		int rwndReceiver = RECEIVER_BUFFER_SIZE; //initial value
		int receiveBase = 0;
		//first case: send all packets first
		if(data.charAt(data.length() - 1) != '\n')
			data += "\n"; //add delimiter

		cwnd = RDTPacket.MSS;
		isSlowStartPhase = true;

		while(receiveBase < data.length()){

			int windowSize = Math.max(Math.min((int)cwnd, rwndReceiver), 1);
			System.out.format("--windowSize = %d\n", windowSize);
			//window to send: [receiveBase, receiveBase + windowSize - 1] both inclusive

			ackMap = new HashMap<Integer, Boolean>();
			for(int packetSeq = receiveBase; packetSeq < Math.min(receiveBase + windowSize, data.length()); packetSeq += RDTPacket.MSS){
				String dataFortThisPacket = data.substring(packetSeq, Math.min(packetSeq + RDTPacket.MSS, data.length()));
				if(packetSeq + dataFortThisPacket.length() - 1 >= Math.min(receiveBase + windowSize, data.length())){
					//don't send this packet, it won't fit.
					//send it in next batch
					continue;
				}
				RDTPacket packet = new RDTPacket(0, this.destPortNumber_, packetSeq, 0, 0, 0, this.rwnd, dataFortThisPacket);
				ackMap.put(packetSeq, false);
				DatagramPacket packetToSend = packet.toUDPPacket(this.destAddress_, this.destPortNumber_);
				try{
					//drop packets deliberately for demonstration
					if(Math.random() > probDropPacket){
						System.out.format("--sending packet seq=#%d\n", packetSeq);
						this.udpSocket.send(packetToSend);
					}else{
						System.out.format("--dropping packet seq=#%d\n", packetSeq);
					}
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}

			//now two cases can happen: either 1) timeout or 2) ACK receive

			//1) set up the timeout mechanism
			timer = new Timer();
			TimeoutTask timeoutTask = new TimeoutTask(receiveBase, data);
			timer.schedule(timeoutTask, TIMEOUT_INTERVAL_MILLISECONDS);

			//2) receive ACKs
			Map<Integer, Integer> countMap = new HashMap<Integer, Integer>();
			while(ackMap.containsValue(false)){
				
				byte[] buf = new byte[RDTPacket.MSS + RDTPacket.HEADER_LENGTH];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				try{
					this.udpSocket.receive(packet);
				}catch(Exception ex){
					ex.printStackTrace();
				}
				RDTPacket ackPacket = new RDTPacket(packet);
				if(isSlowStartPhase)
					cwnd += RDTPacket.MSS;
				else
					cwnd += RDTPacket.MSS * (RDTPacket.MSS / (int)cwnd);
				
				if(countMap.get(ackPacket.getAckNumber()) == null){
					System.out.format("--received ack with ackNumber=#%d, rwnd=%d\n", ackPacket.getAckNumber(), ackPacket.getRwnd());
					countMap.put(ackPacket.getAckNumber(), 1);
				}else{
					System.out.format("--received ack with ackNumber=#%d(duplicate #%d), rwnd=%d\n", ackPacket.getAckNumber(), countMap.get(ackPacket.getAckNumber()), ackPacket.getRwnd());
					countMap.put(ackPacket.getAckNumber(), countMap.get(ackPacket.getAckNumber()) + 1);
				}
				rwndReceiver = ackPacket.getRwnd(); //update rwnd value

				if(ackPacket.getAckNumber() > receiveBase){
					// marks all packets with seq # < ackPacket.getAckNumber() as ACKd
					for(Integer seqNum : ackMap.keySet()){
						if(seqNum < ackPacket.getAckNumber())
							ackMap.put(seqNum, true);
					}
					receiveBase = ackPacket.getAckNumber();

					//restart timer if there are currently any not-yet-ACKed-segments
					int smallestUnackedSeqNumber = (int)1e9;
					if(ackMap.containsValue(false)){
						for(Integer seqNum : ackMap.keySet()){
							if(ackMap.get(seqNum) == false){
								if(seqNum < smallestUnackedSeqNumber)
									smallestUnackedSeqNumber = seqNum;
							}
						}
					}
					timer.cancel();
					timer = new Timer();
					timer.schedule(new TimeoutTask(smallestUnackedSeqNumber, data), TIMEOUT_INTERVAL_MILLISECONDS);

				}
			}

			//received ack for all
			timer.cancel();

		}

		System.out.format("\n");
	}

	public class TimeoutTask extends TimerTask{
		int packetSeq_;
		String data;

		public TimeoutTask(int packetSeq, String data){
			super();
			this.packetSeq_ = packetSeq;
			this.data = data;
		}

		public void run(){
			if(RDTSocket.this.ackMap.get(this.packetSeq_) == true)
				return; //has been acked, no problem then

			//didn't receive ack, a timeout has happened
			//so retransmit that segment which caused timeout
			RDTPacket packet = new RDTPacket(0, RDTSocket.this.destPortNumber_, packetSeq_, 0, 0, 0, RDTSocket.this.rwnd, data.substring(packetSeq_, Math.min(packetSeq_ + RDTPacket.MSS, data.length())));
			DatagramPacket packetToSend = packet.toUDPPacket(RDTSocket.this.destAddress_, RDTSocket.this.destPortNumber_);
			try{
				RDTSocket.this.udpSocket.send(packetToSend);
				System.out.format("--timeout packet seq=#%d, retransmitted this packet\n", packetSeq_);
			}catch(Exception ex){
				ex.printStackTrace();
			}

			//restart timer
			timer.schedule(new TimeoutTask(packetSeq_, data), TIMEOUT_INTERVAL_MILLISECONDS);

			//exit slow start phase
			if(isSlowStartPhase)
				System.out.format("--exiting slow start phase\n");
			isSlowStartPhase = false;
			System.out.format("--cwnd decreased from %f to %f\n", cwnd, cwnd / 2.0);
			cwnd = cwnd / 2.0;
		}
	}

	private void sendAck(int sendBase, int numBytesFreeBuffer){
		//sendBase == seq # of next byte expected from the other side
		String data = "just an ack\n";
		this.rwnd = numBytesFreeBuffer;
		RDTPacket ack = new RDTPacket(0, this.destPortNumber_, 0, sendBase, 0, 0, this.rwnd, data);
		DatagramPacket ackToSend = ack.toUDPPacket(this.destAddress_, this.destPortNumber_);
		try{
			this.udpSocket.send(ackToSend);
			System.out.format("--acked with ackNumber=#%d, rwnd=%d\n", sendBase, this.rwnd);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

	//receive() call blocks until a message is available, returns null if connection terminated
	public String receive(){
		boolean gapExists = true;
		boolean gotNewline = false;
		int sendBase = 0; //sendBase - 1 denotes byte number of the last byte known to have been correctly received and in-order at the receiver
		int numBytesFreeBuffer = bufferData.length;

		String dataToUpperLayer = "";

		//clear buffer initially
		clearBuffer(0, bufferData.length - 1);

		while(gapExists || !gotNewline){
			byte[] buf = new byte[RDTPacket.MSS + RDTPacket.HEADER_LENGTH];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try{
				this.udpSocket.receive(packet);
			}catch(Exception ex){
				ex.printStackTrace();
			}
			RDTPacket rdtPacket = new RDTPacket(packet);

			if(rdtPacket.getFIN() == 1){
				System.out.format("--received FIN request, disconnecting\n");
				return null;
			}

			// check if duplicate
			if(bufferData[rdtPacket.getSequenceNumber() - sendBase] != '\0'){
				System.out.format("--received duplicate packet seq=#%d\n", rdtPacket.getSequenceNumber());
			}else{
				//new packet, reduce number of available bytes
				numBytesFreeBuffer -= rdtPacket.getData().length();
			}

			for(int byteNum = rdtPacket.getSequenceNumber(); byteNum < rdtPacket.getSequenceNumber() + rdtPacket.getData().length(); byteNum++){
				bufferData[byteNum - sendBase] = rdtPacket.getData().substring(byteNum - rdtPacket.getSequenceNumber(), byteNum - rdtPacket.getSequenceNumber() + 1).charAt(0);
				if(bufferData[byteNum - sendBase] == '\n'){
					gotNewline = true;
				}
			}

			if(gotNewline){
				int check = 0;
				while(bufferData[check] != '\0' && bufferData[check] != '\n'){check++;};
				if(bufferData[check] == '\n'){
					gapExists = false;
				}
			}
			
			System.out.format("--received packet seq=#%d\n", rdtPacket.getSequenceNumber());
			//update sendBase
			int oldSendBase = sendBase;
			while((sendBase - oldSendBase < bufferData.length) && bufferData[sendBase - oldSendBase] != '\0'){sendBase++;};
			
			//deliver new data to upper layer
			if(sendBase > oldSendBase){
				String newData = String.valueOf(bufferData, 0, sendBase - oldSendBase);
				System.out.format("--received \'%s\', delivered to upper layer\n", newData);
				dataToUpperLayer += newData;
				numBytesFreeBuffer += newData.length();
				
				// shift buffer contents left by newData.length() since sendBase changed
				for(int bufNum = newData.length(); bufNum < bufferData.length; bufNum++){
					bufferData[bufNum - newData.length()] = bufferData[bufNum];
				}
				clearBuffer(bufferData.length - newData.length(), bufferData.length - 1); //insert null characters after having done left-shifting
			}
			
			//send ack
			sendAck(sendBase, numBytesFreeBuffer);

		}

		return dataToUpperLayer;
	}

	private void clearBuffer(int start, int end){
		for(int byteNum = start; byteNum <= end; byteNum++){
			bufferData[byteNum] = '\0'; 
		}
	}

	//end connection
	public void close(){
		//send a FIN packet to terminate connection
		RDTPacket packet = new RDTPacket(0, this.destPortNumber_, 0, 0, 0, 1, this.rwnd, "no data, fin request");
		DatagramPacket packetToSend = packet.toUDPPacket(this.destAddress_, this.destPortNumber_);
		try{
			this.udpSocket.send(packetToSend);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}