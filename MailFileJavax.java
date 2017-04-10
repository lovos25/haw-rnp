package com.bai;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Transport;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Created by zujiry on 4/3/17.
 */
public class MailFileJavax {
    // Init logger
    static Logger logger;

    public static void main(String args[]) throws MessagingException {
        //if (!(0 < args.length)) {
        //    throw new SyntaxException("Did not give any arguments");
        //}
        enableLogging();

        // Sender
        String senderUsername = "";
        String senderPassword = "";
        String senderEmail = "";
        String senderHost = "";
        String senderPort = "";
        // Receiver
        String receiverEmail = "amanpreet.kaur@haw-hamburg.de";//args[2];
        // Email content
        String emailSubject = "Test Email testtest... Test";
        String emailText = "Bitte sehen sie im Anhang nach was das hier soll";
        // Attachment
        String fileName = "Anhang";
        String attachmentFilePath = "/home/zujiry/Documents/Anhang.jpg";
        String filePath = "/home/zujiry/Documents/Kontodaten";//args[3];

        logger.info("Reading File");

        try {
            // Read from specified file
            FileReader file = new FileReader(filePath);
            BufferedReader reader = new BufferedReader(file);

            String data;
            String obj;
            String line = reader.readLine();

            logger.info("Entering While-Loop");
            while (line != null) {
                logger.info("Reads: " + line);
                if (line.startsWith("#")) {
                    data = line;
                    if (data.equals("#Sender")) {
                        senderEmail = reader.readLine();
                    } else if (data.equals("#User")) {
                        senderUsername = reader.readLine();
                    } else if (data.equals("#Password")) {
                        senderPassword = reader.readLine();
                    } else if (data.equals("#Host")) {
                        senderHost = reader.readLine();
                    } else if (data.equals("#Port")) {
                        senderPort = reader.readLine();
                    } else logger.info("Missed something in document of sender data");
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Checking correctness of data");

        if (senderEmail.length() <= 0 || senderUsername.length() <= 0 || senderPassword.length() <= 0 || senderHost.length() <= 0 || senderPort.length() <= 0) {
            throw new MessagingException("Data given of the sender is incorrect, please validate in the userdata configuration");
        }

        logger.info("Configuring Properties");

        Properties props = new Properties();

        if (senderPort.equals("587") || senderPort.equals("465")) {
            logger.info("Connecting via TLS/SSL");
            props.put("mail.smtp.host", senderHost);
            props.put("mail.smtp.socketFactory.port", senderPort);
            props.put("mail.smtp.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.port", senderPort);
        } else {
            logger.info("Connecting via TLS");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", senderHost);
            props.put("mail.smtp.port", senderPort);
        }
        String finalSenderUsername = senderUsername;
        String finalSenderPassword = senderPassword;

        logger.info("Authenticating");

        Session session = Session.getDefaultInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(finalSenderUsername, finalSenderPassword);
                    }
                });

            logger.info("Creating Message");
            // Create and configure message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(receiverEmail));
            message.setSubject(emailSubject);

            // For the attachement, we need two parts (Multipart)
            Multipart multipart = new MimeMultipart();

            // Create message text part
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(emailText);
            multipart.addBodyPart(messageBodyPart);

            logger.info("Adding attachement");
            // Create attachement part
            messageBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(attachmentFilePath);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(fileName);
            multipart.addBodyPart(messageBodyPart);

            // Build and send message
            message.setContent(multipart);
            message.saveChanges();

            logger.info("Sending message");
            Transport.send(message);

            logger.info("Boomchakalaka, send mail");
    }

    private static void enableLogging() {
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
