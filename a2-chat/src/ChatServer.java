import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class ChatServer {
    // Init logger
    static Logger logger;
    
	// Server status: An oder Aus
	private boolean serverStatus;
		
	// Port über den die Connection läuft
	private int port;
	
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
			
			ClientThread ct = new ClientThread
		}
		
	}
	
	/**
	 * Die vorhandene Socketverbindung wird hier unterbrochen
	 */
	public void stop() {
		
	}
	
	private void logger(String msg) {
		logger.info(msg);
	}
}
