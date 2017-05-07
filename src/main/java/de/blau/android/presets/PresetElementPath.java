package de.blau.android.presets;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Container for the path to a specific PresetElement
 * @author simon
 *
 */
public class PresetElementPath implements Serializable {
	private static final long serialVersionUID = 1L;
	final ArrayList<String>path;
	
	public PresetElementPath() {
		path = new ArrayList<String>();
	}
	
	public PresetElementPath(PresetElementPath path2) {
		path = new ArrayList<String>(path2.path);
	}
	
	@Override
	public String toString() {
		String result = "";
		for (String s:path) {
			result += s + "|";
		}
		return result;
	}
}
