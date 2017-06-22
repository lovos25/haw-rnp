package src.chat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


// TODO: Till 	nachricht nich an sich selbst versenden - Done
// TODO: Till	message type für server aendern
// TODO: Lovo	funktionen testen
// TODO: Lovo	Lege eine Worddatei mir inhaltsverzeihnis und notizen
public class ChatServer {
	public static void main(String[] args) {
		final int PORT = 50000;
		new ChatServer(PORT);
	}

	// Base chatroom where all the users get by first login
	private static String GENERAL_CHAT_ROOM = "General";

	// Alle Nachrichtentypen die der Server akzeptiert
	private final int
			LOGOUT = 0, 		// logout from room
			MESSAGE = 1, 		// send message
			LIST_USERS = 2, 	// list of users
			JOIN_CHATROOM = 3, 	// join a chat room
			LIST_CHATROOMS = 4, // list of avalible chatroms
			CHATROOM_LOGOUT = 5,// logout from chatroom
			CREATE_CHATROOM = 6,// create chatrrom
			ERROR = 7, 			// fehler
			IN_CHATROOM = 8,	// chatroom now
			USERS_IN_CHATROOM = 9, 	// list of users in chatroom
			ERR_USERNAME = 10, 		// hä
			INITIALIZE = 11;		// first register

	// Timeout sind 5 minuten (hier in Millisekunden)
	final int USER_TIMEOUT = 300000;

	// Server status: An oder Aus
	private boolean serverStatus;
	
	// Socket to connect to
	private ServerSocket serverSocket;
	
	// Port über den die Connection läuft
	private int port;
	
	// Server logger
	static Logger logger;
	
	// Server status: An oder Aus
	private List<ChatRoom> roomList = new ArrayList<>();
	
	// Server status: An oder Aus
	private List<ClientThread> clientList = new ArrayList<ClientThread>();
	
	// Room -> Client Mapping
	private HashMap<ChatRoom, ArrayList<Integer>> roomClientMap = new HashMap<>();

