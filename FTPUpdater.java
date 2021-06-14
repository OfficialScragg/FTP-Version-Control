//IMPORTS
import java.io.*;
import java.util.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

public class FTPUpdater{

	// ------ GLOBAL VARIABLES ------
    private static ServerSocket dataSocket;
    private static Socket FTPSocket;
    private static String LPORT_FORMAT = ",30,97"; // (firstNumber*256)+secondNumber = PORT
    private static int LPORT = 7777;
    private static int RPORT = 21;
    private static String REMOTE_IP_FORMAT = "127,0,0,1";
    private static String REMOTE_IP = "127.0.0.1";
    private static String LASTMOD = "original";
    private static String USERNAME = "<REDACTED>";
    private static String PASSWORD = "<REDACTED>";
    // ------------------------------

	public static void main(String[] args){
        //Run Server
        serverLoop();
    }

    // RUN TIMED CHECKS FOR MODIFIED FILE
    private static void serverLoop(){
    	try{
	    	while(true){
	    		checkForUpdate();
	    		TimeUnit.SECONDS.sleep(10);
	    	}
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }

    // CHECK IF FILE IS MODIFIED AND CALL UPDATE-FILE
    private static void checkForUpdate(){
    	try{
	    	String filename = "./index.html";
	    	File file = new File(filename);
	    	SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy-HH-mm-ss");
	    	
	    	if(LASTMOD.equals("original") || !LASTMOD.equals(sdf.format(file.lastModified()))){
	    		updateFile(LASTMOD);
	    		LASTMOD = sdf.format(file.lastModified());
	    	}

    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }

    // UPLOAD NEW FILE VIA FTP
    private static void updateFile(String time){
    	try{
    		// ------ GET FILE AND SOCKET ------
	    	String filename = "index.html";
	    	File file = new File(filename);
	    	FTPSocket = new Socket(REMOTE_IP, RPORT);
	    	FileInputStream fis = new FileInputStream(file);
			byte[] data = new byte[(int) file.length()];
			fis.read(data);
			fis.close();

	    	// ----------- SETUP I/O -----------
	    	BufferedReader in = new BufferedReader(new InputStreamReader(FTPSocket.getInputStream()));
	    	PrintWriter out = new PrintWriter(FTPSocket.getOutputStream(), true);

	    	// ----------- LOGIN TO SERVER AND ENTER DIRECTORY -----------
	    	out.println("USER "+USERNAME);
	    	in.readLine();
	    	out.println("PASS "+PASSWORD);
			in.readLine();
			in.readLine();
			out.println("PORT "+REMOTE_IP_FORMAT+LPORT_FORMAT);
			in.readLine();
			out.println("CWD /var/www/html");
			in.readLine();
			out.println("MKD old");
			in.readLine();
			out.println("RNFR index.html");
			in.readLine();
			out.println("RNTO old/index-"+LASTMOD+".html");
			in.readLine();

			// ------- CREATE DATA CONNECTION LISTENER -------
			dataSocket = new ServerSocket(LPORT);

			// ------------- SEND UPLOAD COMMAND -------------
			out.println("STOR "+filename);

			// ----------- ACCEPT DATA CONNECTION ------------
			Socket dataConnection = dataSocket.accept();
			in.readLine();

			// ----------- SEND FILE DATA TO SERVER ------------
			PrintWriter dataOut = new PrintWriter(dataConnection.getOutputStream(), true);
			dataOut.println(new String(data, "utf-8"));
			dataConnection.close();
			dataSocket.close();

    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
}