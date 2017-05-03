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
    public static String GENERAL_CHAT_ROOM = "GENERAL";
    public static int CHATROOM_INDEX_POS = 0;
    public static String MESSAGE_FROM_OTHER_CLIENT = "##";
    public static String SERVER_CALL = "################";
    public static String ERROR = "####";
    public static String ERR_USERNAME = "#####";
    public static String STANDARD_USER = "STANDARD_USER";

	// Server status: An oder Aus
	private boolean serverStatus;

	// Server status: An oder Aus
	private List<ChatRoom> roomList = new ArrayList<ChatRoom>();

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
	    uniqueId = 0;
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
                for (int i = 0; i < clientList.size(); ++i) {
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

	// We want to have a small group of users but the ids always near each other
	private int getGoodIndex(){
	    for(int i = 0; i < clientList.size(); i++){
	        if(clientList.get(i) == null){
	            return i;
            }
        }
        return clientList.size();
    }

	public String loggedUsers() {
	    String userList = "";
		int size = clientList.size();
	    for(int i = 0; i < size; i++){
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
                logger(getUsername() + " sent following message: " + message);

                ChatRoom cr = null;
                switch (cm.getType()) {
                    case ChatMessage.INITIALIZE:
                        while(true) {
                            try {
                                int size = clientList.size();
                                for(int i = 0; i < size; i++){
                                    if(clientList.get(i).getUsername().equals(message)){
                                        logger("Username already taken");
                                        sendMessage("Username already taken",GENERAL_CHAT_ROOM,ChatMessage.ERR_USERNAME);
                                        ChatMessage msg =(ChatMessage) sInput.readObject();
                                        message = msg.getText();
                                        continue;
                                    }
                                }
                                username = message;
                                logger(getUsername() + " logged successfully in");
                                break;
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    case ChatMessage.USERS_IN_CHATROOM:
                        String users = chatRoom + "\n";
                        if(chatRoom.equals(GENERAL_CHAT_ROOM)){
                            sendMessage(loggedUsers(),chatRoom,ChatMessage.USERS_IN_CHATROOM);
                            break;
                        }
                        ArrayList<Integer> clientsInChat = roomClientMap.get(chatRoom);
                        for(int i = 0; i < clientsInChat.size(); i++){
                            users += (clientList.get(clientsInChat.get(i)-1).getUsername()) + "\n";
                        }
                        sendMessage(users,chatRoom,ChatMessage.USERS_IN_CHATROOM);

                    // Login to a chatroom
                    case ChatMessage.CHATROOM:
                        if (roomClientMap.containsKey(message)) {
                            if(!(roomClientMap.get(message).contains(id))) {
                                roomClientMap.get(message).add(id);
                                this.chatRoom = message;
                                sendMessage("", GENERAL_CHAT_ROOM, ChatMessage.MESSAGE);
                                List<ChatMessage> messageLog = roomList.get(roomClientMap.get(message).get(CHATROOM_INDEX_POS)).getMessages();
                                int size = messageLog.size();
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
                                break;
                            }
                        }
                        cr = new ChatRoom(message);
                        ArrayList usersArray = new ArrayList<Integer>();
                        roomList.add(cr);
                        usersArray.add(roomList.indexOf(cr));
                        usersArray.add(id);
                        System.out.println("Users size " + usersArray.size());
                        roomClientMap.put(cr.getName(),usersArray);
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
                        break;

                    // Ask for the current chatroom
                    case ChatMessage.IN_CHATROOM:
                        sendMessage(chatRoom,GENERAL_CHAT_ROOM,ChatMessage.ERROR);
                        break;

                    // Send message to all users in the chatroom that is specified
                    case ChatMessage.MESSAGE:
                        if(roomName.equals(ChatServer.GENERAL_CHAT_ROOM)){
                            sendMessage(message,GENERAL_CHAT_ROOM,ChatMessage.MESSAGE);
                            break;
                        }
                        if (roomClientMap.containsKey(roomName)) {
                            ArrayList<Integer> clientListofRoom = roomClientMap.get(roomName);
                            // -1 because of the chatroomindex
                            int size = clientListofRoom.size()-1;

                            for(int i = 0; i < size; i++){
                                int a = i+1;
                                // -1 because of uniqueId beginning at 1, but we need to start from 0
                                ClientThread akkuCT = clientList.get(clientListofRoom.get(a));
                                long threadId = akkuCT.getClientId();
                                if(threadId != id){
                                    String send = getUsername() + "|" + roomName + " > " + message;
                                    boolean isMessageSent = akkuCT.sendMessage(send,roomName,ChatMessage.MESSAGE);
                                    if(!isMessageSent){
                                        //TODO: sinnvolle reaktion?
                                        clientList.remove(akkuCT);
                                    } else {
                                        cr = roomList.get(roomClientMap.get(roomName).get(CHATROOM_INDEX_POS));
                                        cr.logMessage(new ChatMessage(send,ChatMessage.MESSAGE,roomName));
                                        logger.info(getUsername() + " sent a message to " +  roomName);
                                    }
                                } else {
                                    sendMessage(message,GENERAL_CHAT_ROOM,ChatMessage.MESSAGE);
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
                    case ChatMessage.OTHER_USERS:
                        sendMessage(loggedUsers(),roomName,ChatMessage.OTHER_USERS);
                        break;
                }
            }
        }

        private boolean sendMessage(String message, String room, int type) {
            logger("Sending message");
            try {
                switch (type) {
                    case ChatMessage.MESSAGE:
                        if(!(room.equals(GENERAL_CHAT_ROOM))) {
                            sOutput.writeObject(MESSAGE_FROM_OTHER_CLIENT);
                            sOutput.writeObject(message);
                        }
                        sOutput.writeObject("");
                        return true;
                    case ChatMessage.ERR_USERNAME:
                        logger("Already used username");
                        sOutput.writeObject(ERR_USERNAME);
                        sOutput.writeObject(message);
                        return true;
                    case ChatMessage.ERROR:
                        //System.out.println("ERROOOOOOOOOOOR");
                        sOutput.writeObject(ERROR);
                        sOutput.writeObject(message);
                        return true;
                    case ChatMessage.CREATE_CHATROOM:
                    case ChatMessage.OTHER_USERS:
                    case ChatMessage.CHATROOM:
                    case ChatMessage.LIST_CHATROOMS:
                    default:
                        //System.out.println("SERVERCALL");
                        sOutput.writeObject(SERVER_CALL);
                        sOutput.writeObject(message);
                        return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        public int getClientId() {
            return id;
        }

        public String getUsername() {
            return username;
        }
    }
}
