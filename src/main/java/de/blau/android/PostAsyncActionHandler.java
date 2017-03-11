package de.blau.android;

import java.io.Serializable;

public abstract class PostAsyncActionHandler implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;

	/**
	 * call this on success
	 */
	public abstract void onSuccess();
	
	/**
	 * method for error handling 
	 */
	public abstract void onError();
}
