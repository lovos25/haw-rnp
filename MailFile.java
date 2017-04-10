package com.bai;

/**
 * Created by zujiry on 4/10/17.
 */
import sun.rmi.runtime.Log;

import java.util.Base64;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Created by zujiry on 4/3/17.
 */
public class MailFile {
    // Init logger
    static Logger logger;

    public static void main(String args[]) throws IOException, InterruptedException {
        //if (!(0 < args.length)) {
        //    throw new SyntaxException("Did not give any arguments");
        //}

        //Logging
        final boolean LOGGING = true;
        enableLogging(LOGGING);

        // Sender
        String senderUsername = "";
        String senderPassword = "";
        String senderEmail = "";
        String senderHost = "";
        String senderPort = "";
        // Receiver
        String receiverEmail = "pagan.metall@gmx.de";//args[2];
        // Email content
        String emailSubject = "";
        String emailText = "";
        // Attachment
        String fileName = "Anhang";
        String attachmentFilePath = "/home/zujiry/Documents/Anhang.jpg";
        String filePath = "/home/zujiry/Documents/Kontodaten";//args[3];

        logger.info("Reading data file");

        try {
            // Read from specified file
            FileReader file = new FileReader(filePath);
            BufferedReader reader = new BufferedReader(file);

            String data;
            String line = reader.readLine();
            String nextLine;

            logger.info("Entering While-Loop");

            while (line != null) {
                logger.info("Reads: " + line);
                if (line.startsWith("#")) {
                    data = line;
                    if (data.equals("#Text")) {
                        nextLine = reader.readLine();
                        while (!(nextLine == null)) {
                            emailText += nextLine;
                            nextLine = reader.readLine();
                        }
                    } else if (data.equals("#Sender")) {
                        senderEmail = reader.readLine();
                    } else if (data.equals("#User")) {
                        senderUsername = reader.readLine();
                    } else if (data.equals("#Password")) {
                        senderPassword = reader.readLine();
                    } else if (data.equals("#Host")) {
                        senderHost = reader.readLine();
                    } else if (data.equals("#Port")) {
                        senderPort = reader.readLine();
                    } else if (data.equals("#Subject")) {
                        emailSubject = reader.readLine();
                    } else logger.info("Missed something in document of sender data");
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Checking correctness of data from given file
        logger.info("Checking correctness of data");

        if (senderEmail.length() <= 0 || senderUsername.length() <= 0 || senderPassword.length() <= 0 || senderHost.length() <= 0 || senderPort.length() <= 0) {
            throw new IOException("Data given of the sender is incorrect, please validate in the userdata configuration");
        }

        String senderUsernameB64 = Base64.getEncoder().encodeToString(senderUsername.getBytes());
        String senderPasswordB64 = Base64.getEncoder().encodeToString(senderPassword.getBytes());

        logger.info("Starting sending of smtp mail");

        SMTP mail = new SMTP(senderHost,logger);

        boolean mailCheck = (mail == null);
        logger.info("Is mail null?: " + mailCheck);

        if (mail != null){
            if (mail.send(senderEmail,receiverEmail,emailText, emailSubject, Integer.valueOf(senderPort),senderUsernameB64,senderPasswordB64)){
                logger.info("Email sent");
            } else {
                logger.info("Email could not be sent");
            }
        }
    }

    //SMTP class to send data
    static class SMTP {
        InetAddress mailHost;
        InetAddress localHost;
        BufferedReader br;
        PrintWriter pw;
        Logger logger;

        public SMTP(String senderHost, Logger logger) throws UnknownHostException {
            mailHost = InetAddress.getByName(senderHost);
            localHost = InetAddress.getLocalHost();
            this.logger = logger;
            logger.info("Created SMTP Object");
        }

        public boolean send(String sender, String receiver, String subject, String text, int senderPort, String senderUsername, String senderPassword) throws IOException, InterruptedException {
            logger.info("Starting socket setup");
            InputStream inputStream;
            OutputStream outputStream;
            Socket socket;
            boolean ssl;

            if (senderPort == 587 || senderPort == 465) {
                logger.info("Establishing SSL/TLS connection");
                SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                socket = sslSocketFactory.createSocket(mailHost,senderPort);
            } else {
                logger.info("Establishing unencrypted connection");
                socket = new Socket(mailHost,senderPort);
            }

            logger.info("Checking Socket, connected? " + socket.isConnected());

            if (socket == null){
                logger.info("Socket could not be created correctly");
                return false;
            }

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
            //logger.info("RCPT " + br.readLine());

            pw.println("DATA");
            //logger.info("DATA " + br.readLine());
            pw.println("Subject " + subject);
            pw.print(text);
            //logger.info("Text " + br.readLine());

            pw.println("QUIT");


            logger.info("Leaving send and closing socket");

            socket.close();
            return true;
        }

    }

    private static void enableLogging(boolean enable) {
        if(enable) {
            logger = Logger.getLogger("EmailLog");
            FileHandler fh;

            try {
                // This block configure the logger with handler and formatter
                fh = new FileHandler("/home/zujiry/Documents/EmailLogFile.log");
                logger.addHandler(fh);
                SimpleFormatter formatter = new SimpleFormatter();
                fh.setFormatter(formatter);

            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
