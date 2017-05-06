package chat;

import java.util.ArrayList;
import java.util.List;

public class ChatRoom {
	// Chatroom name
	private int id;
		
	// Chatroom name
	private String name;
	
	private List<ChatMessage> Messages = new ArrayList<ChatMessage>(50);
	
	
	public ChatRoom(String name) {
		this.name = name;
	}

	public void logMessage(ChatMessage cm){
		Messages.add(cm);
	}

	public String getName() {
		return name;
	}

	public List<ChatMessage> getMessages() {
		return Messages;
	}

	public String toString(){
	    return name;
    }
}
