import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
//import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * This class acts as the server that maintains the ongoing lecture information
 * It also provides students with the IP of the instructor so that they can 
 * communicate with the instructor directly
 * @author Gaurav & Amol
 *
 */
public class OnlineClassroomServer {

	static Map<Integer, InetAddress> currentLetcures = null;

	/**
	 * Constructor that initialized the data structure which stores the current
	 * ongoing lectures
	 */
	public OnlineClassroomServer() {
		currentLetcures = new HashMap<Integer, InetAddress>();
	}

	/**
	 * The main method keeps listening for requests from instructor or student
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		OnlineClassroomServer onlineClassroomServer = new OnlineClassroomServer();
		System.out.println("Main server started...");
		@SuppressWarnings("resource")
		DatagramSocket serverSocket = new DatagramSocket(50000);
		byte[] byteArr = new byte[100000];
		DatagramPacket receivedPacket = new DatagramPacket(byteArr, byteArr.length);
		DatagramPacket toBeSentPacket = new DatagramPacket(byteArr, byteArr.length);

		while (true) {
//			System.out.println("Beginning of while..");
			System.out.println("Waiting for instructors/students to connect...");
			serverSocket.receive(receivedPacket);
			System.out.println("Message received...");
			byte[] received = Arrays.copyOfRange(receivedPacket.getData(), 0, receivedPacket.getLength());
			String type = received[0] + "" + received[1];
			System.out.println("Request type: "+type);
			// type 01 - connect request by instructor
			if("01".equals(type)) {
				try {
					System.out.println("Connect request by instructor...");
					// generate random number
					int classNumber = new Random().nextInt(10000);

					// store InetAddress of instructor against random number in currentLectures
					while(currentLetcures.containsKey(classNumber))
						classNumber = new Random().nextInt(10000);

					currentLetcures.put(classNumber, receivedPacket.getAddress());

					// send random number to instructor
					byte[] byteArr1 = new byte[2];
					byteArr1[0] = 0;
					byteArr1[1] = 1;

					byte[] byteArr2 = new String(classNumber+"").getBytes();

					byteArr = new byte[byteArr1.length + byteArr2.length];
					System.arraycopy(byteArr1, 0, byteArr, 0, byteArr1.length);
					System.arraycopy(byteArr2, 0, byteArr, byteArr1.length, byteArr2.length);

					toBeSentPacket = new DatagramPacket(byteArr, byteArr1.length + byteArr2.length, receivedPacket.getAddress(), 55000);
					serverSocket.send(toBeSentPacket);
					System.out.println("Class number assigned to instructor "+classNumber);
					System.out.println("Current ongoing lectures: "+currentLetcures);
				}
				catch(Exception e) {
					e.printStackTrace();
					// send error
					byteArr = new byte[2];
					byteArr[0] = 0;
					byteArr[1] = 0;
					toBeSentPacket = new DatagramPacket(byteArr, 2, receivedPacket.getAddress(), 55000);
					serverSocket.send(toBeSentPacket);
					System.out.println("Error occurred...");
				}
			}
			// type 02 - connect request by student
			else if("02".equals(type)) {
				System.out.println("Connect request by student... from "+receivedPacket.getAddress()+"@"+receivedPacket.getPort());
				// extract random number
				byte[] classToJoinByteArr = Arrays.copyOfRange(received, 2, received.length);
				int classToJoin = Integer.parseInt(new String(classToJoinByteArr));
				byte[] instructorInetAddress = null;
				// check if random number of present
				System.out.println("Class number to requested to be joined: "+classToJoin+"...");
				if(currentLetcures.containsKey(classToJoin)) {
					// if yes send InetAddress
					System.out.println("Class found...");
					instructorInetAddress = currentLetcures.get(classToJoin).getAddress();
					toBeSentPacket = new DatagramPacket(instructorInetAddress, instructorInetAddress.length, receivedPacket.getAddress(), 60000);
					serverSocket.send(toBeSentPacket);
				}
				// else send not found
				else {
					System.out.println("Class not found...");
					instructorInetAddress = new byte[]{0, 4};
					toBeSentPacket = new DatagramPacket(instructorInetAddress, instructorInetAddress.length, receivedPacket.getAddress(), 60000);
//					System.out.println("instructorInetAddress: "+instructorInetAddress);
//					System.out.println("instructorInetAddress[0 and 1]"+instructorInetAddress[0]+instructorInetAddress[1]);
//					System.out.println("instructorInetAddress.length: "+instructorInetAddress.length);
					serverSocket.send(toBeSentPacket);
				}
			}
			// type 03 - leave request by instructor
			else {
				System.out.println("Leave request by instructor...");
				try {
					byte[] classNumberReceived = Arrays.copyOfRange(receivedPacket.getData(), 2, receivedPacket.getLength());
					int classNumber = Integer.parseInt(new String(classNumberReceived));
					// remove InetAddress from currentLectures
					currentLetcures.remove(classNumber);

					// send success to instructor
					byteArr = new byte[2];
					byteArr[0] = 0;
					byteArr[1] = 3;
					toBeSentPacket = new DatagramPacket(byteArr, 2, receivedPacket.getAddress(), 55000);
					serverSocket.send(toBeSentPacket);

				} catch (Exception e) {
					// TODO: handle exception
					byteArr = new byte[2];
					byteArr[0] = 0;
					byteArr[1] = 0;
					toBeSentPacket = new DatagramPacket(byteArr, 2, receivedPacket.getAddress(), 55000);
					serverSocket.send(toBeSentPacket);
				}
				System.out.println("Current ongoing lectures: "+currentLetcures);
			}
		}
	}
}
