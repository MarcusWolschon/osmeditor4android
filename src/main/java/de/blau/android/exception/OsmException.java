package de.blau.android.exception;

import java.io.IOException;

public class OsmException extends IOException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9160300298635675666L;

	public OsmException(final String string) {
		super(string);
	}

    OsmException(final String string, final Throwable e) {
        super(string);
        initCause(e);
    }

}
