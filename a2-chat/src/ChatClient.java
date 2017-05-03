package src;

import java.io.*;
import java.net.Socket;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ChatClient {
    // Messages
    public static String LOGOUT = "LOGOUT";
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

    public ChatClient(){
    }

    public void start(){
        Scanner scan = new Scanner(System.in);
        while(true) {
            // read message from user
            String msg = scan.nextLine();

            // logout if message is LOGOUT
            if(msg.equalsIgnoreCase(LOGOUT)) {
                logout();
                break;
            }else if(msg.equalsIgnoreCase(IN_CHATROOM)){
                sendMessage(ChatServer.GENERAL_CHAT_ROOM,"which chatroom am I in?",ChatMessage.IN_CHATROOM);
            }else if(msg.equalsIgnoreCase(USERS_IN_CHATROOM)){
                sendMessage(ChatServer.GENERAL_CHAT_ROOM,"which chatroom am I in?",ChatMessage.USERS_IN_CHATROOM);
            } else if(msg.equalsIgnoreCase(LIST_USERS)){
                sendMessage(ChatServer.GENERAL_CHAT_ROOM, username + " requesting list of users",ChatMessage.OTHER_USERS);
            } else if(msg.equalsIgnoreCase(LIST_CHATROOMS)) {
                sendMessage(ChatServer.GENERAL_CHAT_ROOM, username + " requesting list of chatrooms",ChatMessage.LIST_CHATROOMS);
            } else if(msg.equalsIgnoreCase(USERS_IN_CHATROOM)) {
                sendMessage(ChatServer.GENERAL_CHAT_ROOM, username + " requesting list of users in current chatroom",ChatMessage.USERS_IN_CHATROOM);
            } else if(msg.equalsIgnoreCase(CREATE_CHATROOM)) {
                System.out.print("Write down the name for your new chatroom > ");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                String roomname = "Empty";
                try {
                    roomname = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Set roomname if message could be sent
                if (sendMessage(ChatServer.GENERAL_CHAT_ROOM,roomname,ChatMessage.CREATE_CHATROOM)) {
                    oldChatRoom = chatRoom;
                    chatRoom = roomname;
                }
            } else if(msg.equalsIgnoreCase(JOIN)){
                System.out.print("Which clubroom do you want to join? > ");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                String roomname = "";
                try {
                    roomname = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sendMessage(ChatServer.GENERAL_CHAT_ROOM, roomname, ChatMessage.CHATROOM);
                oldChatRoom = chatRoom;
                chatRoom = roomname;
                System.out.println(chatRoom);
                System.out.println(oldChatRoom);
            } else {
                sendMessage(chatRoom,msg,ChatMessage.MESSAGE);
                System.out.println(chatRoom);
            }
        }
        // done disconnect
        logout();
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

//        System.out.println("Connection accepted " + socket.getPort());
        //TODO: Serverlist add

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
            while(true) {
                System.out.print("Username > ");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                username = br.readLine();
                sendMessage(ChatServer.GENERAL_CHAT_ROOM,ChatServer.STANDARD_USER,ChatMessage.INITIALIZE);
                sendMessage(ChatServer.GENERAL_CHAT_ROOM,username,ChatMessage.INITIALIZE);
                // Check if username has been taken already
                String taken = (String) sInput.readObject();
                if (taken.equals(ChatServer.ERR_USERNAME)){
                    taken = (String) sInput.readObject();
                    System.out.println(taken);
                } else {
                    System.out.println("@Username saved and logged in");
                    break;
                }
            }
        } catch (IOException eIO) {
            logout();
            return false;
        } catch (ClassNotFoundException e) {
            logout();
            return false;
        }
        return true;
    }
    private void logout() {
        if(sInput != null && socket != null ) {
            sendMessage(ChatServer.GENERAL_CHAT_ROOM, "", ChatMessage.LOGOUT);
        }

        try {
            if(sInput != null)
                sInput.close();
        }
        catch(Exception e) {}
        try {
            if(sOutput != null)
                sOutput.close();
        } catch(Exception e) {}

        try{
            if(socket != null)
                socket.close();
        }
        catch(Exception e) {}
    }

    public boolean sendMessage(String chatRoom, String messageText, int type){
        ChatMessage cm = new ChatMessage(messageText,type,chatRoom);

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
                    if(realMsg){
                        System.out.println(msg);
                        //System.out.print(username + "|" + chatRoom + " > ");
                        realMsg = false;
                    } else if(error) {
                        //TODO: Correct output when joining a nonexistent chatroom
                        System.out.println(msg);
                        chatRoom = oldChatRoom;
                        System.out.println("Error " + chatRoom);
                        //System.out.print(username + "|" + chatRoom + " > ");
                        error = false;
                    }else if(serverCall) {
                        System.out.println(msg);
                        //System.out.print(username + "|" + chatRoom + " > ");
                        serverCall = false;
                    }else if(username_error) {
                        System.out.println(msg);
                        serverCall = false;
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
                        serverCall = true;
                        continue;
                    } else {
                        //System.out.print(username + "|" + chatRoom + " > ");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        public void setRunning(boolean running) {
            this.running = running;
        }
    }

}
