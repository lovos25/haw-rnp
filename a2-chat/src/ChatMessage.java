package src;

import java.io.Serializable;
import java.text.SimpleDateFormat;

public class ChatMessage implements Serializable{
	// Datum
	private SimpleDateFormat date;

	// Nachricht
	private int type;
	public static final int LOGOUT = 0, MESSAGE = 1, OTHER_USERS = 2, CHATROOM = 3, LIST_CHATROOMS = 4, CHATROOM_LOGOUT = 5,
			CREATE_CHATROOM = 6, ERROR = 7, IN_CHATROOM = 8,USERS_IN_CHATROOM = 9, ERR_USERNAME = 10, INITIALIZE = 11;
	private String text;
	private String chatRoomId;
	
	public ChatMessage(String text, int type, String chatRoomId) {
		this.text = text;
		this.type = type;
		this.chatRoomId = chatRoomId;
	}

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
