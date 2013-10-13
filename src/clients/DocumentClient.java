package clients;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import nioEngine.I_NioEngine;
import nioEngine.I_RecvMsgHandler;
import nioEngine.NioEngine;
import documents.Document;
import documents.RevisionNumber;

/**
 * This class contains the client methods to manage documents
 * TODO failures
 * TODO modify document
 */
public class DocumentClient implements I_RecvMsgHandler {

	private I_NioEngine nioEngine;
	private Thread threadEngine;
	private I_DocumentServer serverProxy;
	private Cache cache;
	private String name;
	private Set<String> documentsLocked; // set of documents for which this client owns a lock
	private ClientGUI clientGui;

	private static final Logger logger = Logger.getLogger(DocumentClient.class.getName());

	public DocumentClient(String clientName) {
		logger.log(Level.INFO, "DocumentClient creation");
		cache = new Cache();
		name = clientName;
		clientGui = new ClientGUI();
		clientGui.setHandler(this);
		documentsLocked = new HashSet<String>();
		nioEngine = new NioEngine();
		threadEngine = new Thread(nioEngine);
		try {
			nioEngine.InitializeAsClient(InetAddress.getByName("localhost"), 9090, this);
			threadEngine.start();
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {}
			serverProxy = new ServerProxy(nioEngine);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void receivedCB(byte[] data, SocketChannel socketChannel) {
		String stringData = new String(data);
		int code = Integer.valueOf(stringData.substring(0,1));
		String dataContent = stringData.substring(1);
		switch (code) {
		case I_NioEngine.DOWNLOAD:
			logger.log(Level.INFO, "client received (DOWNLOAD): " + dataContent);
			handleDownload(dataContent);
			break;
		case I_NioEngine.DOWNLOAD_LOCKED:
			logger.log(Level.INFO, "client received (DOWNLOAD_LOCKED): " + dataContent);
			handleDownloadLocked(dataContent);
			break;
		case I_NioEngine.LOCK:
			logger.log(Level.INFO, "client received (LOCK): " + dataContent);
			handleLock(dataContent);
			break;
		case I_NioEngine.ALREADY_LOCKED:
			logger.log(Level.INFO, "client received (ALREADY_LOCKED): " + dataContent);
			clientGui.displayMessage("The document "+dataContent+" is already locked."
					+ " It will be downloaded later.");
			break;
		case I_NioEngine.DOCUMENTS_LIST:
			logger.log(Level.INFO, "client received (DOCUMENTS_LIST): " + dataContent);
			clientGui.displayDocument(dataContent.replaceAll(I_NioEngine.documentsSeparator, "\n"));
			break;
		default:
			logger.log(Level.SEVERE, "client received unexpected message :"
					+ dataContent);
			break;
		}
		clientGui.updateInfo(cachedAndLockedDocuments());
	}
	
	/**
	 * Main method to get a document from its url
	 * Go through every cases (cached, locked...)
	 * @param url the document URL
	 */
	public void requestDocument (String url) {
		// the document is in the cache
		if (cache.exists(url)) {
			// asks the server the revision number and a lock
			serverProxy.lockDocument(url);
		}
		// the document is not in the cache
		else {
			// if the client owns the lock
			if (documentsLocked.contains(url) && !cache.exists(url)) {
				serverProxy.downloadLockedDocument(url);
			}			
			// the client doesn't have the document neither the lock
			else {
				serverProxy.downloadDocument(url);
			}
		}
	}
	
	/**
	 * Ask the server which documents are available
	 */
	public void requestDocumentsList() {
		serverProxy.requestDocumentsList();
	}
	
	/**
	 * Add the document to the cache and remember that
	 * the client owns the lock on it
	 * @param data
	 */
	private void handleDownload(String data){
		cache.addCachedDocument(data);
		documentsLocked.add(data.split(I_NioEngine.nameContentSeparator)[0]);
		clientGui.displayDocument(data.split(I_NioEngine.nameContentSeparator)[1]);
	}
	
	/**
	 * Retrieve a document that was locked before
	 * @param data
	 */
	private void handleDownloadLocked(String data){
		cache.addCachedDocument(data);
		clientGui.displayDocument(data.split(I_NioEngine.nameContentSeparator)[1]);
	}
	
	/**
	 * Get the lock on the document requested
	 * If the cached version is not up to date, then the document
	 * is downloaded again
	 * @param data format: <document_name><separator><revision_number>
	 */
	private void handleLock(String data){
		String documentName = data.split(I_NioEngine.nameContentSeparator)[0];
		String revisionNumber = data.split(I_NioEngine.nameContentSeparator)[1];
		Document document = cache.getDocumentFromName(documentName);
		documentsLocked.add(documentName);
		if (document == null){
			serverProxy.downloadDocument(documentName);
		}
		else if (document.getRevision().value() != RevisionNumber.decode(revisionNumber)) {
			serverProxy.downloadLockedDocument(documentName);
		}
	}
	
	/**
	 * Upload and unlock a document
	 * @param url
	 * @return true if the document has been uploaded, i.e. it is in cache
	 */
	public boolean unlockDocument(String url) {
		Document document = cache.getDocumentFromName(url);
		if (document != null && documentsLocked.contains(url)) {
			serverProxy.unlockDocument(document);
			documentsLocked.remove(document.getName());
			clientGui.updateInfo(cachedAndLockedDocuments());
			return true;
		}
		return false;
	}
	
	public I_DocumentServer getServerProxy() {
		return serverProxy;
	}
	
	public Cache getCache() {
		return cache;
	}
	
	public String getName() {
		return name;
	}
	
	/**
	 * List of documents that are cached and/or locked by the client
	 * @return
	 */
	public String cachedAndLockedDocuments() {
		String result = "Locked documents:\n";
		Iterator<String> docName = documentsLocked.iterator();
		while (docName.hasNext()) {
			result += docName.next() + "\n";
		}
		result += "\nCached documents:\n" + cache.documentsList();
		return result;
	}
	
	public void modifyDocument(String url, String newContent) {
		cache.modifyDocument(url, newContent.getBytes());
	}
	
	public static void main(String[] args) {
		@SuppressWarnings("unused")
		DocumentClient documentClient = new DocumentClient("Client");
	}

}
