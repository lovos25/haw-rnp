package src.chat;


import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

//TODO: Correct output when joining a non existent chatroom

public class ChatClient {
    // Messages
    public final static String LOGOUT = "LOGOUT";
    public final static String LOGOUT_ROOM = "ROOMLOGOUT";
    public final static String LIST_CHATROOMS = "CHATROOMS";
    public final static String LIST_USERS = "ALLUSERS";
    public final static String CREATE_CHATROOM = "CREATE";
    public final static String JOIN = "JOIN";
    public final static String IN_CHATROOM = "CURCHAT";
    public final static String USERS_IN_CHATROOM = "CHATUSERS";
    public final static String HELP_SERVER = "HELPME";

    // for I/O
    private ObjectInputStream sInput;       // to read from the socket
    private ObjectOutputStream sOutput;     // to write on the socket
    private String username;
    private Socket socket;

    ArrayList<Integer> serverList;
    boolean runningClient = true;

    public ChatClient(){
    }

    public void start(){
        Scanner scan = new Scanner(System.in);
        while(runningClient) {
            System.out.print(" > ");
            // read message from user
            String msg = scan.nextLine();
            String roomname = "Empty";
            
            switch (msg.toUpperCase()) {

                case HELP_SERVER:
                    printHelp(false);
                    break;
	            case LOGOUT: // logout if message is LOGOUT
	            	logout();
	            	break;
	            case LOGOUT_ROOM: // logout if message is LOGOUT
	            	logoutRoom();
	            	break;
	            case IN_CHATROOM:
	            	sendMessage("which chatroom am I in?", ChatMessage.IN_CHATROOM);
	            	break;
	            case USERS_IN_CHATROOM:
	                sendMessage("which chatroom am I in?", ChatMessage.USERS_IN_CHATROOM);
	            	break;
	            case LIST_USERS:
                    sendMessage(username + " requesting list of users", ChatMessage.LIST_USERS);
	            	break;
	            case LIST_CHATROOMS:
	            	getChatrooms();
	            	break;
	            case CREATE_CHATROOM:
	            	System.out.print("Write down the name for your new chatroom > ");
	                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	                
	                try {
	                    roomname = br.readLine();
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	                sendMessage(roomname, ChatMessage.CREATE_CHATROOM);
	                break;
	            case JOIN:
	            	getChatrooms();
	            	System.out.print("Which chatroom do you want to join? > ");
	                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	                try {
	                    roomname = reader.readLine();
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
                    // TODO: Chatroom gets written here but is wrong if the chatroom does not exist!
	                sendMessage(roomname, ChatMessage.JOIN_CHATROOM);
	                break;
	            default:
	            	sendMessage(msg, ChatMessage.MESSAGE);
	            	break;
            }

        }
    }
    
    public void getChatrooms() {
    	sendMessage(username + " requesting list of chatrooms", ChatMessage.LIST_CHATROOMS);
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
            sendMessage(username, ChatMessage.INITIALIZE);
        
            System.out.println("@Username saved and logged in");
            printHelp(true);
        } catch (IOException eIO) {
            logout();
            return false;
        } 
        return true;
    }
    
    private void logout() {
        if(sOutput != null && socket != null ) {
            sendMessage("", ChatMessage.LOGOUT);
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

    private void printHelp(boolean initialization){
        String helpString = "\n ### Command help ###" +
                "\n (you can type in whatever case you want) " +
                "\nType: " + LOGOUT +" to logout of the server" +
                "\nType: " + CREATE_CHATROOM +" to create a chatroom on the server you are logged in to" +
                "\nType: " + JOIN +" to join a chatroom" +
                "\nType: " + LIST_CHATROOMS +" to list all available chatrooms" +
                "\nType: " + LIST_USERS +" to list all users on the server" +
                "\nType: " + USERS_IN_CHATROOM +" to list all users in the chatroom you are currently in" +
                "\nType: " + IN_CHATROOM +" to display the chatroom you are currently in" +
                "\nType: " + LOGOUT +" to join a chatroom" +
                "\nType: " + LOGOUT_ROOM +" to logout of the current chatroom and into the general chatroom" +
                "\nType: " + HELP_SERVER +" to display this help" +
                "\n ### End ###\n";

        System.out.println(helpString);
    }

    private void logoutRoom() {
    	boolean msg = sendMessage("", ChatMessage.CHATROOM_LOGOUT);
    	
    	if(msg) {
    		System.out.println("Logout war Erfolgreich");
    	} else {
    		System.out.println("Logout war nicht Erfolgreich");
    	}
    }
    
    public boolean sendMessage(String message, int type) {
        ChatMessage cm = new ChatMessage(message, type);

        try {
            sOutput.writeObject(cm);
            return true;
        } catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    class ListenFromServer extends Thread {
        public boolean running = true;

        public void run() {
            while(running) {
                try {
                    String msg = (String) sInput.readObject();
                    System.out.println(msg);
                    System.out.print(" > ");
                } catch (IOException e) {
                    running = false;
                    System.out.println("@Socket Connection has been closed");
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
