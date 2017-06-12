package others.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class TCPServer extends Thread {
	private Map<String, TCPChatroom> chatrooms = new HashMap<String, TCPChatroom>();
	private InetAddress address = null;
	private int port;
	private ServerSocket serverSocket = null;
	private Socket clientSocket = null;
	private BufferedReader inFromClient = null;

	public TCPServer(String ip, int port) {
		this.port = port;
		System.err.println("Binding on " + this.port);
		try {
			this.address = InetAddress.getByName(ip);
			this.serverSocket = new ServerSocket(port, 5, this.address);
			{
				this.chatrooms.put("eins", new TCPChatroom("eins", this));
				this.chatrooms.put("zwei", new TCPChatroom("zwei", this));
			}
			start();
		} catch (IOException e) {
			System.err.println("Server on Port:" + this.port + " couldn't be created");
		}
	}

	@Override
	public void run() {
		System.err.println(
				"Server on " + this.serverSocket.getInetAddress().getHostAddress() + ":" + this.port + " is running!");
		while (true) {
			try {
				String input = "";
				if (this.clientSocket == null || this.inFromClient == null || this.clientSocket.isClosed()) {
					this.clientSocket = this.serverSocket.accept();
					System.err.println("Client " + this.clientSocket.getInetAddress().getHostAddress() + ":" + this.port
							+ " is connected");
					open();
					input = readFromClient();
					String[] clientInfo = input.split(": ");
					if (clientInfo[0].equals("server")) {
						//TODO init output and send message back to the client
						System.out.println("invalid user name, failed to connect");
						this.clientSocket.close();
						continue;
					}
					sendToChatroom(clientInfo[0], clientInfo[1], this.clientSocket);
					this.clientSocket = null;
					this.inFromClient = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public String[] getAllRooms() {
		String[] allRoomsList = new String[chatrooms.size()];
		int x = 0;
		for (TCPChatroom chatroom : chatrooms.values()) {
			allRoomsList[x] = chatroom.getChatroomName();
			x++;
		}
		return allRoomsList;
	}

	public void removeChatroom(String roomname) {
		if (roomname.equals("eins") || roomname.equals("zwei")) {
			return;
		} else {
			chatrooms.remove(roomname);
		}
	}

	private String readFromClient() throws IOException {
		String request = inFromClient.readLine();
		System.out.println("[connect client to room]: " + request);
		return request;
	}

	private void sendToChatroom(String id, String roomname, Socket clientSocket) {
		//TODO dont create chatroom and send error message
		if (chatrooms.containsKey(roomname)) {
			chatrooms.get(roomname).addClient(id, clientSocket);
			return;
		} else {
			chatrooms.put(roomname, new TCPChatroom(roomname, this));
			chatrooms.get(roomname).addClient(id, clientSocket);
		}
	}

	private void open() {
		try {
			this.inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
