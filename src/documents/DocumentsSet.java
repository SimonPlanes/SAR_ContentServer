package documents;

/**
 * Server documents set
 * Manages clients locks and queues on documents
 * TODO change owner after unlock
 */

import java.io.File;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import nioEngine.I_NioEngine;

public class DocumentsSet {
	
	private File directory;
	private Map<String, Document> documents;
	
	// for each document, the client who owns it (or null)
	private Map<Document, SocketChannel> lockOwners;
	// for each document, a FIFO list of clients who want it
	private Map<Document, Queue<SocketChannel>> lockRequests;
	
	public DocumentsSet(String currentDirectory) {
		documents = new HashMap<String, Document>();
		lockOwners = new HashMap<Document, SocketChannel>();
		lockRequests = new HashMap<Document, Queue<SocketChannel>>();
		directory = new File(currentDirectory);
		if (!directory.isDirectory()) {
			System.out.println("not a directory");
			System.exit(1);
		}
		File[] listFiles = directory.listFiles();
		for (File f : listFiles) {
			if (!f.isDirectory() && !f.getName().startsWith(".")) {
				Document doc = new Document(f.getName(), directory+"/"+f);
				documents.put(f.getName(), doc);
				lockOwners.put(doc, null);
				lockRequests.put(doc, new LinkedList<SocketChannel>());
			}
		}
	}
	
	public Document getDocument(String name) {
		return documents.get(name);
	}
	
	public void addDocument(Document document) {
		lockOwners.put(document, null);
	}
	
	public void setOwner(Document document, SocketChannel client) {
			lockOwners.put(document, client);
	}
	
	public SocketChannel getOwner(Document document) {
		return lockOwners.get(document);
	}
	
	public SocketChannel getOwner(String name) {
		return lockOwners.get(getDocument(name));
	}
	
	public void freeDocument(Document document) {
		setOwner(document, null);
	}
	
	public SocketChannel updateOwner(Document document) {
		LinkedList<SocketChannel> queue = (LinkedList<SocketChannel>) lockRequests.get(document);
		if (queue != null) {
			if (queue.isEmpty()) {
				freeDocument(document);
			} else {
				// there are waiting clients: retrieve and remove the first one
				SocketChannel socketChannel = queue.removeFirst();
				setOwner(document, socketChannel);
				return socketChannel;
			}
		} else {
			freeDocument(document);
		}
		return null;
	}
	
	public void addWaitingClient(Document document, SocketChannel client) {
		lockRequests.get(document).add(client);
	}
	
	public String getDocumentsList() {
		Set<String> docs = documents.keySet();
		String result = "";
		for (String doc : docs) {
			result += doc + I_NioEngine.documentsSeparator;
		}
		return result;
	}

}
