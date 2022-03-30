package MultithreadWebServer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

public class NewClientThread extends Thread {
	private String reqPath;
	private String reqConnection;
	private String rootDir;
	private String ipAddress;
	private String port;
	private String status;
	private BufferedReader br;
	private OutputStream os;
	private Socket sock;
	
	public NewClientThread(Socket sock) {
		this.sock = sock;
	}
	
	@Override
	public void run() {
		getServerConfig();
		
		
		try {
                	br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                	os = (sock.getOutputStream());
                	
                	String req = "";
                	System.out.println("nunggunih");
                	String line = br.readLine();
                	req += line + "\n";
                	
//                	if (line == null) continue;
                	
                	while (!line.isEmpty()) {
                		line = br.readLine();
                		req += line + "\n";
                	}
                	
//                	System.out.println(req);
                	parseReqPath(req);
                	parseHost(req);
                	parseKeepAlive(req);
                	if (reqConnection.contains("keep-alive")) {
                		System.out.println("Koneksi Tetap Hidup");
                		sock.setKeepAlive(true);
                	}

                	setStatus();
                	long contentLength = getFileLength();
                	String res = generateResponse(contentLength);
                	os.write(res.getBytes());
                	
                	if (contentLength > 0)
                		sentFile();
                	
//                	try {
//                		sock = server.accept();                		
//                	}
//                	catch (SocketTimeoutException e) {
//                		System.out.println("Waktu habis");
//                		break;
//                	}
                	System.out.println("koneksi baru");
//                }
                //close connection
//                System.out.println("koneksi habis");
					sock.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            
	}
	
	public void getServerConfig() {
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
            this.port = m.group(3);
        }
	}
	
