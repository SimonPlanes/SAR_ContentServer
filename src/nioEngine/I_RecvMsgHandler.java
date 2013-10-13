package nioEngine;

import java.nio.channels.SocketChannel;

public interface I_RecvMsgHandler {
	
	// callback called when an entire message has been received
	public void receivedCB(byte[] data, SocketChannel socketChannel);

}