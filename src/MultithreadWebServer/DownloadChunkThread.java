package MultithreadWebServer;

import java.io.IOException;
import java.io.OutputStream;

public class DownloadChunkThread extends Thread {
	private OutputStream os;
	private byte[] chunk;
	
	DownloadChunkThread(OutputStream os, byte[] chunk) {
		this.os = os;
		this.chunk = chunk;
	}
	
	@Override
	public void run() {
		try {
			os.write(chunk);
			os.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
