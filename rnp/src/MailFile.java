/**
 * Created by zujiry on 4/10/17.
 */
import sun.rmi.runtime.Log;

import java.util.Base64;
import javax.activation.FileDataSource;
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
        //if (args.length != 2) throw new IOException("Please enter two arguments, correct syntax is:\njava -cp . MailFile <recipient mail address> <path to attachment file>");

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
        String receiverEmail = "till.pohland@haw-hamburg.de";//args[2];
        // Email content
        String emailSubject = "";
        String emailText = "";
        // Attachment
        String fileName = "Anhang";
        String attachmentFilePath = "D:\\Git-Repos\\haw-rnp\\rnp\\src\\TextFile.docx";
        String senderConfigFilePath = "D:\\Git-Repos\\haw-rnp\\rnp\\src\\EmailData.txt";

        logger.info("Reading data file");

        try {
            // Read from specified file
            FileReader file = new FileReader(senderConfigFilePath);
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

        File attachmentFile = new File(attachmentFilePath);
        String senderUsernameB64 = Base64.getEncoder().encodeToString(senderUsername.getBytes());
        String senderPasswordB64 = Base64.getEncoder().encodeToString(senderPassword.getBytes());

        logger.info("Starting sending of smtp mail");

        SMTP mail = new SMTP(senderHost,logger);

        boolean mailCheck = (mail == null);
        logger.info("Is mail null?: " + mailCheck);

        if (mail != null){
            if (mail.send(senderEmail,receiverEmail,emailText, emailSubject, Integer.valueOf(senderPort),senderUsernameB64,senderPasswordB64,fileName,attachmentFile)){
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
        BufferedWriter pw;
        Logger logger;

        //Constructor
        public SMTP(String senderHost, Logger logger) throws UnknownHostException {
            mailHost = InetAddress.getByName(senderHost);
            localHost = InetAddress.getLocalHost();
            this.logger = logger;
            logger.info("Created SMTP Object");
        }

        //Sending of the Mail
        public boolean send(String sender, String receiver, String text, String subject, int senderPort, String senderUsername, String senderPassword, String fileName,File attachmentFile) throws IOException, InterruptedException {
            logger.info("Starting socket setup");
            InputStream inputStream;
            OutputStream outputStream;
            Socket socket;

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

            //Streams
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            br = new BufferedReader(new InputStreamReader(inputStream));
            pw = new BufferedWriter(new OutputStreamWriter(outputStream));
            String mimeBoundary = "DataSeperatorString";

            //Sending
            logger.info("Initial: " +  br.readLine());
            send("EHLO " + mailHost.getHostName(),pw);
            logger.info("\nEHLO " + br.readLine() + "\n" + br.readLine() + "\n" + br.readLine() +
                    "\n" + br.readLine() + "\n" + br.readLine() + "\n" + br.readLine() +
                    "\n" + br.readLine() + "\n" + br.readLine() + "\n" + br.readLine());

            send("AUTH LOGIN",pw);
            send(senderUsername,pw);
            send(senderPassword,pw);
            logger.info("AUTH " + br.readLine() + " " + br.readLine());
            logger.info("SUCCESS? " + br.readLine());

            send("MAIL From:<" + sender + ">",pw);
            logger.info("From " + br.readLine());

            send("RCPT TO:<" + receiver + ">",pw);
            logger.info("RCPT " + br.readLine());

            send("DATA",pw);
            logger.info("" + br.readLine());
            send("MIME-Version: 1.0",pw);
            send("Subject: " + subject,pw);
            send("Content-Type: multipart/mixed; boundary=\"" + mimeBoundary +"\"",pw);
            send("\r\n--" + mimeBoundary,pw);

            send("Content-Type: text/plain\r\n",pw);
            send(text,pw);
            send("\r\n\r\n",pw);
            send("\r\n--" + mimeBoundary,pw);

            // Send doc
            send("Content-Type: text/plain; name="+fileName,pw);
            send("Content-Disposition: attachment;filename=\""+fileName+"\"",pw);
            send("Content-Transfer-Encoding: base64",pw);
            byte[] bytes = loadFile(new File("D:\\Git-Repos\\haw-rnp\\rnp\\src\\File.txt"));
            String encoded = Base64.getEncoder().encodeToString(bytes);
            send(encoded,pw);

            // Send docx
            send("\r\n--" + mimeBoundary,pw);
            fileName = "docx";
            send("Content-Type: application/msword; name="+fileName,pw);
            send("Content-Disposition: attachment;filename=\""+fileName+"\"",pw);
            send("Content-Transfer-Encoding: base64",pw);
            byte[] attachment = loadFile(attachmentFile);
            String encodedAttach = Base64.getEncoder().encodeToString(attachment);
            send(encodedAttach,pw);
            send("\r\n\r\n--" + mimeBoundary + "--\r\n",pw);


            send("\r\n.\r\n.",pw);
            logger.info("QUEUED " + br.readLine());
            logger.info("" + br.readLine());
            send("QUIT",pw);
            logger.info("BYE " + br.readLine());

            logger.info("Leaving send and closing socket");

            socket.close();
            return true;
        }

        private void send(String string, BufferedWriter pw) throws IOException {
            pw.write(string + "\r\n");
            pw.flush();
        }

        private static byte[] loadFile(File file) throws IOException {
            InputStream is = new FileInputStream(file);

            long length = file.length();
            if (length > Integer.MAX_VALUE) {
                // File is too large
            }
            byte[] bytes = new byte[(int)length];

            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length
                    && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }

            if (offset < bytes.length) {
                throw new IOException("Could not completely read file "+file.getName());
            }

            is.close();
            return bytes;
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