	// Constructor
	public ChatServer(int port) {
		logging();

		this.port = port;

		// Erstelle einen generellen chat room
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
			FileHandler fileHandler = new FileHandler(System.getProperty("user.dir") + "\\logging\\ServerLog.log");
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
	 * Die Socketverbindung wir hier implementiert
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
				synchronized (clientList) {
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
		synchronized (clientList) {
			int niceId = getGoodIndex();
			ClientThread clientThread = new ClientThread(socket, niceId);
			clientList.add(niceId, clientThread);
			clientThread.start();
		}
    }

	/**
	 * We want to have a small group of users but the ids always near each other
	 * 
	 * @return int the next free number
	 */
	private synchronized int getGoodIndex() {
		synchronized (clientList) {
			for (int i = 0; i < clientList.size(); i++) {
				if (clientList.get(i) == null) {
					return i;
				}
			}
			return clientList.size();
		}
	}

	/**
	 * Gibt ein String mit allen eingeloggten Usern zurück
	 * 
	 * @return
	 */
	public String getClientListString() {
		String userList = "All users on the server:\n";

		synchronized (clientList) {
			for (ClientThread client : clientList) {
				if (client != null) {
					userList += client.getUsername() + "\n";
				}
			}
			return userList;
		}
	}
	
	/**
	 * Gibt ein String mit allen eingeloggten Usern zurück in dem angefragten Raum
	 * 
	 * @return
	 */
	public String getClientListString(ChatRoom room) {
		String userList = "Users in the Chatroom you are in:\n";

		synchronized (clientList) {
			for (Integer clientId : roomClientMap.get(room)) {
				ClientThread client = clientList.get(clientId);
				if (client != null) {
					userList += client.getUsername() + "\n";
				}
			}
		}
		System.out.println(userList);
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
	private synchronized boolean broadcast(String message, ChatRoom room, int type, int sendingClientId) {
		logger("Broadcast - Nachricht wird verbreitet!");
		synchronized (roomList) {
			if (roomClientMap.isEmpty()) {
				logger("Broadcast - Kein RaumClient Mapping vorhanden!!");
				// Es wird nichts vom GENERAL chat zu anderen Clients gesendet
			} else if (room == roomList.get(0) && type == MESSAGE) {
				logger("Broadcast - Im General Chatroom, Nachricht wird nicht versendet");
			} else {
				synchronized (roomClientMap) {
					ArrayList<Integer> clientsInChat = roomClientMap.get(room);
					logger("Broadcast an folgende Clients " + clientsInChat.toString());

					// Nur wenn die message eine MESSAGE ist, wird es an andere gesendet
					if (type == MESSAGE) {
						synchronized (clientList) {
							ClientThread sendingClient = clientList.get(sendingClientId);
							room.logMessage(new ChatMessage(sendingClient.getUsername() + " > " + message, sendingClientId));
							for (Integer clientId : clientsInChat) {
								// Der Client soll die Nachricht nicht an sich selbst senden
								if (sendingClientId != clientId) {
									ClientThread client = clientList.get(clientId);
									if (client != null) {
										// Neuer String, sonst addieren die sich alle
										String messageToSent = room.getName() + " | " + sendingClient.getUsername() + " > " + message;
										boolean messageSent = client.writeMsg(messageToSent);

										if (!messageSent) {
											disconnectClient(clientId, room); // Benutzer war nicht verfuegbar, wird geloescht
											logger("Disconnected Client " + client.username + " removed from list.");
										}
									}
								}
							}
						}
						//Falls der Nachrichtentyp nicht MESSAGE ist, wird das Ergebnis einfach an den sender gesendet
					} else {
						// # bedeutet die Nachricht ist vom server
						synchronized (clientList) {
							clientList.get(sendingClientId).writeMsg("# " + message);
						}
					}
				}
			}
			return true;
		}
    }

	/**
	 * Client nicht mehr verfuegbar, sodass die Verbindung geschlossen wird
	 * @param clientId
	 */
	private synchronized  void disconnectClient(int clientId, ChatRoom room) {
		synchronized (clientList){
			removeClientFromRoom(clientId, room);
			clientList.get(clientId).close();
			clientList.set(clientId,null);
		}
	}
	
	/**
	 * Client loggt sich aus einem Chatroom aus, 
	 * sodass er aus der RoomClientMap geloescht werden muss
	 * @param clientId
	 */
	protected synchronized void removeClientFromRoom(int clientId, ChatRoom room) {
		logger.info("Moechte " + clientId + " loeschen aus " + room.toString());
		synchronized (roomList) {
			synchronized (roomClientMap) {
				ArrayList<Integer> roomClientIDs = roomClientMap.get(room);
				if (roomClientIDs.contains(clientId)) {
					roomClientIDs.remove(roomClientIDs.indexOf(clientId));
					logger.info("Erledigt " + clientId + " loeschen aus " + room.toString());
				}
				// Loesche Chatroom falls er empty ist für
				if (roomClientIDs.isEmpty() && room != roomList.get(0)) {
					roomClientMap.remove(room);
					roomList.remove(roomList.indexOf(room));
				}
			}
		}
	}

    /**
     * Sendet neuen clients das log des chatrooms (max 50)
     * @param chatRoom
     * @param clientId
     */
    protected synchronized void broadcastChatRoomLog(ChatRoom chatRoom, int clientId){
        logger("Broadcasting chatroom log to new client in: " + chatRoom.getName());
        String log = "# Log #\n";
        synchronized (clientList) {
			if (chatRoom.getMessages().isEmpty()) {
				// Wir machen nichts, sonst kommt da eine leere Nachricht an
			} else {
				for (ChatMessage chatMessage : chatRoom.getMessages()) {
					log += chatRoom.getName() + " | " + chatMessage.getText() + "\n";
				}
				ClientThread client = clientList.get(clientId);
				client.writeMsg(log);
			}
		}
    }

    /**
     * Der Client wird dem chatroom hinzugefügt und aus dem alten entfernt
     * @param clientId
     * @param room
     * @param previousRoom
     */
    protected synchronized void addClientToRoom(int clientId, ChatRoom room, ChatRoom previousRoom){
		synchronized ( clientList) {
			removeClientFromRoom(clientId, previousRoom);
			ClientThread client = clientList.get(clientId);
			if (client != null) {
				client.chatRoom = room;
				ArrayList<Integer> userList;
				synchronized (roomClientMap) {
					userList = roomClientMap.get(room);
				}

				if (!userList.contains(clientId)) {
					userList.add(clientId);
				} else {
					client.writeMsg("Joined already " + room);
				}
			}
		}
    }
	
	/**
	 * Den Client bei general Chat beitreten lassen 
	 * @param clientId
	 */
	protected synchronized void addClientToGeneral(int clientId) {

		synchronized (roomList) {
			ArrayList<Integer> clientIds;
			synchronized (roomClientMap) {
				clientIds = roomClientMap.get(roomList.get(0));
			}
			if (!clientIds.contains(clientId)) {
				clientIds.add(clientId);
				//roomClientMap.put(roomList.get(0), clientIds);
			}

			synchronized (clientList) {
				clientList.get(clientId).chatRoom = roomList.get(0);
			}
		}
	}

	/**
	 * Für jeden Client wird eine ClientThread Instance erstellt
	 */
	public class ClientThread extends Thread {
		Socket socket; 				// the socket where to listen/talk
		InputStream sInput;  // eingehende Nachricht
		OutputStream sOutput;// aussgehene Nachricht
		int id; 					// unique id

		//ChatMessage cm; // message type we receive
		ChatRoom chatRoom;
		String username; // the client username

		ClientThread(Socket socket, int index) {
			id = index;
			this.socket = socket;
			synchronized (roomList) {
				this.chatRoom = roomList.get(0);
			}

			logger("Thread trying to create Object Input/Output Streams");
			try {
				sInput = socket.getInputStream();
				sOutput = socket.getOutputStream();

			} catch (IOException e) {
				logger("Exception beim Erstellen von new Input/output Streams: " + e);
				e.printStackTrace();
			}

			logger.info("Initialized ClientThread with ID: " + id);
		}

        /**
         * Initialisiert den neuen Client auf dem Server
         * @param message
         */
		private synchronized void initializeUser(String message) {
			username = message;
			
			addClientToGeneral(id);
			
			logger("Login erfolgreich fuer " + getUsername() + ", zum general Chatroom hinzugefuegt");

			synchronized (roomList) {
				broadcast("Hey " + getUsername() + " wilkommen im " + roomList.get(0).getName() + " Room!", roomList.get(0), INITIALIZE, id);
			}
		}

		@Override
		public void run() {
			logger.info("ClientThread " + getUsername() + " wurde gestartet");
			boolean running = true;
			int type = 100;
			int length = 0;
			
			while (running) {
				byte[] messageBytes;
				try {
					socket.setSoTimeout(USER_TIMEOUT);
					type = sInput.read();					//cm = (ChatMessage) sInput.readObject();
					length = sInput.read();
					messageBytes = new byte[length];
					sInput.read(messageBytes);
				} catch (IOException e) {
					logger("ClientThread " + getUsername() + " wurde geschlossen");
					disconnectClient(id,chatRoom);
					close();
					break;
				}

				String message = new String(messageBytes);
				//String message = cm.getText();

				Integer size = null;
				ChatRoom cr = null;

				// The switch case for the various message types sent by the current client
				switch (type) {
					// Intialize User
					case INITIALIZE:
						initializeUser(message);
						break;

					case USERS_IN_CHATROOM:
						broadcast(getClientListString(chatRoom), chatRoom, USERS_IN_CHATROOM, id);
						break;
						
					case LIST_USERS:
						broadcast(getClientListString(), chatRoom, LIST_USERS, id);
						break;
						
					// List all the available chatrooms
					case LIST_CHATROOMS:
						broadcast(roomList.toString(), chatRoom, LIST_CHATROOMS, id);
						logger.info("Chatrooms auf dem Server aufgelistet fuer " + getUsername());
						break;
						

					// Login to a chatroom
					case JOIN_CHATROOM:
						
						ChatRoom askedRoom = getRoomByString(message);
						
						if (roomClientMap.containsKey(askedRoom)) {
                            if(askedRoom != chatRoom) {
                                logger.info("Client " + getUsername() + " moechte eintreten in " + askedRoom.toString());
                                // Client zum neuen chatRoom hinzufügen
                                addClientToRoom(id, askedRoom, chatRoom);

                                broadcast("Joined chatroom " + chatRoom, chatRoom, JOIN_CHATROOM, id);
                                logger.info("Client " + getUsername() + " ist eingetreten in " + chatRoom.toString());

                                broadcastChatRoomLog(chatRoom, id);
                            } else {
                                broadcast("You are already in the desired chatroom", chatRoom,JOIN_CHATROOM, id);
                            }
						} else {
							broadcast("Error: Chatroom " + message + " does not exist", chatRoom, ERROR, id);
						}
						break;
	
					// Create a chatroom
					case CREATE_CHATROOM:
						boolean taken = false;
						synchronized (roomList) {
							for (ChatRoom chatroom : roomList) {
								if (chatroom.getName().equals(message)) {
									broadcast("Room name is already taken, please choose another one", roomList.get(0), ERROR, id);
									taken = true;
									break;
								}
							}

							if (taken) break;

							removeClientFromRoom(id, chatRoom);

							cr = new ChatRoom(message);
							this.chatRoom = cr;
							roomList.add(cr);
						}
						
						ArrayList<Integer> usersArray = new ArrayList<Integer>();
						usersArray.add(id);

						synchronized (roomClientMap) {
							roomClientMap.put(cr, usersArray);
						}
						
						// Answer and logging
						broadcast("Connected to chatroom: " + cr.getName(), cr, CREATE_CHATROOM, id);
						logger.info("Created chatroom: " + cr.getName());
						break;
	
					// Logout from chatroom
					case CHATROOM_LOGOUT:
						removeClientFromRoom(id, chatRoom);
						addClientToGeneral(id);
						logger.info("Joined chatroom: " + chatRoom.getName());
						break;
	
					// Ask for the current chatroom
					case IN_CHATROOM:
						broadcast(chatRoom.toString(), chatRoom, IN_CHATROOM, id);
						break;
	
					// Send message to all users in the chatroom that is specified
					case MESSAGE:
						broadcast(message, chatRoom, MESSAGE, id);
						break;
						
					// Logout from the server
					case LOGOUT:
						disconnectClient(id, chatRoom);
						running = false;
						break;
					}
			}

		}
		
		public ChatRoom getRoomByString(String sRoom) {
			synchronized (roomList) {
				return roomList.stream().filter(r -> r.getName().equals(sRoom))
						.findAny().orElse(null);
			}
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
				sOutput.write(message.length());
				sOutput.write(message.getBytes());
			} catch (IOException e) {
				logger("ERROR: Nachricht an " + username + " wurde nicht versendet!");
				logger(e.toString());
				e.printStackTrace();
			}
            logger("Nachricht an " + chatRoom + " wurde versendet!");
			return true;
		}

		/**
		 * Close the socket connection
		 */
		private void close() {
			// Try to close the Output Steam
			try {
				if (sOutput != null)
					sOutput.close();
			} catch (IOException e1) {
				logger("ERROR: sOutput vom " + username + "konnte nicht geschlossen werden");
				e1.printStackTrace();
			}

			// Try to close the Input Steam
			try {
				if (sInput != null)
					sInput.close();
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
