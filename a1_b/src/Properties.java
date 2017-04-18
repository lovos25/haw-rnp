import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Base64;
import java.util.logging.Logger;

class Properties {
	String filePath = "/Users/LovelyKaur/workspace/haw-rnp/a1_b/src/EmailData.txt";
	
    // Sender
    String senderUsername = "";
    String senderPassword = "";
    String senderEmail = "";
    String senderHost = "";
    String senderPort = "";
    
    // Email content
    String emailSubject = "";
    String emailText = "";
    
	public Properties(Logger logger) {
		
		this.readEmailData(logger);
	}

	@SuppressWarnings("resource")
	private void readEmailData(Logger logger) {
		try {
			// Read from specified file
			FileReader file = new FileReader(filePath);
			BufferedReader reader = new BufferedReader(file);
			String data;
			String line = reader.readLine();
			String nextLine;
		    String emailBody = "";

			logger.info("Entering While-Loop");

			while (line != null) {
				logger.info("Reads: " + line);
				if (line.startsWith("#")) {
					data = line;
					if (data.equals("#Text")) {
						nextLine = reader.readLine();
						while (!(nextLine == null)) {
							emailBody += nextLine;
							nextLine = reader.readLine();
						}
						this.setEmailText(emailBody);
					} else if (data.equals("#Sender")) {
						this.setSenderEmail(reader.readLine());
					} else if (data.equals("#User")) {
						this.setSenderUsername(reader.readLine());
					} else if (data.equals("#Password")) {
						this.setSenderPassword(reader.readLine());
					} else if (data.equals("#Host")) {
						this.setSenderHost(reader.readLine());
					} else if (data.equals("#Port")) {
						this.setSenderPort(reader.readLine());
					} else if (data.equals("#Subject")) {
						this.setEmailSubject(reader.readLine());
					} else
						logger.info("Missed something in document of sender data");
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public boolean propertiesValidate() {
		return !(getSenderEmail().length() <= 0 || 
				getSenderUsernameB64().length() <= 0 || 
						getSenderPasswordB64().length() <= 0 || 
        		getSenderHost().length() <= 0 || 
        		getSenderPort() <= 0
        	);
	}
	
	public String getSenderUsernameB64() {
		return Base64.getEncoder().encodeToString(senderUsername.getBytes());
	}

	public void setSenderUsername(String senderUsername) {
		this.senderUsername = senderUsername;
	}

	public String getSenderPasswordB64() {
		return Base64.getEncoder().encodeToString(senderPassword.getBytes());
	}

	public void setSenderPassword(String senderPassword) {
		this.senderPassword = senderPassword;
	}

	public String getSenderEmail() {
		return senderEmail;
	}

	public void setSenderEmail(String senderEmail) {
		this.senderEmail = senderEmail;
	}

	public String getSenderHost() {
		return senderHost;
	}

	public void setSenderHost(String senderHost) {
		this.senderHost = senderHost;
	}

	public Integer getSenderPort() {
		return Integer.valueOf(senderPort);
	}

	public void setSenderPort(String senderPort) {
		this.senderPort = senderPort;
	}

	public String getEmailSubject() {
		return emailSubject;
	}

	public void setEmailSubject(String emailSubject) {
		this.emailSubject = emailSubject;
	}

	public String getEmailText() {
		return emailText;
	}

	public void setEmailText(String emailText) {
		this.emailText = emailText;
	}
}
