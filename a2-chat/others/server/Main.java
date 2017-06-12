package others.server;

public class Main {
	public static void main(String[] args) {
		if (args.length != 1 || (args[0].equals("-s") && args.length == 1)) {
			String[] ip = DNS.getIP(System.getProperty("user.dir") + "\\a2-chat\\others\\server\\server");
			int port = Integer.parseInt(ip[1]);
			new TCPServer(ip[0], port);
		} else if (args[0].equals("-c") && args.length == 4) {
			String[] ip = DNS.getIP(args[1]);
			int port = Integer.parseInt(ip[1]);
			new TCPClientOutput(ip[0], port, args[2], args[3]);
		} else {
			System.out.println("java Main <-c|-s>[ <hostname> <chatroom><username>]");
			return;
		}

	}
}
