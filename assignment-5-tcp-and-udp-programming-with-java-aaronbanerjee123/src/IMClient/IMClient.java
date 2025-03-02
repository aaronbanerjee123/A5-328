package IMClient;
/*
IMClient.java - Instant Message client using UDP and TCP communication.

Text-based communication of commands.
*/

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

public class IMClient {
	// Protocol and system constants
	public static String serverAddress = "localhost";
	public static int TCPServerPort = 1234; // connection to server

	/*
	 * This value will need to be unique for each client you are running
	 */

   
	public static int TCPMessagePort =1248;


	public static String onlineStatus = "100 ONLINE";
	public static String offlineStatus = "101 OFFLINE";

	private BufferedReader reader; // Used for reading from standard input

	// Client state variables
	private String userId;
	private String status;

	Socket clientSocket;// Client socket to connect to server
	DataOutputStream toServer;
	BufferedReader inFromServer;
	DatagramSocket clientUDPSocket;
	InetAddress IPAddress;

	Socket messageSocket;

	DataOutputStream toUser;
	BufferedReader inFromUser;

	Socket buddySocket;

	byte[] sendData = new byte[1024];
	byte[] receiveData = new byte[1024];

	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
	DatagramPacket sendPacket;

	TCPMessenger messenger;

	public static void main(String[] argv) throws Exception {
		IMClient client = new IMClient();
		client.execute();
	}

