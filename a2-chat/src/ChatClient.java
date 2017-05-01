package src;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class ChatClient {
    // for I/O
    private ObjectInputStream sInput;       // to read from the socket
    private ObjectOutputStream sOutput;     // to write on the socket
    private String username;
    private Socket socket;

    ArrayList<Integer> serverList;
    String chatRoom = ChatServer.GENERAL_CHAT_ROOM;

    public ChatClient(){

    }

    public void start(){
        Scanner scan = new Scanner(System.in);
        boolean room = false;
        while(true) {
            if(room){
                System.out.print(username + "|" + chatRoom + " > ");
            } else {
                System.out.print(username + " > ");
            }

            // read message from user
            String msg = scan.nextLine();

            // logout if message is LOGOUT
            if(msg.equalsIgnoreCase("LOGOUT")) {
                logout();

                break;
            } else if(msg.equalsIgnoreCase("CHATROOMS")) {
                sendMessage(ChatServer.GENERAL_CHAT_ROOM, "",ChatMessage.LIST_CHATROOMS);
            } else if(msg.equalsIgnoreCase("CREATE")) {
                System.out.print("Write down the name for your new chatroom > ");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                String roomname = "Empty";
                try {
                    roomname = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (sendMessage(ChatServer.GENERAL_CHAT_ROOM, roomname, ChatMessage.CREATE_CHATROOM)) {
                    room = true;
                    chatRoom = roomname;
                }
            } else if(msg.equalsIgnoreCase("JOIN")){
                System.out.print("Which clubroom do you want to join? > ");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                String roomname = "";
                try {
                    roomname = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (sendMessage(ChatServer.GENERAL_CHAT_ROOM, roomname, ChatMessage.CHATROOM)) {
                    room = true;
                    chatRoom = roomname;
                }
            } else {
                if(sendMessage(chatRoom,msg,ChatMessage.MESSAGE)){

                }
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

        // creates the Thread to listen from the server
        new ListenFromServer().start();

        try {
            System.out.print("Username > ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            username = br.readLine();
            sOutput.writeObject(username);
            System.out.println("@Username saved and logged in");
        } catch (IOException eIO) {
            logout();
            return false;
        }
        // success we inform the caller that it worked
        start();
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

    public void loginChatRoom(String chatRoomId){
        sendMessage(chatRoomId,"",ChatMessage.CHATROOM);
    }

    public void logoutChatRoom(String chatRoomId){
        sendMessage(chatRoomId,"",ChatMessage.CHATROOM_LOGOUT);
    }

    public void listChatRooms(){
        ChatMessage message = new ChatMessage("",ChatMessage.LIST_CHATROOMS,ChatServer.GENERAL_CHAT_ROOM);
        try {
            sOutput.writeObject(message);
            message = (ChatMessage) sInput.readObject();
            System.out.println(message.getText());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
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
        public boolean running = true;
        public void run() {
            while(running) {
                try {
                    String msg = (String) sInput.readObject();
                    // if console mode print the message and add back the prompt
                    System.out.println(msg);
                    System.out.print(username + "|" + chatRoom + " > ");
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
