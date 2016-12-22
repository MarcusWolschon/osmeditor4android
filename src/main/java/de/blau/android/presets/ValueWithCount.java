package de.blau.android.presets;

public class ValueWithCount implements Comparable<ValueWithCount> {
	private final String value;
	private String description = null;
	private int count = -1;
	private boolean descriptionOnly = false;
	
	
	public ValueWithCount(final String value) {
		this.value = value;
	}
	
	public ValueWithCount(final String value, final String description) {
		this.value = value;
		this.description = description;
	}
	
	public ValueWithCount(final String value, final String description, final boolean descriptionOnly) {
		this.value = value;
		this.description = description;
		this.descriptionOnly = descriptionOnly;
	}
	
	public ValueWithCount(final String value, final int count) {
		this.value = value;
		this.count = count;
	}

	
	@Override
	public String toString() {
		if (count == -1) {
			return descriptionOnly ?  (description != null ? description : value) : (description != null ? value + " - " + description : value);
		} else if (count >= 1) {
			return value + " (" + count + ")" + (description != null ? value + " - " + description : "");
		}
		return null;
	}

	public String getValue() {
		return value;
	}
	
	
	public String getDescription() {
		return description;
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
