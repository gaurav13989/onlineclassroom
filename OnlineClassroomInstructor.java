import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

import javax.imageio.ImageIO;

import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IError;
import com.xuggle.xuggler.IMetaData;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.Utils;
import com.xuggle.xuggler.demos.VideoImage;

/**
 * This class acts as the instructor class and is responsible for starting a lecture
 * and accommodating students at appropriate positions in the binary tree
 * @author Gaurav & Amol
 *
 */

public class OnlineClassroomInstructor
{
	static DatagramSocket datagramSocket = null; //new DatagramSocket(49151);
	static DatagramPacket datagramPacket = null;
	static int destPort = 60000;
	static InetAddress hostLeft = null;
	static InetAddress hostRight = null;
	static InetAddress server = null;
	static String classNumber = null;
	static DatagramSocket alwaysListeningSocket = null;
	static DatagramPacket packetOnAlwaysOnLine = null;
	static int[] positionsBeingMonitored = new int[100];
	static String serverName = "newyork.cs.rit.edu";//"192.168.2.7";////"192.168.2.16"; //; 
	static InetAddress[] students = null;
	static DatagramSocket portMonitoringStudents = null;
	static DatagramPacket packetOnPortMonitoringStudents = null;
	static int totalStudents = 0;
	// screen for the instructor
	private static VideoImage instructorWindow = null;

	/**
	 * This method takes a BufferedImage object as an input and updates the swing 
	 * window with it giving the illusion of a video
	 * @param bufferedImage
	 */
	private static void updateJavaWindow(BufferedImage bufferedImage) {
		instructorWindow.setImage(bufferedImage);
	}

