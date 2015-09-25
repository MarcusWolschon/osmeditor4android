package de.blau.android.util;

public class StringWithDescription implements Comparable<StringWithDescription> {
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
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof String) { // FIXME hack
			return this.value.equals((String)o);
		}
		return this.equals((StringWithDescription)o) 
				|| (this.value.equals(((StringWithDescription)o).value) && ((this.description == null && ((StringWithDescription)o).description == null) || (this.description != null && this.description.equals(((StringWithDescription)o).description))));
	}
}
