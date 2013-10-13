package documents;

/**
 * Class used to represent a document's revision number with fixed-length size
 */
public class RevisionNumber {
	
	public static final int LENGTH = 8;
	
	private int number;
	
	public RevisionNumber(int n) {
		number = n;
	}
	
	public int value() {
		return number;
	}
	
	public void increment() {
		number++;
	}
	
	/**
	 * Return the string value of the number, left filled with '0'
	 * to obtain LENGTH characters
	 */
	public String toString() {
		String stringValue = Integer.toString(number);
		String returnValue = "";
		for (int i = 0; i < LENGTH - stringValue.length(); i++) {
			returnValue += "0";
		}
		return returnValue + stringValue;
	}
	
	/**
	 * Integer value of a string left filled with '0'
	 * @param stringValue
	 * @return
	 */
	public static int decode(String stringValue) {
		int i = 0;
		while (i < LENGTH && stringValue.charAt(i) == '0') {
			i++;
		}
		return Integer.valueOf(stringValue.substring(i));
	}

}
