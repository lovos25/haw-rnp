package src;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.activation.FileDataSource;
import javax.net.ssl.SSLSocketFactory;

public class MailFileNew {
    static Logger logger;

    public MailFileNew() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        //if(args.length != 2) {
        //    throw new IOException("Please enter two arguments, correct syntax is:\njava -cp . mail.MailFile <recipient mail address> <path to attachment file>");
        //} else {
            // Logging
            enableLogging(true);

            // Variables
            // -Sender
            String user = "";
            String password = "";
            String sender = "";
            String host = "";
            String port = "";
            String receipient = "pagan.metall@gmx.de";//args[0];
            String subject = "";
            String text = "";
            final String senderDataFilePath = "D:\\Git-Repos\\haw-rnp\\a1_b\\src\\EmailData.txt";

            // -Attachment
            String attachmentName = "Anhang";
            String attachmentPath = "D:\\Git-Repos\\haw-rnp\\a1_b\\testfiles\\File.txt";

            logger.info("Reading data file");

            // Reading the data from the sender
            try {
                FileReader fr = new FileReader(senderDataFilePath);
                BufferedReader br = new BufferedReader(fr);
                String currentLine = br.readLine();
                logger.info("Entering While-Loop");

                for(; currentLine != null; currentLine = br.readLine()) {
                    logger.info("Reads: " + currentLine);
                    if(currentLine.startsWith("#")) {
                        if(currentLine.equals("#Text")) {
                            for(String textLine = br.readLine(); textLine != null; textLine = br.readLine()) {
                                text = text + textLine;
                            }
                        } else if(currentLine.equals("#Sender")) {
                            sender = br.readLine();
                        } else if(currentLine.equals("#User")) {
                            user = br.readLine();
                        } else if(currentLine.equals("#Password")) {
                            password = br.readLine();
                        } else if(currentLine.equals("#Host")) {
                            host = br.readLine();
                        } else if(currentLine.equals("#Port")) {
                            port = br.readLine();
                        } else if(currentLine.equals("#Subject")) {
                            subject = br.readLine();
                        } else {
                            logger.info("Missed something in document of sender data");
                        }
                    }
                }
            } catch (IOException var18) {
                var18.printStackTrace();
            }

            logger.info("Checking correctness of data");