	public String getFileMimeType(String filename) {
		
		FileInputStream fis;
		BufferedInputStream bis;
		String tmp = "";
		try {
			fis = new FileInputStream("ext2mime.txt");
			bis = new BufferedInputStream(fis);

			byte[] bRes = new byte[1024];
			int c = bis.read(bRes);
			
			while (c != -1) {
				tmp += (new String(bRes));
				c = bis.read(bRes);
			}
			
			bis.close();
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String ext = "";
		try {
			ext = filename.substring(filename.indexOf('.'));			
		}
		catch (StringIndexOutOfBoundsException e) {
			return "";
		}
		String pattern = ext + " ([^\n]+)";
		Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(tmp);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "";
	}
	
	public String generateDirectoryPage(File[] listDir) {
		//
		String fileContent = "";
		fileContent += "<!DOCTYPE html>\r\n" + 
				"<html>\r\n" + 
				"<head>\r\n" + 
				"<title>/" + this.reqPath + "</title>\r\n" + 
				"<style>\r\n" + 
				"        .tab1 {\r\n" + 
				"            tab-size: 2;\r\n" + 
				"        }\r\n" + 
				"  \r\n" + 
				"        .tab2 {\r\n" + 
				"            tab-size: 4;\r\n" + 
				"        }\r\n" + 
				"  \r\n" + 
				"        .tab4 {\r\n" + 
				"            tab-size: 8;\r\n" + 
				"        }\r\n" + 
				"    </style>" +
				"</head>\r\n" + 
				"<body>";
		for (File file : listDir) {
			fileContent += "<pre class='tab1'><a href='" ;
			if (!this.reqPath.isEmpty()) fileContent += "/";
			
			// Date Format
			DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			format.setTimeZone(TimeZone.getTimeZone("Australia/Sydney"));
	        String formatted = format.format(file.lastModified());
			
	        // File Size
	        long fileSize;
	        if (file.isFile()) {
	        	fileSize = file.length();
	        }
	        else {
	        	fileSize = FileUtils.sizeOfDirectory(file);
	        }
	        
			fileContent += this.reqPath + "/" + file.getName() + "'>" + 
					this.reqPath + "/" + file.getName() + "</a>		" + fileSize + " bytes		" +
					formatted + "</pre>";
//			System.out.println("Path: " + this.reqPath);
		}
		fileContent += "</body>\r\n" + 
				"</html>";
		return fileContent;
	}
	
	public File[] getListDir(File dir) {
		//
		File filesList[] = dir.listFiles();
		
		return filesList;
	}
	
	public String generateResponse(long contentLength) {
		//
		String fullPath = rootDir + this.reqPath;
		String mimeType = "";
		
		if (isFolder(fullPath) && (new File(fullPath + "\\index.html")).exists()) fullPath += "\\index.html";
		
		if (isFolder(fullPath) && !(new File(fullPath + "\\index.html")).exists()) {
			mimeType = "text/html";
		}
		else {
			Path path = Paths.get(fullPath);
			Path fileName = path.getFileName();
			mimeType = getFileMimeType(fileName.toString());			
			System.out.println(fileName.toString());
		}
		
//		if (mimeType.contains("image")) {
//			byte[] fileContent;
//			String content = "";
//			try {
//				fileContent = FileUtils.readFileToByteArray(new File(fullPath));
//				content = Base64.getEncoder().encodeToString(fileContent);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			
//			content = "<img src='data:" + mimeType + ";base64," + content + "'>";
//			contentLength = content.length();
//			mimeType = "image/webp";
//		}

		String response = "HTTP/1.0 " + status + "\r\n";
		response += "Content-Type: " + mimeType + "\r\n";
		response += "Content-Length: " + contentLength + "\r\n";
		response += "Connection: " + reqConnection + "\r\n";
		
		if (reqConnection.contains("keep-alive")) {
			response += "Keep-Alive: timeout=2; max=1000\r\n";
		}
		
		if (mimeType.contains("image")) {
			File f = new File(fullPath);
			response += "Content-Disposition: inline; " + f.getName() + "\r\n";
		}
		else if (!isFolder(fullPath) && !mimeType.contains("text")) {
			File f = new File(fullPath);
			response += "Content-Disposition: attachment; " + f.getName() + "\r\n";
		}
		
		response += "\r\n";
		System.out.println(response);
		return response;
	}
	
	public void sentFile() {
		//
		String fullPath = rootDir + this.reqPath;
		
		File f = new File(fullPath);
		// File found
		if (f.isFile()) {			
			FileInputStream fis;
			BufferedInputStream bis;
			
//			if (getFileMimeType(f.getName()).contains("image")) {
//				String byteImage = "data:" + getFileMimeType(f.getName()) + ";base64,";
//				
//				try {
//					byte[] fileContent = FileUtils.readFileToByteArray(new File(fullPath));
//					byteImage += Base64.getEncoder().encodeToString(fileContent);
////					System.out.println(fileContent.toString().length());
//					System.out.println(byteImage);
//					bw.write(byteImage);
//					bw.flush();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//				status = "200 OK";
//				return;
//			}
			
			try {
				fis = new FileInputStream(fullPath);
				
				bis = new BufferedInputStream(fis);
				byte[] bRes = new byte[1024];
				int c = bis.read(bRes);
				
				while (c != -1) {
//					os.write((bRes));
//					os.flush();
					List <Thread> threads = new ArrayList <Thread>();
				
					DownloadChunkThread myThread = new DownloadChunkThread(this.os, bRes);
					myThread.start();
					threads.add(myThread);
					
					for (Thread thread : threads) {
						try {
							thread.join();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
//					System.out.println(new String(bRes));
					c = bis.read(bRes);
				}
				
				bis.close();
				fis.close();
//				System.out.println(fileContent);
			} catch (FileNotFoundException e) {
				try {
					os.write((new String("File Not Found")).getBytes());
					os.flush();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("asfdagag");
		}
		else if (f.exists()) {
			// Check index.html
			File f2 = new File(rootDir + this.reqPath + "\\index.html");
			if (f2.exists()) {
				FileInputStream fis;
				BufferedInputStream bis;
				try {
					fullPath += "\\index.html";
//					System.out.println(fullPath);
					fis = new FileInputStream(fullPath);
					
					bis = new BufferedInputStream(fis);
					byte[] bRes = new byte[1024];
					int c = bis.read(bRes);
					
					while (c != -1) {
						os.write((bRes));
						os.flush();
						c = bis.read(bRes);
					}
					
					bis.close();
					fis.close();
				} catch (FileNotFoundException e) {
					try {
						os.write((new String("File Not Found")).getBytes());
						os.flush();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// No index.html
			else {
				try {
					os.write(generateDirectoryPage(getListDir(f)).getBytes());
					os.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		// Not Found
		else {
			try {
				os.write((new String("File Not Found")).getBytes());
				os.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
	}
	
	public String getReqHeader(String req) {
		//
		String header = req.split("\r\n\r\n")[0];
		return header;
	}
	
	public void parseReqPath(String reqHeader) {
		//
		String pattern = "GET ([^\\s]+)";
		Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(reqHeader);
        if (m.find()) {
            this.reqPath = m.group(1);
            this.reqPath = this.reqPath.substring(1);
        }
        if (this.reqPath.equals("\\")) {
        	this.reqPath = "";
        }

        try {
			this.reqPath = URLDecoder.decode(this.reqPath, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	
	public void parseHost(String reqHeader) {
		//
		String host = "";
		String pattern = "Host: ([^\n]+)";
		Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(reqHeader);
        if (m.find()) {
            host = m.group(1);
        }
        
        // Ambil config rootdir virtual host
        if (!host.equals("localhost")) {
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
        	
        	pattern = "VirtualHost: " + host + " ([^\n]+)";
        	r = Pattern.compile(pattern);
        	m = r.matcher(tmp);
        	if (m.find()) {
        		rootDir = m.group(1);
        		rootDir = rootDir.substring(0, rootDir.length()-1);
        	}
        }
	}
	
	public void parseKeepAlive(String reqHeader) {
		//
		String pattern = "Connection: ([^\\n]+)";
		Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(reqHeader);
        if (m.find()) {
            reqConnection = m.group(1);
        }
	}
	
	public long getFileLength() {
		String fullPath = rootDir + this.reqPath;
		Path path = Paths.get(fullPath);
		
		File f = new File(fullPath);
		if (isFolder(fullPath)) {
			return generateDirectoryPage(getListDir(f)).length();
		}
				
//		if (f.isFile() && getFileMimeType(f.getName()).contains("image")) {
//			String byteImage = "data:" + getFileMimeType(f.getName()) + ";base64,";
//			byte[] fileContent;
//			try {
//				fileContent = FileUtils.readFileToByteArray(new File(fullPath));
//				byteImage += Base64.getEncoder().encodeToString(fileContent);
//				return byteImage.length();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
		
		try {

            // size of a file (in bytes)
            long bytes = Files.size(path);
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
        }
		return 0;
	}
	
	public boolean isFolder(String path) {
		File f = new File(path);
		if (!f.isFile() && f.exists()) return true;
		return false;
	}
	
	public void setStatus() {
		String fullPath = rootDir + this.reqPath;
		File f = new File(fullPath);
		
		if (f.isFile()) {
			status = "200 OK";
		}
		else if (f.exists()) {
			status = "200 OK";
		}
		else {
			status = "404 Not Found";
		}
	}
}
