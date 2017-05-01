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
    public static String GENERAL_CHAT_ROOM = "GENERAL";
    public static int CHATROOM_INDEX_POS = 0;

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

            while (serverStatus) {
                logger.info("Server ist auf dem Port " + port + " gestartet");

                // Lauscht auf eine Verbindung
                Socket socket = null;
                try {
                    socket = serverSocket.accept();
                    logger.info("Accepted connection");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(socket == null){
                    logger.info("Acceoted socket is null");
                }

                // Abbruch, wenn der Server gestoppt wird
                if (!serverStatus) break;

                logger.info("Starting client thread");
                ClientThread ct = new ClientThread(socket);
                clientList.add(ct);
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

	public class ClientThread extends Thread {
        Socket socket;
        int id;
        String date;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        ChatMessage cm;
        ChatRoom chatRoom;
        String username;

        ClientThread(Socket socket) {

            // a unique id
            id = ++uniqueId;
            this.socket = socket;

            System.out.println("Thread trying to create Object Input/Output Streams");
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            logger.info("Initialized ClientThread " +  getUsername());
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
                String roomName = cm.getChatRoomId();
                logger.info(getUsername() + " sent following message: " + message);

                ChatRoom cr = null;
                switch (cm.getType()) {

                    // Login to a chatroom
                    case ChatMessage.CHATROOM:
                        if (roomClientMap.containsKey(roomName)) {
                            roomClientMap.get(roomName).add(id);

                            if (roomClientMap.containsKey(roomName)) {
                                cr = roomList.get(roomClientMap.get(roomName).get(CHATROOM_INDEX_POS));
                                ArrayList<Integer> clientListofRoom = roomClientMap.get(roomName);
                                int size = clientListofRoom.size();

                                for(int i = 0; i < size; i++){
                                    long threadId = clientList.get(clientListofRoom.get(i)).getId();
                                    if(threadId != id && threadId != CHATROOM_INDEX_POS){
                                        ClientThread ct = clientList.get(clientListofRoom.get(i));
                                        boolean isMessageSent = ct.sendMessage(getUsername() + " joined the chatroom");
                                        if(!isMessageSent){
                                            clientList.remove(ct);
                                        } else {
                                            System.out.println("message sent!");
                                        }
                                    }
                                }
                                // Adding message to log of chatroom
                                cr.logMessage(cm);
                            }
                            //TODO: Send log of chatroom to user
                        }
                        break;

                    // List all the available chatrooms
                    case ChatMessage.LIST_CHATROOMS:
                        try {
                            sOutput.writeObject(roomList.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;

                    // Create a chatroom
                    case ChatMessage.CREATE_CHATROOM:
                        cr = new ChatRoom(message);
                        ArrayList users = new ArrayList<Integer>();
                        roomList.add(cr);
                        users.add(roomList.indexOf(cr));
                        users.add(id);
                        roomClientMap.put(cr.getName(),users);

                        try {
                            sOutput.writeObject("\nConnected to " + cr.getName());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        logger.info("Created chatroom: " + cr.getName());
                        break;

                    // Logout from chatroom
                    case ChatMessage.CHATROOM_LOGOUT:
                        if (roomClientMap.containsKey(roomName)) {
                            roomClientMap.get(roomName).remove(id);
                        }
                        break;

                    // Send message to all users in the chatroom that is specified
                    case ChatMessage.MESSAGE:
                        cr = null;
                        if(roomName.equals(ChatServer.GENERAL_CHAT_ROOM)){
                            logger.info("General chat room, no message sent");
                            break;
                        }
                        if (roomClientMap.containsKey(roomName)) {
                            cr = roomList.get(roomClientMap.get(roomName).get(CHATROOM_INDEX_POS));
                            ArrayList<Integer> clientListofRoom = roomClientMap.get(roomName);
                            int size = clientListofRoom.size();

                            for(int i = 0; i < size; ++i){
                                System.out.println(clientListofRoom.get(i));
                                long threadId = clientList.get(clientListofRoom.get(i)-1).getId();
                                if(threadId != id && threadId != CHATROOM_INDEX_POS){
                                    ClientThread ct = clientList.get(clientListofRoom.get(i)-1);
                                    System.out.println("" + ct.getUsername());
                                    boolean isMessageSent = ct.sendMessage(message);
                                    if(!isMessageSent){
                                        clientList.remove(ct);
                                    } else {
                                        System.out.println("message sent!");
                                    }
                                }
                            }
                            // Adding message to log of chatroom
                            cr.logMessage(cm);
                        }
                        break;
                    // Logout from the server
                    case ChatMessage.LOGOUT:
                        running = false;
                        break;
                    case ChatMessage.OTHER_USERS:
                        try {
                            sOutput.writeObject(loggedUsers());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        }

        private boolean sendMessage(String message) {
            try {
                sOutput.writeObject(message);
                return true;
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
