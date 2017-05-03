package src;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ChatServer {
    //TODO: ask for users in chatroom, protocol? (Send all the static things in the beginning)
    // Base chatroom
    public static String GENERAL_CHAT_ROOM = "GENERAL";
    // The index on which the chatroom  index is saved inside the Integer array of the roomClientMap
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

	private HashMap<String,ArrayList<Integer>> roomClientMap = new HashMap<>();

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
	    this.port = port;
        logging();

	    ChatRoom generalChatRoom = new ChatRoom("General Chatroom");
        this.roomList.add(generalChatRoom);
        System.out.println("Created Server");
        this.start();
	}

	private void logging(){
        this.logger = Logger.getLogger("ServerLogger");

        try {
            FileHandler fileHandler = new FileHandler("D:\\Git-Repos\\haw-rnp\\a2-chat\\src\\ServerLog.log");
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
	 * @throws IOException 
	 */
	public void start() {
	    clientList.add(null);
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

                if(socket == null){
                    logger("Accepted socket is null");
                }

                // Abbruch, wenn der Server gestoppt wird
                if (!serverStatus) break;

                logger("Starting client thread");
                int goodIndex = getGoodIndex();
                ClientThread ct = new ClientThread(socket,goodIndex);
                clientList.add(goodIndex,ct);
                ct.start();
            }

            try {
                serverSocket.close();
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
            }
        } catch (IOException e) {}
    }
	
	/**
	 * Die vorhandene Socketverbindung wird hier unterbrochen
	 */
	public void stop() {

	}

	public String help(){
	    String help = "help_server: show this help\n" +
                "logout: Logout from the server\n" +
                "logout_room: Logout from current chatroom to General\n" +
                "in_chatroom: Show in which chatroom you are currently in\n" +
                "users_in_chatroom: list all users in current chatroom\n" +
                "list_users: list all users on the server\n" +
                "list_chatrooms: list all chatrooms on the server\ncreate_chatroom: create chatroom with given name\n" +
                "join: join given chatroom";
	    return help;
    }

	// We want to have a small group of users but the ids always near each other
	private int getGoodIndex(){
	    for(int i = 1; i < clientList.size(); i++){
	        if(clientList.get(i) == null){
	            return i;
            }
        }
        return clientList.size();
    }

	public String loggedUsers() {
	    String userList = "";
		int size = clientList.size();
	    for(int i = 1; i < size; i++){
	        userList =  userList + clientList.get(i).getUsername() + "\n";
        }
	    return userList;
	}
	
	private void logger(String msg) {
		logger.info(msg);
	}


	// ClientThread
	public class ClientThread extends Thread {
        Socket socket;
        int id;
        String date;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        ChatMessage cm;
        String chatRoom;
        String username;

        ClientThread(Socket socket, int index) {
            // a unique id
            id = index;
            this.socket = socket;
            this.chatRoom = GENERAL_CHAT_ROOM;
            System.out.println("Thread trying to create Object Input/Output Streams");
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                ChatMessage chatMessage = (ChatMessage) sInput.readObject();
                username = chatMessage.getText();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            logger.info("Initialized ClientThread " +  getUsername());
            //TODO: Send date?
            date = new Date().toString() + "\n";
        }

        @Override
        public void run() {
            logger.info("ClientThread " + getUsername() + " started");
            boolean running = true;
            while (running) {
                try {
                    cm = (ChatMessage) sInput.readObject();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                String message = cm.getText();
                String roomName = cm.getChatRoomName();
                logger(chatRoom +  ": " + getUsername() + " sent following message: " + message);
                Integer size = null;
                ChatRoom cr = null;
                switch (cm.getType()) {
                    case ChatMessage.INITIALIZE:
                        size = clientList.size();
                        boolean initialized = true;
                        for(int i = 1; i < size; i++){
                            if(clientList.get(i).getUsername().equals(message)){
                                logger("Username already taken");
                                sendMessage("Username already taken",GENERAL_CHAT_ROOM,ChatMessage.ERR_USERNAME);
                                initialized = false;
                                break;
                            }
                        }
                        if(initialized) {
                            username = message;
                            sendMessage("Login successful", GENERAL_CHAT_ROOM, ChatMessage.MESSAGE);
                            logger(getUsername() + " logged successfully in");
                            break;
                        }
                        break;
                    case ChatMessage.HELP_SERVER:
                        sendMessage(help(),chatRoom,ChatMessage.HELP_SERVER);
                    case ChatMessage.USERS_IN_CHATROOM:
                        String users = "Chatroom " + chatRoom + ":\n";
                        if(chatRoom.equals(GENERAL_CHAT_ROOM)){
                            sendMessage(loggedUsers(),chatRoom,ChatMessage.USERS_IN_CHATROOM);
                            break;
                        }

                        ArrayList<Integer> clientsInChat = roomClientMap.get(chatRoom);
                        for(int i = 1; i < clientsInChat.size(); i++){
                            if(clientList.get(clientsInChat.get(i)) != null) users += (clientList.get(clientsInChat.get(i)).getUsername()) + "\n";
                        }
                        sendMessage(users,chatRoom,ChatMessage.USERS_IN_CHATROOM);
                        break;

                    // Login to a chatroom
                    case ChatMessage.JOIN_CHATROOM:
                        System.out.println("Join " + message);
                        if (roomClientMap.containsKey(message)) {
                            boolean idInMap = false;
                            size = roomClientMap.size();
                            ArrayList<Integer> userList = roomClientMap.get(message);
                            System.out.println(userList.size());
                            for(int i = 1; i < size ; i++){
                                if(userList.get(i).equals(id)){
                                    idInMap = true;
                                    break;
                                }
                            }
                            System.out.println(idInMap);
                            if(!idInMap) {
                                roomClientMap.get(message).add(id);
                                this.chatRoom = message;
                                sendMessage("Joined chatroom " + chatRoom, chatRoom, ChatMessage.MESSAGE);
                                List<ChatMessage> messageLog = roomList.get(roomClientMap.get(message).get(CHATROOM_INDEX_POS)).getMessages();
                                size = messageLog.size();
                                for(int i = 0; i < size ; i++){
                                    sendMessage(messageLog.get(i).getText(),messageLog.get(i).getChatRoomName(),messageLog.get(i).getType());
                                }
                            } else {
                                sendMessage("Error: Already in chatroom: " + message,chatRoom,ChatMessage.ERROR);
                            }
                        }else {
                            sendMessage("Error: Chatroom " + message + " does not exist",chatRoom,ChatMessage.ERROR);
                        }
                        break;

                    // List all the available chatrooms
                    case ChatMessage.LIST_CHATROOMS:
                        sendMessage(roomList.toString(),roomName,ChatMessage.LIST_CHATROOMS);
                        logger.info("Listed server for " + getUsername());
                        break;

                    // Create a chatroom
                    case ChatMessage.CREATE_CHATROOM:
                        // Creating a new chatroom
                        for(int i = 0; i < roomList.size(); i++){
                            if(roomList.get(i).getName().equals(message)){
                                sendMessage("Room name is already taken, please choose another one",GENERAL_CHAT_ROOM,ChatMessage.ERROR);
                                //TODO: break just out of for, but has to break through switch
                                break;
                            }
                        }
                        cr = new ChatRoom(message);
                        ArrayList usersArray = new ArrayList<Integer>();
                        roomList.add(cr);
                        usersArray.add(roomList.indexOf(cr));
                        usersArray.add(id);
                        roomClientMap.put(cr.getName(),usersArray);
                        System.out.println(chatRoom);
                        if (!(chatRoom.equals(GENERAL_CHAT_ROOM)) && roomClientMap.containsKey(chatRoom)) {
                            System.out.println(chatRoom);
                            System.out.println(roomClientMap.get(chatRoom).contains(id));
                            roomClientMap.get(chatRoom).remove(id);
                            System.out.println(roomClientMap.get(chatRoom).contains(id));
                        }
                        this.chatRoom = message;
                        // Answer and logging
                        sendMessage("\nConnected to chatroom: " + cr.getName(),roomName,ChatMessage.CREATE_CHATROOM);
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
                        sendMessage(chatRoom,GENERAL_CHAT_ROOM,ChatMessage.JOIN_CHATROOM);
                        break;

                    // Send message to all users in the chatroom that is specified
                    case ChatMessage.MESSAGE:
                        if(roomName.equals(ChatServer.GENERAL_CHAT_ROOM)){
                            sendMessage(message,GENERAL_CHAT_ROOM,ChatMessage.MESSAGE);
                            break;
                        } else if (roomClientMap.containsKey(roomName)) {
                            ArrayList<Integer> clientListofRoom = roomClientMap.get(roomName);
                            // -1 because of the chatroomindex
                            size = clientListofRoom.size()-1;

                            for(int i = 0; i < size; i++){
                                int a = i+1;
                                // -1 because of uniqueId beginning at 1, but we need to start from 0
                                ClientThread akkuCT = clientList.get(clientListofRoom.get(a));
                                long threadId = akkuCT.getClientId();
                                if(threadId != id){
                                    String send = getUsername() + "|" + roomName + " > " + message;
                                    boolean isMessageSent = akkuCT.sendMessage(send,roomName,ChatMessage.MESSAGE);
                                    if(!isMessageSent){
                                        logger("Removing ClientThread, message could not be sent");
                                        //TODO: sinnvolle reaktion?
                                        clientList.remove(akkuCT);
                                    } else {
                                        cr = roomList.get(roomClientMap.get(roomName).get(CHATROOM_INDEX_POS));
                                        cr.logMessage(new ChatMessage(send,ChatMessage.MESSAGE,roomName));
                                        logger("Logged message to chatroom " + cr.getName());
                                    }
                                } else {
                                    String send = getUsername() + "|" + roomName + " > " + message;
                                    sendMessage(message,GENERAL_CHAT_ROOM,ChatMessage.MESSAGE);
                                    cr = roomList.get(roomClientMap.get(roomName).get(CHATROOM_INDEX_POS));
                                    cr.logMessage(new ChatMessage(send,ChatMessage.MESSAGE,roomName));
                                    logger("Logged message to chatroom " + cr.getName());
                                }
                            }
                        }
                        break;
                    // Logout from the server
                    case ChatMessage.LOGOUT:
                        clientList.remove(this);
                        int index = roomClientMap.get(chatRoom).indexOf(this.id);
                        roomClientMap.get(chatRoom).remove(index);
                        running = false;
                        break;
                    case ChatMessage.LIST_USERS:
                        sendMessage(loggedUsers(),roomName,ChatMessage.LIST_USERS);
                        break;
                }
            }
        }

        private boolean sendMessage(String message, String room, int type) {
            try {
                switch (type) {
                    case ChatMessage.MESSAGE:
                        if(!(room.equals(GENERAL_CHAT_ROOM))) {
                            sOutput.writeObject(MESSAGE_FROM_OTHER_CLIENT);
                            sOutput.writeObject(message);
                        }
                        sOutput.writeObject("");
                        break;
                    case ChatMessage.ERR_USERNAME:
                        logger("Already used username");
                        sOutput.writeObject(ERR_USERNAME);
                        sOutput.writeObject(message);
                        break;
                    case ChatMessage.ERROR:
                        //System.out.println("ERROOOOOOOOOOOR");
                        sOutput.writeObject(ERROR);
                        sOutput.writeObject(message);
                        break;
                    case ChatMessage.CREATE_CHATROOM:
                    case ChatMessage.LIST_USERS:
                    case ChatMessage.JOIN_CHATROOM:
                    case ChatMessage.LIST_CHATROOMS:
                    default:
                        //System.out.println("SERVERCALL");
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
    }
}
