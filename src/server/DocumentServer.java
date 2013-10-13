package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import nioEngine.I_NioEngine;
import nioEngine.I_RecvMsgHandler;
import nioEngine.NioEngine;
import documents.Document;
import documents.DocumentsSet;

/**
 * This class contains the server methods to manage documents
 * TODO failures
 */
public class DocumentServer implements I_RecvMsgHandler {
	
	private I_NioEngine nioEngine;
	private Thread threadEngine;
	private DocumentsSet documentsSet;
	
	private static final Logger logger = Logger.getLogger(DocumentServer.class.getName());
	
	public DocumentServer() throws UnknownHostException, IOException {
		logger.log(Level.INFO, "DocumentServer creation");
		documentsSet = new DocumentsSet(".");
		nioEngine = new NioEngine();
		nioEngine.InitializeAsServer(InetAddress.getByName("localhost"), I_NioEngine.port, this);
		threadEngine = new Thread(nioEngine);
		threadEngine.start();
	}

	@Override
	public void receivedCB(byte[] data, SocketChannel socketChannel) {
		String stringData = new String(data);
		int code = Integer.valueOf(stringData.substring(0,1));
		String dataContent = stringData.substring(1);
		switch (code) {
		case I_NioEngine.DOWNLOAD:
			downloadDocument(dataContent, socketChannel);
			break;
		case I_NioEngine.DOWNLOAD_LOCKED:
			downloadLockedDocument(dataContent, socketChannel);
			break;
		case I_NioEngine.LOCK:
			lockDocument(dataContent, socketChannel);
			break;
		case I_NioEngine.UPLOAD:
			uploadDocument(dataContent, socketChannel);
			break;
		case I_NioEngine.UNLOCK:
			unlockDocument(dataContent, socketChannel);
			break;
		case I_NioEngine.DOCUMENTS_LIST:
			sendDocumentsList(socketChannel);
		default:
			logger.log(Level.SEVERE, "server received unexpected message");
			break;
		}
	}
	
	/**
	 * Lock a document for a particular client, identified by a SocketChannel
	 * Send the document's revision number to the client
	 * @param document
	 * @param socketChannel
	 */
	private void lockDocument(Document document, SocketChannel socketChannel) {
		// if the document is not locked
		// send document's revision number and a lock
		if (documentsSet.getOwner(document) == null) {
			// format: LOCK<document_name><separator><revision_number>
			nioEngine.send(socketChannel, new String(
					I_NioEngine.LOCK
					+ document.getName()
					+ I_NioEngine.nameContentSeparator
					+ document.getRevision().toString()
					).getBytes());
			documentsSet.setOwner(document, socketChannel);
		}
		// if the document is locked by the particular client
		else if (documentsSet.getOwner(document) == socketChannel){
			nioEngine.send(socketChannel, new String(
					I_NioEngine.LOCK
					+ document.getName()
					+ I_NioEngine.nameContentSeparator
					+ document.getRevision().toString()
					).getBytes());
		}
		// if the document is locked by an other client, add the client to the waiters queue
		else {
			nioEngine.send(socketChannel, new String(I_NioEngine.ALREADY_LOCKED
					+ document.getName()).getBytes());
			documentsSet.addWaitingClient(document, socketChannel);
		}
	}

	/**
	 * Lock a document for a particular client, identified by a SocketChannel
	 * Send the document's revision number to the client
	 * @param url
	 * @param socketChannel
	 */
	private void lockDocument(String url, SocketChannel socketChannel) {
		logger.log(Level.INFO, "DocumentServer: lock "+url);
		Document document = documentsSet.getDocument(url);
		if (document != null) {
			lockDocument(document, socketChannel);
		}
	}

	/**
	 * Unlock a document for a particular client, identified by a SocketChannel
	 * If other clients are waiting for the lock, then the first of them gets it
	 * @param url
	 * @param socketChannel
	 */
	private void unlockDocument(String url, SocketChannel socketChannel) {
		logger.log(Level.INFO, "DocumentServer: unlock "+url);
		Document document = documentsSet.getDocument(url);
		if (document == null) {
			return;
		}
		if (documentsSet.getOwner(document) == socketChannel){
			// change document owner
			SocketChannel localSocketChannel = documentsSet.updateOwner(document);
			if (localSocketChannel != null) {
				lockDocument(document, localSocketChannel);
			}
		}
	}

	private void createDocument(String url) {
		logger.log(Level.INFO, "DocumentServer: create "+url);
		// TODO complete
	}

	private void disposeDocument(String url) {
		logger.log(Level.INFO, "DocumentServer: dispose "+url);
		// TODO complete
	}
	
	/**
	 * If the document is not locked, it is sent to the client and locked for it.
	 * Otherwise the client is added to the waiting queue and notified
	 * @param url
	 * @param socketChannel
	 */
	private void downloadDocument(String url, SocketChannel socketChannel) {
		logger.log(Level.INFO, "DocumentServer: download "+url);
		Document document = documentsSet.getDocument(url);
		if (document == null) {
			return;
		}
		if (documentsSet.getOwner(document) == null || documentsSet.getOwner(document) == socketChannel) {
			documentsSet.setOwner(document, socketChannel);
			try {
				nioEngine.send(socketChannel, NioEngine.wholeMessage(I_NioEngine.DOWNLOAD, document));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			nioEngine.send(socketChannel, new String(I_NioEngine.ALREADY_LOCKED
					+ document.getName()).getBytes());
			documentsSet.addWaitingClient(document, socketChannel);
		}
	}
	
	/**
	 * Send a document to a client who already has the lock on it
	 * @param url
	 * @param socketChannel
	 */
	private void downloadLockedDocument(String url, SocketChannel socketChannel) {
		logger.log(Level.INFO, "DocumentServer: download (locked) "+url);
		Document document = documentsSet.getDocument(url);
		if (document == null) {
			return;
		}
		if (documentsSet.getOwner(document).equals(socketChannel)) {
			try {
				nioEngine.send(socketChannel, NioEngine.wholeMessage(I_NioEngine.DOWNLOAD_LOCKED, document));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Get back a document and save physically the changes
	 * @param dataContent
	 * @param socketChannel
	 */
	private void uploadDocument(String dataContent, SocketChannel socketChannel) {
		logger.log(Level.INFO, "DocumentServer: upload "+dataContent);
		String[] strings = dataContent.split(I_NioEngine.nameContentSeparator);
		Document document = documentsSet.getDocument(strings[0]);
		if (document != null) {
			if (documentsSet.getOwner(document) == socketChannel) {
				document.setData(strings[1].getBytes());
				document.update(); // write changes on physical file
			}
		}
	}
	
	private void sendDocumentsList(SocketChannel socketChannel) {
		nioEngine.send(socketChannel, (String.valueOf(I_NioEngine.DOCUMENTS_LIST)
				+ documentsSet.getDocumentsList()).getBytes());
	}

	public void close() {
		logger.log(Level.INFO, "DocumentServer close");
		nioEngine.terminate();
	}
	
	
	public static void main(String[] args) {
		try {
			@SuppressWarnings("unused")
			DocumentServer documentServer = new DocumentServer();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
