package de.blau.android;

import java.io.Serializable;

public abstract class PostAsyncActionHandler implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public abstract void execute();
}
