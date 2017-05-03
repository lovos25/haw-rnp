package src;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocketFactory;

//SMTP class to send data
class Smtp {
	InetAddress mailHost;
	InetAddress localHost;
	BufferedReader br;
	PrintWriter pw;
	Logger logger;

	public Smtp(String senderHost, Logger logger) throws UnknownHostException {
		mailHost = InetAddress.getByName(senderHost);
		localHost = InetAddress.getLocalHost();
		this.logger = logger;
		logger.info("Created SMTP Object");
	}

	public boolean send(String sender, String receiver, String subject, String text, int senderPort,
			String senderUsername, String senderPassword) throws IOException, InterruptedException {
		logger.info("Starting socket setup");
		InputStream inputStream;
		OutputStream outputStream;
		Socket socket;

		if (senderPort == 587 || senderPort == 465) {
			logger.info("Establishing SSL/TLS connection");
			SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			socket = sslSocketFactory.createSocket(mailHost, senderPort);
		} else {
			logger.info("Establishing unencrypted connection");
			socket = new Socket(mailHost, senderPort);
		}

		logger.info("Checking Socket, connected? " + socket.isConnected());

		logger.info("Sending...");

		inputStream = socket.getInputStream();
		outputStream = socket.getOutputStream();
		br = new BufferedReader(new InputStreamReader(inputStream));
		pw = new PrintWriter(new OutputStreamWriter(outputStream));

		pw.println("EHLO " + mailHost);
		logger.info("EHLO " + br.readLine());

		pw.println("AUTH PLAIN");
		pw.println(senderUsername);
		pw.println(senderPassword);
		logger.info(br.readLine());

		pw.println("MAIL From:<" + sender + ">");
		logger.info("From " + br.readLine());

		pw.println("RCPT TO:<" + receiver + ">");
		// logger.info("RCPT " + br.readLine());

		pw.println("DATA");
		// logger.info("DATA " + br.readLine());
		pw.println("Subject " + subject);
		pw.print(text);
		// logger.info("Text " + br.readLine());

		pw.println("QUIT");

		logger.info("Leaving send and closing socket");

		socket.close();
		return true;
	}

}