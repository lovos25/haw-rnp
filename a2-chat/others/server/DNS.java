package others.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class DNS {
	public static String[] getIP(String host) {
		FileReader in = null;
		BufferedReader inBuffer = null;
		String[] ip = new String[2];
		try {
			in = new FileReader(new File(host));
			inBuffer = new BufferedReader(in);
			ip = inBuffer.readLine().split(":");
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ip;
	}
}
