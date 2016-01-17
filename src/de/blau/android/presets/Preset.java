package de.blau.android.presets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.AttributeList;
import org.xml.sax.HandlerBase;
import org.xml.sax.SAXException;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ScrollView;
import android.widget.TextView;
import ch.poole.poparser.Po;
import de.blau.android.Application;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.PresetEditorActivity;
import de.blau.android.util.Hash;
import de.blau.android.util.MultiHashMap;
import de.blau.android.util.SearchIndexUtils;
import de.blau.android.util.StringWithDescription;
import de.blau.android.views.WrappingLayout;

/**
 * This class loads and represents JOSM preset files.
 * 
 * Presets can come from one of three sources:
 * a) the default preset, which is loaded from the default asset locations (see below)
 * b) an APK-based preset, which is loaded from an APK
 * c) a downloaded preset, which is downloaded to local storage by {@link PresetEditorActivity}
 * 
 * For APK-based presets, the APK must have a "preset.xml" file in the asset directory,
 * and may have images in the "images" subdirectory in the asset directory.
 * A preset is considered APK-based if the constructor receives a package name.
 * In the preset editor, use the package name prefixed by the {@link APKPRESET_URLPREFIX}
 * to specify an APK preset.
 * 
 * The preset.xml is loaded from the following sources:
 * a) for the default preset, "preset.xml" in the default asset locations
 * b) for APK-based presets, "preset.xml" in the APK asset directory
 * c) for downloaded presets, "preset.xml" in the preset data directory
 * 
 * Icons referenced in the XML preset definition by relative URL are loaded from the following locations:
 *  1. If a package name is given and the APK contains a matching asset, from the asset ("images/" is prepended to the path)
 *  2. Otherwise, from the default asset location (see below, "images/" is prepended to the path)
 * 
 * Icons referenced in the XML preset by a http or https URL are loaded from the presets data directory,
 * where they should be placed under a name derived from the URL hash by {@link PresetEditorActivity}.
 * Default and APK presets cannot have http/https icons.
 * 
 * If an asset needs to be loaded from the default asset locations, the loader checks for the existence
 * of an APK with the package name specified in {@link PresetIconManager#EXTERNAL_DEFAULT_ASSETS_PACKAGE}.
 * If this package exists and contains a matching asset, it is loaded from there.
 * Otherwise, it is loaded from the Vespucci asset directory.
 * The external default assets package just needs an asset directory that can contain a preset.xml and/or image directory.
 * 
 * @author Jan Schejbal
 */
