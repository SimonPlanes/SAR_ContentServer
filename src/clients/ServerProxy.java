package clients;

import java.io.IOException;

import nioEngine.I_NioEngine;
import nioEngine.NioEngine;
import documents.Document;

public class ServerProxy implements I_DocumentServer {
	
	private I_NioEngine nioEngine;
	
	public ServerProxy(I_NioEngine nioEngine) {
		this.nioEngine = nioEngine;
	}

	@Override
	/**
	 * Request a lock on a document
	 * Will then receive the lock and the document's revision number
	 */
	public Document lockDocument(String url) {
//		nioEngine.send(new String(I_NioEngine.DOWNLOAD + url).getBytes());
		nioEngine.send(new String(I_NioEngine.LOCK + url).getBytes());
		return null;
	}

	@Override
	/**
	 * Send an upload and an unlock message for a document
	 */
	public void unlockDocument(Document document) {
		try {
			nioEngine.send(NioEngine.wholeMessage(I_NioEngine.UPLOAD, document));
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			nioEngine.send((I_NioEngine.UNLOCK + document.getName()).getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Document createDocument(String url) {
		// TODO complete
		return null;
	}

	@Override
	public void disposeDocument(String url) {
		// TODO complete		
	}

	@Override
	/**
	 * Send a download message
	 */
	public Document downloadDocument(String url) {
		nioEngine.send(new String(I_NioEngine.DOWNLOAD + url).getBytes());
		return null;
	}

	@Override
	/**
	 * Send a download message for a document that is already locked by the client
	 */
	public Document downloadLockedDocument(String url) {
		nioEngine.send(new String(I_NioEngine.DOWNLOAD_LOCKED + url).getBytes());
		return null;
	}

	/**
	 * Ask the server the documents list
	 */
	public void requestDocumentsList() {
		nioEngine.send(String.valueOf(I_NioEngine.DOCUMENTS_LIST).getBytes());
	}

}
