package src.chat;

import java.io.Serializable;

public class ChatMessage implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// The different types of message sent by the Client
	private int type;
	
	// The plant text message
	private String message;

	public ChatMessage(String message, int type){
		this.message = message;
		this.type = type;
	}
	
	// getter
	public String getText() {
		return message;
	}

	public int getType() {
		return type;
	}
}
