package others;

import java.io.DataInputStream;
import java.io.IOException;

public class ListenFromServer extends Thread {
    public boolean running = true;
	private DataInputStream iStream;
    
    public ListenFromServer(DataInputStream iStream) {
    	this.iStream = iStream;
    }
    
    public void run() {
        while(running) {
            try {
                String msg = iStream.readLine();
                System.out.println(msg);
                System.out.print(" > ");
            } catch (IOException e) {
                running = false;
                
                System.out.println("@Socket Connection has been closed");
            }
        }
    }
}
