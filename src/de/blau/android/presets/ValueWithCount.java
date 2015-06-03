package de.blau.android.presets;

public class ValueWithCount implements Comparable<ValueWithCount> {
	private String value = null;
	private int count = -1;
	
	
	public ValueWithCount(String value) {
		this.value = value;
	}
	
	public ValueWithCount(String value, int count) {
		this.value = value;
		this.count = count;
	}
	
	@Override
	public String toString() {
		if (count == -1) {
			return value;
		} else if (count >= 1) {
			return value + " (" + count + ")";
		}
		return null;
	}

	public String getValue() {
		return value;
	}

	@Override
	public int compareTo(ValueWithCount arg0) {
		if (arg0.count < count) {
			return -1;
		} else if (arg0.count > count) {
			return +1;
		}
		return 0;
	}
}
