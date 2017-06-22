package others.server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TCPChatroom {
	private String chatroomName = "";
	private TCPServer server = null;
	private Map<String, TCPChatroomClientOutput> clientList = new HashMap<String, TCPChatroomClientOutput>();
	private File protocol = null;

	public TCPChatroom(String chatroomName, TCPServer server) {
		this.server = server;
		this.chatroomName = chatroomName;
		this.protocol = new File("SERVER" + this.chatroomName);
		try {
			this.protocol.createNewFile();
		} catch (IOException e) {
		}
	}

	public synchronized void sendToAll(String message) {
		writeToProtocol(message);
		System.out.println("snd: " + message);
		for (TCPChatroomClientOutput client : this.clientList.values()) {
			try {
				client.writeToClient(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public boolean addClient(String id, Socket socket) {
		if (this.clientList.size() < 30) {
			TCPChatroomClientOutput clientOutput = new TCPChatroomClientOutput(socket);
			TCPChatroomClientInput clientInput = new TCPChatroomClientInput(socket, this, clientOutput, id);
			this.clientList.put(id, clientOutput);
			try {
				System.err.println(id + " connected to room");
				this.clientList.get(id).writeToClient("server: welcome to room " + this.chatroomName);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		}
		return false;
	}

	public boolean removeClient(String id) {
		if (this.clientList.size() > 0) {
			this.clientList.remove(id);
			System.err.println(id + " left the room");
			if (this.clientList.size() == 0) {
				this.server.removeChatroom(this.chatroomName);
			}
			return true;
		}
		return false;
	}

	public String getChatroomName() {
		return new String(this.chatroomName);
	}

	public String[] getAllRooms() {
		return server.getAllRooms();
	}

	public Set<String> getAllUsers() {
		return clientList.keySet();
	}

	private void writeToProtocol(String message) {
		try {
			FileWriter fw = new FileWriter(this.chatroomName, true);
			fw.write(message + "\n");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