public class Preset implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4L;
	/** name of the preset XML file in a preset directory */
	public static final String PRESETXML = "preset.xml";
	/** name of the MRU serialization file in a preset directory */
	private static final String MRUFILE = "mru.dat";
	public static final String APKPRESET_URLPREFIX = "apk:";
	
	// hardwired layout stuff
	public static final int SPACING = 5;
	
	//
	private static final int MAX_MRU_SIZE = 50;
	private static final String DEBUG_TAG = Preset.class.getName();
	
	/** The directory containing all data (xml, MRU data, images) about this preset */
	private File directory;

	/** Lists items having a tag. The map key is tagkey+"\t"+tagvalue.
	 * tagItems.get(tagkey+"\t"+tagvalue) will give you all items that have the tag tagkey=tagvalue */
	protected final MultiHashMap<String, PresetItem> tagItems = new MultiHashMap<String, PresetItem>();

	/** The root group of the preset, containing all top-level groups and items */
	protected PresetGroup rootGroup;

	/** {@link PresetIconManager} used for icon loading */
	protected transient PresetIconManager iconManager;	
	
	/** all known preset items in order of loading */
	protected ArrayList<PresetItem> allItems = new ArrayList<PresetItem>();
	
	public static enum PresetKeyType {
		/**
		 * arbitrary single value
		 */
		TEXT,
		/**
		 * multiple values, single select
		 */
		COMBO,
		/**
		 * multiple values, multiple select
		 */
		MULTISELECT,
		/**
		 * single value, set or unset
		 */
		CHECK
	}

	/** Maps all possible keys to the respective values for autosuggest (only key/values applying to nodes) */
	protected final MultiHashMap<String, StringWithDescription> autosuggestNodes = new MultiHashMap<String, StringWithDescription>(true);
	/** Maps all possible keys to the respective values for autosuggest (only key/values applying to ways) */
	protected final MultiHashMap<String, StringWithDescription> autosuggestWays = new MultiHashMap<String, StringWithDescription>(true);
	/** Maps all possible keys to the respective values for autosuggest (only key/values applying to closed ways) */
	protected final MultiHashMap<String, StringWithDescription> autosuggestClosedways = new MultiHashMap<String, StringWithDescription>(true);
	/** Maps all possible keys to the respective values for autosuggest (only key/values applying to closed ways) */
	protected final MultiHashMap<String, StringWithDescription> autosuggestRelations = new MultiHashMap<String, StringWithDescription>(true);
	
	/** for search support */
	protected final MultiHashMap<String, PresetItem> searchIndex = new MultiHashMap<String, PresetItem>();
	protected final MultiHashMap<String, PresetItem> translatedSearchIndex = new MultiHashMap<String, PresetItem>();
		
	protected Po po = null;
	
	/**
	 * Serializable class for storing Most Recently Used information.
	 * Hash is used to check compatibility.
	 */
	protected static class PresetMRUInfo implements Serializable {
		private static final long serialVersionUID = 7708132207266548489L;

		protected PresetMRUInfo(String presetHash) {
			this.presetHash = presetHash;
		}
		
		/** hash of current preset (used to check validity of recentPresets indexes) */
		protected final String presetHash;
	
		/** indexes of recently used presets (for use with allItems) */
		protected LinkedList<Integer> recentPresets = new LinkedList<Integer>();

		public volatile boolean changed = false;
	}
	protected final PresetMRUInfo mru;
	private String externalPackage;
	
	private class PresetFilter implements FilenameFilter {
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".xml");
		}
	}
	
	/**
	 * create a dummy preset
	 */
	public Preset() {
		mru = null;
	}
	
	/**
	 * Creates a preset object.
	 * @param ctx context (used for preset loading)
	 * @param directory directory to load/store preset data (XML, icons, MRUs)
	 * @param name of external package containing preset assets for APK presets, null for other presets
	 * @throws Exception
	 */
	public Preset(Context ctx, File directory, String externalPackage) throws Exception {
		this.directory = directory;
		this.externalPackage = externalPackage;
		rootGroup = new PresetGroup(null, "", null);
		
		directory.mkdir();
		
		InputStream fileStream;
		if (directory.getName().equals(AdvancedPrefDatabase.ID_DEFAULT)) {
			Log.i("Preset", "Loading default preset");
			iconManager = new PresetIconManager(ctx, null, null);
			fileStream = iconManager.openAsset(PRESETXML, true);
			// get translations
			InputStream poFileStream = iconManager.openAsset("preset_"+Locale.getDefault()+".po", true);
			if (poFileStream == null) {
				poFileStream = iconManager.openAsset("preset_"+Locale.getDefault().getLanguage()+".po", true);
			}
			if (poFileStream != null) {
				try {
					po = new Po(poFileStream);
				} catch (Exception ignored) {
					Log.e("Preset","Parsing translation file for " + Locale.getDefault() + " or " + Locale.getDefault().getLanguage() + " failed");
				} catch (Error ignored) {
					Log.e("Preset","Parsing translation file for " + Locale.getDefault() + " or " + Locale.getDefault().getLanguage() + " failed");
				}
			}
		} else if (externalPackage != null) {
			Log.i("Preset", "Loading APK preset, package=" + externalPackage + ", directory="+directory.toString());
			iconManager = new PresetIconManager(ctx, directory.toString(), externalPackage);
			fileStream = iconManager.openAsset(PRESETXML, false);
			// po = new Po(iconManager.openAsset("preset_"+Locale.getDefault()+".po", false));
		} else {
			Log.i("Preset", "Loading downloaded preset, directory="+directory.toString());
			iconManager = new PresetIconManager(ctx, directory.toString(), null);
			File indir = new File(directory.toString());
			fileStream = null; // force crash and burn
			if (indir != null) {
				File[] list = indir.listFiles(new PresetFilter());
				if (list != null && list.length > 0) { // simply use the first XML file found
					String presetFilename = list[0].getName();
					Log.i("Preset", "Preset file name " + presetFilename);
					fileStream = new FileInputStream(new File(directory, presetFilename));
					// get translations
					presetFilename = presetFilename.substring(0, presetFilename.length()-4);
					InputStream poFileStream = null;
					// try to open .po files either with the same name as the preset file or the standard name
					try {
						poFileStream = new FileInputStream(new File(directory,presetFilename+"_"+Locale.getDefault()+".po"));
					} catch (FileNotFoundException fnfe) {
						try {
							poFileStream = new FileInputStream(new File(directory,presetFilename+"_"+Locale.getDefault().getLanguage()+".po"));
						} catch (FileNotFoundException fnfe2) {
							try {
								presetFilename = PRESETXML.substring(0, PRESETXML.length()-4);
								poFileStream = new FileInputStream(new File(directory,presetFilename+"_"+Locale.getDefault()+".po"));
							} catch (FileNotFoundException fnfe3) {
								try {
									poFileStream = new FileInputStream(new File(directory,presetFilename+"_"+Locale.getDefault().getLanguage()+".po"));
								} catch (FileNotFoundException fnfe4) {
									// no translations
								}
							}
						}
					}
					if (poFileStream != null) {
						try {
							po = new Po(poFileStream);
						} catch (Exception ignored) {
							Log.e("Preset","Parsing translation file for " + Locale.getDefault() + " or " + Locale.getDefault().getLanguage() + " failed");
						} catch (Error ignored) {
							Log.e("Preset","Parsing translation file for " + Locale.getDefault() + " or " + Locale.getDefault().getLanguage() + " failed");
						}
					}
				}
			} 			
		}		
		
		DigestInputStream hashStream = new DigestInputStream(
				fileStream,
				MessageDigest.getInstance("SHA-256"));

		parseXML(hashStream);
        
        // Finish hash
        String hashValue = Hash.toHex(hashStream.getMessageDigest().digest());
        // in theory, it could be possible that the stream parser does not read the entire file
        // and maybe even randomly stops at a different place each time.
        // in practice, it does read the full file, which means this gives the actual sha256 of the file,
        //  - even if you add a 1 MB comment after the document-closing tag.
        
        // remove chunks - this messes up the index disabled for now
//        for (PresetItem c:new ArrayList<PresetItem>(allItems)) {
//        	if (c.isChunk()) {
//        		allItems.remove(c);
//        	}
//        }
        
        mru = initMRU(directory, hashValue);
        
//        for (String k:searchIndex.getKeys()) {
//        	String l = k;
//        	for (PresetItem pi:searchIndex.get(k)) {
//        		l = l + " " + pi.getName();
//        	}
//        	Log.d("SearchIndex",l);
//        }
        Log.d("SearchIndex","length: " + searchIndex.getKeys().size());
	}
	
	PresetIconManager getIconManager(Context ctx) {
		if (directory.getName().equals(AdvancedPrefDatabase.ID_DEFAULT)) {
			return new PresetIconManager(ctx, null, null);
		} else if (externalPackage != null) {
			return  new PresetIconManager(ctx, directory.toString(), externalPackage);
		} else {
			return  new PresetIconManager(ctx, directory.toString(), null);
		}		
	}

	/**
	 * Parses the XML during import
	 * @param input the input stream from which to read XML data
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	private void parseXML(InputStream input)
			throws ParserConfigurationException, SAXException, IOException {
		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		
        saxParser.parse(input, new HandlerBase() {
        	/** stack of group-subgroup-subsubgroup... where we currently are*/
        	private Stack<PresetGroup> groupstack = new Stack<PresetGroup>();
        	/** item currently being processed */
        	private PresetItem currentItem = null;
        	/** true if we are currently processing the optional section of an item */
        	private boolean inOptionalSection = false;
        	/** hold reference to chunks */
        	private HashMap<String,PresetItem> chunks = new HashMap<String,PresetItem>();
        	/** store current combo or multiselect key */
        	private String listKey = null;
        	private ArrayList<StringWithDescription> listValues = null;
        	
        	{
        		groupstack.push(rootGroup);
        	}
        	
            /** 
             * ${@inheritDoc}.
             */
			@Override
            public void startElement(String name, AttributeList attr) throws SAXException {
            	if ("group".equals(name)) {
            		PresetGroup parent = groupstack.peek();
            		PresetGroup g = new PresetGroup(parent, attr.getValue("name"), attr.getValue("icon"));
            		String context = attr.getValue("name_context");
            		if (context != null) {
            			g.setNameContext(context);
            		}
            		groupstack.push(g);
            	} else if ("item".equals(name)) {
            		if (currentItem != null) throw new SAXException("Nested items are not allowed");
            		PresetGroup parent = groupstack.peek();
            		String type = attr.getValue("type");
            		if (type == null) {
            			type = attr.getValue("gtype"); // note gtype seems to be undocumented
            		}
            		currentItem = new PresetItem(parent, attr.getValue("name"), attr.getValue("icon"), type);
            		String context = attr.getValue("name_context");
            		if (context != null) {
            			currentItem.setNameContext(context);
            		}
            	} else if ("chunk".equals(name)) {
                	if (currentItem != null) throw new SAXException("Nested items are not allowed");
                	String type = attr.getValue("type");
            		if (type == null) {
            			type = attr.getValue("gtype"); // note gtype seems to be undocumented
            		}
                	currentItem = new PresetItem(null, attr.getValue("id"), attr.getValue("icon"), type);
                	currentItem.setChunk();
            	} else if ("separator".equals(name)) {
            		new PresetSeparator(groupstack.peek());
            	} else if ("optional".equals(name)) {
            		inOptionalSection = true;
            	} else if ("key".equals(name)) {
            		if (!inOptionalSection) {
            			currentItem.addTag(attr.getValue("key"), PresetKeyType.TEXT, attr.getValue("value"), attr.getValue("text"));
            		} else {
            			// Optional fixed tags should not happen, their values will NOT be automatically inserted.
            			currentItem.addTag(true, attr.getValue("key"), PresetKeyType.TEXT, attr.getValue("value"));
            		}
            	} else if ("text".equals(name)) {
            		currentItem.addTag(inOptionalSection, attr.getValue("key"), PresetKeyType.TEXT, (String)null);
            		String text = attr.getValue("text");
            		if (text != null) {
            			currentItem.addHint(attr.getValue("key"),text);
            		}
            	} else if ("link".equals(name)) {
            		currentItem.setMapFeatures(attr.getValue("href")); // just English for now
            	} else if ("check".equals(name)) {
            		String value_on = attr.getValue("value_on") == null ? "yes" : attr.getValue("value_on");
            		String value_off = attr.getValue("value_off") == null ? "no" : attr.getValue("value_off");
            		String disable_off = attr.getValue("disable_off");
            		String values = value_on;
            		// zap value_off if disabled
            		if (disable_off != null && disable_off.equals("true")) {
            			value_off = "";
            		} else {
            			values = "," + value_off;
            		}
             		currentItem.addTag(inOptionalSection, attr.getValue("key"), PresetKeyType.CHECK, values);
            		String defaultValue = attr.getValue("default") == null ? value_off : (attr.getValue("default").equals("on") ? value_on : value_off);
            		if (defaultValue != null) {
            			currentItem.addDefault(attr.getValue("key"),defaultValue);
            		}
               		String text = attr.getValue("text");
            		if (text != null) {
            			currentItem.addHint(attr.getValue("key"),text);
            		}
            	} else if ("combo".equals(name)) {
            		String delimiter = attr.getValue("delimiter");
            		if (delimiter == null) {
            			delimiter = ","; // combo uses "," as default
            		}
            		String comboValues = attr.getValue("values");
            		if (comboValues != null) {
            			currentItem.addTag(inOptionalSection, attr.getValue("key"), PresetKeyType.COMBO, comboValues, delimiter);
            		} else {
            			listKey = attr.getValue("key");
            			listValues = new ArrayList<StringWithDescription>();
            		}
            		String defaultValue = attr.getValue("default");
            		if (defaultValue != null) {
            			currentItem.addDefault(attr.getValue("key"),defaultValue);
            		}
               		String text = attr.getValue("text");
            		if (text != null) {
            			currentItem.addHint(attr.getValue("key"),text);
            		}
            	} else if ("multiselect".equals(name)) {
            		String delimiter = attr.getValue("delimiter");
            		if (delimiter == null) {
            			delimiter = ";"; // multiselect uses ";" as default
            		}
            		String multiselectValues = attr.getValue("values");
            		if (multiselectValues != null) {
            			currentItem.addTag(inOptionalSection, attr.getValue("key"), PresetKeyType.MULTISELECT, multiselectValues, delimiter); 
            		} else {
            			listKey = attr.getValue("key");
            			listValues = new ArrayList<StringWithDescription>();
            		}
            		String defaultValue = attr.getValue("default");
            		if (defaultValue != null) {
            			currentItem.addDefault(attr.getValue("key"),defaultValue);
            		}
               		String text = attr.getValue("text");
            		if (text != null) {
            			currentItem.addHint(attr.getValue("key"),text);
            		}
            	} else if ("role".equals(name)) {
            		currentItem.addRole(attr.getValue("key")); 
            	} else if ("reference".equals(name)) {
            		PresetItem chunk = chunks.get(attr.getValue("ref")); // note this assumes that there are no forward references
            		if (chunk != null) {
            			currentItem.fixedTags.putAll(chunk.getFixedTags());
            			if (!currentItem.isChunk()) {
            				for (Entry<String,StringWithDescription> e:chunk.getFixedTags().entrySet()) {
            					StringWithDescription v = e.getValue();
            					String value = "";
            					if (v != null && v.getValue() != null) {
            						value = v.getValue();
            					}
            					tagItems.add(e.getKey()+"\t"+value, currentItem);
            				}
            			}
            			currentItem.optionalTags.putAll(chunk.getOptionalTags());
            			addToTagItems(currentItem, chunk.getOptionalTags());
            			
            			currentItem.recommendedTags.putAll(chunk.getRecommendedTags());
            			addToTagItems(currentItem, chunk.getRecommendedTags());
               			
            			currentItem.hints.putAll(chunk.hints);
            			currentItem.defaults.putAll(chunk.defaults);
            			currentItem.keyType.putAll(chunk.keyType);
            			currentItem.roles.addAll(chunk.roles); // FIXME this and the following could lead to duplicate entries
            			currentItem.linkedPresetNames.addAll(chunk.linkedPresetNames);
            		}
            	} else if ("list_entry".equals(name)) {
            		if (listValues != null) {
            			String v = attr.getValue("value");
            			if (v != null) {
            				String d = attr.getValue("short_description");
            				listValues.add(new StringWithDescription(v,po != null ? po.t(d):d));
            			}
            		}
            	} else if ("preset_link".equals(name)) {
            		String presetName = attr.getValue("preset_name");
            		if (presetName != null) {
            			currentItem.addLinkedPresetName(presetName);
            		}
            	}
            }
			
			void addToTagItems(PresetItem currentItem, Map<String,StringWithDescription[]>tags) {
				if (currentItem.isChunk()) { // only do this on the final expansion
					return;
				}
      			for (Entry<String,StringWithDescription[]> e:tags.entrySet()) {
    				StringWithDescription values[] = e.getValue();
    				if (values != null) {
    					for (StringWithDescription v:values) {
    						String value = "";
    						if (v != null && v.getValue() != null) {
    							value = v.getValue();
    						}
    						tagItems.add(e.getKey()+"\t"+value, currentItem);
    					}
    				}
    			}
			}
            
            @Override
            public void endElement(String name) throws SAXException {
            	if ("group".equals(name)) {
            		groupstack.pop();
            	} else if ("optional".equals(name)) {
            		inOptionalSection = false;
            	} else if ("item".equals(name)) {
                    // Log.d("Preset","PresetItem: " + currentItem.toString());
            		currentItem.buildSearchIndex();
            		currentItem = null;
              		listKey = null;
            		listValues = null;
            	} else if ("chunk".equals(name)) {
                    chunks.put(currentItem.getName(),currentItem);
            		currentItem = null;
              		listKey = null;
            		listValues = null;
            	} else if ("combo".equals(name) || "multiselect".equals(name)) {
            		if (listKey != null && listValues != null) {
            			StringWithDescription[] v = new StringWithDescription[listValues.size()];
            			currentItem.addTag(inOptionalSection, 
            					listKey, "combo".equals(name)?PresetKeyType.COMBO:PresetKeyType.MULTISELECT, 
            					listValues.toArray(v));
            		}
            		listKey = null;
            		listValues = null;
            	} 
            }
        });
	}

	/**
	 * Initializes Most-recently-used data by either loading them or creating an empty list
	 * @param directory data directory of the preset
	 * @param hashValue XML hash value to check if stored data fits the XML
	 * @returns a MRU object valid for this Preset, never null
	 */
	private PresetMRUInfo initMRU(File directory, String hashValue) {
		PresetMRUInfo tmpMRU;
		ObjectInputStream mruReader = null;
        try {
        	mruReader = new ObjectInputStream(new FileInputStream(new File(directory, MRUFILE)));
        	tmpMRU = (PresetMRUInfo) mruReader.readObject();
        	if (!tmpMRU.presetHash.equals(hashValue)) throw new InvalidObjectException("hash mismatch");
        } catch (Exception e) {
        	tmpMRU = new PresetMRUInfo(hashValue);
        	// Deserialization failed for whatever reason (missing file, wrong version, ...) - use empty list
        	Log.i("Preset", "No usable old MRU list, creating new one ("+e.toString()+")");
        } finally {
				try { if (mruReader != null) mruReader.close(); } catch (Exception e) {} // ignore IO exceptions
        }
    	return tmpMRU;
	}
	
	/**
	 * Returns a list of icon URLs referenced by a preset
	 * @param presetDir a File object pointing to the directory containing this preset
	 * @return an ArrayList of http and https URLs as string, or null if there is an error during parsing
	 */
	@SuppressWarnings("deprecation")
	public static ArrayList<String> parseForURLs(File presetDir) {
		final ArrayList<String> urls = new ArrayList<String>();
		try {
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			
	        saxParser.parse(new File(presetDir, PRESETXML), new HandlerBase() {
	            /** 
	             * ${@inheritDoc}.
	             */
				@Override
	            public void startElement(String name, AttributeList attr) throws SAXException {
	            	if ("group".equals(name) || "item".equals(name)) {
	            		String url = attr.getValue("icon");
	            		if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
	            			urls.add(url);
	            		}
	            	}
	            }
	        });
		} catch (Exception e) {
			Log.e("PresetURLParser", "Error parsing preset", e);
			return null;
		}
		
		return urls;
	}
	
	/** @return the root group of the preset, containing all top-level groups and items */
	public PresetGroup getRootGroup() {
		return rootGroup;
	}
	
	/*
	 * return true if the item is from this Preset
	 */
	public boolean contains(PresetItem pi) {
		return allItems.contains(pi);
	}
	
	/**
	 * Return the index of the preset by sequential search FIXME
	 * @param name
	 * @return
	 */
	public Integer getItemIndexByName(String name) {
		Log.d("Preset","getItemIndexByName " + name);
		for (PresetItem pi:allItems) {
			if (pi.getName().equals(name)) {
				return Integer.valueOf(pi.getItemIndex());
			}
		}
		return null;
	}
	
	/**
	 * Returns a view showing the most recently used presets
	 * @param handler the handler which will handle clicks on the presets
	 * @param type filter to show only presets applying to this type
	 * @return the view
	 */
	public View getRecentPresetView(Context ctx, Preset[] presets, PresetClickHandler handler, ElementType type) {
		PresetGroup recent = new PresetGroup(null, "recent", null);
		for (Preset p: presets) {
			if (p != null && p.hasMRU()) {
				for (Integer index : p.mru.recentPresets) {
					recent.addElement(p.allItems.get(index.intValue()));
				}
			}
		}
		return recent.getGroupView(ctx, handler, type);
	}
	
	public boolean hasMRU()
	{
		return mru.recentPresets.size() > 0;
	}
	
	/**
	 * Add a preset to the front of the MRU list (removing old duplicates and limiting the list to 50 entries if needed)
	 * @param item the item to add
	 */
	public void putRecentlyUsed(PresetItem item) {
		Integer id = item.getItemIndex();
		// prevent duplicates
		if (!mru.recentPresets.remove(id)) { // calling remove(Object), i.e. removing the number if it is in the list, not the i-th item
			// preset is not in the list, add linked presets first
			PresetItem pi = allItems.get(id.intValue());
			LinkedList<String>linkedPresetNames = new LinkedList<String>(pi.linkedPresetNames);
			Collections.reverse(linkedPresetNames);
			for (String n:linkedPresetNames) {
				if (!mru.recentPresets.contains(id)) {
					Integer presetIndex = getItemIndexByName(n);
					if (presetIndex != null) {  // null if the link wasn't found
						if (!mru.recentPresets.contains(presetIndex)) { // only add if not already present
							mru.recentPresets.addFirst(presetIndex);
							if (mru.recentPresets.size() > MAX_MRU_SIZE) {
								mru.recentPresets.removeLast();
							}
						}
					} else {
						Log.e("Preset","linked preset not found for " + n + " in preset " + pi.getName());
					}
				}
			}
		}	
		mru.recentPresets.addFirst(id);
		if (mru.recentPresets.size() > MAX_MRU_SIZE) {
			mru.recentPresets.removeLast();
		}
		mru.changed  = true;
	}

	/**
	 * Remove a preset
	 * @param item the item to remove
	 */
	public void removeRecentlyUsed(PresetItem item) {
		Integer id = item.getItemIndex();
		// prevent duplicates
		mru.recentPresets.remove(id); // calling remove(Object), i.e. removing the number if it is in the list, not the i-th item
		mru.changed  = true;
	}
	
	/**
	 * Remove a preset
	 * @param item the item to remove
	 */
	public void resetRecentlyUsed() {
		mru.recentPresets = new LinkedList<Integer>(); 
		mru.changed  = true;
		saveMRU();
	}
	
	/** Saves the current MRU data to a file */
	public void saveMRU() {
		if (mru.changed) {
			try {
				ObjectOutputStream out =
					new ObjectOutputStream(new FileOutputStream(new File(directory, MRUFILE)));
				out.writeObject(mru);
				out.close();
			} catch (Exception e) {
				Log.e("Preset", "MRU saving failed", e);
			}
		}
	}
	
    
    public String toJSON() {
    	String result = "";
    	for (PresetItem pi:allItems) {
    		result = result + pi.toJSON();
    	}
    	return result;
    }

	/**
	 * 
	 * Finds the preset item best matching a certain tag set, or null if no preset item matches.
	 * To match, all (mandatory) tags of the preset item need to be in the tag set.
	 * The preset item does NOT need to have all tags in the tag set, but the tag set needs
	 * to have all (mandatory) tags of the preset item.
	 * 
	 * If multiple items match, the most specific one (i.e. having most tags) wins.
	 * If there is a draw, no guarantees are made.
	 * @param tags tags to check against (i.e. tags of a map element)
	 * @return null, or the "best" matching item for the given tag set
	 */
    static public PresetItem findBestMatch(Preset presets[], Map<String,String> tags) {
	
		int bestMatchStrength = 0;
		PresetItem bestMatch = null;
		
		if (tags==null || presets==null) {
			Log.e("Preset", "findBestMatch " + (tags==null?"tags null":"presets null"));
			return null;
		}
		
		// Build candidate list
		LinkedHashSet<PresetItem> possibleMatches = new LinkedHashSet<PresetItem>();
		for (Preset p:presets) {
			if (p != null) {
				for (Entry<String, String> tag : tags.entrySet()) {
					String tagString = tag.getKey()+"\t"+tag.getValue();
					possibleMatches.addAll(p.tagItems.get(tagString));
				}
			}
		}
		// Find best
		for (PresetItem possibleMatch : possibleMatches) {
			if ((possibleMatch.getFixedTagCount() <= bestMatchStrength) && (possibleMatch.getRecommendedTags().size()) <= bestMatchStrength) continue; // isn't going to help
			if (possibleMatch.getFixedTagCount() > 0) { // has required tags			
				if (possibleMatch.matches(tags)) {
					bestMatch = possibleMatch;
					bestMatchStrength = bestMatch.getFixedTagCount();
				}
			} else if (possibleMatch.getRecommendedTags().size() > 0) {
				int matches = possibleMatch.matchesRecommended(tags);
				if (matches > bestMatchStrength) {
					bestMatch = possibleMatch;
					bestMatchStrength = matches;
				}
			}
		}
		// Log.d(DEBUG_TAG,"findBestMatch " + bestMatch);
		return bestMatch;
	}
	
	/**
	 * Filter a list of elements by type
	 * @param originalElements the list to filter
	 * @param type the type to allow
	 * @return a filtered list containing only elements of the specified type
	 */
	private static ArrayList<PresetElement> filterElements(
			ArrayList<PresetElement> originalElements, ElementType type)
	{
		ArrayList<PresetElement> filteredElements = new ArrayList<PresetElement>();
		for (PresetElement e : originalElements) {
			if (e.appliesTo(type)) {
				filteredElements.add(e);
			} else if ((e instanceof PresetSeparator) && !filteredElements.isEmpty() &&
					!(filteredElements.get(filteredElements.size()-1) instanceof PresetSeparator)) {
				// add separators iff there is a non-separator element above them
				filteredElements.add(e);
			}
		}
		return filteredElements;
	}
	
	/**
	 * Represents an element (group or item) in a preset data structure
	 */
	public abstract class PresetElement implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4L;
		protected String name;
		protected String nameContext = null;
		private String iconpath;
		private String mapiconpath;
		private transient Drawable icon;
		private transient BitmapDrawable mapIcon;
		protected PresetGroup parent;
		protected boolean appliesToWay;
		protected boolean appliesToNode;
		protected boolean appliesToClosedway;
		protected boolean appliesToRelation;
		private String mapFeatures;

		/**
		 * Creates the element, setting parent, name and icon, and registers with the parent
		 * @param parent parent group (or null if this is the root group)
		 * @param name name of the element
		 * @param iconpath The icon path (either "http://" URL or "presets/" local image reference)
		 */
		public PresetElement(PresetGroup parent, String name, String iconpath) {
			this.parent = parent;
			this.name = name;
			this.iconpath = iconpath;
			mapiconpath = iconpath;
			icon = null;
			mapIcon = null;
			if (parent != null)	parent.addElement(this);
		}		
		
		public String getName() {
			return name;
		}
		
		/**
		 * Return the name of this preset element, potentially translated
		 * @return
		 */
		public String getTranslatedName() {
			if (nameContext!=null) {
				return po!=null?po.t(nameContext,getName()):getName();
			}
			return po!=null?po.t(getName()):getName();
		}
		
		/**
		 * Return the icon for the preset or a place holder
		 * @return
		 */
		public Drawable getIcon() {
			if (icon == null) {
				if (iconManager == null) {
					iconManager = getIconManager(Application.mainActivity);
				}
				if (iconpath != null) {
					icon = iconManager.getDrawableOrPlaceholder(iconpath, 36);
					iconpath = null;
				} else {
					return iconManager.getPlaceHolder(36);
				}
			}
			return icon;
		}
		
		public BitmapDrawable getMapIcon() {
			if (mapIcon == null && mapiconpath != null) {
				if (iconManager == null) {
					iconManager = getIconManager(Application.mainActivity);
				}
				mapIcon = iconManager.getDrawable(mapiconpath, de.blau.android.Map.ICON_SIZE_DP);
				mapiconpath = null;
			}
			return mapIcon;
		}
		
		public PresetGroup getParent() {
			return parent;
		}
		
		public void setParent(PresetGroup pg) {
			parent = pg;
		}
		
		/**
		 * Returns a basic view representing the current element (i.e. a button with icon and name).
		 * Can (and should) be used when implementing {@link #getView(PresetClickHandler)}.
		 * @return the view
		 */
		private final TextView getBaseView(Context ctx) {
			Resources res = ctx.getResources();
//			GradientDrawable shape =  new GradientDrawable();
//			shape.setCornerRadius(8);
			TextView v = new TextView(ctx);
			float density = res.getDisplayMetrics().density;
			v.setText(getTranslatedName());
			v.setTextColor(res.getColor(R.color.preset_text));
			v.setTextSize(TypedValue.COMPLEX_UNIT_SP,10);
			v.setEllipsize(TextUtils.TruncateAt.END);
			v.setMaxLines(2);
			v.setPadding((int)(4*density), (int)(4*density), (int)(4*density), (int)(4*density));
			// v.setBackgroundDrawable(shape);
			if (this instanceof PresetGroup) {
				v.setBackgroundColor(res.getColor(R.color.dark_grey));
			} else {
				v.setBackgroundColor(res.getColor(R.color.preset_bg));
			}
			Drawable icon = getIcon();
			if (icon != null) {
				v.setCompoundDrawables(null, icon, null, null);
				v.setCompoundDrawablePadding((int)(4*density));
			} else {
				// no icon, shouldn't happen anymore leave in logging for now
				Log.d(DEBUG_TAG,"No icon for " + getName());
			}
			v.setWidth((int)(72*density));
			v.setHeight((int)(72*density));
			v.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
			return v;
		}
		
		/**
		 * Returns a view representing this element (i.e. a button with icon and name)
		 * Implement this in subtypes
		 * @param handler handler to handle clicks on the element (may be null)
		 * @return a view ready to display to represent this element
		 */
		public abstract View getView(Context ctx, final PresetClickHandler handler);
		
		public boolean appliesTo(ElementType type) {
			switch (type) {
				case NODE: return appliesToNode;
				case WAY: return appliesToWay;
				case CLOSEDWAY: return appliesToClosedway;
				case RELATION: return appliesToRelation;
			}
			return true; // should never happen
		}

		/**
		 * Recursivly sets the flag indicating that this element is relevant for nodes
		 */
		protected void setAppliesToNode() {
			if (!appliesToNode) {
				appliesToNode = true;
				if (parent != null) parent.setAppliesToNode();
			}
		}
		
		/**
		 * Recursivly sets the flag indicating that this element is relevant for nodes
		 */
		protected void setAppliesToWay() {
			if (!appliesToWay) {
				appliesToWay = true;
				if (parent != null) parent.setAppliesToWay();
			}
		}
		
		/**
		 * Recursivly sets the flag indicating that this element is relevant for nodes
		 */
		protected void setAppliesToClosedway() {
			if (!appliesToClosedway) {
				appliesToClosedway = true;
				if (parent != null) parent.setAppliesToClosedway();
			}
		}
		
		/**
		 * Recursivly sets the flag indicating that this element is relevant for nodes
		 */
		protected void setAppliesToRelation() {
			if (!appliesToRelation) {
				appliesToRelation = true;
				if (parent != null) parent.setAppliesToRelation();
			}
		}
		
		protected void setMapFeatures(String url) {
			if (url != null) {
				mapFeatures = url;
			}
		}
		
		public Uri getMapFeatures() {
			return Uri.parse(mapFeatures);
		}
		
		protected void setNameContext(String context) {
			nameContext = context;
		}
		
		@Override
		public String toString() {
			return name + " " + iconpath + " " + mapiconpath + " " + appliesToWay + " " + appliesToNode + " " + appliesToClosedway + " " + appliesToRelation;
		}
	}
	
	/**
	 * Represents a separator in a preset group
	 */
	public class PresetSeparator extends PresetElement {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public PresetSeparator(PresetGroup parent) {
			super(parent, "", null);
		}

		@Override
		public View getView(Context ctx, PresetClickHandler handler) {
			View v = new View(ctx);
			v.setMinimumHeight(1);
			v.setMinimumWidth(99999); // for WrappingLayout
			return v;
		}
	}
	
	/**
	 * Represents a preset group, which may contain items, groups and separators
	 */
	public class PresetGroup extends PresetElement {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		/** Elements in this group */
		private ArrayList<PresetElement> elements = new ArrayList<PresetElement>();
		
		
		public PresetGroup(PresetGroup parent, String name, String iconpath) {
			super(parent,name,iconpath);
		}

		public void addElement(PresetElement element) {
			elements.add(element);
		}
		
		public ArrayList<PresetElement> getElements() {
			return elements;
		}
		
		/**
		 * Returns a view showing this group's icon
		 * @param handler the handler handling clicks on the icon
		 */
		@Override
		public View getView(Context ctx, final PresetClickHandler handler) {
			TextView v = super.getBaseView(ctx);
			v.setTypeface(null,Typeface.BOLD);
			if (handler != null) {
				v.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						handler.onGroupClick(PresetGroup.this);
					}
				});
			}
			return v;
		}
		
		/**
		 * @return a view showing the content (nodes, subgroups) of this group
		 */
		public View getGroupView(Context ctx, PresetClickHandler handler, ElementType type) {
			ScrollView scrollView = new ScrollView(ctx);		
			scrollView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			return getGroupView(ctx, scrollView, handler, type);
		}
		
		/**
		 * @return a view showing the content (nodes, subgroups) of this group
		 */
		public View getGroupView(Context ctx, ScrollView scrollView, PresetClickHandler handler, ElementType type) {
			scrollView.removeAllViews();
			WrappingLayout wrappingLayout = new WrappingLayout(ctx);
			float density = ctx.getResources().getDisplayMetrics().density;
			// wrappingLayout.setBackgroundColor(ctx.getResources().getColor(android.R.color.white));
			wrappingLayout.setBackgroundColor(ctx.getResources().getColor(android.R.color.transparent)); // make transparent
			wrappingLayout.setHorizontalSpacing((int)(SPACING*density));
			wrappingLayout.setVerticalSpacing((int)(SPACING*density));
			ArrayList<PresetElement> filteredElements = type == null ? elements : filterElements(elements, type);
			ArrayList<View> childViews = new ArrayList<View>();
			for (PresetElement element : filteredElements) {
				childViews.add(element.getView(ctx, handler));
			}
			wrappingLayout.setWrappedChildren(childViews);
			scrollView.addView(wrappingLayout);
			return scrollView;
		}

	}
	
	/** Represents a preset item (e.g. "footpath", "grocery store") */
	public class PresetItem extends PresetElement {
		/**
		 * 
		 */
		private static final long serialVersionUID = 5L;

		/** "fixed" tags, i.e. the ones that have a fixed key-value pair */
		private LinkedHashMap<String, StringWithDescription> fixedTags = new LinkedHashMap<String, StringWithDescription>();
		
		/** Tags that are not in the optional section, but do not have a fixed key-value-pair.
		 *  The map key provides the key, while the map value (String[]) provides the possible values. */
		private LinkedHashMap<String, StringWithDescription[]> recommendedTags = new LinkedHashMap<String, StringWithDescription[]>();
		
		/** Tags that are in the optional section.
		 *  The map key provides the key, while the map value (String[]) provides the possible values. */
		private LinkedHashMap<String, StringWithDescription[]> optionalTags = new LinkedHashMap<String, StringWithDescription[]>();
		
		/**
		 * Hints to be displayed in a suitable form
		 */
		private LinkedHashMap<String, String> hints = new LinkedHashMap<String, String>();
		
		/**
		 * Default values
		 */
		private LinkedHashMap<String, String> defaults = new LinkedHashMap<String, String>();
		
		/**
		 * Roles
		 */
		private LinkedList<String> roles =  new LinkedList<String>();
		
		/**
		 * Linked names of presets
		 */
		private LinkedList<String> linkedPresetNames = new LinkedList<String>();
		
		/**
		 * Key to key type
		 */
		private HashMap<String,PresetKeyType> keyType = new HashMap<String,PresetKeyType>(); 
		
		/**
		 * Translation contexts
		 */
		private String nameContext = null;
		private String valueContext = null;
		
		
		/**
		 * true if a chunk
		 */
		private boolean chunk = false;
		
		private int itemIndex;

		public PresetItem(PresetGroup parent, String name, String iconpath, String types) {
			super(parent, name, iconpath);
			if (types == null) {
				// Type not specified, assume all types
				setAppliesToNode();
				setAppliesToWay();
				setAppliesToClosedway();
				setAppliesToRelation();
			} else {
				String[] typesArray = types.split(",");
				for (String type : typesArray) {
					if (Node.NAME.equals(type)) setAppliesToNode();
					else if (Way.NAME.equals(type)) setAppliesToWay();
					else if ("closedway".equals(type)) setAppliesToClosedway();
					else if (Relation.NAME.equals(type)) setAppliesToRelation();
				}
			}	
			itemIndex = allItems.size();
			allItems.add(this);
		}

		/**
		 * build the search index
		 */
		void buildSearchIndex() {
			addToSearchIndex(name);
			if (parent != null) {
				String parentName = parent.getName();
				if (parentName != null && parentName.length() > 0) {
					addToSearchIndex(parentName);
				}
			}
			for (String k:fixedTags.keySet()) {
				addToSearchIndex(k);
				addToSearchIndex(fixedTags.get(k).getValue());
				addToSearchIndex(fixedTags.get(k).getDescription());
			}
		}
		
		/**
		 * Add a name, any translation and the individual words to the index.
		 * Currently we assume that all words are significant
		 * @param term
		 */
		void addToSearchIndex(String term) {
			// search support
			if (term != null) {
				String normalizedName = SearchIndexUtils.normalize(term);
				searchIndex.add(normalizedName,this);
				String words[] = normalizedName.split(" ");
				if (words.length > 1) {
					for (String w:words) {
						searchIndex.add(w,this);
					}
				}
				if (po != null) { // and any translation
					String normalizedTranslatedName = SearchIndexUtils.normalize(po.t(term));
					translatedSearchIndex.add(normalizedTranslatedName,this);
					String translastedWords[] = normalizedName.split(" ");
					if (translastedWords.length > 1) {
						for (String w:translastedWords) {
							translatedSearchIndex.add(w,this);
						}
					}
				}
			}
		}
		
		/**
		 * Adds a fixed tag to the item, registers the item in the tagItems map and populates autosuggest.
		 * @param key key name of the tag
		 * @param value value of the tag
		 */
		public void addTag(final String key, final PresetKeyType type, String value, String text) {
			if (key == null) throw new NullPointerException("null key not supported");
			if (value == null) value = "";
			if (text != null && po != null) {
				text = po.t(text);
			}
			fixedTags.put(key, new StringWithDescription(value, text));
			if (!chunk) {
				tagItems.add(key+"\t"+value, this);
			}
			// Log.d(DEBUG_TAG,name + " key " + key + " type " + type);
			keyType.put(key,type);
			if (appliesTo(ElementType.NODE)) autosuggestNodes.add(key, value.length() > 0 ? new StringWithDescription(value, text) : null);
			if (appliesTo(ElementType.WAY)) autosuggestWays.add(key, value.length() > 0 ? new StringWithDescription(value, text) : null);
			if (appliesTo(ElementType.CLOSEDWAY)) autosuggestClosedways.add(key, value.length() > 0 ? new StringWithDescription(value, text) : null);
			if (appliesTo(ElementType.RELATION)) autosuggestRelations.add(key, value.length() > 0 ? new StringWithDescription(value, text) : null);
		}
		
		/**
		 * Adds a recommended or optional tag to the item and populates autosuggest.
		 * @param optional true if optional, false if recommended
		 * @param key key name of the tag
		 * @param values values string from the XML (comma-separated list of possible values)
		 */
		public void addTag(boolean optional, String key, PresetKeyType type, String values) {
			addTag(optional, key, type, values, ",");
		}
		
		public void addTag(boolean optional, String key, PresetKeyType type, String values, String seperator) {
			String[] valueArray = (values == null) ? new String[0] : values.split(Pattern.quote(seperator));
			StringWithDescription[] valuesWithDesc = new StringWithDescription[valueArray.length];
			for (int i=0;i<valueArray.length;i++){
				valuesWithDesc[i] = new StringWithDescription(valueArray[i]);
			}
			addTag(optional, key, type, valuesWithDesc);
		}
		
		public void addTag(boolean optional, String key, PresetKeyType type, StringWithDescription[] valueArray) {
		    if (!chunk){
		    	for (StringWithDescription v:valueArray) {
		    		tagItems.add(key+"\t"+v.getValue(), this);
		    	}
		    }
			// Log.d(DEBUG_TAG,name + " key " + key + " type " + type);
			keyType.put(key,type);
			if (appliesTo(ElementType.NODE)) autosuggestNodes.add(key, valueArray);
			if (appliesTo(ElementType.WAY)) autosuggestWays.add(key, valueArray);
			if (appliesTo(ElementType.CLOSEDWAY)) autosuggestClosedways.add(key, valueArray);
			if (appliesTo(ElementType.RELATION)) autosuggestRelations.add(key, valueArray);
			
			(optional ? optionalTags : recommendedTags).put(key, valueArray);
		}
		
		public void addRole(String value)
		{
			roles.add(value);
		}
		
		/**
		 * Save hint for the tag
		 * @param key
		 * @param hint
		 */
		public void addHint(String key, String hint) {
			hints.put(key, hint);
		}
		
		/**
		 * Return, potentially translated, "text" field from preset
		 * @param key
		 * @return
		 */
		public String getHint(String key) {
			if (po != null) {
				return po.t(hints.get(key));
			}
			return hints.get(key);
		}

		/**
		 * Save default for the tag
		 * @param key
		 * @param defaultValue
		 */
		public void addDefault(String key, String defaultValue) {
			defaults.put(key, defaultValue);
			
		}
		
		public String getDefault(String key) {
			return defaults.get(key);
		}
		
		public void addLinkedPresetName(String presetName) {
			linkedPresetNames.add(presetName);
		}
		
		/**
		 * @return the fixed tags belonging to this item (unmodifiable)
		 */
		public Map<String,StringWithDescription> getFixedTags() {
			return Collections.unmodifiableMap(fixedTags);
		}
		
		/**
		 * Return the number of keys with fixed values
		 * @return
		 */
		public int getFixedTagCount() {
			return fixedTags.size();
		}
		
		public boolean isFixedTag(String key) {
			return fixedTags.containsKey(key);
		}
		
		public boolean isRecommendedTag(String key) {
			return recommendedTags.containsKey(key);
		}
		
		public boolean isOptionalTag(String key) {
			return optionalTags.containsKey(key);
		}
		
		public Map<String,StringWithDescription[]> getRecommendedTags() {
			return Collections.unmodifiableMap(recommendedTags);
		}

		public Map<String,StringWithDescription[]> getOptionalTags() {
			return Collections.unmodifiableMap(optionalTags);
		}
		
		public List<String> getRoles() {
			return Collections.unmodifiableList(roles);
		}
		
		/**
		 * Return a ist of the values suitable for autocomplete, note vales for fixed tags are not returned
		 * @param key
		 * @return
		 */
		public Collection<StringWithDescription> getAutocompleteValues(String key) {
			Collection<StringWithDescription> result = new HashSet<StringWithDescription>();
			if (recommendedTags.containsKey(key)) {
				result.addAll(Arrays.asList(recommendedTags.get(key)));
			} else if (optionalTags.containsKey(key)) {
				result.addAll(Arrays.asList(optionalTags.get(key)));
			}
			return result;
		}
		
		/**
		 * Return what kind of selection applies to the values of this key
		 * @param key
		 * @return
		 */
		public PresetKeyType getKeyType(String key) {
			PresetKeyType result = keyType.get(key);
			if (result==null) {
				return PresetKeyType.TEXT;
			}
			return result;
		}
		
		/**
		 * Checks if all tags belonging to this item exist in the given tagSet,
		 * i.e. the node to which the tagSet belongs could be what this preset specifies.
		 * @param tagSet the tagSet to compare against this item
		 * @return
		 */
		public boolean matches(Map<String,String> tagSet) {
			for (Entry<String, StringWithDescription> tag : fixedTags.entrySet()) { // for each own tag
				String otherTagValue = tagSet.get(tag.getKey());
				if (otherTagValue == null || !tag.getValue().equals(otherTagValue)) return false;
			}
			return true;
		}
		
		/**
		 * Returns the number of matches between the list of recommended tags (really a misnomer) and the provided tags
		 * @param tagSet
		 * @return number of matches
		 */
		public int matchesRecommended(Map<String,String> tagSet) {
			int matches = 0;
			for (Entry<String, StringWithDescription[]> tag : recommendedTags.entrySet()) { // for each own tag
				String otherTagValue = tagSet.get(tag.getKey());
				if (otherTagValue != null) {
					for (StringWithDescription v:tag.getValue()) {
						if (v.equals(otherTagValue)) {
							matches++;
							break;
						}
					}
				}
			}
			return matches;
		}
		

		@Override
		public View getView(Context ctx, final PresetClickHandler handler) {
			View v = super.getBaseView(ctx);
			if (handler != null) {
				v.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						handler.onItemClick(PresetItem.this);
					}
					
				});
				v.setOnLongClickListener(new OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						return handler.onItemLongClick(PresetItem.this);
					}	
				});
			}
			return v;
		}

		public int getItemIndex() {
			return itemIndex;
		}
		
		@Override
		public String toString() {
			String tagStrings = "";
			tagStrings = " required: ";
			for (String k:fixedTags.keySet()) {
				tagStrings = tagStrings + " " + k + "=" + fixedTags.get(k);
			}
			tagStrings = tagStrings + " recommended: ";
			for (String k:recommendedTags.keySet()) {
				tagStrings = tagStrings + " " + k + "="; 
				for (StringWithDescription v:recommendedTags.get(k)) {
					tagStrings = tagStrings + " " + v.getValue();
				}
			}
			tagStrings = tagStrings + " optional: ";
			for (String k:optionalTags.keySet()) {
				tagStrings = tagStrings + " " + k + "=";
				for (StringWithDescription v:optionalTags.get(k)) {
					tagStrings = tagStrings + " " + v.getValue();
				}
			}
			return super.toString() + tagStrings;
		}
	
		protected void setChunk() {
			chunk = true;
		}
		
		protected boolean isChunk() {
			return chunk;
		}
		
		public String toJSON() {
			String jsonString = "";
			for (String k:fixedTags.keySet()) {
				jsonString = jsonString + tagToJSON(k, null);
				jsonString = jsonString + tagToJSON(k, fixedTags.get(k).getValue());
			}
			for (String k:recommendedTags.keySet()) {
				jsonString = jsonString + tagToJSON(k, null);
				for (StringWithDescription v:recommendedTags.get(k)) {
					jsonString = jsonString + tagToJSON(k, v.getValue());
				}
			}
			for (String k:optionalTags.keySet()) {
				jsonString = jsonString + tagToJSON(k, null);
				for (StringWithDescription v:optionalTags.get(k)) {
					jsonString = jsonString + tagToJSON(k, v.getValue());
				}
			}
			return jsonString;
		}
		
		private String tagToJSON(String key, String value) {
			String result = "{ \"key\": \"" + key + "\"" + (value == null ? "" : ", \"value\": \"" + value + "\"");
			result = result + " , \"object_types\": [";
			if (appliesToNode) {
				result = result + "\"node\"";
			}
			if (appliesToRelation) {
				if (appliesToNode) {
					result = result + ",";
				}
				result = result + "\"node\"";
			}
			if (appliesToWay || appliesToClosedway) {
				if (appliesToRelation) {
					result = result + ",";
				}
				result = result + "\"way\"";
			}			
			return  result + "]},\n";
		}
	}
	
	
	/**
	 * Adapter providing the preset elements in this group
	 * currently unused, left here in case it is later needed
	 */
	@SuppressWarnings("unused")
	private class PresetGroupAdapter extends BaseAdapter {
	
		private final ArrayList<PresetElement> elements;
		private PresetClickHandler handler;
		private final Context context;
		
		private PresetGroupAdapter(Context ctx, ArrayList<PresetElement> content, ElementType type, PresetClickHandler handler) {
			this.handler = handler;
			context = ctx;
			elements = filterElements(content, type);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getItem(position).getView(context, handler);
		}
		
		@Override
		public int getCount() {
			return elements.size();
		}
	
		@Override
		public PresetElement getItem(int position) {
			return elements.get(position);
		}
	
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public boolean isEnabled(int position) {
			return !(getItem(position) instanceof PresetSeparator);
		}
	}

	/** Interface for handlers handling clicks on item or group icons */
	public interface PresetClickHandler {
		public void onItemClick(PresetItem item);
		public boolean onItemLongClick(PresetItem item);
		public void onGroupClick(PresetGroup group);
	}

	static public Collection<String> getAutocompleteKeys(Preset[] presets, ElementType type) {
		Collection<String> result = new HashSet<String>();
		for (Preset p:presets) {
			if (p!=null) {
				switch (type) {
				case NODE: result.addAll(p.autosuggestNodes.getKeys()); break;
				case WAY: result.addAll(p.autosuggestWays.getKeys()); break;
				case CLOSEDWAY: result.addAll(p.autosuggestClosedways.getKeys()); break;
				case RELATION: result.addAll(p.autosuggestRelations.getKeys()); break;
				default: return null; // should never happen, all cases are covered
				}
			}
		}
		List<String> r = new ArrayList<String>(result);
		Collections.sort(r);
		return r; 
	}
	
	static public Collection<StringWithDescription> getAutocompleteValues(Preset[] presets, ElementType type, String key) {
		Collection<StringWithDescription> result = new HashSet<StringWithDescription>();
		for (Preset p:presets) {
			if (p!=null) {
				switch (type) {
				case NODE: result.addAll(p.autosuggestNodes.get(key)); break;
				case WAY: result.addAll(p.autosuggestWays.get(key)); break;
				case CLOSEDWAY: result.addAll(p.autosuggestClosedways.get(key)); break;
				case RELATION: result.addAll(p.autosuggestRelations.get(key)); break;
				default: return Collections.emptyList();
				}
			}
		}
		List<StringWithDescription> r = new ArrayList<StringWithDescription>(result);
		Collections.sort(r);
		return r;
	}
	
	static public MultiHashMap<String, PresetItem> getSearchIndex(Preset[] presets) {
		MultiHashMap<String, PresetItem> result = new MultiHashMap<String, PresetItem>();
		for (Preset p:presets) {
			if (p != null) {
				result.addAll(p.searchIndex);
			}
		}
		return result;
	}
	
	static public MultiHashMap<String, PresetItem> getTranslatedSearchIndex(Preset[] presets) {
		MultiHashMap<String, PresetItem> result = new MultiHashMap<String, PresetItem>();
		for (Preset p:presets) {
			if (p!=null) {
				result.addAll(p.translatedSearchIndex);
			}
		}
		return result;
	}
}

