package de.blau.android.presets;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ValueWithCount implements Comparable<ValueWithCount> {
	private final String value;
	private String description = null;
	private int count = -1;
	private boolean descriptionOnly = false;
	
	
	public ValueWithCount(@NonNull final String value) {
		this.value = value;
	}
	
	public ValueWithCount(@NonNull final String value, final String description) {
		this.value = value;
		this.description = description;
	}
	
	public ValueWithCount(@NonNull final String value, final String description, final boolean descriptionOnly) {
		this.value = value;
		this.description = description;
		this.descriptionOnly = descriptionOnly;
	}
	
	public ValueWithCount(@NonNull final String value, final int count) {
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
		return null; //NOSONAR this breaks the implicit contract for toString, however this class is specifically for use in ArrayAdapters
	}

	/**
	 * 
	 * @return the value
	 */
	@NonNull
	public String getValue() {
		return value;
	}
	
	/**
	 * 
	 * @return the description or null if none
	 */
	@Nullable
	public String getDescription() {
		return description;
	}

	@Override
	public int compareTo(@NonNull ValueWithCount other) {
	    int result = this.value.compareTo(other.value);
	    if (result == 0) {
	        if (other.count < count) {
			    return -1;
		    } else if (other.count > count) {
			    return +1;
		    }
		    return 0;
	    } 
	    return result;
	}
	
	@Override
    public boolean equals(Object obj) {
	    if (obj == null || !(obj instanceof ValueWithCount)) {
	        return false;
	    }
	    return value.equals(((ValueWithCount)obj).value) && count==((ValueWithCount)obj).count;
	}
}
