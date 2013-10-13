package nioEngine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;

import documents.Document;


public class NioEngine implements I_NioEngine {

	private static enum ReadState {READ_SIZE, READ_CONTENT};

	private I_RecvMsgHandler handler;

	private ServerSocketChannel	serverChannel;
	private SocketChannel 		clientChannel;
	private Selector selector;

	private ByteBuffer outBuffer;
	private ByteBuffer inBuffer;
	private ByteBuffer sizeBuffer;
	private ReadState readState;

	private boolean initialized;

	@Override
	public void InitializeAsServer(InetAddress hostAddress, int port,
			I_RecvMsgHandler handler) throws IOException {
		this.handler = handler;
		selector = SelectorProvider.provider().openSelector();

		// Create a new non-blocking server socket channel
		serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);
		serverChannel.socket().bind(new InetSocketAddress(hostAddress, port));

		// Be notified when connection requests arrive
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);

		readState = ReadState.READ_SIZE;
		initialized = true;
		System.out.println("Server initialized");
	}

	@Override
	public void InitializeAsClient(InetAddress hostAddress, int port,
			I_RecvMsgHandler handler) throws IOException {
		this.handler = handler;
		selector = SelectorProvider.provider().openSelector();

		clientChannel = SocketChannel.open();
		clientChannel.configureBlocking(false);
		clientChannel.socket().setTcpNoDelay(true);
		clientChannel.connect(new InetSocketAddress(hostAddress, port));

		clientChannel.register(selector, SelectionKey.OP_CONNECT);

		readState = ReadState.READ_SIZE;
		initialized = true;
		System.out.println("Client initialized");
	}

	@Override
	public void mainloop() {
		while (true) {
			try {
				// Wait for an event one of the registered channels
				selector.select();

				Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
				while (keyIterator.hasNext()) {

					SelectionKey key = (SelectionKey) keyIterator.next();
					keyIterator.remove();

					if (!key.isValid()) { continue; }

					// Handle the event					
					if (key.isConnectable()){
						handleConnect(key);
					}
					else if (key.isAcceptable()){
						handleAccept(key);
					}
					else if (key.isReadable()){
						handleRead(key);
					}
					else if (key.isWritable()){
						handleWrite(key);
					}

				}
			}
			catch (Exception e){
				System.out.println("Exception in NioEngine " + e);
			}
		}
	}

	// Basic Accept Handling
	private void handleAccept(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

		// Accept the connection and make it non-blocking
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		socketChannel.finishConnect();

		// Register the new SocketChannel with our Selector, indicating
		// we'd like to be notified when there's data waiting to be read
		socketChannel.register(selector, SelectionKey.OP_READ);
	}

	public void handleConnect(SelectionKey key) throws IOException{
		while (!clientChannel.finishConnect()) {
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {}
		}

		key.interestOps(SelectionKey.OP_READ);
		System.out.println("Connection...");
	}

	private void handleRead(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		// Attempt to read off the channel
		sizeBuffer = ByteBuffer.allocate(4);
		sizeBuffer.clear();
		int size = 0;
		int numBytesRead = 0;

		try {

			if (readState == ReadState.READ_SIZE)
			{
				numBytesRead = socketChannel.read(sizeBuffer);

				if (sizeBuffer.remaining() == 0)
				{
					sizeBuffer.rewind();
					size = sizeBuffer.getInt();
					inBuffer = ByteBuffer.allocate(size);
					readState = ReadState.READ_CONTENT;
				}

				if (numBytesRead == -1){
					System.out.println("An error occured while reading the message size\n");
					key.channel().close();
					key.cancel();
					return;
				}
			}

			if (readState == ReadState.READ_CONTENT)
			{
				inBuffer.clear();
				numBytesRead = socketChannel.read(inBuffer);

				if (inBuffer.remaining() == 0) {
					inBuffer.rewind();
					//Process the received data, we are now sure that everything has been read
//					System.out.println("message READ = "+ new String(inBuffer.array()));
					handler.receivedCB(inBuffer.array(), socketChannel);
					readState = ReadState.READ_SIZE;
				}
				if (numBytesRead == -1){
					System.out.println("An error occured while reading the message content\n");
					key.channel().close();
					key.cancel();
					return;
				}
			}

		}
		catch (IOException e) {
			// The remote forcibly closed the connection, cancel the selection key and close the channel.
			System.out.println("An error occured while reading the message (catch)\n"+e);
			key.cancel();
			socketChannel.close();
			return;
		}
	}

	private void handleWrite(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		//outBuffer contains the data to write
		try {
			outBuffer.clear();
			while (outBuffer.hasRemaining()) {
				socketChannel.write(outBuffer);
			}
//			System.out.println("message WRITTEN = " + new String(outBuffer.array()));
			key.interestOps(SelectionKey.OP_READ);
		} catch (IOException e) {
			// The channel has been closed
			key.cancel();
			socketChannel.close();
			return;
		}		
	}

	@Override
	public void send(byte[] data) {
		int length = data.length;
		outBuffer = ByteBuffer.allocate(length + 4);
		outBuffer.putInt(length);
		outBuffer.put(data);
		SelectionKey key = clientChannel.keyFor(selector);
		// Indicate we want to select OP_WRITE from now
		key.interestOps(SelectionKey.OP_WRITE);
		selector.wakeup();
	}

	@Override
	public void send(SocketChannel socketChannel, byte[] data) {
		// Put the data we want to send in outBuffer
		int length = data.length;
		outBuffer = ByteBuffer.allocate(length + 4);
		outBuffer.putInt(length);
		outBuffer.put(data);
		SelectionKey key = socketChannel.keyFor(selector);
		// Indicate we want to select OP_WRITE from now
		key.interestOps(SelectionKey.OP_WRITE);
	}

	public void sendToAll(Set<SocketChannel> socketChannels, byte[] data) {
		for (SocketChannel sc : socketChannels){
			send(sc, data);
		}
	}

	@Override
	public void terminate() {
		initialized = false;
		try {
			if (clientChannel != null) {
				clientChannel.keyFor(selector).cancel();
				clientChannel.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		mainloop();
	}

	public boolean isInitialized() {
		return initialized;
	}
	
	/**
	 * Make a message containing the code, the document name, a separator
	 * and the document content
	 * @param code
	 * @param doc
	 * @return
	 * @throws IOException
	 */
	public static byte[] wholeMessage(int code, Document doc) throws IOException {
		byte[] name = doc.getName().getBytes();
		byte[] data = doc.getData();
		int nameLength = name.length;
		int dataLength = data.length;
		byte[] dataToSend = new byte[nameLength + dataLength + 3];
		System.arraycopy(String.valueOf(code).getBytes(), 0, dataToSend, 0, 1);
		System.arraycopy(name, 0, dataToSend, 1, nameLength);
		System.arraycopy(I_NioEngine.nameContentSeparator.getBytes(), 0, dataToSend, 1+nameLength, 2);
		System.arraycopy(data, 0, dataToSend, 3+nameLength, data.length);
		return dataToSend;
	}

}