	/**
	 * This method initialized the VideoImage object which opens the swing window.
	 * Before doing this it connects with the server, registers a class and receives
	 * a class number.
	 */
	private static void startInstructor() throws IOException {
		classNumber = connect();
		instructorWindow = new VideoImage();
		instructorWindow.setTitle("Instructor Window - "+classNumber);
		//		instructorWindow.setLocationRelativeTo(null);
		instructorWindow.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				stopInstructor();
			}
		});
	}

	/**
	 * This method communicates with the server asking it to remove the session that
	 * the instructor started and closes the video feed 
	 */
	private static void stopInstructor() {
		// sending server a message to remove class from current sessions...
		byte[] leaveRequestByteArr1 = new byte[]{0, 3};
		byte[] classNumberByteArr = classNumber.getBytes();
		byte[] serverResponseByteArr = new byte[2];

		byte[] byteArr = new byte[leaveRequestByteArr1.length + classNumberByteArr.length];
		System.arraycopy(leaveRequestByteArr1, 0, byteArr, 0, leaveRequestByteArr1.length);
		System.arraycopy(classNumberByteArr, 0, byteArr, leaveRequestByteArr1.length, classNumberByteArr.length);

		datagramPacket = new DatagramPacket(byteArr, leaveRequestByteArr1.length+classNumberByteArr.length, server, 50000);
		int attempt = 1;
		while(true) {
			try {
				datagramSocket.send(datagramPacket);
				datagramSocket.setSoTimeout(2000);
				datagramPacket = new DatagramPacket(serverResponseByteArr, 2);
				datagramSocket.receive(datagramPacket);
				break;
			} catch (Exception e) {
				// TODO: handle exception
				System.out.println("Attempt: "+attempt);
				attempt++;
				if(attempt == 4)
					break;
			}
		}
		if("03".equals(serverResponseByteArr[0]+""+serverResponseByteArr[1]))
			System.exit(0);
		else if("00".equals(serverResponseByteArr[0]+""+serverResponseByteArr[1]))
		{
			instructorWindow.setTitle(instructorWindow.getTitle()+" - Could not remove online session from server... Closing anyhow..");
			System.exit(0);
		}
	}

	/**
	 * This methods connects with the server and returns a class number that
	 * the instructor provides the students for them to join the class. 
	 * @return
	 * @throws IOException
	 */
	private static String connect() throws IOException {
		String classNumber = "";
		byte[] classNumberByteArr = new byte[1000];
		datagramPacket = new DatagramPacket(new byte[]{0, 1}, 2, server, 50000);
		int attempt = 1;
		while(true) {
			try {
				datagramSocket.send(datagramPacket);
				datagramSocket.setSoTimeout(2000);
				datagramPacket = new DatagramPacket(classNumberByteArr, 1000);
				datagramSocket.receive(datagramPacket);
				break;
			} catch (Exception e) {
				// TODO: handle exception
				System.out.println("Attempt: "+attempt);
				attempt++;
				if(attempt == 4)
					break;
			}
		}

		if(attempt == 4 || (classNumberByteArr[0]+""+classNumberByteArr[1]).equals("00")) {
			classNumber = "Error encountered. Please try again. Class could not be registered.";
		}
		else {
			classNumber = new String(Arrays.copyOfRange(classNumberByteArr, 2, datagramPacket.getLength()));
		}
		return ""+classNumber;
	}
	
	/**
	 * This method creates a socket to poll students to check if they are up or not.
	 * If not it readjusts the tree by removing the not responding node and replacing
	 * it with the last entered node in the tree.
	 * @throws SocketException
	 */
	// add method and reduce code
	public static void startPollingStudents() throws SocketException {
		// 60001 student's always on port
		portMonitoringStudents = new DatagramSocket(62500);
		packetOnPortMonitoringStudents = new DatagramPacket(new byte[100000], 100000);
		new Thread(new Runnable() {
			@Override
			public void run() {
				// ping each student on their always on port and wait thrice for a second waiting for their reply
				while(true) {
					int i = 1;
					while(i <= totalStudents) {
						int attempt = 1;
						if(students[i] != null) {
							attempt = 1;
							while(attempt < 4) {
								try {
									portMonitoringStudents.send(new DatagramPacket(new byte[]{3, 3}, 2, students[i], 60001));
									portMonitoringStudents.setSoTimeout(2500);
									portMonitoringStudents.receive(packetOnPortMonitoringStudents);
									break;
								} catch (IOException e) {
//									e.printStackTrace();
									System.out.println("No response from "+students[i]+"... Retrying... Attempt "+attempt+"...");
									attempt++;
								}								
							}
							// re adjust tree since node did not reply back in time
							if(attempt > 3) {
								System.out.println("Re-adjusting tree due to failure of "+students[i]+"...");
								int treePositionCompromised = i;
								int parentPosition = (i-1)/2;
								int leftChildPosition = (2*i)+1;
								int rightChildPosition = (2*i)+2;
								int lastChildPosition = 0;
								int parentOfLastChild = 0;
								while(students[++lastChildPosition] != null) {}
								--lastChildPosition;
								parentOfLastChild = (lastChildPosition-1)/2;
//								System.out.println("treePositionCompromised: "+treePositionCompromised);
//								System.out.println("parentPosition: "+parentPosition);
//								System.out.println("leftChildPosition: "+leftChildPosition);
//								System.out.println("rightChildPosition: "+rightChildPosition);
//								System.out.println("lastChildPosition: "+lastChildPosition);
//								System.out.println("parentOfLastChild: "+parentOfLastChild);
								if(lastChildPosition < 3) {
									if(treePositionCompromised == 1) {
//										System.out.println("hostLeft: "+hostLeft);
//										System.out.println("hostRight: "+hostRight);
										if(totalStudents == 2)
										{
											hostLeft = hostRight;
											hostRight = null;
											students[1] = students[2];
											students[2] = null;
//											System.out.println("hostLeft: "+hostLeft);
//											System.out.println("hostRight: "+hostRight);
										}
										else {
											hostLeft = null;
											students[1] = null;
										}
//										System.out.println("hostLeft:"+hostLeft);
//										System.out.println("hostRight:"+hostRight);
									}
									else {
										hostRight = null;
										students[2] = null;											
									}
								}
								else {
									// informing lastChild's parent to leave it
									byte[] byteArr1 = new byte[]{5, 5};
									byte[] byteArr2 = "//***null***//".getBytes();
									byte[] byteArr = new byte[byteArr1.length+byteArr2.length];
									if(parentPosition != lastChildPosition) {
										System.arraycopy(byteArr1, 0, byteArr, 0, byteArr1.length);
										System.arraycopy(byteArr2, 0, byteArr, byteArr1.length, byteArr2.length);
										if(lastChildPosition%2 == 1)
										{
											byteArr1 = new byte[]{4, 4};
											byteArr = new byte[byteArr1.length+byteArr2.length];
											System.arraycopy(byteArr1, 0, byteArr, 0, byteArr1.length);
											System.arraycopy(byteArr2, 0, byteArr, byteArr1.length, byteArr2.length);
										}
										try {
											packetOnPortMonitoringStudents = new DatagramPacket(byteArr, byteArr.length, students[parentOfLastChild], 60001);
											portMonitoringStudents.send(packetOnPortMonitoringStudents);
										} catch (IOException e) {
											e.printStackTrace();
										}
									}
									
									// informing parent to take lastChild as its new child
									System.out.println("Parent position is "+parentPosition+"...");
									if(parentPosition == 0)
									{
										if(treePositionCompromised%2 == 1)
											hostLeft = students[lastChildPosition];
										else
											hostRight = students[lastChildPosition];
									}
										// if parent is not the root
									else {
										byteArr2 = students[lastChildPosition].toString().getBytes();
										if(treePositionCompromised%2 == 1) {
											byteArr1 = new byte[]{4, 4};
										}
										else {
											byteArr1 = new byte[]{5, 5};
										}
										byteArr = new byte[byteArr1.length+byteArr2.length];
										System.arraycopy(byteArr1, 0, byteArr, 0, byteArr1.length);
										System.arraycopy(byteArr2, 0, byteArr, byteArr1.length, byteArr2.length);
										try {
											packetOnPortMonitoringStudents = new DatagramPacket(byteArr, byteArr.length, students[parentPosition], 60001);
											portMonitoringStudents.send(packetOnPortMonitoringStudents);
//											portMonitoringStudents.setSoTimeout(1500);
//											portMonitoringStudents.receive(packetOnPortMonitoringStudents);
										} catch (IOException e) {
											e.printStackTrace();
										}
									}
									
									// informing lastChild about parent
//									System.out.println("parent position: "+parentPosition);
									byteArr1 = new byte[]{1, 1};
									byteArr2 = students[parentPosition].toString().getBytes();
									byteArr = new byte[byteArr1.length+byteArr2.length];
									System.arraycopy(byteArr1, 0, byteArr, 0, byteArr1.length);
									System.arraycopy(byteArr2, 0, byteArr, byteArr1.length, byteArr2.length);
									try {
										packetOnPortMonitoringStudents = new DatagramPacket(byteArr, byteArr.length, students[lastChildPosition], 60001);
										portMonitoringStudents.send(packetOnPortMonitoringStudents);
									} catch (IOException e) {	
										e.printStackTrace();
									}
									
									// informing lastChild about its new left and right children and parent
									// informing about left child
									if(students[leftChildPosition] != null && leftChildPosition != lastChildPosition) {
										byteArr1 = new byte[]{4, 4};
										byteArr2 = students[leftChildPosition].toString().getBytes();
										byteArr = new byte[byteArr1.length+byteArr2.length];
										System.arraycopy(byteArr1, 0, byteArr, 0, byteArr1.length);
										System.arraycopy(byteArr2, 0, byteArr, byteArr1.length, byteArr2.length);
										try {
											packetOnPortMonitoringStudents = new DatagramPacket(byteArr, byteArr.length, students[lastChildPosition], 60001);
											portMonitoringStudents.send(packetOnPortMonitoringStudents);
										} catch (IOException e) {	
											e.printStackTrace();
										}										
									}
									
									// informing about right child
									if(students[rightChildPosition] != null && rightChildPosition != lastChildPosition) {
										byteArr1 = new byte[]{5, 5};
										byteArr2 = students[rightChildPosition].toString().getBytes();
										byteArr = new byte[byteArr1.length+byteArr2.length];
										System.arraycopy(byteArr1, 0, byteArr, 0, byteArr1.length);
										System.arraycopy(byteArr2, 0, byteArr, byteArr1.length, byteArr2.length);
										try {
											packetOnPortMonitoringStudents = new DatagramPacket(byteArr, byteArr.length, students[lastChildPosition], 60001);
											portMonitoringStudents.send(packetOnPortMonitoringStudents);
										} catch (IOException e) {	
											e.printStackTrace();
										}										
									}
									
									// informing leftChild and rightChild about new parent lastChild
//									System.out.println("students[lastChildPosition].toString(): "+students[lastChildPosition].toString());
									byteArr1 = new byte[]{1, 1};
									byteArr2 = students[lastChildPosition].toString().getBytes();
									byteArr = new byte[byteArr1.length+byteArr2.length];
									System.arraycopy(byteArr1, 0, byteArr, 0, byteArr1.length);
									System.arraycopy(byteArr2, 0, byteArr, byteArr1.length, byteArr2.length);
									try {
										// printing class
//										System.out.println("Class>>");
//										int j = 1;
//										while(j <= totalStudents)
//										{
//											System.out.println(j+": "+students[j]);
//											j++;
//										}
//										System.out.println("leftChildPosition:"+leftChildPosition);
//										System.out.println("students[leftChildPosition]: "+students[leftChildPosition]);
										if(students[leftChildPosition] != null && leftChildPosition != lastChildPosition) {
											packetOnPortMonitoringStudents = new DatagramPacket(byteArr, byteArr.length, students[leftChildPosition], 60001);
											portMonitoringStudents.send(packetOnPortMonitoringStudents);											
										}
									} catch (IOException e) {
										e.printStackTrace();
									}
									try {
										if(students[rightChildPosition] != null && rightChildPosition != lastChildPosition) {
											packetOnPortMonitoringStudents = new DatagramPacket(byteArr, byteArr.length, students[rightChildPosition], 60001);
											portMonitoringStudents.send(packetOnPortMonitoringStudents);											
										}
									} catch (Exception e) {
										// TODO: handle exception
										e.printStackTrace();
									}

									// changing lastChild's position in students to treeCompromised position and assigning lastChild position as null
									students[treePositionCompromised] = students[lastChildPosition];
									students[lastChildPosition] = null;
								}
								totalStudents--;
								// printing class
								System.out.println("Class>>");
								int j = 1;
								while(j <= totalStudents)
								{
									System.out.println(j+": "+students[j]);
									j++;
								}
							}
						}
						i++;
					}
					i = 0;
				}
				// if no reply all three times then host is down

				// 33 ping from instructor
				// 44 re adjust send leftChild
				// 55 re adjust send rightChild
				// 11 send parent
			}
		}).start();
	}

	/**
	 * This method starts a socket at port 65000 to listen for students that want to join 
	 * the lecture. It accepts the request and places the student at the last position in the tree
	 * using level order insertion. Once inserted the instructor also informs the appropriate parent
	 * to send its feed to this newly added child.
	 * @throws SocketException
	 */
	public static void startListeningForStudents() throws SocketException {
		alwaysListeningSocket = new DatagramSocket(65000);
		byte[] bytesOnAlwaysOnLine = new byte[100000];
		packetOnAlwaysOnLine = new DatagramPacket(bytesOnAlwaysOnLine, 100000);
		new Thread(new Runnable() {
			@Override
			public void run() {
				// listen
				while(true) {
					try {
						System.out.println("Started listening on Instructor...");
						alwaysListeningSocket.receive(packetOnAlwaysOnLine);
						System.out.println("Received message on always on port...");
						// process packet
						byte[] bytesOnAlwaysOnLine = Arrays.copyOfRange(packetOnAlwaysOnLine.getData(), 0, packetOnAlwaysOnLine.getLength());
						InetAddress student = null;
						int studentInsertedAt = 0;
						// join request by student 01
						if("01".equals(bytesOnAlwaysOnLine[0]+""+bytesOnAlwaysOnLine[1])) {
							// get InetAddress
							student = packetOnAlwaysOnLine.getAddress();
							// place student at a position in the class > 0
							for(int i = 1; i < 100; i++)
							{
								if(students[i] == null) {
									students[i] = student;
									studentInsertedAt = i;
									break;
								}
							}
							System.out.println("Student added at position "+studentInsertedAt+"...");
							// if position == 1 or == 2 OK
							if(studentInsertedAt == 1) {
								hostLeft = student;
							}
							else if(studentInsertedAt == 2) {
								hostRight = student;
							}
							else { // inserted at > 2
								InetAddress parent = students[(studentInsertedAt-1)/2];	// parent = (student-1)/2
								// let student know about parent InetAddress
								byte[] byteArr1 = new byte[]{1, 1};
								byte[] parentInetAddressByteArr = parent.toString().getBytes();
								byte[] byteArr = new byte[byteArr1.length+parentInetAddressByteArr.length];
								System.arraycopy(byteArr1, 0, byteArr, 0, byteArr1.length);
								System.arraycopy(parentInetAddressByteArr, 0, byteArr, byteArr1.length, parentInetAddressByteArr.length);

								packetOnAlwaysOnLine = new DatagramPacket(byteArr, byteArr.length, student, 60001);
								alwaysListeningSocket.send(packetOnAlwaysOnLine);	// telling student about parent
								System.out.println("Message sent to student "+student+" about parent "+parent+"...");

								Thread.sleep(100);
								// let parent know about child InetAddress
								byteArr1 = new byte[]{2, 2}; // letting parent know of child
								byte[] childInetAddress = student.toString().getBytes();
								byteArr = new byte[byteArr1.length+childInetAddress.length];
								System.arraycopy(byteArr1, 0, byteArr, 0, byteArr1.length);
								System.arraycopy(childInetAddress, 0, byteArr, byteArr1.length, childInetAddress.length);

								packetOnAlwaysOnLine = new DatagramPacket(byteArr, byteArr.length, parent, 60001); // always on socket on each student for receiving child information
								alwaysListeningSocket.send(packetOnAlwaysOnLine);	// telling parent about child		
								System.out.println("Message sent to parent "+parent+" about student "+student+"...");
							}
//							System.out.println("studentInsertedAt:"+studentInsertedAt);
//							System.out.println("hostLeft: "+hostLeft);
//							System.out.println("hostRight: "+hostRight);
							totalStudents++;
							// printing class
							System.out.println("Class>>");
							int j = 1;
							while(j <= totalStudents)
							{
								System.out.println(j+": "+students[j++]);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}					
				}
			}
		}).start();
	}

	/**
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException
	{
		students = new InetAddress[100];
		students[0] = InetAddress.getByName(InetAddress.getLocalHost().getHostAddress());
		startListeningForStudents();
		startPollingStudents();
		server = InetAddress.getByName(serverName); // glados.cs.rit.edu
		datagramSocket = new DatagramSocket(55000); // server.. receives connect 

		// default driver name for camera
		String driverName = "vfwcap";

		// default device for camera
		String deviceName=  "0";

		// ************ Let's make sure that we can actually convert video pixel formats.
		if (!IVideoResampler.isSupported(IVideoResampler.Feature.FEATURE_COLORSPACECONVERSION))
			throw new RuntimeException("Xuggler GPL version needed!");

		// creating a xuggler container
		IContainer iContainer = IContainer.make();

		// specifying the camera driver
		IContainerFormat format = IContainerFormat.make();
		if (format.setInputFormat(driverName) < 0)
			throw new IllegalArgumentException("could not open webcam device: " + driverName);

		// specifying frame-rate and video_size
		IMetaData params = IMetaData.make();
		params.setValue("framerate", "10/1");
		params.setValue("video_size", "320x240");

		// starting the camera using the parameters specified 
		// above and the device name mentioned earlier with the 
		// help of the xuggler container
		int retval = iContainer.open(deviceName, IContainer.Type.READ, format, false, true, params, null);
//		if (retval < 0)
//		{
//			IError error = IError.make(retval);
//			throw new IllegalArgumentException("could not open file: " + deviceName + "; Error: " + error.getDescription());
//		}      

		// getting number of streams
		int noOfStreams = iContainer.getNumStreams();

		// and iterate through the streams to find the first video stream
		int videoStreamId = -1;
		IStreamCoder iStreamCoder = null;
		for(int i = 0; i < noOfStreams; i++)
		{
			// stream object
			IStream stream = iContainer.getStream(i);
			// getting pre-configured decoder that can decode this stream;
			IStreamCoder coder = stream.getStreamCoder();

			if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO)
			{
				videoStreamId = i;
				iStreamCoder = coder;
				break;
			}
		}

		if (videoStreamId == -1)
			throw new RuntimeException("could not find video stream in container: "+deviceName);

		if (iStreamCoder.open() < 0)
			throw new RuntimeException("could not open video decoder for container: "+deviceName);

		IVideoResampler iVideoResampler = null;
		// converting the stream to BGR24 type with the help of IVideoResampler
		if (iStreamCoder.getPixelType() != IPixelFormat.Type.BGR24)
		{
			iVideoResampler = IVideoResampler.make(iStreamCoder.getWidth(), iStreamCoder.getHeight(), IPixelFormat.Type.BGR24,
					iStreamCoder.getWidth(), iStreamCoder.getHeight(), iStreamCoder.getPixelType());
			if (iVideoResampler == null)
				throw new RuntimeException("could not create color space resampler for: " + deviceName);
		}

		// starting the instructor swin window
		startInstructor();

		// checking container for pakcets
		IPacket packet = IPacket.make();
		while(iContainer.readNextPacket(packet) >= 0)
		{
			// checking if packet belongs to the active video stream
			if (packet.getStreamIndex() == videoStreamId)
			{
				/*
				 * We allocate a new picture to get the data out of Xuggler
				 */
				IVideoPicture frame = IVideoPicture.make(iStreamCoder.getPixelType(), iStreamCoder.getWidth(), iStreamCoder.getHeight());

				int offset = 0;
				while(offset < packet.getSize())
				{
					// decoding the video for any errors
					int noOfDecodedBytes = iStreamCoder.decodeVideo(frame, packet, offset);
					if (noOfDecodedBytes < 0)
						throw new RuntimeException("Error in decoding video: " + deviceName);
					offset += noOfDecodedBytes;

					// checking if image received from webcam is complete
					// if not we retrieve it again
					if (frame.isComplete())
					{
						// video is nothing but a set of frames captures
						IVideoPicture newFrame = frame;

						if (iVideoResampler != null)
						{
							// we must resample
							newFrame = IVideoPicture.make(iVideoResampler.getOutputPixelFormat(), frame.getWidth(), frame.getHeight());
							if (iVideoResampler.resample(newFrame, frame) < 0)
								throw new RuntimeException("could not resample video from: " + deviceName);
						}
						if (newFrame.getPixelType() != IPixelFormat.Type.BGR24)
							throw new RuntimeException("could not decode video as BGR 24 bit data in: " + deviceName);

						// Convert the BGR24 to an java BufferedImage
						BufferedImage bufferedImage = Utils.videoPictureToImage(newFrame);

						// send image over the network
						send(bufferedImage);

						// and display it on the Java Swing window
						updateJavaWindow(bufferedImage);
					}
				}
			}
		}
		// cleaning up
		if (iStreamCoder != null)
		{
			iStreamCoder.close();
			iStreamCoder = null;
		}
		if (iContainer !=null)
		{
			iContainer.close();
			iContainer = null;
		}
		stopInstructor();

	}

	/**
	 * This method send frames to hostLeft and hostRight which are the first two children
	 * of this instructor
	 * @param image
	 * @throws IOException
	 */
	private static void send(BufferedImage image) throws IOException {
		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ImageIO.write(image, "jpg", byteArrayOutputStream);
			byteArrayOutputStream.flush();
			byte[] byteArr = byteArrayOutputStream.toByteArray();
			byteArrayOutputStream.close();
			if(hostLeft != null)
			{
				datagramPacket = new DatagramPacket(byteArr, byteArr.length, hostLeft, 60000);
				datagramSocket.send(datagramPacket);
			}
			if(hostRight != null)
			{
				datagramPacket = new DatagramPacket(byteArr, byteArr.length, hostRight, 60000);
				datagramSocket.send(datagramPacket);
			}			
		} catch (Exception e) {
//			e.printStackTrace();
		}
	}
}