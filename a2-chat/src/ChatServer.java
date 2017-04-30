import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ChatServer {
	
	// Server status: An oder Aus
	private boolean serverStatus;

	// Server status: An oder Aus
	private List<ChatRoom> roomList = new ArrayList<ChatRoom>();

	// Server status: An oder Aus
	private List<ChatClient> clientList = new ArrayList<ChatClient>();
	
	// Port über den die Connection läuft
	private int port;

	// Init logger
    static Logger logger;
	
	public ChatServer(int port) {
		this.port = port;
	}
	
	/**
	 * Die Socketverbindung wir hier implimentiert
	 * @throws IOException 
	 */
	public void start() throws IOException {
		serverStatus = true;
		ServerSocket serverSocket = new ServerSocket(port);
		
		while(serverStatus) {
			logger("Server ist auf dem Port " + port + " gestartet");
			
			// Lauscht auf eine Verbindung
			Socket socket = serverSocket.accept();
			
			// Abbuch, wenn der Server gestoppt wird
			if(!serverStatus) break;
			
			//ClientThread ct = new ClientThread
		}
		
	}
	
	/**
	 * Die vorhandene Socketverbindung wird hier unterbrochen
	 */
	public void stop() {
		
	}
	
	public boolean sendMessage(ChatRoom c) {
		
		return true;
	}
	
	public boolean acceptClients(ChatClient c) {
		
		return true;
	}
	
	public List<ChatRoom> listRooms() {
		return null;
	}
	
	public boolean logoutClient() {
		
		return true;
	}
	
	public List<ChatClient> loggedUsers() {
		
		return null;
	}
	
	
	public String showLogs() {
		return null;
	}
	
	private void logger(String msg) {
		logger.info(msg);
	}
}
