package clients;

import documents.Document;

/**
 * This interface defines the methods that can be called by a client.
 * Implementing classes methods send messages (download/upload/lock/unlock)
 * to the server.
 */
public interface I_DocumentServer {
	
	public Document lockDocument(String url);
	public void unlockDocument(Document document);
	public Document downloadDocument(String url);
	public Document downloadLockedDocument(String url);
	public Document createDocument(String url);
	public void disposeDocument(String url);
	public void requestDocumentsList();

}
