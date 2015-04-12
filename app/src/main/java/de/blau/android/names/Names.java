package de.blau.android.names;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import de.blau.android.util.MultiHashMap;
import de.blau.android.util.jsonreader.JsonReader;


/**
 * Support for the name suggestion index 
 * see https://github.com/simonpoole/name-suggestion-index
 * @author simon
 *
 */
public class Names {
	
	public class TagMap extends TreeMap<String,String> {
		
		private static final long serialVersionUID = 1L;
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			for (String s:this.keySet()) {
				s.replace("|", " ");
				builder.append(s + "=" + this.get(s) + "|");
			}
			if (builder.length() > 0)
				builder.deleteCharAt(builder.length()-1);
			return builder.toString();
		}
	}
	
	public class NameAndTags implements Comparable<NameAndTags>{
		private String name;
		TagMap tags;
		
		public NameAndTags(String name, TagMap tags) {
			this.setName(name);
			this.tags = tags;
		}
		
		@Override
		public String toString() {
			return getName() + " (" + tags.toString() + ")"; 
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}
		
		/**
		 * @return the name
		 */
		public TagMap getTags() {
			return tags;
		}

		@Override
		public int compareTo(NameAndTags another) {
			if (another.name.equals(name)) {
				// more tags is better
				if (tags.size() > ((NameAndTags)another).tags.size()) {
					return +1;
				} else if (tags.size() < another.tags.size()) {
					return -1;
				} 
				return 0;
			}
			return name.compareTo(another.name);
		}
	}
	
	private static MultiHashMap<String,TagMap> nameList = new MultiHashMap<String,TagMap>(false); // names -> tags
	private static HashMap<String,TagMap> tagList = new HashMap<String,TagMap>(); // tags string to tags
	private static MultiHashMap<TagMap, String> tags2namesList = new MultiHashMap<TagMap,String>(false); //
	private static MultiHashMap<String, String> categories =  new MultiHashMap<String,String>(false);
	
	private static boolean ready = false;
	
	public Names(Context ctx) {
		synchronized (nameList) {
			
			if (!ready) {
				Log.d("Names","Parsing configuration files");
	
				AssetManager assetManager = ctx.getAssets();
				try {
					InputStream is = assetManager.open("name-suggestions.min.json");
					JsonReader reader = new JsonReader(new InputStreamReader(is));
						
					try {
						try {
							// key object
							String key = null;
							reader.beginObject();
							while (reader.hasNext()) {
								key = reader.nextName(); // amenity, shop
								// value object
								String value = null;
								reader.beginObject();
								while (reader.hasNext()) { // restaurant, fast_food, ....
									value = reader.nextName();
									// name object
									String name = null;
									int count = 0;
									reader.beginObject();
									while (reader.hasNext()) {
										name = reader.nextName(); // name of estabishment
										reader.beginObject();
										TagMap secondaryTags = null; // any extra tags store here
										while (reader.hasNext()) {
											String jsonName = reader.nextName();
											if (jsonName.equals("count")) {
												count = reader.nextInt();
											} else if (jsonName.equals("tags")) {
												reader.beginObject();
												while (reader.hasNext()) {
													secondaryTags = new TagMap();
													secondaryTags.put(reader.nextName(), reader.nextString());
												}
												reader.endObject(); // tags
											} else {
												reader.skipValue();
											}
										}
										reader.endObject(); // name
										
										// add to lists here
										TagMap primaryTags = new TagMap();
										primaryTags.put(key, value);
										if (secondaryTags != null) {
											primaryTags.putAll(secondaryTags);
										}
										String tagKey = primaryTags.toString();
										TagMap tm = tagList.get(tagKey);
										if (tm == null) {
											tagList.put(tagKey, primaryTags);
											tm = primaryTags;
										}
										nameList.add(name, tm);
										tags2namesList.add(tm, name);
									}
									reader.endObject(); // value
							    }
								reader.endObject(); // key
							}
							reader.endObject();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
					}
					finally {
					       reader.close();
					}
					is = assetManager.open("categories.json");
					reader = new JsonReader(new InputStreamReader(is));
					try {
						try{
							String category = null;
							reader.beginObject();
							while (reader.hasNext()) {
								category = reader.nextName();
								String poiType = null;
								reader.beginObject();
								while (reader.hasNext()) {
									poiType = reader.nextName();
									reader.beginArray();
									while (reader.hasNext()) {
										categories.add(category,poiType+"="+reader.nextString());
									}
									reader.endArray();
								}
								reader.endObject();
							}
							reader.endObject();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
					}
					finally {
					       reader.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				ready = true;
			}
		}
	}
	

	public Collection<NameAndTags> getNames(SortedMap<String, String> tags) {
		// remove irrelevant tags, TODO refine
		TagMap tm = new TagMap();
		String v = tags.get("amenity");
		if (v!=null) {
			tm.put("amenity",v);
			// Log.d("Names","filtering for amenity="+v);
		} else {
			v = tags.get("shop");
			if (v!=null) {
				tm.put("shop",v);
				// Log.d("Names","filtering for shop="+v);
			}
		}
		if (tm.isEmpty())
			return getNames();
		
		Collection<NameAndTags> result = new ArrayList<NameAndTags>();
		
		String origTagKey = tm.toString();
		TreeSet<String> seen = new TreeSet<String>();
		// check categories for similar tags and add names from them too
		for (String category:categories.getKeys()) {    	// loop over categories
			Set<String>set = categories.get(category);
			if (set.contains(origTagKey)) {
				for (String catTagKey:set) {				// loop over categories content
					if (!seen.contains(catTagKey)) {		// suppress dups
						for (String tagKey:tagList.keySet()) { 	
							if (tagKey.contains(catTagKey)) {	
								TagMap storedTagMap = tagList.get(tagKey);
								for (String n:tags2namesList.get(storedTagMap)) {
									NameAndTags nt = new NameAndTags(n,storedTagMap);
									result.add(nt);
								}
							}
						}
						seen.add(catTagKey);
					}
				}
			}
		}	
		return result;
	}
	
	public Collection<NameAndTags> getNames() {
		Collection<NameAndTags> result = new ArrayList<NameAndTags>();
		for (String n:nameList.getKeys()) {
			TagMap bestTags = null;
			for (TagMap t:nameList.get(n)) {
				if (bestTags == null || bestTags.size() < t.size()) {
					bestTags = t;
				}
			}
			if (bestTags != null)
				result.add(new NameAndTags(n, bestTags));
		}
		return result;
	}
	
	public void dump2Log() {
		Log.d("Names","Name List");
		for (String n:nameList.getKeys()) {
			Set<TagMap> tmList = nameList.get(n);
			String tags = n +": ";
			for (TagMap tm:tmList) {
				tags = tags + tm.toString() + "|";
			}
			Log.d("Names", tags);
		}
		Log.d("Names","tag List");
		for (TagMap tm:tags2namesList.getKeys()) {
			Set<String> names = tags2namesList.get(tm);
			String nameStr = tm.toString() +": ";
			for (String n:names) {
				 nameStr = nameStr + n + "|";
			}
			Log.d("Names", nameStr);
		}
	}
}
