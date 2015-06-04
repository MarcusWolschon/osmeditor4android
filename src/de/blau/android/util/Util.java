package de.blau.android.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

public class Util {
	
	static public ArrayList<String> getArrayList(String s) {
		ArrayList<String> v = new ArrayList<String>();
		v.add(s);
		return v;
	}

	static public LinkedHashMap<String,ArrayList<String>> getArrayListMap(Map<String,String>map) {
		LinkedHashMap<String,ArrayList<String>> result = new LinkedHashMap<String,ArrayList<String>>();
		for (Entry<String, String> e:map.entrySet()) {
			result.put(e.getKey(), getArrayList(e.getValue()));
		}
		return result;
	}
	
}
