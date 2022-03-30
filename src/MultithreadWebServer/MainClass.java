package MultithreadWebServer;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainClass {
	
	private static String rootDir;
	private static String ipAddress;
	private static String port;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		getServerConfig();
		try {
			System.out.println(Integer.parseInt(port));
			ServerSocket server = new ServerSocket(Integer.parseInt(port));
			
			while (true) {
                server.setSoTimeout(0);
                Socket client = server.accept();
                server.setSoTimeout(5000);
                (new NewClientThread(client)).start();
			}
		}
		catch (IOException e) {
            e.printStackTrace();
        }
	}

	public static void getServerConfig() {
		//
		FileInputStream fis;
		BufferedInputStream bis;
		String tmp = "";
		try {
			fis = new FileInputStream("config.txt");
			bis = new BufferedInputStream(fis);
			
			byte[] c;
			c = new byte[bis.available()];
			bis.read(c);
			tmp = new String(c);
			
			bis.close();
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String pattern = "rootDir=([^\n]+)\nip=([^\n]+)\nport=([\\d]+)";
		Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(tmp);
        if (m.find()) {
            rootDir = m.group(1);
            rootDir = rootDir.substring(0, rootDir.length()-1);
            ipAddress = m.group(2);
            ipAddress = ipAddress.substring(0, ipAddress.length()-1);
            port = m.group(3);
        }
	}
}
