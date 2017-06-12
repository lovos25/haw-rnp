package others.server;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class TCPClientInput extends Thread {
	private BufferedReader streamFromServer = null;
	private Socket socket = null;
	private String fileOfSession = "";

	public TCPClientInput(Socket socket, String fileOfSession) {
		this.fileOfSession = fileOfSession;
		this.socket = socket;
		open();

		start();
	}

	@Override
	public void run() {
		while (!isInterrupted()) {
			try {
				writeToProtocol(readFromServer());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		close();
	}

	private String readFromServer() throws IOException {
		String msgFromServer = streamFromServer.readLine();
		System.out.println(msgFromServer);
		return msgFromServer;
	}

	private void writeToProtocol(String message) {
		if (!(message.length() >= "server".length() && "server: ".equals(message.substring(0, "server: ".length())))) {
			try {
				FileWriter fw = new FileWriter(this.fileOfSession, true);
				fw.write(message + "\n");
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void open() {
		try {
			this.streamFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void close() {
		try {
			this.streamFromServer.close();
			this.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
