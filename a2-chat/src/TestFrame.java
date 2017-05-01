package src;

/**
 * Created by Zujiry on 01/05/2017.
 */
public class TestFrame {
    private int serverPort;

    public static void main(String[] args) {
        ChatServer server;
        int port = 13000;
        String serverOrClient = "1";

        if(serverOrClient == "0"){
            server = new ChatServer(port);//Integer.parseInt(args[1]));
            System.out.println("Through server");
        } else {
            ChatClient client = new ChatClient();
            client.login("localhost",port);
        }
    }
}
