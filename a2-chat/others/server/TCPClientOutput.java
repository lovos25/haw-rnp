package others.server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;

public class TCPClientOutput extends Thread {
	private String usr = "";
	private String chatroom = "";
	private Socket socket = null;
	private String fileOfSession = "";
	private File protocol = null;
	private DataOutputStream streamToServer = null;
	private TCPClientInput clientInput = null;

	public TCPClientOutput(String ipAddr, int serverPort, String chatroom, String usr) {
		this.usr = usr;
		this.chatroom = chatroom;
		this.fileOfSession = "CLIENT[" + this.usr + "]" + this.chatroom + LocalDateTime.now();
		try {
			this.socket = new Socket(InetAddress.getByName(ipAddr), serverPort);
			open();
			this.protocol = new File(fileOfSession);
			this.protocol.createNewFile();
			start();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void run() {
		String input = "";
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		try {
			writeToServer(this.chatroom);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		do {
			try {
				input = reader.readLine();
				if (!input.equals("!log")) {
					writeToServer(input);
				} else {
					readProtocol();
				}
			} catch (IOException e) {
				e.printStackTrace();
				close();
				return;
			}
		} while (!input.equals("!bye"));
		close();
	}

	private void writeToServer(String msgToServer) throws IOException {
		streamToServer.write((usr + ": " + msgToServer + '\r' + '\n').getBytes(Charset.forName("UTF-8")));
	}

	private void open() {
		try {
			this.clientInput = new TCPClientInput(socket, this.fileOfSession);
			this.streamToServer = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readProtocol() {
		FileReader in = null;
		BufferedReader inBuffer = null;
		try {
			in = new FileReader(protocol);
			inBuffer = new BufferedReader(in);
			String line = inBuffer.readLine();
			while (line != null) {
				System.out.println("log: " + line);
				line = inBuffer.readLine();
			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void close() {
		try {
			this.clientInput.interrupt();
			this.clientInput.join();
			this.streamToServer.close();
			this.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
