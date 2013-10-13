package test;

import java.io.IOException;
import java.net.UnknownHostException;

import clients.DocumentClient;
import clients.I_DocumentServer;
import clients.ServerProxy;
import documents.RevisionNumber;

import server.DocumentServer;

public class Main {

	public static void main(String[] args) {		
		try {
			@SuppressWarnings("unused")
			DocumentServer documentServer = new DocumentServer();
			DocumentClient documentClient = new DocumentClient("client1");
			
			
			documentClient.requestDocument("essaidoc1.txt");
			pause();
			documentClient.requestDocument("essaidoc1.txt");
			
			
//			documentClient.getServerProxy().lockDocument("essaidoc1.txt");
//			pause();
//			documentClient.getServerProxy().lockDocument("essaidoc2.txt");
//			pause();
//			documentClient.getCache().print();
//			documentClient.getCache().appendToDocument(0, new String("de nouvelles données").getBytes());
//			pause();
//			documentClient.getCache().print();
//			documentClient.unlockDocument(0);
//			pause();
//			documentServer.close();
//			System.exit(0);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void pause() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
