package de.blau.android.util;

/**
 * 
 * @author simon
 *
 */
public class KeyValue {
	String key = null;
	String value = null;
	
	public KeyValue(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
