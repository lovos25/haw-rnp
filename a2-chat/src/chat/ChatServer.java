package chat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
	
	// Base chatroom wher all the users get by first login
	public static String GENERAL_CHAT_ROOM = "General";

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
	public String getClientListString() {
		String userList = "";

		for (ClientThread client: clientList) {
			userList += client.getUsername() + "\n";
		}
		return userList;
	}
	
	/**
	 * Gibt ein String mit allen eingeloggten Usern zurück in dem angefragten Raum
	 * 
	 * @return
	 */
	public String getClientListString(ChatRoom room) {
		String userList = "";
		
		for (Integer client : roomClientMap.get(room)) {
			userList += clientList.get(client).getUsername() + "\n";
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
	private synchronized boolean broadcast(String message, ChatRoom room, int type, int sendingClientId) {
		logger("Broadcast - Nachricht wird verbreitet!");
		
		if(roomClientMap.isEmpty()){
			logger("Broadcast - Kein RaumClient Mapping vorhanden!!");
        // Es wird nichts vom GENERAL chat zu anderen Clients gesendet
		} else if(room == roomList.get(0) && type == ChatMessage.MESSAGE) {
            logger("Broadcast - Im General Chatroom, Nachricht wird nicht versendet");
        } else {
            ArrayList<Integer> clientsInChat = roomClientMap.get(room);
            logger("Broadcast an folgende Clients " + clientsInChat.toString());

            // Nur wenn die message eine MESSAGE ist, wird es an andere gesendet
            if(type == ChatMessage.MESSAGE) {
                ClientThread sendingClient = clientList.get(sendingClientId);
                room.logMessage(new ChatMessage(sendingClient.getUsername() + " > " + message,room.getName(),sendingClientId));
                for (Integer clientId : clientsInChat) {
                    // Der Client soll die Nachricht nicht an sich selbst senden
                    if (sendingClientId != clientId) {
                        ClientThread client = clientList.get(clientId);
                        if (client != null) {
                            // Neuer String, sonds addieren die sich alle
                            String messageToSent = sendingClient.getUsername() + " > " + message;
                            boolean messageSent = client.writeMsg(messageToSent);

                            if (!messageSent) {
                                disconnectClient(clientId, room); // Benutzer war nicht verfuegbar, wird geloescht
                                logger("Disconnected Client " + client.username + " removed from list.");
                            }
                        }
                    }
                }
            //Falls der Nachrichtentyp nicht MESSAGE ist, wird das Ergebnis einfach an den sender gesendet
            } else {
                clientList.get(sendingClientId).writeMsg(message);
            }
        }
        return true;
    }

	/**
	 * Client nicht mehr verfuegbar, sodass die Verbindung geschlossen wird
	 * @param clientId
	 */
	private synchronized void disconnectClient(int clientId, ChatRoom room) {
		removeClientFromRoom(clientId, room);
		clientList.get(clientId).close();
		clientList.remove(clientId);
	}
	
	/**
	 * Client loggt sich aus einem Chatroom aus, 
	 * sodass er aus der RoomClientMap geloescht werden muss
	 * @param clientId
	 */
	protected synchronized void removeClientFromRoom(int clientId, ChatRoom room) {
		System.out.println(roomClientMap.toString());
		logger.info("Moechte " + clientId + " loeschen aus " + room.toString());
		
		ArrayList<Integer> roomClientIDs = roomClientMap.get(room);
		if(roomClientIDs.contains(clientId)) {
			roomClientIDs.remove(roomClientIDs.indexOf(clientId));
			logger.info("Erledigt " + clientId + " loeschen aus " + room.toString());
		}
        // Loesche Chatroom falls er empty ist für
        if(roomClientIDs.isEmpty() && room != roomList.get(0)){
            roomClientMap.remove(room);
        }
	}

    /**
     * Sendet neuen clients das log des chatrooms (max 50)
     * @param chatRoom
     * @param clientId
     */
    protected synchronized void broadcastChatRoomLog(ChatRoom chatRoom, int clientId){
        logger("Broadcasting chatroom log to new client in: " + chatRoom.getName());
        String log = "#Log#\n";
        if(chatRoom.getMessages().isEmpty()){
            // Wir machen nichts, sonst kommt da eine leere Nachricht an
        }else {
            for (ChatMessage chatMessage : chatRoom.getMessages()) {
                log += chatMessage.getText() + "\n";
            }
            System.out.println(log);
            ClientThread client = clientList.get(clientId);
            client.writeMsg(log);
        }
    }

    /**
     * Der Client wird dem chatroom hinzugefügt und aus dem alten entfernt
     * @param clientId
     * @param room
     * @param previousRoom
     */
    protected synchronized void addClientToRoom(int clientId, ChatRoom room, ChatRoom previousRoom){
        removeClientFromRoom(clientId, previousRoom);
        ClientThread client = clientList.get(clientId);
        if(client != null) {
            client.chatRoom = room;

            ArrayList<Integer> userList = roomClientMap.get(room);

            if (!userList.contains(clientId)) {
                userList.add(clientId);
            } else {
                client.writeMsg("Joined already " + room);
            }
        }
    }
	
	/**
	 * Den Client bei general Chat beitreten lassen 
	 * @param clientId
	 */
	protected synchronized void addClientToGeneral(int clientId) {
		ArrayList<Integer> clientIds = roomClientMap.get(roomList.get(0));
		System.out.println(clientIds.toString());
		if(! clientIds.contains(clientId)) {
			clientIds.add(clientId);
			//roomClientMap.put(roomList.get(0), clientIds);
		}
		
		clientList.get(clientId).chatRoom = roomList.get(0);
	}

	/**
	 * Für jeden Client wird eine ClientThread Instance erstellt
	 */
	public class ClientThread extends Thread {
		Socket socket; 				// the socket where to listen/talk
		ObjectInputStream sInput; 	// eingehende Nachricht
		ObjectOutputStream sOutput; // aussgehene Nachricht
		int id; 					// unique id

		ChatMessage cm; // message type we receive
		ChatRoom chatRoom;
		String username; // the client username

		ClientThread(Socket socket, int index) {
			id = index;
			this.socket = socket;
			this.chatRoom = roomList.get(0);

			logger("Thread trying to create Object Input/Output Streams");
			try {
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput = new ObjectInputStream(socket.getInputStream());

			} catch (IOException e) {
				logger("Exception beim Erstellen von new Input/output Streams: " + e);
				e.printStackTrace();
			}

			logger.info("Initialized ClientThread with ID: " + id);
		}

		private synchronized void initializeUser(ChatMessage message) {
			username = message.getText();
			
			addClientToGeneral(id);
			
			logger("Login successful fuer " + getUsername());
			logger("Added to General Chatroom" + getUsername());
			
			broadcast("Hey " + getUsername() +" wilkommen im " + roomList.get(0).getName() + " Room!", roomList.get(0), ChatMessage.INITIALIZE, id);
		}

		@Override
		public void run() {
			logger.info("ClientThread " + getUsername() + " wurde gestartet");
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

				Integer size = null;
				ChatRoom cr = null;
				switch (cm.getType()) {
					// Intialize User
					case ChatMessage.INITIALIZE:
						initializeUser(cm);
						break;

					// List of Users in Chatroom
					case ChatMessage.USERS_IN_CHATROOM:
						
						String users = "Chatroom " + chatRoom + ":\n";
						if (chatRoom.equals(GENERAL_CHAT_ROOM)) {
							broadcast(getClientListString(chatRoom), chatRoom, ChatMessage.USERS_IN_CHATROOM, id);
							break;
						}
	
						break;
						
					case ChatMessage.LIST_USERS:
						broadcast(getClientListString(), chatRoom, ChatMessage.LIST_USERS, id);
						break;
						
					// List all the available chatrooms
					case ChatMessage.LIST_CHATROOMS:
						broadcast(roomList.toString(), chatRoom, ChatMessage.LIST_CHATROOMS, id);
						logger.info("Chatrooms auf dem Server aufgelistet fuer " + getUsername());
						break;
						

					// Login to a chatroom
					case ChatMessage.JOIN_CHATROOM:
						
						ChatRoom askedRoom = getRoomByString(message);
						
						if (roomClientMap.containsKey(askedRoom)) {
                            logger.info("Client " + getUsername() + " moechte eintreten in " + askedRoom.toString());
                            // Client zum neuen chatRoom hinzufügen
                            addClientToRoom(id, askedRoom, chatRoom);

							broadcast("Joined chatroom " + chatRoom, chatRoom, ChatMessage.JOIN_CHATROOM, id);
							logger.info("Client " + getUsername() + " ist eingetreten in " + chatRoom.toString());

                            broadcastChatRoomLog(chatRoom, id);
							
						} else {
							broadcast("Error: Chatroom " + message + " does not exist", chatRoom, ChatMessage.ERROR, id);
						}
						break;
	
					// Create a chatroom
					case ChatMessage.CREATE_CHATROOM:
						
						for (ChatRoom chatroom : roomList) {
							if (chatroom.getName().equals(message)) {
								broadcast("Room name is already taken, please choose another one", roomList.get(0), ChatMessage.ERROR, id);
								break;
							}
						}
						removeClientFromRoom(id, chatRoom);
						
						cr = new ChatRoom(message);
						this.chatRoom = cr;
						roomList.add(cr);
						
						ArrayList<Integer> usersArray = new ArrayList<Integer>();
						usersArray.add(id);
						
						roomClientMap.put(cr, usersArray);
						
						// Answer and logging
						broadcast("Connected to chatroom: " + cr.getName(), cr, ChatMessage.CREATE_CHATROOM, id);
						logger.info("Created chatroom: " + cr.getName());
						break;
	
					// Logout from chatroom
					case ChatMessage.CHATROOM_LOGOUT:
						removeClientFromRoom(id, chatRoom);
						addClientToGeneral(id);
						logger.info("Joined chatroom: " + chatRoom.getName());
						break;
	
					// Ask for the current chatroom
					case ChatMessage.IN_CHATROOM:
						broadcast(chatRoom.toString(), chatRoom, ChatMessage.IN_CHATROOM, id);
						break;
	
					// Send message to all users in the chatroom that is specified
					case ChatMessage.MESSAGE:
						broadcast(message, chatRoom, ChatMessage.MESSAGE, id);
						break;
						
					// Logout from the server
					case ChatMessage.LOGOUT:
						disconnectClient(id, chatRoom);
						running = false;
						break;
					}
			}

		}
		
		public ChatRoom getRoomByString(String sRoom) {
			return roomList.stream().filter(r -> r.getName().equals(sRoom))
					.findAny().orElse(null);
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
