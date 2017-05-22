package src.chat;

/**
 * Created by Zujiry on 01/05/2017.
 */
public class TestFrame {

    public static void main(String[] args) {
        int port = 3000;
        String serverOrClient = "0";

        if(serverOrClient == "1"){
            new ChatServer(port);
            System.out.println("Server could not be created or has been shutdown");
        } else {
            ChatClient client = new ChatClient();
            client.login("localhost",port);
        }
    }
}
