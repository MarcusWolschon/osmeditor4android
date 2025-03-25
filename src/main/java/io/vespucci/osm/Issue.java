package io.vespucci.osm;

import java.io.Serializable;

import io.vespucci.util.TranslatedString;

public interface Issue extends Serializable, TranslatedString {

    /**
     * Check if this is an error or just a warning
     * 
     * @return true if an error
     */
    public boolean isError();
}
