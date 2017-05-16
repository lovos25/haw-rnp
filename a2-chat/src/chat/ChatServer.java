package chat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ChatServer {
	// TODO: ask for users in chatroom, protocol? (Send all the static things in
	// the beginning)

	// Base chatroom wher all the users get by first login
	public static String GENERAL_CHAT_ROOM = "GENERAL";

	// The index on which the chatroom index is saved inside the Integer array
	// of the roomClientMap
	public static int CHATROOM_INDEX_POS = 0;

	// Message types for the clients
	public static String MESSAGE_FROM_OTHER_CLIENT = "##";
	public static String SERVER_CALL = "################";
	public static String ERROR = "####";
	public static String ERR_USERNAME = "#####";
	public static String CHATROOM_LOGOUT = "#####";

	// First initialization of user
	public static String STANDARD_USER = "STANDARD_USER";

	// Server status: An oder Aus
	private boolean serverStatus;

	// Server status: An oder Aus
	private List<ChatRoom> roomList = new ArrayList<>();

	// Romm -> Client Mapping
	private HashMap<ChatRoom, ArrayList<Integer>> roomClientMap = new HashMap<>();

	// Server status: An oder Aus
	private List<ClientThread> clientList = new ArrayList<ClientThread>();
	private HashMap<Integer, ClientThread> clientMapList = new HashMap<Integer, ClientThread>();

	// Port über den die Connection läuft
	private int port;

	// Server logger
	static Logger logger;

	// Socket to connect to
	private ServerSocket serverSocket;

	// ID for the threads
	private static int uniqueId;

	// Constructor
	public ChatServer(int port) {
		logging();

		this.port = port;

		// Erstelle eine general chat room
		ChatRoom generalChatRoom = new ChatRoom(GENERAL_CHAT_ROOM);
		this.roomList.add(generalChatRoom);
		this.roomClientMap.put(generalChatRoom, new ArrayList<Integer>());
		
		logger(" Server wird gestartet...");
		this.start();
	}

	/**
	 * Logging
	 */
	private void logging() {
		ChatServer.logger = Logger.getLogger("ServerLogger");

		try {
			FileHandler fileHandler = new FileHandler(System.getProperty("user.dir") + "//logging//ServerLog.log");
			logger.addHandler(fileHandler);
			SimpleFormatter sf = new SimpleFormatter();
			fileHandler.setFormatter(sf);
		} catch (SecurityException se) {
			se.printStackTrace();
		} catch (IOException io) {
			io.printStackTrace();
		}
	}

	/**
	 * Die Socketverbindung wir hier implimentiert
	 * 
	 * @throws IOException
	 */
	public void start() {
		serverStatus = true;

		try {
			serverSocket = new ServerSocket(port);
			logger.info("Server ist auf dem Port " + port + " gestartet");

			while (serverStatus) {

				// Lauscht auf eine Verbindung
				Socket socket = null;

				try {
					socket = serverSocket.accept();
					logger("Socket Verbindung akzeptiert!");
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (socket == null) {
					logger("Keine Socketverbingungen");
				}

				// Abbruch, wenn der Server gestoppt wird
				if (!serverStatus)
					break;

				logger("Starting client thread");
				intializeClientThread(socket);
				
			}

			// socket Verbindung schliessen
			try {
				serverSocket.close();
				// Jedes Client thread schliessen
				for (int i = 1; i < clientList.size(); ++i) {
					ClientThread tc = clientList.get(i);
					try {
						tc.sInput.close();
						tc.sOutput.close();
						tc.socket.close();
					} catch (IOException ioE) {
						ioE.printStackTrace();
					}
				}
			} catch (Exception e) {
				logger("Exception: Server und Client geschlossen " + e);
			}
		} catch (IOException e) {
			logger("Exception: Server socket kann nicht erstellt werden " + e);
			return;
		}
	}
	
	/**
	 * Client Thread erstellen
	 * @param socket
	 * @return
	 */
	private synchronized void intializeClientThread(Socket socket) {
		ClientThread clientThread = new ClientThread(socket, getGoodIndex());
		clientList.add(clientThread);
		clientThread.start();
    }
	
	/**
	 * Help
	 * @return
	 */
	public String help() {
		String help = "########     help_server:    ##########\n" + "show this help\n"
				+ "logout: Logout from the server\n" + "logout_room: Logout from current chatroom to General\n"
				+ "in_chatroom: Show in which chatroom you are currently in\n"
				+ "users_in_chatroom: list all users in current chatroom\n"
				+ "list_users: list all users on the server\n"
				+ "list_chatrooms: list all chatrooms on the server\ncreate_chatroom: create chatroom with given name\n"
				+ "join: join given chatroom";
		return help;
	}

	/**
	 * We want to have a small group of users but the ids always near each other
	 * 
	 * @return int the next free number
	 */
	private int getGoodIndex() {
		for (int i = 1; i < clientList.size(); i++) {
			if (clientList.get(i) == null) {
				return i;
			}
		}
		return clientList.size();
	}

	/**
	 * Gibt ein String mit allen eingeloggten Usern zurück
	 * 
	 * @return
	 */
	public String loggedUsers() {
		String userList = "";

		for (int i = 1; i < clientList.size(); i++) {
			userList += clientList.get(i).getUsername() + "\n";
		}
		return userList;
	}

	/**
	 * Zeige die Nachricht auf der Konsole
	 * 
	 * @param msg
	 */
	private void logger(String msg) {
		logger.info(msg);
	}
	
	/**
	 * Sende Nachricht an den entsprechenden Emfaenger 
	 * @param message	die Nachricht fuer den Emfaenger
	 * @param room		Raum für den die Nachricht bestimmt ist
	 * @param type		Nachrichten Typ
	 * @return boolean 	True bei erfolgreichem Versand, sonst false			
	 */
	private synchronized boolean broadcast(String message, ChatRoom room, int type) {
		logger("Broadcast - Nachricht wird verbreitet!");
		
		if(roomClientMap.isEmpty()){
			logger("Broadcast - Kein RaumClient Mapping vorhanden!!");
		} else {
			ArrayList<Integer> clientsInChat = roomClientMap.get(room);
			logger("Broadcast an folgende Clients " + clientsInChat.toString());
			
			for (Integer clientId : clientsInChat) {
	            ClientThread client = clientList.get(clientId);
	            System.out.println(client.username);
	            if(client != null) {
	            	client.writeMsg(message);
					if(!client.writeMsg(message)) {
						removeClient(clientId); // user war not available remove it
						logger("Disconnected Client " + client.username + " removed from list.");
					}
				} 
	            
	        }
		}
		
		return true;
	}

	private void removeClient(int i) {
		clientList.remove(i);

		for (Entry<ChatRoom, ArrayList<Integer>> entry : roomClientMap.entrySet()) {
			ArrayList<Integer> values = entry.getValue();

			if (values.contains(i)) {
				values.remove(values.indexOf(i));
			}
		}

		logger("Client removed!");
	}

	/**
	 * Für jeden Client wird ein ClientThread Instance erstellt
	 */
	public class ClientThread extends Thread {
		Socket socket; // the socket where to listen/talk
		ObjectInputStream sInput; // eingehende Nachricht
		ObjectOutputStream sOutput; // aussgehene Nachricht
		int id; // unique id

		ChatMessage cm; // message type we receive
		String chatRoom; // the chatroom
		String username; // the client username

		ClientThread(Socket socket, int index) {
			// a unique id
			id = index;
			this.socket = socket;
			this.chatRoom = GENERAL_CHAT_ROOM;
			clientMapList.put(id, this);

			logger("Thread trying to create Object Input/Output Streams");
			try {
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput = new ObjectInputStream(socket.getInputStream());

			} catch (IOException e) {
				logger("Exception beim Erstellen von new Input/output Streams: " + e);
				e.printStackTrace();
			}

			logger.info("Initialized ClientThread " + getUsername());
		}

		private synchronized void initializeUser(ChatMessage message) {
			boolean initialized = true;
			
/*			for (ClientThread clientThread : clientList) {
				System.out.println("vergleiche " + clientThread.getUsername() + " mit " + message.getText());
				if (clientThread.getUsername().equals(message.getText())) {
					logger("Username bereits vergeben");
					broadcast("Username already taken", roomList.get(0), ChatMessage.ERR_USERNAME);
					initialized = false;
					break;
				}
			}
*/
			
			//if (initialized) {
			username = message.getText();
			
			ArrayList<Integer> roomClients = roomClientMap.get(roomList.get(0));
			roomClients.add(id);
			roomClientMap.put(roomList.get(0), roomClients );
			
			logger("Login successful fuer " + getUsername());
			logger("Added to General Chatroom" + getUsername());
			broadcast("Hey " + getUsername() +" wilkommen!", roomList.get(0), ChatMessage.MESSAGE);

		}

		@Override
		public void run() {
			logger.info("ClientThread " + getUsername() + " started");
			boolean running = true;
			
			while (running) {
				try {
					cm = (ChatMessage) sInput.readObject();
				} catch (IOException e) {
					logger("ClientThread lesen " + getUsername() + " closed");
					e.printStackTrace();
					break;
				} catch (ClassNotFoundException e) {
					logger("ClientThread " + getUsername() + " closed");
					e.printStackTrace();
				}
				
				String message = cm.getText();
				String roomName = cm.getChatRoomName();
				logger(getUsername() + " sent following message: " + message + " to chatroom: " + chatRoom);

				Integer size = null;
				ChatRoom cr = null;
				switch (cm.getType()) {
					case ChatMessage.INITIALIZE:
						initializeUser(cm);
						break;
					case ChatMessage.HELP_SERVER:
						sendMessage(help(), chatRoom, ChatMessage.HELP_SERVER);
						break;
					case ChatMessage.USERS_IN_CHATROOM:
						String users = "Chatroom " + chatRoom + ":\n";
						if (chatRoom.equals(GENERAL_CHAT_ROOM)) {
							sendMessage(loggedUsers(), chatRoom, ChatMessage.USERS_IN_CHATROOM);
							break;
						}
	
						/*ArrayList<ChatClient> clientsInChat = roomClientMap.get(chatRoom);
						for (int i = 1; i < clientsInChat.size(); i++) {
							if (clientList.get(clientsInChat.get(i)) != null)
								users += (clientList.get(clientsInChat.get(i)).getUsername()) + "\n";
						}
						sendMessage(users, chatRoom, ChatMessage.USERS_IN_CHATROOM);*/
						break;
	
					// Login to a chatroom
					case ChatMessage.JOIN_CHATROOM:
						
						ChatRoom askedRoom = roomList.stream().filter(r -> r.getName().equals(message)).findAny().orElse(null);
						
						if (roomClientMap.containsKey(askedRoom)) {
							chatRoom = askedRoom.toString();
							ArrayList<Integer> userList = roomClientMap.get(askedRoom);
							
							if(!userList.contains(id)) {
								userList.add(id);
							} else {
								sendMessage("Joined already " + chatRoom, chatRoom, ChatMessage.MESSAGE);
							}
							
							sendMessage("Joined chatroom " + chatRoom, chatRoom, ChatMessage.MESSAGE);
							List<ChatMessage> messageLog = askedRoom.getMessages(); 
							for (ChatMessage chatMessage : messageLog) {
								sendMessage(chatMessage.toString(), chatMessage.getChatRoomName(), chatMessage.getType());
							}
							
						} else {
							sendMessage("Error: Chatroom " + message + " does not exist", chatRoom, ChatMessage.ERROR);
						}
						break;
	
					// List all the available chatrooms
					case ChatMessage.LIST_CHATROOMS:
						sendMessage(roomList.toString(), roomName, ChatMessage.LIST_CHATROOMS);
						logger.info("Listed server for " + getUsername());
						break;
	
					// Create a chatroom
					case ChatMessage.CREATE_CHATROOM:
						
						for (ChatRoom chatroom : roomList) {
							if (chatroom.getName().equals(message)) {
								sendMessage("Room name is already taken, please choose another one", GENERAL_CHAT_ROOM, ChatMessage.ERROR);
								break;
							}
						}
						
						cr = new ChatRoom(message);
						roomList.add(cr);
						
						ArrayList<Integer> usersArray = new ArrayList<Integer>();
						usersArray.add(id);
						
						roomClientMap.put(cr, usersArray);
						
						if (!(chatRoom.equals(GENERAL_CHAT_ROOM)) && roomClientMap.containsKey(chatRoom)) {
							roomClientMap.get(chatRoom).remove(id);
						}
						this.chatRoom = message;
						// Answer and logging
						sendMessage("\nConnected to chatroom: " + cr.getName(), roomName, ChatMessage.CREATE_CHATROOM);
						logger.info("Created chatroom: " + cr.getName());
						break;
	
					// Logout from chatroom
					case ChatMessage.CHATROOM_LOGOUT:
						if (roomClientMap.containsKey(roomName)) {
							roomClientMap.get(roomName).remove(id);
						}
						chatRoom = GENERAL_CHAT_ROOM;
						break;
	
					// Ask for the current chatroom
					case ChatMessage.IN_CHATROOM:
						broadcast(chatRoom, roomList.get(0), ChatMessage.JOIN_CHATROOM);
						break;
	
					// Send message to all users in the chatroom that is specified
					case ChatMessage.MESSAGE:
						System.out.println(getRoomByString(roomName).toString() + " msg: " + message);
						broadcast(message, getRoomByString(roomName), ChatMessage.JOIN_CHATROOM);
						break;
					// Logout from the server
					case ChatMessage.LOGOUT:
						clientList.remove(this);
						int index = roomClientMap.get(chatRoom).indexOf(this.id);
						roomClientMap.get(chatRoom).remove(index);
						running = false;
						break;
					case ChatMessage.LIST_USERS:
						sendMessage(loggedUsers(), roomName, ChatMessage.LIST_USERS);
						break;
					}
			}

		}

		private boolean sendMessage(String message, String room, int type) {
			try {
				switch (type) {
				case ChatMessage.MESSAGE:
					broadcast(message, getRoomByString(room), type);
					break;
				case ChatMessage.ERR_USERNAME:
					logger("Already used username");
					sOutput.writeObject(ERR_USERNAME);
					sOutput.writeObject(message);
					break;
				case ChatMessage.ERROR:
					sOutput.writeObject(ERROR);
					sOutput.writeObject(message);
					break;
				case ChatMessage.CREATE_CHATROOM:
				case ChatMessage.LIST_USERS:
				case ChatMessage.JOIN_CHATROOM:
				case ChatMessage.LIST_CHATROOMS:
				default:
					sOutput.writeObject(SERVER_CALL);
					sOutput.writeObject(message);

				}
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		
		public ChatRoom getRoomByString(String sRoom) {
			ChatRoom cRoom = roomList.get(0);
			for (ChatRoom room : roomList) {
				if(room.getName() == sRoom) {
					return room;
				}
			}
			return cRoom;
		}

		public int getClientId() {
			return id;
		}

		public String getUsername() {
			return username;
		}

		/*
		 * Write a String to the Client output stream
		 */
		protected synchronized boolean writeMsg(String message) {
			// Check if client is still connected
			if (!socket.isConnected()) {
				close();
				return false;
			}

			// send the message to client
			try {
				logger("Nachricht an " + username + " wurde versendet!");
				sOutput.writeObject(message);
			} catch (IOException e) {
				logger("ERROR: Nachricht an " + username + " wurde nicht versendet!");
				logger(e.toString());
				e.printStackTrace();
			}

			return true;
		}

		/**
		 * Close the socket connection
		 */
		private void close() {
			// Try to close the Output Steam
			try {
				if (sOutput == null)
					sOutput.close();
			} catch (IOException e1) {
				logger("ERROR: sOutput vom " + username + "konnte nicht geschlossen werden");
				e1.printStackTrace();
			}

			// Try to close the Input Steam
			try {
				if (sInput == null)
					sOutput.close();
			} catch (IOException e1) {
				logger("ERROR: sInput vom " + username + "konnte nicht geschlossen werden");
				e1.printStackTrace();
			}

			// Try to close the socket connection
			try {
				socket.close();
			} catch (IOException e) {
				logger("ERROR: Socketverbindung vom " + username + "konnte nicht geschlossen werden");
				e.printStackTrace();
			}

		}
	}
}
