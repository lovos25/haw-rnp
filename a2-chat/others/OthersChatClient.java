package others;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class OthersChatClient extends Thread {
	
	private Socket socket = null;
	boolean runningClient = true;
	
	private String username;
	private String chatroom;
	
    private DataInputStream sInput;       // to read from the socket
    private DataOutputStream sOutput;     // to write on the socket
	
    public OthersChatClient() {
    	String server = "141.22.27.104";
    	int port = 50000;
        
    	login(server, port);
    	runStream();

    }

	private void runStream() {
		try {
            sInput  = new DataInputStream(socket.getInputStream());
            sOutput = new DataOutputStream(socket.getOutputStream());
        } catch (IOException eIO) {
            System.out.println("Exception creating new Input/output Streams: " + eIO);
        }
		
		new ListenFromServer(sInput).start();
		
	}

	private void login(String server, int port) {
		try {
            socket = new Socket(server, port);
            start();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
		
	}

	public void start(){
        Scanner scan = new Scanner(System.in);
        while(runningClient) {
        	String msg = scan.nextLine();
        }
    }


}