            if(sender.length() > 0 && user.length() > 0 && password.length() > 0 && host.length() > 0 && port.length() > 0) {
                logger.info("Starting sending of smtp mail");
                MailFileNew.SMTP smtp = new MailFileNew.SMTP(host, logger);
                boolean mail = smtp == null;
                logger.info("Is mail null?: " + mail);
                if(smtp != null) {
                    //for(int i = 0; i < 50; i++) {
                        if (smtp.sendMail(sender, receipient, user, password, Integer.valueOf(port).intValue(), text, subject, attachmentName, attachmentPath)) {
                            logger.info("Email sent");
                        } else {
                            logger.info("Email could not be sent");
                        }
                    //}
                }

            } else {
                throw new IOException("Data given of the sender is incorrect, please validate in the userdata configuration");
            }
        //}
    }

    private static void enableLogging(boolean isLogged) {
        if(isLogged) {
            logger = Logger.getLogger("EmailLog");

            try {
                FileHandler fileHandler = new FileHandler("D:\\Git-Repos\\haw-rnp\\rnp\\src\\SMTPLog.log");
                logger.addHandler(fileHandler);
                SimpleFormatter sf = new SimpleFormatter();
                fileHandler.setFormatter(sf);
            } catch (SecurityException se) {
                se.printStackTrace();
            } catch (IOException io) {
                io.printStackTrace();
            }
        }

    }

    static class SMTP {
        InetAddress mailHost;
        InetAddress localHost;
        BufferedReader in;
        BufferedWriter out;
        Logger logger;

        // Seperator for mime parts
        String mimeSeperator = "MimeSeperator";
        String mimePartSeperator = "\r\n--" + mimeSeperator;

        public SMTP(String host, Logger logger) throws UnknownHostException {
            this.mailHost = InetAddress.getByName(host);
            this.localHost = InetAddress.getLocalHost();
            this.logger = logger;
            logger.info("Created SMTP Object");
        }

        public boolean sendMail(String sender, String receipent, String user, String password, int port, String text, String subject, String attachmentName, String attachmentPath) throws IOException, InterruptedException {
            this.logger.info("Starting socket setup");
            Socket socket;

            // Check for encryption
            if (port != 587 && port != 465) {
                this.logger.info("Establishing unencrypted connection");
                socket = new Socket(this.mailHost, port);
            } else {
                this.logger.info("Establishing SSL/TLS connection");
                SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                socket = sslSocketFactory.createSocket(this.mailHost, port);
            }

            // Check socket
            this.logger.info("Checking socket connection. Is connected? " + socket.isConnected());
            if (socket == null) {
                this.logger.info("Socket could not be created correctly");
                return false;
            } else {
                this.logger.info("Sending...");
                InputStream inStream = socket.getInputStream();
                OutputStream outStream = socket.getOutputStream();
                this.in = new BufferedReader(new InputStreamReader(inStream));
                this.out = new BufferedWriter(new OutputStreamWriter(outStream));

                // Enocde user and password
                String userB64 = Base64.getEncoder().encodeToString(user.getBytes());
                String passwordB64 = Base64.getEncoder().encodeToString(password.getBytes());

                // Send email header
                sendHeader(sender,receipent,userB64,passwordB64);

                // Send mime header
                sendMimeHeader(subject,sender,receipent);

                // Send mail text
                sendMailText(text);

                // Send attached file
                sendAttachment(attachmentName,attachmentPath);

                // End of mail
                send("\r\n--" + mimeSeperator + "--\r\n","\r\n.\r\n","QUIT");
                readOut("");
                this.logger.info("Leaving send and closing socket");
                socket.close();
                return true;
            }
        }

        private void sendHeader(String sender, String receipent, String user, String password) throws IOException {
            final String AUTH = "AUTH LOGIN";
            final String EHLO = "EHLO ";
            final String HELO = "HELO ";
            final String MAIL_FROM = "MAIL From:<" + sender + ">";
            final String RCPT_TO = "RCPT TO:<" + receipent + ">";
            final String DATA = "DATA";

            sendIn(EHLO + this.mailHost.getHostName());
            send(AUTH,user,password,MAIL_FROM,RCPT_TO,DATA);
            readOut("");
        }

        private void sendMimeHeader(String subject, String from, String to) throws IOException {
            final String MIME_V = "MIME-Version: 1.0";
            final String SUBJ = "Subject: " + subject;
            final String FROM = "From: " +  from;
            final String TO = "To: " +  to;
            final String MULTIPART = "Content-Type: multipart/mixed; boundary=\"" + mimeSeperator + "\"";

            send(MIME_V,FROM,TO,SUBJ,MULTIPART,mimePartSeperator);
            readOut("");
        }

        private void sendMailText(String text) throws IOException {
            final String TEXT_PLAIN = "Content-Type: text/plain\r\n";

            send(TEXT_PLAIN, text, mimePartSeperator);
            readOut("");
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

        private void sendIn(String... args) throws IOException {
            for (String arg : args) {
                this.out.write(arg + "\r\n");
                this.out.flush();
                readOut(arg);
            }
        }

        private void send(String... args) throws IOException {
            for (String arg : args) {
                out.write(arg + "\r\n");
                out.flush();
            }
        }

        private void readOut(String arg) throws IOException {
            String line = "";
            while (true) {
                if(line.contains("DSN") || line.contains("Authentication") || line.contains("Bye") || line.contains("<CR><LF>.<CR><LF>")
                        || line.contains("data") || line.contains("2.1.5")||line.contains("text")){
                    logger.info(line);
                    return;
                }
                line = in.readLine();
            }
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

        private static String getFileExtension(File file) {
            String fileName = file.getName();
            if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
                return fileName.substring(fileName.lastIndexOf(".")+1);
            else return "";
        }
    }
}