	public IMClient() {
		// Initialize variables
		userId = null;
		status = null;
		try {
			ServerSocket temp = new ServerSocket(0);
			TCPMessagePort = temp.getLocalPort();
			temp.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public void execute() throws IOException {
		UDPProcessor udpProcessor = new UDPProcessor(this);
		Thread udpThread = new Thread(udpProcessor);
		udpThread.start();
        
		messenger = new TCPMessenger(this);
		Thread messengerThread = new Thread(messenger);
		messengerThread.start();
		
		initializeThreads();

		String choice;
		reader = new BufferedReader(new InputStreamReader(System.in));
		try {
			// clientSocket = new Socket("localhost", 1234);
			// toServer = new DataOutputStream(clientSocket.getOutputStream());
			// inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			clientUDPSocket = new DatagramSocket();
			IPAddress = InetAddress.getByName("localhost");

		} catch (Exception e) {
			System.out.println("Couldn't initialize socket");
		}


		printMenu();
		choice = getLine().toUpperCase();

		while (!choice.equals("X")) {
			if (choice.equals("Y")) { // Must have accepted an incoming connection
				acceptConnection();
			} else if (choice.equals("N")) { // Must have rejected an incoming connection
				rejectConnection();
			} else if (choice.equals("R")) // Register
			{
				registerUser();
			} else if (choice.equals("L")) // Login as user id
			{
				loginUser();
			} else if (choice.equals("A")) // Add buddy
			{
				addBuddy();
			} else if (choice.equals("D")) // Delete buddy
			{
				deleteBuddy();
			} else if (choice.equals("S")) // Buddy list status
			{
				buddyStatus();
			} else if (choice.equals("M")) // Start messaging with a buddy
			{
				buddyMessage();
			} else
				System.out.println("Invalid input!");

			printMenu();
			choice = getLine().toUpperCase();
		}
		shutdown();
	}

	private Socket createNewServerConnection() throws IOException {
		Socket newConnection = new Socket("localhost", TCPServerPort);
		return newConnection;
	}

	private void initializeThreads() {

		// try {
		// clientSocket = new Socket("localhost", 6789);
		// } catch (Exception e) {
		// // TODO: handle exception
		// }
	}

	private void registerUser() {

		try {
			System.out.println("Enter your new user id: ");
			String userId = getLine();

		
			String registerRequest = String.format("REG %s", userId);

			Socket tempSocket = createNewServerConnection();
			DataOutputStream tempOut = new DataOutputStream((tempSocket.getOutputStream()));
			BufferedReader tempIn = new BufferedReader(new InputStreamReader(tempSocket.getInputStream()));

			tempOut.writeBytes(registerRequest + "\n");
			System.out.println(String.format("Registering user id:%s", userId));
			String regResponse = tempIn.readLine();
			System.out.println(regResponse);

			status = onlineStatus;
			tempSocket.close();

			// messageSocket = new Socket("localhost", TCPMessagePort);
			// toUser = new DataOutputStream(messageSocket.getOutputStream());
			// inFromUser = new BufferedReader(new InputStreamReader(messageSocket.getInputStream()));
			// InputStreamReader(messageSocket.getInputStream()));

			String updateStatusRequest = String.format("SET %s %s %s", userId, status, TCPMessagePort);
			sendData = updateStatusRequest.getBytes();
			sendPacket = new DatagramPacket(sendData, updateStatusRequest.length(), IPAddress, 1235);
			clientUDPSocket.send(sendPacket);

		} catch (Exception e) {
			System.out.println("Unable to register user");
		}

	}

	private void loginUser() { // Login an existing user (no verification required - just set userId to input)
		System.out.print("Enter user id: ");
		userId = getLine();
		System.out.println("User id set to: " + userId);
		status = onlineStatus;

		try {


			// messageSocket = new Socket("localhost", TCPMessagePort);
			// toUser = new DataOutputStream(messageSocket.getOutputStream());
			// inFromUser = new BufferedReader(new InputStreamReader(messageSocket.getInputStream()));

			// InputStreamReader(messageSocket.getInputStream()));

			String updateStatusRequest = String.format("SET %s %s %s", userId, status, TCPMessagePort);
			sendData = updateStatusRequest.getBytes();
			sendPacket = new DatagramPacket(sendData, updateStatusRequest.length(), IPAddress, 1235);
			clientUDPSocket.send(sendPacket);

		} catch (Exception e) {
			System.out.println("Unable to update user status");
		}

	}

	private void addBuddy() { // Add buddy if have current user id
		try {
			System.out.println("Enter buddy id: ");
			String buddyId = getLine();
			String addRequest = String.format("ADD %s %s", userId, buddyId);
			
			Socket tempSocket = createNewServerConnection();
			DataOutputStream tempOut = new DataOutputStream((tempSocket.getOutputStream()));
			BufferedReader tempIn = new BufferedReader(new InputStreamReader(tempSocket.getInputStream()));

			tempOut.writeBytes(addRequest + "\n");

			String addResponse = tempIn.readLine();

			System.out.println(addResponse);
			tempSocket.close();
		} catch (Exception e) {
			System.out.println("Unable to add user");
			System.out.println("hi");
		}

	}

	private void deleteBuddy() { // Delete buddy if have current user id
		try {
			System.out.println("Enter buddy id: ");
			String buddyId = getLine();
			String deleteRequest = String.format("DEL %s %s", userId, buddyId);


			Socket tempSocket = createNewServerConnection();
			DataOutputStream tempOut = new DataOutputStream((tempSocket.getOutputStream()));
			BufferedReader tempIn = new BufferedReader(new InputStreamReader(tempSocket.getInputStream()));

			tempOut.writeBytes(deleteRequest + "\n");
			String deleteResponse = tempIn.readLine();

			System.out.println(deleteResponse);
			tempSocket.close();

		} catch (Exception e) {
			System.out.println("Unable to delete user");
		}
	}

	public String getUserId() {
		return this.userId;
	}

	private void buddyStatus() { // Print out buddy status (need to store state in instance variable that
									// received from previous UDP message)
		try {
			String buddyStatusRequest = String.format("GET %s", userId);
			sendData = buddyStatusRequest.getBytes();

			sendPacket = new DatagramPacket(sendData, buddyStatusRequest.length(), IPAddress, 1235);
			clientUDPSocket.send(sendPacket);

			clientUDPSocket.receive(receivePacket);
			String buddyStatusResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
			System.out.println(buddyStatusResponse);
		} catch (Exception e) {
			System.out.println("Unable to get buddy status");
		}

	}

	private void buddyMessage() { // Make connection to a buddy that is online
		System.out.println("Enter buddy id: ");
		String buddyId = getLine();
		System.out.println("Attempting to connect...");
	
		try {
			String buddyStatusRequest = String.format("GET %s", userId);
			sendData = buddyStatusRequest.getBytes();
	
			sendPacket = new DatagramPacket(sendData, buddyStatusRequest.length(), IPAddress, 1235);
			clientUDPSocket.send(sendPacket);
	
			clientUDPSocket.receive(receivePacket);
			String buddyStatusResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
			System.out.println(buddyStatusResponse);
			String[] allStatus = buddyStatusResponse.split("\n");
	
			int buddyPort = -1;
			System.out.println(allStatus);
	
			for(int i = 0; i < allStatus.length; i++) {
				if(allStatus[i].startsWith(buddyId)) {
					 buddyPort = Integer.parseInt(allStatus[i].split(" ")[4]);
				}
			}
			System.out.println(buddyPort);
	
			buddySocket = new Socket("localhost", buddyPort);
		
			toUser = new DataOutputStream(buddySocket.getOutputStream());
			String connectionRequest = String.format("REQUEST %s %d", userId, TCPMessagePort);
	
			toUser.writeBytes(connectionRequest + "\n");
	
			BufferedReader fromBuddy = new BufferedReader(new InputStreamReader(buddySocket.getInputStream()));
	
			String messageFromUser = fromBuddy.readLine();
	
			if(messageFromUser != null && messageFromUser.equals("ACCEPT")) {
				System.out.println("Connection established.");
				System.out.println("Enter your text to send to buddy. Enter q to quit. ");
				System.out.print("> ");
	
				// Flag to track connection state
				final boolean[] connectionActive = {true};
	
				Thread receiveThread = new Thread(() -> {
					try {
						String buddyMessage;
						while ((buddyMessage = fromBuddy.readLine()) != null) {
							if (buddyMessage.equals("q") || buddyMessage.equals("REJECT\n")) {
								System.out.println("\nBuddy connection closed.");
								connectionActive[0] = false;
								printMenu();
								break;
							}
							System.out.println("\nB: " + buddyMessage);
							System.out.print("> "); // Reprint prompt
						}
						// If we reach here with null message, connection was closed
						if (connectionActive[0]) {
							System.out.println("\nBuddy connection closed.");
							connectionActive[0] = false;
						}
					} catch (IOException e) {
						if (connectionActive[0]) {
							System.out.println("\nConnection closed: " + e.getMessage());
							connectionActive[0] = false;
						}
					}
				});
				receiveThread.start();
	
				String messageInput;
				while(connectionActive[0]) {
					messageInput = getLine();
					
					if (!connectionActive[0]) {
						break;
					}
	
					if(messageInput.equals("q")) {
						try {
							toUser.writeBytes("q\n");
							System.out.println("Buddy connection closed.");
						} catch (IOException e) {
					
						}
						break;
					}
	
					try {
						toUser.writeBytes(messageInput + "\n");
						toUser.flush();
						System.out.print("> ");
					} catch (IOException e) {
						System.out.println("\nCannot send message: connection closed");
						break;
					}
				}
	
				// Make sure connection is properly closed
				try {
					buddySocket.close();
				} catch (Exception e) {
					// Ignore, just ensuring it's closed
				}
			} else {
				System.out.println("Connection request was rejected or timed out");
				buddySocket.close();
				printMenu();

			}
		} catch (Exception e) {
			System.out.println("Unable to get buddy status");
			System.out.println(e);
			// Make sure socket is closed if still open
			if (buddySocket != null && !buddySocket.isClosed()) {
				try {
					buddySocket.close();
				} catch (Exception ex) {
				}
			}
		}
	}

	private void shutdown() { 
		try{
			if(userId != null){
				status = offlineStatus;
				String updateStatusRequest = String.format("SET %s %s %s", userId, status, TCPMessagePort);
				sendData = updateStatusRequest.getBytes();
				sendPacket = new DatagramPacket(sendData, updateStatusRequest.length(), IPAddress, 1235);
				clientUDPSocket.send(sendPacket);
				System.out.println("User status set to offline.");
			}
			if(buddySocket != null && !buddySocket.isClosed()){
				buddySocket.close();
			}

			if(clientUDPSocket != null && !clientUDPSocket.isClosed()){
				clientUDPSocket.close();
			}

			if(TCPMessenger.incomingConnection != null && !TCPMessenger.incomingConnection.isClosed()){
				TCPMessenger.incomingConnection.close();
				TCPMessenger.incomingConnection=null;

			}
			System.out.println("All connections closed");
		}catch(Exception e){

		}
	}

	private void acceptConnection() { // User pressed 'Y' on this side to accept connection
	System.out.println("Connection accepted");
	System.out.println("Enter your text to send to buddy. Enter q to quit.");
	System.out.print("> ");
	
	try {
		DataOutputStream acceptOut = new DataOutputStream(TCPMessenger.incomingConnection.getOutputStream());
		acceptOut.writeBytes("ACCEPT\n");
		
		BufferedReader fromBuddy = new BufferedReader(new InputStreamReader(TCPMessenger.incomingConnection.getInputStream()));

		// Flag to track connection state
		final boolean[] connectionActive = {true};

		Thread receiveThread = new Thread(() -> {
			try {
				String message = null;

				while((message = fromBuddy.readLine()) != null) {
					if(message.equals("q")) {
						System.out.println("\nBuddy has closed the connection");
						connectionActive[0] = false;
						break;
					}
					System.out.println("\nB: " + message);
					System.out.print("> ");
				}
				// If we reach here with null message, connection was closed from other side
				if (connectionActive[0]) {
					System.out.println("\nBuddy has closed the connection");
					connectionActive[0] = false;
				}
			} catch (Exception e) {
				if (connectionActive[0]) {
					System.out.println("\nConnection with buddy closed");
					connectionActive[0] = false;
				}
			}
		});
		receiveThread.start();

		String messageInput;
		while(connectionActive[0]) {
			messageInput = getLine();
			
			if (!connectionActive[0]) {
				break;
			}
			
			if(messageInput.equals("q")) {
				try {
					acceptOut.writeBytes("q\n");
					System.out.println("You have closed the connection.");
				} catch (IOException e) {
					// Connection already closed, ignore
				}
				break;
			}
			
			try {
				acceptOut.writeBytes(messageInput + "\n");
				acceptOut.flush();
				System.out.print("> ");
			} catch (IOException e) {
				System.out.println("\nCannot send message: connection closed");
				break;
			}
		}
		
		// Make sure connection is properly closed
		try {
			TCPMessenger.incomingConnection.close();
			TCPMessenger.incomingConnection = null;
			messenger.resetPrompt();
		} catch (Exception e) {
			// Ignore, just ensuring it's closed
		}
	} catch (Exception e) {
		System.out.println("Chat session ended: " + e.getMessage());
	}
}

	private void rejectConnection() { // User pressed 'N' on this side to decline connection
    try {
        if (TCPMessenger.incomingConnection != null) {
            DataOutputStream rejectOut = new DataOutputStream(TCPMessenger.incomingConnection.getOutputStream());
            rejectOut.writeBytes("REJECT\n");
            TCPMessenger.incomingConnection.close();
            TCPMessenger.incomingConnection = null;
            messenger.resetPrompt(); // Reset the prompt flag
            System.out.println("Connection request rejected.");
        } else {
            System.out.println("No incoming connection to reject.");
        }
    } catch (Exception e) {
        System.out.println("Error rejecting connection: " + e.getMessage());
        e.printStackTrace();
    }
}



	private String getLine() { // Read a line from standard input
		String inputLine = null;
		try {
			inputLine = reader.readLine();
		} catch (IOException e) {
			System.out.println(e);
		}
		return inputLine;
	}

	private void printMenu() {
		System.out.println("\n\nSelect one of these options: ");
		System.out.println("  R - Register user id");
		System.out.println("  L - Login as user id");
		System.out.println("  A - Add buddy");
		System.out.println("  D - Delete buddy");
		System.out.println("  M - Message buddy");
		System.out.println("  S - Buddy status");
		System.out.println("  X - Exit application");
		System.out.print("Your choice: ");
	}

	class UDPProcessor implements Runnable {
		private static int UDPPort = 1235;
		private IMClient client;
		InetAddress IPAddress;

		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		DatagramSocket clientUDPSocket;

		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		DatagramPacket sendPacket;
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				sendBuddyRequest();
			}
		};

		public UDPProcessor(IMClient c) {
			client = c;
			try {
				clientUDPSocket = new DatagramSocket();
				IPAddress = InetAddress.getByName("localhost");
			} catch (Exception e) {
				System.out.println("Couldn't initialize Client UDP socket");
			}
		}

		public void run() {
			try {

				timer.schedule(task, 0, 10000);
				printMenu();
			} catch (Exception e) {
				System.out.println(e);
			}
		}

		public void sendBuddyRequest() {

			try {
				if (client.getUserId() != null) {

					String buddyStatusRequest = String.format("GET %s", client.getUserId());
					sendData = buddyStatusRequest.getBytes();

					sendPacket = new DatagramPacket(sendData, buddyStatusRequest.length(), IPAddress, 1235);

					clientUDPSocket.setSoTimeout(10000);

					try {
						clientUDPSocket.send(sendPacket);

						clientUDPSocket.receive(receivePacket);
						String buddyStatusResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
						System.out.println("\nAutomatic status request\n" + buddyStatusResponse);
					} catch (Exception e) {
						System.out.println("Packed receive timed out");
					}

				}
			} catch (Exception e) {
				System.out.println("Unable to get buddy status");
				System.out.println(e);
			}

		}
	}
}

