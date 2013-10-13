package clients;

import java.util.logging.Level;
import java.util.logging.Logger;

import nioEngine.I_NioEngine;
import documents.Document;

public class Cache {
	
	public static final int CAPACITY = 10; // max number of documents in the cache
	
	/**
	 * Cache representation of a document (bytes array)
	 */
	private class CachedDocument {
		String name;
		byte[] data;
		public CachedDocument(String name, byte[] data) {
			this.name = name;
			this.data = data;
		}
		public String getName() {
			return name;
		}
		public byte[] getData() {
			return data;
		}
		public void setData(byte[] data) {
			this.data = data;
		}
	}
	
	private CachedDocument[] documents;
	private int cachedDocumentsNumber;
	
	public int getCachedDocumentsNumber() {
		return cachedDocumentsNumber;
	}

	private static final Logger logger = Logger.getLogger(DocumentClient.class.getName());
	
	public Cache() {
		documents = new CachedDocument[CAPACITY];
		cachedDocumentsNumber = 0;
	}
	
	/**
	 * Add a document to the cache
	 * @param document a string containing the name and the content
	 */
	public void addCachedDocument(String document) {
		if (cachedDocumentsNumber == CAPACITY) {
			logger.log(Level.SEVERE, "the cache has reached is maximum capacity");
			// TODO implement FIFO replacement
			return;
		}
		String[] s = document.split(I_NioEngine.nameContentSeparator);
		CachedDocument doc = new CachedDocument(s[0], s[1].getBytes());
		documents[cachedDocumentsNumber++] = doc;
	}
	
	/**
	 * Return a physical document from a cached document (bytes)
	 * @param index
	 * @return
	 */
	public Document toDocument(int index) {
		if (index >=0 && index < cachedDocumentsNumber) {
			CachedDocument cachedDocument = documents[index];
			Document document = new Document(cachedDocument.getName(), cachedDocument.getName());
			document.setData(cachedDocument.getData());
			return document;
		}
		return null;
	}
	
	public void appendToDocument(int index, byte[] data) {
		if (index >=0 && index <= cachedDocumentsNumber) {
			CachedDocument cachedDocument = documents[index];
			int currentLength = cachedDocument.getData().length;
			byte[] newData = new byte[currentLength + data.length];
			System.arraycopy(cachedDocument.getData(), 0, newData, 0, currentLength);
			System.arraycopy(data, 0, newData, currentLength, data.length);
			cachedDocument.setData(newData);
		}
	}
	
	public void modifyDocument(String url, byte[] newContent) {
		getDocument(url).setData(newContent);
	}
	
	public boolean exists(String url) {
		for (int doc = 0; doc < cachedDocumentsNumber; doc++) {
			if (documents[doc].getName().equals(url)) {
				return true;
			}
		}
		return false;
	}
	
	public Document getDocumentFromName(String documentName) {
		for (int doc = 0; doc < cachedDocumentsNumber; doc++) {
			if (documents[doc].getName().equals(documentName)) {
				return toDocument(doc);
			}
		}
		return null;
	}
	
	public byte[] getDocumentContent(String url) {
		CachedDocument document = getDocument(url);
		if (document == null) {
			return null;
		}
		return document.getData();
	}
	
	public String documentsList() {
		String result = "";
		for (int docIdx = 0; docIdx < cachedDocumentsNumber; docIdx++) {
			result += documents[docIdx].getName() + "\n";
		}
		return result;
	}
	
	public String textContent() {
		String result = "";
		for (int docIdx = 0; docIdx < cachedDocumentsNumber; docIdx++) {
			result += "Document "+docIdx+": "+documents[docIdx].getName() + "\n";
			result += new String(documents[docIdx].getData()) + "\n";
			result += "-----------------------\n";
		}
		return result;
	}
	
	public void print() {
		System.out.println("--- cache content ---");
		for (int docIdx = 0; docIdx < cachedDocumentsNumber; docIdx++) {
			System.out.println("Document "+docIdx+": "+documents[docIdx].getName());
			System.out.println(new String(documents[docIdx].getData()));
			System.out.println("-----------------------");
		}
	}
	
	private CachedDocument getDocument(String url) {
		for (int doc = 0; doc < cachedDocumentsNumber; doc++) {
			if (documents[doc].getName().equals(url)) {
				return documents[doc];
			}
		}
		return null;
	}

}
