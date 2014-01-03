import java.awt.Color;
import java.awt.Dimension;
//import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
//import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
//import javax.swing.SwingUtilities;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * This class acts as the student.
 * 
 * @author Gaurav & Amol
 *
 */
@SuppressWarnings("serial")
public class OnlineClassroomStudent extends JFrame {
	
	static boolean keepGoing = false;
	static DatagramSocket datagramSocket = null;
	static DatagramPacket datagramPacket = null;
	static JLabel image = null;
	static JPanel panel = null;
	
	static int destPort = 60000; // port that listens for video feed from parent
	static InetAddress hostLeft = null; // this node's left child
	static InetAddress hostRight = null; // this node's right child
	static InetAddress server = null; // main server like glados.cs.rit.edu
	static InetAddress parent = null; // this node's parent
	static String classNumber = null; // classNumber to be joined
	static JButton startButton = null; // the Start button on the swing UI
	static JButton leaveButton = null; // the Leave button on the swing UI
	static JTextField textField = null; // text field for entering class number on the UI
	static OnlineClassroomStudent onlineClassroomStudent = null; // this student
	static String serverName = "newyork.cs.rit.edu";//"192.168.2.7";//"glados.cs.rit.edu";// //"glados.cs.rit.edu"; 
	
	/**
	 * Constructor that calls the initialize method
	 * @throws SocketException
	 */
	public OnlineClassroomStudent() throws SocketException {
		initialize();
	}

