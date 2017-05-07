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
		
		logger("Created Server");

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
		clientList.add(null); // FAGEN hä warum?
		serverStatus = true;

		try {
			serverSocket = new ServerSocket(port);
			logger.info("Server ist auf dem Port " + port + " gestartet");

			while (serverStatus) {

				// Lauscht auf eine Verbindung
				Socket socket = null;

				try {
					socket = serverSocket.accept();
					logger("Accepted connection");
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (socket == null) {
					logger("Accepted socket is null");
				}

				// Abbruch, wenn der Server gestoppt wird
				if (!serverStatus)
					break;

				logger("Starting client thread");

				// Client Thread erstellen
				int goodIndex = getGoodIndex();
				ClientThread ct = new ClientThread(socket, goodIndex);
				clientList.add(goodIndex, ct);
				ct.start();
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

	private synchronized boolean broadcast(String message, ChatRoom room, int type) {
		if(room.equals(ChatServer.GENERAL_CHAT_ROOM)){
			
		} else {
			ArrayList<Integer> clientsInChat = roomClientMap.get(room);
			
			for(int i = 1; i < clientsInChat.size(); i++){
	            ClientThread client = clientList.get(i);
				if(client != null) {
					if(!client.writeMsg(message)) {
						removeClient(i); // user war not available remove it
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

	public boolean isPortInUse(int port) {
		return (this.port == port);
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

			logger("Thread trying to create Object Input/Output Streams");
			try {
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput = new ObjectInputStream(socket.getInputStream());

				ChatMessage chatMessage = (ChatMessage) sInput.readObject();
				initializeUser(chatMessage);

				username = chatMessage.getText();
				logger(username + " hat sich connected");
			} catch (IOException e) {
				logger("Exception beim Erstellen von new Input/output Streams: " + e);
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace(); // wird nicht auftreten
			}

			logger.info("Initialized ClientThread " + getUsername());
		}

		private void initializeUser(ChatMessage message) {
			switch (message.getType()) {
				case ChatMessage.INITIALIZE:
					boolean initialized = true;
					for (int i = 1; i < clientList.size(); i++) {
						System.out.println("vergleiche " + clientList.get(i).getUsername() + " mit " + message.getText());
						if (clientList.get(i).getUsername().equals(message.getText())) {
							logger("Username bereits vergeben");
							broadcast("Username already taken", roomList.get(0), ChatMessage.ERR_USERNAME);
							initialized = false;
							break;
						}
					}
					
					System.out.println(id);
					if (initialized || clientList.isEmpty()) {
						username = message.getText();
						
						ArrayList<Integer> roomClients = roomClientMap.get(roomList.get(0));
						roomClients.add(id);
						roomClientMap.put(roomList.get(0), roomClients );
						

						logger("Login successful" + getUsername());
						logger("Added to General Chatroom" + getUsername());
						broadcast("Login successful", roomList.get(0), ChatMessage.MESSAGE);
					}
					break;
			}

		}

		@Override
		public void run() {
			logger.info("ClientThread " + getUsername() + " started");
			boolean running = true;

			while (running) {
				try {
					cm = (ChatMessage) sInput.readObject();
					System.out.println("Type " + cm.getType());
				} catch (IOException e) {
					logger("ClientThread lesen " + getUsername() + " closed");
					// TODO: Delete everything from clienthtread
					e.printStackTrace();
					break;
				} catch (ClassNotFoundException e) {
					logger("ClientThread " + getUsername() + " closed");
					e.printStackTrace();
				}

				String message = cm.getText();
				String roomName = cm.getChatRoomName();
				logger(chatRoom + ": " + getUsername() + " sent following message: " + message);

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
						if (roomClientMap.containsKey(message)) {
							boolean idInMap = false;
							ArrayList<Integer> userList = roomClientMap.get(message);
							size = userList.size();
							for (int i = 1; i < size; i++) {
								if (userList.get(i).equals(id)) {
									idInMap = true;
									break;
								}
							}
							System.out.println(idInMap);
							/*if (!idInMap) {
								roomClientMap.get(message).add(id);
								this.chatRoom = message;
								sendMessage("Joined chatroom " + chatRoom, chatRoom, ChatMessage.MESSAGE);
								List<ChatMessage> messageLog = roomList
										.get(roomClientMap.get(message).get(CHATROOM_INDEX_POS)).getMessages();
								size = messageLog.size();
								for (int i = 0; i < size; i++) {
									sendMessage(messageLog.get(i).getText(), messageLog.get(i).getChatRoomName(),
											messageLog.get(i).getType());
								}
							} else {
								sendMessage("Error: Already in chatroom: " + message, chatRoom, ChatMessage.ERROR);
							}*/
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
						// Creating a new chatroom
						for (int i = 0; i < roomList.size(); i++) {
							if (roomList.get(i).getName().equals(message)) {
								sendMessage("Room name is already taken, please choose another one", GENERAL_CHAT_ROOM,
										ChatMessage.ERROR);
								// TODO: break just out of for, but has to break
								// through switch
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
						ChatRoom cRoom = roomList.get(0);
						for (ChatRoom room : roomList) {
							if(room.getName() == roomName) {
								cRoom = room;
							}
						}
						broadcast(message, cRoom, ChatMessage.JOIN_CHATROOM);
						/*
						 * if(roomName.equals(ChatServer.GENERAL_CHAT_ROOM)){
						 * sendMessage(message,GENERAL_CHAT_ROOM,ChatMessage.MESSAGE
						 * ); break; } else if (roomClientMap.containsKey(roomName))
						 * { ArrayList<Integer> clientListofRoom =
						 * roomClientMap.get(roomName); // -1 because of the
						 * chatroomindex size = clientListofRoom.size()-1;
						 * 
						 * for(int i = 0; i < size; i++){ int a = i+1; // -1 because
						 * of uniqueId beginning at 1, but we need to start from 0
						 * ClientThread akkuCT =
						 * clientList.get(clientListofRoom.get(a)); long threadId =
						 * akkuCT.getClientId(); if(threadId != id){ String send =
						 * getUsername() + "|" + roomName + " > " + message; boolean
						 * isMessageSent =
						 * akkuCT.sendMessage(send,roomName,ChatMessage.MESSAGE);
						 * if(!isMessageSent){
						 * logger("Removing ClientThread, message could not be sent"
						 * ); //TODO: sinnvolle reaktion? clientList.remove(akkuCT);
						 * } else { } } else {
						 * sendMessage(message,GENERAL_CHAT_ROOM,ChatMessage.MESSAGE
						 * ); } } String send = getUsername() + "|" + roomName +
						 * " > " + message; cr =
						 * roomList.get(roomClientMap.get(roomName).get(
						 * CHATROOM_INDEX_POS)); cr.logMessage(new
						 * ChatMessage(send,ChatMessage.MESSAGE,roomName));
						 * logger("Logged message to chatroom " + cr.getName()); }
						 */
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
					broadcast(message, room, type);
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

		public int getClientId() {
			return id;
		}

		public String getUsername() {
			return username;
		}

		/*
		 * Write a String to the Client output stream
		 */
		public boolean writeMsg(String message) {
			// Check if client is still connected
			if (!socket.isConnected()) {
				close();
				return false;
			}

			// send the message to client
			try {
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
