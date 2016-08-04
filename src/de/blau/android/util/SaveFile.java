package de.blau.android.util;

import java.io.Serializable;

import android.net.Uri;

public abstract class SaveFile implements Serializable {
	public abstract boolean save(Uri fileUri);
}
