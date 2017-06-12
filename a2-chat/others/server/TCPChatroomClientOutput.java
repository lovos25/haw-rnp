package others.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;

public class TCPChatroomClientOutput {
	private Socket socket = null;
	private DataOutputStream streamToClient = null;

	public TCPChatroomClientOutput(Socket socket) {
		this.socket = socket;
		open();
	}

	public void writeToClient(String msgToClient) throws IOException {
		streamToClient.write((msgToClient + '\r' + '\n').getBytes(Charset.forName("UTF-8")));
	}

	private void open() {
		try {
			this.streamToClient = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			this.streamToClient.close();
			this.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
