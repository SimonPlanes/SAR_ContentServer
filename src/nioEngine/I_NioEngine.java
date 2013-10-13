package nioEngine;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.util.Set;

public interface I_NioEngine extends Runnable {
	
	public static final int port = 9090;
	
	// communication codes
	public static final int LOCK			= 0; // to lock a document
	public static final int UNLOCK			= 1; // to unlock a document
	public static final int DOWNLOAD		= 2; // to download a document and get a lock on it 
	public static final int DOWNLOAD_LOCKED	= 3; // to download a document whend it's already locked
	public static final int UPLOAD			= 4; // to upload a document
	public static final int ALREADY_LOCKED	= 5; // to tell client the document is locked
	public static final int DOCUMENTS_LIST	= 6; // to communicate the documents list

	public static final String nameContentSeparator = "//";
	public static final String documentsSeparator = "//";
	
	/**
	 * Server-side NIO engine initialization
	 * @param the host address and port of the server
	 * @param handler for received messages, 
         * should implement @code IRecvMsgHandler interface
	 * @throws IOException 
	 */
	public void InitializeAsServer (
                InetAddress hostAddress, int port, I_RecvMsgHandler handler) 
			throws IOException;

	/**
	 * Client-side NIO engine initialization 
	 * Tries to connects to the server.
	 * @param the host address and port of the server
	 * @param handler for received messages, 
               should implement @code IRecvMsgHandler interface
	 * @throws IOException 
	 */
	public void InitializeAsClient (
                InetAddress hostAddress, int port, I_RecvMsgHandler handler) 
			throws IOException;

	
	/**
	 * NIO engine mainloop
	 * Manage message sending and receiving 
         * (see @code I_RecvMsgHandler interface),
	 * If the NioEngine runs as a server, 
         * also manage clients connections and deconnections.
	 */
	public void mainloop();


	/**
	 * Send data on the client channel (for client-side engine)
	 * @param the data that should be sent  
	 */
	public void send(byte[] data);
	
	
	/**
	 * Send data on the given channel (for server-side engine)
	 * @param the key of the channel on which data that should be sent
	 * @param the data that should be sent
	 */
	public void send(SocketChannel socketChannel, byte[] data);
	
	public void sendToAll(Set<SocketChannel> socketChannels, byte[] data);
	
	public boolean isInitialized();
	
	
	/**
	 * Close the client channel
	 * @param the channel to close
	 */
	public void terminate() ;
	
}
