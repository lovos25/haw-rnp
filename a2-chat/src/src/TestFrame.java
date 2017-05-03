package src;

/**
 * Created by Zujiry on 01/05/2017.
 */
public class TestFrame {

    public static void main(String[] args) {
        ChatServer server;
        int port = 13000;
        String serverOrClient = "0";

        if(serverOrClient == "1"){
            server = new ChatServer(port);//Integer.parseInt(args[1]));
            System.out.println("Server could not be created or has been shutdown");
        } else {
            ChatClient client = new ChatClient();
            client.login("",port);
        }
    }
}
