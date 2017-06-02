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

    // Alle Nachrichtentypen die der Server akzeptiert
    private static final int
            LOGOUT_CM = 0, 		// logout from room
            MESSAGE_CM = 1, 		// send message
            LIST_USERS_CM = 2, 	// list of users
            JOIN_CHATROOM_CM = 3, 	// join a chat room
            LIST_CHATROOMS_CM = 4, // list of avalible chatroms
            CHATROOM_LOGOUT_CM = 5,// logout from chatroom
            CREATE_CHATROOM_CM = 6,// create chatrrom
            ERROR_CM = 7, 			// fehler
            IN_CHATROOM_CM = 8,	// chatroom now
            USERS_IN_CHATROOM_CM = 9, 	// list of users in chatroom
            ERR_USERNAME_CM = 10, 		// hä
            INITIALIZE_CM = 11;		// first register

    // for I/O
    //private ObjectInputStream sInput;       // to read from the socket
    //private ObjectOutputStream sOutput;     // to write on the socket
    private OutputStream outputStream;  // to write on the socket
    private InputStream inputStream;    // to read from the socket
    private String username;
    private Socket socket;

    ArrayList<Integer> serverList;
    boolean runningClient = true;

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        client.login("localhost",50000);
    }

    public ChatClient(){
    }

    public void start(){
        Scanner scan = new Scanner(System.in);
        while(runningClient) {
            System.out.print(" > ");
            // read message from user
            String msg = scan.nextLine();
            String roomname = "Empty";
            if(!runningClient)break;
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
	            	sendMessage("which chatroom am I in?", IN_CHATROOM_CM);
	            	break;
	            case USERS_IN_CHATROOM:
	                sendMessage("which chatroom am I in?", USERS_IN_CHATROOM_CM);
	            	break;
	            case LIST_USERS:
                    sendMessage(username + " requesting list of users", LIST_USERS_CM);
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
	                sendMessage(roomname, CREATE_CHATROOM_CM);
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
	                sendMessage(roomname, JOIN_CHATROOM_CM);
	                break;
	            default:
	            	sendMessage(msg, MESSAGE_CM);
	            	break;
            }

        }
    }
    
    public void getChatrooms() {
    	sendMessage(username + " requesting list of chatrooms", LIST_CHATROOMS_CM);
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
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
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
            sendMessage(username, INITIALIZE_CM);
        
            System.out.println("@Username saved and logged in");
            printHelp(true);
        } catch (IOException eIO) {
            logout();
            return false;
        } 
        return true;
    }
    
    private void logout() {
        if(outputStream != null && socket != null ) {
            sendMessage("", LOGOUT_CM);
        }

        try {
            if(inputStream != null)
                inputStream.close();
        } catch(Exception e) {}
        
        try {
            if(outputStream != null)
                outputStream.close();
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
    	boolean msg = sendMessage("", CHATROOM_LOGOUT_CM);
    	
    	if(msg) {
    		System.out.println("Logout was successfull");
    	} else {
    		System.out.println("Logout was not successfull");
    	}
    }
    
    public boolean sendMessage(String message, int type) {
        ChatMessage cm = new ChatMessage(message, type);

        try {
            outputStream.write(type);
            outputStream.write(message.getBytes().length);
            outputStream.write(message.getBytes());
            return true;
        } catch(IOException e) {
            System.out.println("@Server closed the connection");
        }
        return false;
    }

    class ListenFromServer extends Thread {
        public boolean running = true;
        int length = 0;
        byte[] messageBytes;
        String message;
        public void run() {
            while(running) {
                try {
                    length = inputStream.read();
                    messageBytes = new byte[length];
                    inputStream.read(messageBytes);
                    message = new String(messageBytes);
                    System.out.println(message);
                    System.out.print(" > ");
                } catch (IOException e) {
                    running = false;
                    runningClient = false;
                    logout();
                    System.out.println("@Socket Connection has been closed");
                } catch (NegativeArraySizeException e) {
                    System.out.println("@Socket connection has been closed");
                }
            }
        }
    }

}
