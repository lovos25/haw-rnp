package others.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class TCPChatroomClientInput extends Thread {
	private TCPChatroom chatroom = null;
	private String usr = "";
	private TCPChatroomClientOutput clientOutput = null;
	private BufferedReader streamFromClient = null;
	private Socket socket = null;

	public TCPChatroomClientInput(Socket socket, TCPChatroom chatroom, TCPChatroomClientOutput clientOutput,
			String usr) {
		this.usr = usr;
		this.clientOutput = clientOutput;
		this.chatroom = chatroom;
		this.socket = socket;
		open();
		start();
	}

	@Override
	public void run() {
		String msgFromClient = "";
		do {
			try {
				msgFromClient = readFromClient();
				handleInput(msgFromClient);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} while (!isInterrupted() && !msgFromClient.split(": ")[1].equals("!bye"));
		close(msgFromClient.split(": ")[0]);
	}

	private void handleInput(String msgFromClient) throws IOException {
		String option = msgFromClient.split(": ")[1];
		switch (option) {
		case "!rooms": {
			getRooms();
			break;
		}
		case "!users": {
			getUsers();
			break;
		}
		case "!help": {
			clientOutput.writeToClient("server: !bye   - to leave");

			clientOutput.writeToClient("server: !rooms - available rooms");
			clientOutput.writeToClient("server: !users - list of users in this room");
			clientOutput.writeToClient("server: !log   - get the chat history of the room");
			clientOutput.writeToClient("server: !help  - this");
			break;
		}
		case "!log": {
			break;
		}
		case "!bye": {
			clientOutput.writeToClient("server: you are leaving room " + chatroom.getChatroomName());
			clientOutput.close();
			close(usr);
			break;
		}
		default: {
			chatroom.sendToAll(msgFromClient);
			break;
		}
		}
	}

	private String readFromClient() throws IOException {
		String msgFromClient = streamFromClient.readLine();
		System.out.println("rcv: " + msgFromClient);
		return msgFromClient;
	}

	private void getRooms() throws IOException {
		for (String chatroom : this.chatroom.getAllRooms()) {
			clientOutput.writeToClient("server: " + chatroom);
		}
	}

	private void getUsers() throws IOException {
		for (String user : chatroom.getAllUsers()) {
			clientOutput.writeToClient("server: " + user);
		}
	}

	private void open() {
		try {
			this.streamFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void close(String id) {
		try {
			chatroom.removeClient(id);
			this.streamFromClient.close();
			this.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
