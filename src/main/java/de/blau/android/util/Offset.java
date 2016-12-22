package de.blau.android.util;

import java.io.Serializable;

public class Offset implements Serializable { // offsets in WGS84 needed to align imagery
	private static final long serialVersionUID = 1L;
	public double lon = 0;
	public double lat = 0;
}
