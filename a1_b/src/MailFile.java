/**
 * Created by zujiry on 4/10/17.
 */
import java.util.Base64;
import java.io.*;
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

        // Receiver
        String receiverEmail = "";//args[2];
       
        // Attachment
        String fileName = "Anhang";
        
        Properties properties = new Properties(logger);
        String attachmentFilePath = "/home/zujiry/Documents/Anhang.jpg";
        
        logger.info("Reading data file");

        //Checking correctness of data from given file
        logger.info("Checking correctness of data");

        if (!properties.propertiesValidate()) {
            throw new IOException("Data given of the sender is incorrect, please validate in the userdata configuration");
        }

        logger.info("Starting sending of smtp mail");
        
        
        SMTP mail = new SMTP(properties.getSenderHost(), logger);

        boolean mailCheck = (mail == null);
        logger.info("Is mail null?: " + mailCheck);

        if (mail != null){
            if (mail.send(
            		properties.getSenderEmail(),
            		receiverEmail,
            		properties.getEmailText(), 
            		properties.getEmailSubject(), 
            		properties.getSenderPort(),
            		properties.getSenderUsernameB64(),
            		properties.getSenderPasswordB64())
            	){
                logger.info("Email sent");
            } else {
                logger.info("Email could not be sent");
            }
        }
    }

    private static void enableLogging(boolean enable) {
        if(enable) {
            logger = Logger.getLogger("EmailLog");
            FileHandler fh;

            try {
                // This block configure the logger with handler and formatter
                fh = new FileHandler("/Users/LovelyKaur/workspace/haw-rnp/a1_b/testfiles/EmailLogFile.log");
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
