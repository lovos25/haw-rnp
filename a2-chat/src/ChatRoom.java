package src;

import java.util.ArrayList;
import java.util.List;

public class ChatRoom {
	// Chatroom name
	private int id;
		
	// Chatroom name
	private String name;
	
	private List<ChatMessage> Messages = new ArrayList<ChatMessage>();
	
	
	public ChatRoom(String name) {
		this.name = name;
	}

	public void logMessage(ChatMessage cm){
		Messages.add(cm);
	}

	public String getName() {
		return name;
	}

	public String toString(){
	    return name;
    }
}
