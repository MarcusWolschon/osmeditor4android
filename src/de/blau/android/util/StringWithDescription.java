package de.blau.android.util;

import java.io.Serializable;

public class StringWithDescription implements Comparable<StringWithDescription>, Serializable {
	private static final long serialVersionUID = 1L;
	
	private final String value;
	private String description;
	
	public StringWithDescription(final String value) {
		this.value = value;
	}
	
	public StringWithDescription(final String value, final String description) {
		this.value = value;
		this.description = description;
	}
	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return value + (description != null ? " - " + description:"");
	}

	@Override
	public int compareTo(StringWithDescription s) {
		return value.compareTo(s.getValue());
	}
	
	/**
	 * This is likely bad style
	 * @param s
	 * @return
	 */
	public boolean equals(String s) {
		return this.value.equals(s);
	}
	
	@Override
	public boolean equals(Object o) {
		return this == (StringWithDescription)o 
				|| (this.value.equals(((StringWithDescription)o).value) && ((this.description == null && ((StringWithDescription)o).description == null) || (this.description != null && this.description.equals(((StringWithDescription)o).description))));
	}
	
	@Override
	public int hashCode() {
		int result = 1;
		result = 37 * result + (value == null ? 0 : value.hashCode());
		result = 37 * result + (description == null ? 0 : description.hashCode());
		return result;
	}
}
