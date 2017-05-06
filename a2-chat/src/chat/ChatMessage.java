package chat;

import java.io.Serializable;
import java.text.SimpleDateFormat;

public class ChatMessage implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final int 
		LOGOUT = 0, 		// logout from room
		MESSAGE = 1, 		// send message
		LIST_USERS = 2, 	// list of users
		JOIN_CHATROOM = 3, 	// join a chat room
		LIST_CHATROOMS = 4, // list of avalible chatroms
		CHATROOM_LOGOUT = 5,// logout from chatroom
		CREATE_CHATROOM = 6,// create chatrrom
		ERROR = 7, 			// fehler
		IN_CHATROOM = 8,	// chatroom now
		USERS_IN_CHATROOM = 9, 	// list of users in chatroom
		ERR_USERNAME = 10, 		// hä
		INITIALIZE = 11, 		// hä
		HELP_SERVER = 12;		// help
	
	// The different types of message sent by the Client
	private int type;
	
	// The plant text message
	private String text;
	
	// Message belongs to room
	private String chatRoomId;
	
	public ChatMessage(String text, int type, String chatRoomId) {
		this.text = text;
		this.type = type;
		this.chatRoomId = chatRoomId;
	}
	
	// getter
	public String getText() {
		return text;
	}

	public int getType() {
		return type;
	}

	public String getChatRoomName() {
		return chatRoomId;
	}
}
