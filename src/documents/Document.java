package documents;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Document {

	private String name;
	private String url;
	private File file;
	private byte[] data;
	private RevisionNumber revision;
	
	public Document(String name, String url) {
		this.name = name;
		this.url = url;
		file = new File(url);
		revision = new RevisionNumber(1);
		try {
			setData(getBytesFromFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
	
	public RevisionNumber getRevision() {
		return revision;
	}
	 
	public byte[] getBytesFromFile() throws IOException {
		int length = (int) file.length();
		byte[] bytes = new byte[length];
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
		int result = in.read(bytes, 0, length);
		in.close();
		return bytes;
	}
	
	/**
	 * Write data from the byte array to the file
	 */
	public void update() {
		revision.increment();
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(url);
			fos.write(data);
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