	/**
	 *  initializes the jframe with elements an action listener for elements
	 */
	public void initialize() throws SocketException {

		startButton = new JButton();
		startButton.setText("Start");
		leaveButton = new JButton();
		leaveButton.setText("Leave");
		textField = new JTextField();
		textField.setText("");

		setTitle("Student Window");	// setting title of student jframe
		setSize(760, 480);	// setting the size of the jframe
		setDefaultCloseOperation(EXIT_ON_CLOSE); // closing the jframe closes the app
		setLocationRelativeTo(null);	// center window to the screen
		setLayout(null);	

		textField.setSize(90, 40);
		textField.setLocation(650, 5);
		startButton.setSize(90, 40);
		startButton.setLocation(650, 50);
		leaveButton.setSize(90, 40);
		leaveButton.setLocation(650, 95);

		add(textField);
		add(startButton);
		add(leaveButton);

		//		connect();
		image = new JLabel("Enter class number.");
		panel = new JPanel();
		panel.setSize(640, 480);
		//		panel.setBackground(Color.BLUE);
		panel.setPreferredSize(new Dimension(1500, 1500));
		//		panel.setLayout(null);
		panel.add(image);
		add(panel);

		/**
		 *  Actions performed on click on Start button
		 *  1. Starts the student UI
		 *  2. Asks for a class number
		 *  3. Joins class and starts receiving feed from appropriate parent
		 *  4. Shows appropriate error message on incorrect or blank class number
		 */
		startButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				classNumber = textField.getText().trim();
				try {
					//					int clsNo = Integer.parseInt(classNumber);
					System.out.println("Classroom Number: "+classNumber);
					// connect and 
					connect();
					// start feed
					new Thread(
							new Runnable() {
								public void run() {
									// ask server for instructor InetAddress
									// instructor port is 55000
									byte[] byteArr1 = new byte[]{0, 2};
									byte[] byteArr2 = (classNumber+"").getBytes();
									byte[] byteArr = new byte[byteArr2.length+byteArr1.length];

									System.arraycopy(byteArr1, 0, byteArr, 0, byteArr1.length);
									System.arraycopy(byteArr2, 0, byteArr, byteArr1.length, byteArr2.length);
									try {
//										server = InetAddress.getLocalHost();
										server = InetAddress.getByName(serverName);
									} catch (UnknownHostException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
									datagramPacket = new DatagramPacket(byteArr, byteArr.length, server, 50000);
									int attempt = 1;
									while(true) {
										try {
											// asking server for instructor inetaddress
											datagramSocket.send(datagramPacket);
											byteArr = new byte[byteArr2.length+byteArr1.length];
											byteArr = new byte[100000];
											datagramPacket = new DatagramPacket(byteArr, 100000);
											datagramSocket.receive(datagramPacket);
											datagramSocket.setSoTimeout(2000);
											break;
										} catch (Exception e) {
											// TODO: handle exception
											System.out.println("Timeout: "+attempt);
											attempt++;
											if(attempt == 4)
												break;
										}
									}
									byte[] byteArr_ = Arrays.copyOfRange(datagramPacket.getData(), 0, datagramPacket.getLength());
//									System.out.println("byteArr[0]+byteArr[1]: "+byteArr_[0]+""+byteArr_[1]);
//									System.out.println("byteArr_.length: "+byteArr_.length);
									if(byteArr_.length == 2 && "04".equals(byteArr_[0]+""+byteArr_[1])) {
										// class not found
										JOptionPane.showMessageDialog(panel, "Class not found. Please check class number.");
									}
									else {// if("11".equals(byteArr[0]+""+byteArr[1])){
//										System.out.println("in else of class not found");
										try {
//											System.out.println("parent1:"+parent);
//											System.out.println("new String(byteArr_): "+new String(byteArr_));
											// 
											parent = InetAddress.getByAddress(byteArr_);
											updateTitle("Student - Parent "+parent);
//											parent = InetAddress.getLocalHost();//(new String(byteArr_));
//											System.out.println("parent: "+parent);
										} catch (UnknownHostException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										byteArr_ = new byte[100000];
										datagramPacket = new DatagramPacket(new byte[]{0, 1}, 2, parent, 65000); // asking instructor for parents address
//										System.out.println("messaging instructor at: "+parent);
										try {
											datagramSocket.send(datagramPacket); // sent instructor request for joining
																				 // instructor replies on always on port with parent &
																				 // child
//											datagramSocket.receive();
										} catch (IOException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										byte[] byteArr4 = new byte[100000];
										datagramPacket = new DatagramPacket(byteArr4, 100000);
										try {
											datagramSocket.setSoTimeout(5000);
										} catch (SocketException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										keepGoing = true;
										while(keepGoing) {
											try {
//												System.out.println("here");
												datagramSocket.receive(datagramPacket);
												InputStream inputStream = new ByteArrayInputStream(Arrays.copyOfRange(datagramPacket.getData(), 0, datagramPacket.getLength()));
												BufferedImage bufferedImage = ImageIO.read(inputStream);
												if(bufferedImage != null)
												{
													onlineClassroomStudent.updateVideo(bufferedImage);
													send(bufferedImage);
												}
											} catch (IOException ex) {
												// TODO Auto-generated catch block
												ex.printStackTrace();
											}
										}
									}
								}
							}
							).start();

				} catch (Exception e2) {
					// TODO: handle exception
					e2.printStackTrace();
					JOptionPane.showMessageDialog(panel, "Enter class number(integer value) in text field and click on start.");
				}
			}
		});
		/**
		 * actions for leave button
		 * 
		 */
		leaveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				keepGoing = false;
				System.exit(0);
			}
		});
	}

	// creates socket
	// connects to server giving it its InetAddress and Port number
	// and keeps its port open for receiving
	public void connect() throws SocketException, UnknownHostException {
		if(datagramSocket != null && !datagramSocket.isClosed())
		{}
		else {
			System.out.println("Datagram socket on student started at port 60000...");
			datagramSocket = new DatagramSocket(destPort);
		}
	}
	
	/**
	 * Updates the title of the student UI when information about parent
	 * is received
	 * @param title
	 */
	public static void updateTitle(String title) {
		onlineClassroomStudent.setTitle(title);
	}

	/**
	 * Updates the swing with the new image received from the parent
	 * @param bufferedImage
	 * @throws IOException
	 */
	public void updateVideo(BufferedImage bufferedImage) throws IOException {
		image.setText("");
		image.setIcon(new ImageIcon(bufferedImage));
	}

	/**
	 * This is the main method.
	 * It initializes the instructor UI by creating an object of this class.
	 * This method also creates a always listening port on the student to receive 
	 * any change in structure information or sleep check
	 * ping from the instructor
	 * @param args
	 */
	public static void main(String[] args) {
		byte[] byteArr = new byte[1000000];
		datagramPacket = new DatagramPacket(byteArr, 1000000); // byte array and length
		try {
			onlineClassroomStudent = new OnlineClassroomStudent();
			onlineClassroomStudent.setVisible(true);
			//onlineClassroomStudent.connect();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				DatagramSocket alwaysOnSocketOnStudent = null;
				try {
					byte[] byteArr = new byte[100000];
					alwaysOnSocketOnStudent = new DatagramSocket(60001);
					DatagramPacket packetOnAlwaysOnDatagramSocket = new DatagramPacket(byteArr, 100000);
					while(true) {
						alwaysOnSocketOnStudent.receive(packetOnAlwaysOnDatagramSocket);
						byte[] receivedInetAddressByteArr = Arrays.copyOfRange(byteArr, 2, byteArr.length);
						// 11 - set InetAddress as parent
						if("11".equals(byteArr[0]+""+byteArr[1])) {
							parent = InetAddress.getByName(new String(receivedInetAddressByteArr).replace("/", ""));	
							System.out.println("Messaged received from instructor about parent: "+parent);
							updateTitle("Student - Parent "+parent);
						}
						// 22 - set InetAddress as child
						else if("22".equals(byteArr[0]+""+byteArr[1])) {
							if(hostLeft == null) {
//								System.out.println("new String(receivedInetAddressByteArr): "+new String(receivedInetAddressByteArr).replace("/", ""));
								hostLeft = InetAddress.getByName(new String(receivedInetAddressByteArr).replace("/", ""));
								System.out.println("Message received from instructor about child hostLeft: "+hostLeft);
							}
							else if (hostRight == null) {
//								System.out.println("new String(receivedInetAddressByteArr): "+new String(receivedInetAddressByteArr).replace("/", ""));
								hostRight = InetAddress.getByName(new String(receivedInetAddressByteArr).replace("/", ""));	
								System.out.println("Message received from instructor about child hostRight: "+hostRight);
							}
						}
						/**
						 *  ping check from instructor
						 */
						else if("33".equals(byteArr[0]+""+byteArr[1])) {
							/**
							 *  reply back to instructor indication student is wide awake
							 *  using same bits 33
							 */
							alwaysOnSocketOnStudent.send(new DatagramPacket(new byte[]{3, 3},  2, packetOnAlwaysOnDatagramSocket.getAddress(), packetOnAlwaysOnDatagramSocket.getPort()));							
						}
						// re-adjust from instructor
						else if("44".equals(byteArr[0]+""+byteArr[1]) || "55".equals(byteArr[0]+""+byteArr[1])) {
							// 44 set InetAddress as leftChild
							if("44".equals(byteArr[0]+""+byteArr[1])) {
								try {
									hostLeft = InetAddress.getByName(new String(receivedInetAddressByteArr).replace("/", ""));
									if(new String(receivedInetAddressByteArr).contains("***null***")) {
										hostLeft = null;
									}
									// reply back to instructor indication hostLeft was set successfully
									// using same bits 44
//									alwaysOnSocketOnStudent.send(new DatagramPacket(new byte[]{4, 4},  2, packetOnAlwaysOnDatagramSocket.getAddress(), packetOnAlwaysOnDatagramSocket.getPort()));
									System.out.println("Messaged received from instructor about new hostLeft: "+hostLeft);									
								} catch (Exception e2) {
									e2.printStackTrace();
									hostLeft = null;
									System.out.println("hostLeft set to null...");
								}
							}
							/**
							 * 55 set received InetAddress as rightChild
							 */
							else if("55".equals(byteArr[0]+""+byteArr[1])) {
								try {
									hostRight = InetAddress.getByName(new String(receivedInetAddressByteArr).replace("/", ""));	
									if(new String(receivedInetAddressByteArr).contains("***null***")) {
										hostRight = null;
									}
									// reply back to instructor indication hostLeft was set successfully
									// using same bits 55
//									alwaysOnSocketOnStudent.send(new DatagramPacket(new byte[]{5, 5},  2, packetOnAlwaysOnDatagramSocket.getAddress(), packetOnAlwaysOnDatagramSocket.getPort()));
									System.out.println("Messaged received from instructor about new hostRight: "+hostRight);								
								} catch (Exception e) {
									e.printStackTrace();
									hostRight = null;
									System.out.println("hostRight set to null...");
								}
							}
						}
					}
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					alwaysOnSocketOnStudent.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					alwaysOnSocketOnStudent.close();
				}
			}
		}).start();
	}

	/**
	 * Sends the received buffered image to its chilrden if any
	 * @param image
	 * @throws IOException
	 */
	private static void send(BufferedImage image) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ImageIO.write(image, "jpg", byteArrayOutputStream);
		byteArrayOutputStream.flush();
		byte[] byteArr = byteArrayOutputStream.toByteArray();
		byteArrayOutputStream.close();
		if(hostLeft != null) {
			datagramPacket = new DatagramPacket(byteArr, byteArr.length, hostLeft, destPort);
			datagramSocket.send(datagramPacket);
		}
		if(hostRight != null) {
			datagramPacket = new DatagramPacket(byteArr, byteArr.length, hostRight, destPort);
			datagramSocket.send(datagramPacket);
		}
	}
}