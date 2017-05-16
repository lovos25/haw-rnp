package chat;


import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class ChatClient {
    // Messages
    public static String LOGOUT = "LOGOUT";
    public static String LOGOUT_ROOM = "LOGOUT_ROOM";
    public static String LIST_CHATROOMS = "CHATROOMS";
    public static String LIST_USERS = "USERS";
    public static String CREATE_CHATROOM = "CREATE";
    public static String JOIN = "JOIN";
    public static String IN_CHATROOM = "IN_CHATROOM";
    public static String USERS_IN_CHATROOM = "USERS_CHAT";
    // for I/O
    private ObjectInputStream sInput;       // to read from the socket
    private ObjectOutputStream sOutput;     // to write on the socket
    private String username;
    private Socket socket;

    ArrayList<Integer> serverList;
    String chatRoom = ChatServer.GENERAL_CHAT_ROOM;
    String oldChatRoom = ChatServer.GENERAL_CHAT_ROOM;
    boolean runningClient = true;

    public ChatClient(){
    }

    public void start(){
        Scanner scan = new Scanner(System.in);
        while(runningClient) {
            // read message from user
            String msg = scan.nextLine();
            String roomname = "Empty";
            
            switch (msg.toUpperCase()) {

                case "HELP_SERVER":
                    sendMessage("Help please", ChatServer.GENERAL_CHAT_ROOM, ChatMessage.HELP_SERVER);
                    break;
	            case "LOGOUT": // logout if message is LOGOUT
	            	logout();
	            	break;
	            case "LOGOUT_ROOM": // logout if message is LOGOUT
	            	logoutRoom();
	            	break;
	            case "IN_CHATROOM": 
	            	sendMessage("which chatroom am I in?", ChatServer.GENERAL_CHAT_ROOM, ChatMessage.IN_CHATROOM);
	            	break;
	            case "USERS_IN_CHATROOM": 
	                sendMessage("which chatroom am I in?", ChatServer.GENERAL_CHAT_ROOM, ChatMessage.USERS_IN_CHATROOM);
	            	break;
	            case "LIST_USERS":
                    sendMessage(username + " requesting list of users", ChatServer.GENERAL_CHAT_ROOM, ChatMessage.LIST_USERS);
	            	break;
	            case "LIST_CHATROOMS":
	            	getChatrooms();
	            	break;
	            case "CREATE_CHATROOM":
	            	System.out.print("Write down the name for your new chatroom > ");
	                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	                
	                try {
	                    roomname = br.readLine();
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	                // Set roomname if message could be sent
	                if (sendMessage(roomname, ChatServer.GENERAL_CHAT_ROOM, ChatMessage.CREATE_CHATROOM)) {
	                    oldChatRoom = chatRoom;
	                    chatRoom = roomname;
	                }
	                break;
	            case "JOIN":
	            	getChatrooms();
	            	System.out.print("Which chatroom do you want to join? > ");
	                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	                try {
	                    roomname = reader.readLine();
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	                sendMessage(roomname, ChatServer.GENERAL_CHAT_ROOM, ChatMessage.JOIN_CHATROOM);
	                oldChatRoom = chatRoom;
	                chatRoom = roomname;
	                break;
	            default:
	            	System.out.print(chatRoom.toString() + " > ");
	            	sendMessage(msg, chatRoom, ChatMessage.MESSAGE);
	            	break;
            }

        }
    }
    
    public void getChatrooms() {
    	sendMessage(username + " requesting list of chatrooms", ChatServer.GENERAL_CHAT_ROOM, ChatMessage.LIST_CHATROOMS);
    }

    public boolean login(String server, int serverPort){

        this.socket = null;
        // Creation of the socket to listen to the server
        try {
            socket = new Socket(server,serverPort);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        try {
            sInput  = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException eIO) {
            System.out.println("Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        // username decision
        username();

        new ListenFromServer().start();
        // success we inform the caller that it worked
        start();
        return true;
    }


    private boolean username(){
        try {
            System.out.print("Username eingeben > ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            username = br.readLine();
            sendMessage(username, ChatServer.GENERAL_CHAT_ROOM, ChatMessage.INITIALIZE);
            //sendMessage(ChatServer.STANDARD_USER, ChatServer.GENERAL_CHAT_ROOM, ChatMessage.INITIALIZE);

            System.out.println("@Username saved and logged in");
            /*while(true) {
                
                
                // Check if username has been taken already
                String taken = (String) sInput.readObject();
                System.out.println("Vom Server erhaltene: " + taken );
                if (taken.equals(ChatServer.ERR_USERNAME)){
                    taken = (String) sInput.readObject();
                    System.out.println(taken);
                    System.out.print("Username eingeben > ");
                    username = br.readLine();
                } else {
                    System.out.println("@Username saved and logged in");
                    break;
                }
            }*/
        } catch (IOException eIO) {
            logout();
            return false;
        } 
        return true;
    }
    
    private void logout() {
        if(sOutput != null && socket != null ) {
            sendMessage("", ChatServer.GENERAL_CHAT_ROOM, ChatMessage.LOGOUT);
        }

        try {
            if(sInput != null)
                sInput.close();
        } catch(Exception e) {}
        
        try {
            if(sOutput != null)
                sOutput.close();
        } catch(Exception e) {}

        try{
            if(socket != null)
                socket.close();
        } catch(Exception e) {}

        runningClient = false;
    }

    private void logoutRoom() {
    	boolean msg = sendMessage("", ChatServer.GENERAL_CHAT_ROOM, ChatMessage.CHATROOM_LOGOUT);
    	
    	if(msg) {
    		System.out.println("Logout war Erfolgreich");
    	} else {
    		System.out.println("Logout war nicht Erfolgreich");
    	}
    }
    
    public boolean sendMessage(String message, String room, int type) {
        ChatMessage cm = new ChatMessage(message, room, type);

        try {
            sOutput.writeObject(cm);
            return true;
        } catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    class ListenFromServer extends Thread {
        boolean realMsg = false;
        private boolean serverCall = false;
        private boolean error = false;
        public boolean running = true;
        private boolean username_error = false;
        
        public void run() {
            while(running) {
                try {
                    String msg = (String) sInput.readObject();
                    System.out.println("Erste Nachricht " + msg);
                    if(realMsg){
                        System.out.println(msg);
                        realMsg = false;
                    } else if(error) {
                        //TODO: Correct output when joining a nonexistent chatroom
                        System.out.println(msg);
                        chatRoom = oldChatRoom;
                        error = false;
                    }else if(serverCall) {
                        System.out.println(msg);
                        serverCall = false;
                    }else if(username_error) {
                        System.out.println(msg);
                        username_error = false;
                    } else if(msg.equals(ChatServer.ERR_USERNAME)) {
                        username_error = true;
                        continue;
                    } else if(msg.equals(ChatServer.MESSAGE_FROM_OTHER_CLIENT)) {
                        realMsg = true;
                        continue;
                    } else if (msg.equals(ChatServer.SERVER_CALL)){
                        serverCall = true;
                        continue;
                    } else if (msg.equals(ChatServer.ERROR)){
                        error = true;
                        continue;
                    } else if (msg.equals(ChatServer.CHATROOM_LOGOUT)){
                        error = true;
                        continue;
                    } else {
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
