package de.blau.android.exception;

public class OsmIllegalOperationException extends OsmException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public OsmIllegalOperationException(String string) {
		super(string);
		
	}

	public OsmIllegalOperationException(OsmIllegalOperationException e) {
		super(e.getMessage());
	}

}
