import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.io.File;
import java.io.FileInputStream;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.logging.Logger;

import javax.activation.FileDataSource;
import javax.net.ssl.SSLSocketFactory;

//SMTP class to send data
class Smtp {
	InetAddress mailHost;
	InetAddress localHost;
	BufferedReader br;
	BufferedWriter bw;
	Logger logger;
	
    // Seperator for mime parts
    String mimeSeperator = "MimeSeperator";
    String mimePartSeperator = "\r\n--" + mimeSeperator;

	public Smtp(String senderHost, Logger logger) throws UnknownHostException {
		mailHost = InetAddress.getByName(senderHost);
		localHost = InetAddress.getLocalHost();
		this.logger = logger;
		logger.info("Created SMTP Object");
	}

	public boolean send(String sender, String receiver, String subject, String text, int senderPort,
			String senderUsername, String senderPassword, String attachmentName, String attachmentPath) throws IOException, InterruptedException {
		
		logger.info("Starting socket setup");
		InputStream inputStream;
		OutputStream outputStream;
		Socket socket;

		// Check for encryption
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
		this.br = new BufferedReader(new InputStreamReader(inputStream));
        this.bw = new BufferedWriter(new OutputStreamWriter(outputStream));

        // Send email header
        sendHeader(sender,receiver,senderUsername,senderPassword);

        // Send mime header
        sendMimeHeader(subject, sender, receiver);

        // Send mail text
        sendMailText(text);

        // Send attached file
        //sendAttachment(attachmentName,attachmentPath);

        // End of mail
        send("\r\n--" + mimeSeperator + "--\r\n","\r\n.\r\n","QUIT");
        readOut("");
        this.logger.info("Leaving send and closing socket");
        socket.close();
        return true;
	}
	
    private void sendAttachment(String attachmentName, String attachmentPath) throws IOException {
        // Getting the Mime-Type of the attachment
        File attachment = new File(attachmentPath);
        FileDataSource fds = new FileDataSource(new File(attachmentPath));
        logger.info("Content-Type is: " + fds.getContentType());
        String type = fds.getContentType();

        // Getting the file extension
        String attachmentExtension = "." + getFileExtension(attachment);

        // Sending attachment
        final String TYPE = "Content-Type: " + type + "; name=" + attachmentName + attachmentExtension;
        final String C_DISPO =  "Content-Disposition: attachment;filename=" + attachmentName + attachmentExtension;
        final String C_ENCODING = "Content-Transfer-Encoding: base64"; // Default base64
        String attachmentB64 = Base64.getMimeEncoder().encodeToString(loadFile(attachment));
        send(TYPE,C_DISPO,C_ENCODING);
        send("");
        send(attachmentB64);
    }
	
    private void sendMailText(String text) throws IOException {
        final String TEXT_PLAIN = "Content-Type: text/plain\r\n";
        send(TEXT_PLAIN);
        sendWithoutN(text);
        send(mimePartSeperator);
    }
	
    private void sendHeader(String sender, String receipent, String user, String password) throws IOException {
        final String AUTH = "AUTH LOGIN";
        final String EHLO = "EHLO ";
        final String HELO = "HELO ";
        final String MAIL_FROM = "MAIL From:<" + sender + ">";
        final String RCPT_TO = "RCPT TO:<" + receipent + ">";
        final String DATA = "DATA";

        sendIn(EHLO + this.mailHost.getHostName());
        System.out.println("headerset1");
        send(AUTH,user,password,MAIL_FROM,RCPT_TO,DATA);
        System.out.println("headerset2");
        readOut("");
        System.out.println("headerset3");
        readOut("");
    }
    
    private void sendIn(String... args) throws IOException {
        for (String arg : args) {
            this.bw.write(arg + "\r\n");
            this.bw.flush();
            readOut(arg);
        }
    }

    private void send(String... args) throws IOException {
        for (String arg : args) {
            bw.write(arg + "\r\n");
            bw.flush();
        }
    }
    
    private void sendWithoutN(String... args) throws IOException {
        for (String arg : args) {
            bw.write("\n" + arg + "\r\n");
            bw.flush();
        }
    }

	private void sendMimeHeader(String subject, String from, String to) throws IOException {
        final String MIME_V = "MIME-Version: 1.0";
        final String SUBJ = "Subject: " + subject;
        final String FROM = "From: " +  from;
        final String TO = "To: " +  to;
        final String MULTIPART = "Content-Type: multipart/mixed; boundary=\"" + mimeSeperator + "\"";
        
        send(MIME_V,FROM,TO,SUBJ,MULTIPART);
        sendWithoutN(mimePartSeperator);
        System.out.println("sendMimeHeader");
        readOut("");
    }
	
    private void readOut(String arg) throws IOException {
        String line = "";
        while (true) {
        	System.out.println(line);
            if(line.contains("DSN") || line.contains("Authentication") || line.contains("Bye") || line.contains("data") || line.contains("<CR><LF>.<CR><LF>")
                    || line.contains("2.1.5")||line.contains("text")){
                logger.info(line);
                return;
            }
            line = br.readLine();
        }
    }
    
    private static String getFileExtension(File file) {
        String fileName = file.getName();
        if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
            return fileName.substring(fileName.lastIndexOf(".")+1);
        else return "";
    }
    
    private static byte[] loadFile(File fileToLoad) throws IOException {
        long FILE_TOO_LONG = 2147483647L;
        FileInputStream var1 = new FileInputStream(fileToLoad);
        long fileLength = fileToLoad.length();
        if (fileLength > FILE_TOO_LONG) {;}

        byte[] byteChar = new byte[(int) fileLength];
        int var5 = 0;

        int var7;
        for (boolean var6 = false; var5 < byteChar.length && (var7 = var1.read(byteChar, var5, byteChar.length - var5)) >= 0; var5 += var7) {
            ;
        }

        if (var5 < byteChar.length) {
            throw new IOException("Could not completely read file " + fileToLoad.getName());
        } else {
            var1.close();
            return byteChar;
        }
    }


}