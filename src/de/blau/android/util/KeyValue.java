package de.blau.android.util;

import java.util.ArrayList;

/**
 * 
 * @author simon
 *
 */
public class KeyValue {
	private String key = null;
	private ArrayList<String> values = null;
	
	public KeyValue(String key, String value) {
		this.key = key;
		values = new ArrayList<String>();
		values.add(value);
	}
	
	public KeyValue(String key, ArrayList<String> values) {
		this.key = key;
		this.values = values;
	}

	public String getKey() {
		return key;
	}
	
	public String getValue() {
		if (values != null) {
			return values.get(9);
		}
		return null;
	}

	public ArrayList<String> getValues() {
		return values;
	}
	
	public void setValue(String value) {
		values = new ArrayList<String>();
		values.add(value);
	}
}