// A record structure to keep track of each individual buddy's status
class BuddyStatusRecord {
	public String IPaddress;
	public String status;
	public String buddyId;
	public String buddyPort;

	public String toString() {
		return buddyId + "\t" + status + "\t" + IPaddress + "\t" + buddyPort;
	}

	public boolean isOnline() {
		return status.indexOf("100") >= 0;
	}
}

// This class implements the TCP welcome socket for other buddies to connect to.
// I have left it here as an example to show where the prompt to ask for
// incoming connections could come from.
class TCPMessenger implements Runnable {
    private IMClient client;
    private ServerSocket welcomeSocket;
    static BufferedReader inFromUser;
    static Integer requestingBuddyPort;
    static Socket incomingConnection;
    private volatile boolean promptDisplayed = false;

    public TCPMessenger(IMClient c) {
        client = c;
        try {
            welcomeSocket = new ServerSocket(client.TCPMessagePort);
        } catch (Exception e) {
            System.out.println("Unable to initialize server socket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void run() {
        // This thread starts an infinite loop looking for TCP requests.
        try {
            while (true) {
                // Listen for a TCP connection request.
                Socket connection = welcomeSocket.accept();
                inFromUser = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String request = inFromUser.readLine();
                
                if (request != null && request.startsWith("REQUEST")) {
                    String[] parts = request.split("\\s+");
                    if (parts.length >= 3) {
                        String buddyId = parts[1];
                        requestingBuddyPort = Integer.parseInt(parts[2]);
                        
                        incomingConnection = connection;
                        
                        if (!promptDisplayed) {
                            System.out.print("\nIncoming connection request from " + buddyId + 
                                            ". Do you want to accept it (y/n)? ");
                            promptDisplayed = true;
                            
                          
                        }
                    } else {
                        // Invalid request format
                        DataOutputStream rejectOut = new DataOutputStream(connection.getOutputStream());
                        rejectOut.writeBytes("REJECT\n");
                        connection.close();
                    }
                } else {
                    // Not a request message, close the connection
                    connection.close();
                }
            }
        } catch (Exception e) {
            System.out.println("Error in TCP messenger: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Method to reset the prompt flag - call this after handling Y/N response
    public void resetPrompt() {
        promptDisplayed = false;
    }
}