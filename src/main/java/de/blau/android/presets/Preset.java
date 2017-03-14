package de.blau.android.presets;

import java.io.BufferedOutputStream;
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
import java.io.PrintStream;
import java.io.Serializable;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
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
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.PresetEditorActivity;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.FileUtil;
import de.blau.android.util.Hash;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SearchIndexUtils;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.Util;
import de.blau.android.util.collections.MultiHashMap;
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
	private static final long serialVersionUID = 7L;
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
	private final MultiHashMap<String, PresetItem> tagItems = new MultiHashMap<String, PresetItem>();

	/** The root group of the preset, containing all top-level groups and items */
	private PresetGroup rootGroup;

	/** {@link PresetIconManager} used for icon loading */
	private transient PresetIconManager iconManager;
	
	/** all known preset items in order of loading */
	private ArrayList<PresetItem> allItems = new ArrayList<PresetItem>();
	
	public enum PresetKeyType {
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
	
	public enum MatchType {
		NONE,
		KEY,
		KEY_NEG,
		KEY_VALUE,
		KEY_VALUE_NEG,
	}
	
	private final static String COMBO_DELIMITER = ",";
	private final static String MULTISELECT_DELIMITER = ";";

	/** Maps all possible keys to the respective values for autosuggest (only key/values applying to nodes) */
	private final MultiHashMap<String, StringWithDescription> autosuggestNodes = new MultiHashMap<String, StringWithDescription>(true);
	/** Maps all possible keys to the respective values for autosuggest (only key/values applying to ways) */
	private final MultiHashMap<String, StringWithDescription> autosuggestWays = new MultiHashMap<String, StringWithDescription>(true);
	/** Maps all possible keys to the respective values for autosuggest (only key/values applying to closed ways) */
	private final MultiHashMap<String, StringWithDescription> autosuggestClosedways = new MultiHashMap<String, StringWithDescription>(true);
	/** Maps all possible keys to the respective values for autosuggest (only key/values applying to areas (MPs)) */
	private final MultiHashMap<String, StringWithDescription> autosuggestAreas = new MultiHashMap<String, StringWithDescription>(true);
	/** Maps all possible keys to the respective values for autosuggest (only key/values applying to closed ways) */
	private final MultiHashMap<String, StringWithDescription> autosuggestRelations = new MultiHashMap<String, StringWithDescription>(true);
	
	/** for search support */
	private final MultiHashMap<String, PresetItem> searchIndex = new MultiHashMap<String, PresetItem>();
	private final MultiHashMap<String, PresetItem> translatedSearchIndex = new MultiHashMap<String, PresetItem>();
		
	private Po po = null;
	
	/**
	 * Serializable class for storing Most Recently Used information.
	 * Hash is used to check compatibility.
	 */
	protected static class PresetMRUInfo implements Serializable {
		private static final long serialVersionUID = 7708132207266548489L;

		PresetMRUInfo(String presetHash) {
			this.presetHash = presetHash;
		}
		
		/** hash of current preset (used to check validity of recentPresets indexes) */
		final String presetHash;
	
		/** indexes of recently used presets (for use with allItems) */
		LinkedList<Integer> recentPresets = new LinkedList<Integer>();

		public volatile boolean changed = false;
	}
	private final PresetMRUInfo mru;
	private String externalPackage;
	
	private static class PresetFilter implements FilenameFilter {
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

		//noinspection ResultOfMethodCallIgnored
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
				} else {
					Log.e("Preset","Can't find preset file" );
				}
			} else {
				Log.e("Preset","Can't open preset directory " + directory.toString());
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
	
	private PresetIconManager getIconManager(Context ctx) {
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
        	private String valuesContext = null;
        	private String delimiter = null;
        	
        	{
        		groupstack.push(rootGroup);
        	}
        	
            /** 
             * ${@inheritDoc}.
             */
			@Override
			public void startElement(String name, AttributeList attr) throws SAXException {
				if ("presets".equals(name)) {
					// do nothing for now
				} else if ("group".equals(name)) {
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
					currentItem.setDeprecated("true".equals(attr.getValue("deprecated")));
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
				} else if (currentItem != null) { // the following only make sense if we actually found an item
					if ("optional".equals(name)) {
						inOptionalSection = true;
					} else if ("key".equals(name)) {
						String key = attr.getValue("key");
						String match = attr.getValue("match");
						if (!inOptionalSection) {
							if ("none".equals(match)) {// don't include in fixed tags if not used for matching
								currentItem.addTag(false, key, PresetKeyType.TEXT, attr.getValue("value"));
							} else {
								currentItem.addTag(key, PresetKeyType.TEXT, attr.getValue("value"), attr.getValue("text"));
							}
						} else {
							// Optional fixed tags should not happen, their values will NOT be automatically inserted.
							currentItem.addTag(true, key, PresetKeyType.TEXT, attr.getValue("value"));
						}
						if (match != null) {
							currentItem.setMatchType(key,match);
						}
						String textContext = attr.getValue("text_context");
						if (textContext != null) {
							currentItem.setTextContext(key,textContext);
						}
					} else if ("text".equals(name)) {
						String key = attr.getValue("key");
						currentItem.addTag(inOptionalSection, key, PresetKeyType.TEXT, (String)null);
						String text = attr.getValue("text");
						if (text != null) {
							currentItem.addHint(attr.getValue("key"),text);
						}
						String defaultValue = attr.getValue("default");
						if (defaultValue != null) {
							currentItem.addDefault(key,defaultValue);
						}
						String textContext = attr.getValue("text_context");
						if (textContext != null) {
							currentItem.setTextContext(key,textContext);
						}
						String match = attr.getValue("match");
						if (match != null) {
							currentItem.setMatchType(key,match);
						}
						String javaScript = attr.getValue("javascript");
						if (javaScript != null) {
							currentItem.setJavaScript(key,javaScript);
						}
					} else if ("link".equals(name)) {
						String language = Locale.getDefault().getLanguage();
						String href = attr.getValue(language.toLowerCase(Locale.US)+".href");
						if (href==null) {
							href = attr.getValue("href");
						}
						if (href!=null) {
							currentItem.setMapFeatures(href);
						}
					} else if ("check".equals(name)) {
						String key = attr.getValue("key");
						String value_on = attr.getValue("value_on") == null ? "yes" : attr.getValue("value_on");
						String value_off = attr.getValue("value_off") == null ? "no" : attr.getValue("value_off");
						String disable_off = attr.getValue("disable_off");
						String values = value_on;
						// zap value_off if disabled
						if (disable_off != null && disable_off.equals("true")) {
							value_off = "";
						} else {
							values = value_on + COMBO_DELIMITER + value_off;
						}
						String displayValues = ""; //FIXME this is a bit of a hack as there is no display_values attribute for checks
						boolean first = true;
						for (String v:values.split(COMBO_DELIMITER)) {
							if (!first) {
								displayValues = displayValues + COMBO_DELIMITER;
							} else {
								first = false;
							}
							displayValues = displayValues + Util.capitalize(v);
						}
						currentItem.setSort(key,false); // don't sort
						currentItem.addTag(inOptionalSection, key, PresetKeyType.CHECK, values, displayValues, null, COMBO_DELIMITER, null);
						if (!"yes".equals(value_on)) {
							currentItem.addOnValue(key,value_on);
						}
						String defaultValue = attr.getValue("default") == null ? value_off : (attr.getValue("default").equals("on") ? value_on : value_off);
						if (defaultValue != null) {
							currentItem.addDefault(key,defaultValue);
						}
						String text = attr.getValue("text");
						if (text != null) {
							currentItem.addHint(key,text);
						}
						String textContext = attr.getValue("text_context");
						if (textContext != null) {
							currentItem.setTextContext(key,textContext);
						}
						String match = attr.getValue("match");
						if (match != null) {
							currentItem.setMatchType(key,match);
						}
					} else if ("combo".equals(name) || "multiselect".equals(name)) {
						boolean multiselect = "multiselect".equals(name);
						String key = attr.getValue("key");
						delimiter = attr.getValue("delimiter");
						if (delimiter == null) {
							delimiter = multiselect ? MULTISELECT_DELIMITER : COMBO_DELIMITER; 
						}
						String values = attr.getValue("values");
						String displayValues = attr.getValue("display_values");
						String shortDescriptions = attr.getValue("short_descriptions");
						valuesContext = attr.getValue("values_context");
						if (values != null) {
							currentItem.addTag(inOptionalSection, key, multiselect ? PresetKeyType.MULTISELECT : PresetKeyType.COMBO, 
									values, displayValues, shortDescriptions, delimiter, valuesContext);
						} else {
							listKey = key;
							listValues = new ArrayList<StringWithDescription>();
						}
						String defaultValue = attr.getValue("default");
						if (defaultValue != null) {
							currentItem.addDefault(key,defaultValue);
						}
						String text = attr.getValue("text");
						if (text != null) {
							currentItem.addHint(key, text);
						}
						String textContext = attr.getValue("text_context");
						if (textContext != null) {
							currentItem.setTextContext(key,textContext);
						}
						String match = attr.getValue("match");
						if (match != null) {
							currentItem.setMatchType(key,match);
						}
						String sort = attr.getValue("values_sort");
						if (sort != null) {
							currentItem.setSort(key,"yes".equals(sort) || "true".equals(sort)); // normally this will not be set because true is the default
						}
						String editable = attr.getValue("editable");
						if (editable != null) {
							currentItem.setEditable(key,"yes".equals(editable) || "true".equals(editable));
						}
					} else if ("role".equals(name)) {
						String key = attr.getValue("key");
						String text = attr.getValue("text");
						String textContext = attr.getValue("text_context");
						if (textContext != null) {
							currentItem.setTextContext(key,textContext);
						}
						currentItem.addRole(new StringWithDescription(key, po != null && text != null ? (textContext!=null?po.t(textContext,text):po.t(text)) : text));
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
							currentItem.addAllDefaults(chunk.defaults);
							currentItem.keyType.putAll(chunk.keyType);
							currentItem.setAllMatchTypes(chunk.matchType);
							currentItem.addAllRoles(chunk.roles); // FIXME this and the following could lead to duplicate entries
							currentItem.addAllLinkedPresetNames(chunk.linkedPresetNames);
							currentItem.setAllSort(chunk.sort);
							currentItem.setAllJavaScript(chunk.javascript);
							currentItem.setAllEditable(chunk.editable);
							currentItem.addAllDelimiters(chunk.delimiters);
						}
					} else if ("list_entry".equals(name)) {
						if (listValues != null) {
							String v = attr.getValue("value");
							if (v != null) {
								String d = attr.getValue("display_value");
								if (d == null) {
									d = attr.getValue("short_description");
								}
								listValues.add(new StringWithDescription(v,po != null ? (valuesContext != null?po.t(valuesContext,d):po.t(d)):d));
							}
						}
					} else if ("preset_link".equals(name)) {
						String presetName = attr.getValue("preset_name");
						if (presetName != null) {
							currentItem.addLinkedPresetName(presetName);
						}
					}
				} else {
					Log.d(DEBUG_TAG, name + " must be in a preset item");
					throw new SAXException(name + " must be in a preset item");
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
            		if (!currentItem.isDeprecated()) {
            			currentItem.buildSearchIndex();
            		}
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
            					listValues.toArray(v), delimiter);
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
			try { 
				if (mruReader != null) {
					mruReader.close(); 
				}
			} catch (Exception ignored) {
				Log.d(DEBUG_TAG, "Ignored " + ignored);
			} 
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
		File[] list = presetDir.listFiles(new PresetFilter());
		String presetFilename = null;
		if (list != null) {
			if (list.length > 0) { // simply use the first XML file found
				presetFilename = list[0].getName();
			}
		} else {
			return null;
		}
		try {
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			
	        saxParser.parse(new File(presetDir, presetFilename), new HandlerBase() {
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
	private Integer getItemIndexByName(String name) {
		Log.d("Preset","getItemIndexByName " + name);
		for (PresetItem pi:allItems) {
			if (pi != null) {
				String n = pi.getName();
				if (n != null && n.equals(name)) {
					return Integer.valueOf(pi.getItemIndex());
				}
			}
		}
		Log.d("Preset","getItemIndexByName " + name + " not found");
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
			if (pi.getLinkedPresetNames() != null) {
				LinkedList<String>linkedPresetNames = new LinkedList<String>(pi.getLinkedPresetNames());
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
	
    
    private String toJSON() {
    	String result = "";
    	for (PresetItem pi:allItems) {
    		if (!pi.isChunk()) {
    			result = result + pi.toJSON();
    		}
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
    	return findBestMatch(presets, tags, false);
    }
    
    static public PresetItem findBestMatch(Preset presets[], Map<String,String> tags, boolean useAddressKeys) {
		int bestMatchStrength = 0;
		PresetItem bestMatch = null;
		
		if (tags==null || presets==null) {
			Log.e(DEBUG_TAG, "findBestMatch " + (tags==null?"tags null":"presets null"));
			return null;
		}
		
		// Build candidate list
		LinkedHashSet<PresetItem> possibleMatches = new LinkedHashSet<PresetItem>();
		boolean reallyUseAddressKeys = false;
		do {
			for (Preset p:presets) {
				if (p != null) {
					for (Entry<String, String> tag : tags.entrySet()) {
						String kew = tag.getKey();
						if (!kew.startsWith(Tags.KEY_ADDR_BASE) || reallyUseAddressKeys) {
							String tagString = tag.getKey()+"\t";
							possibleMatches.addAll(p.tagItems.get(tagString)); // for stuff that doesn't have fixed values
							possibleMatches.addAll(p.tagItems.get(tagString+tag.getValue()));
						}
					}
				}
			}

			// if we only have address keys retry
			if (useAddressKeys && possibleMatches.size() == 0 && !reallyUseAddressKeys) {
				reallyUseAddressKeys = true;
			}  else {
				break;
			}
		} while (true);
		
		// Find best
		final int FIXED_WEIGHT = 100; // always prioritize presets with fixed keys
		for (PresetItem possibleMatch : possibleMatches) {
			int fixedTagCount = possibleMatch.getFixedTagCount()*FIXED_WEIGHT;
			if (fixedTagCount + possibleMatch.getRecommendedTags().size() < bestMatchStrength) continue; // isn't going to help
			int matches = 0;
			if (fixedTagCount > 0) { // has required tags
				if (possibleMatch.matches(tags)) {
					matches = fixedTagCount;
				}
			} 
			if (possibleMatch.getRecommendedTags().size() > 0) { 
				matches = matches + possibleMatch.matchesRecommended(tags);
			}
			if (matches > bestMatchStrength) {
				bestMatch = possibleMatch;
				bestMatchStrength = matches;
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
			if (!e.isDeprecated()) {
				if (e.appliesTo(type)) {
					filteredElements.add(e);
				} else if ((e instanceof PresetSeparator) && !filteredElements.isEmpty() &&
						!(filteredElements.get(filteredElements.size()-1) instanceof PresetSeparator)) {
					// add separators if there is a non-separator element above them
					filteredElements.add(e);
				}
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
		private static final long serialVersionUID = 5L;
		String name;
		String nameContext = null;
		private String iconpath;
		private String mapiconpath;
		private transient Drawable icon;
		private transient BitmapDrawable mapIcon;
		PresetGroup parent;
		boolean appliesToWay;
		boolean appliesToNode;
		boolean appliesToClosedway;
		boolean appliesToRelation;
		boolean appliesToArea;
		private boolean deprecated = false;
		private String region = null;
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
					iconManager = getIconManager(App.mainActivity);
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
					iconManager = getIconManager(App.mainActivity);
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
		private TextView getBaseView(Context ctx) {
			Resources res = ctx.getResources();
//			GradientDrawable shape =  new GradientDrawable();
//			shape.setCornerRadius(8);
			TextView v = new TextView(ctx);
			float density = res.getDisplayMetrics().density;
			v.setText(getTranslatedName());
			v.setTextColor(ContextCompat.getColor(ctx,R.color.preset_text));
			v.setTextSize(TypedValue.COMPLEX_UNIT_SP,10);
			v.setEllipsize(TextUtils.TruncateAt.END);
			v.setMaxLines(2);
			v.setPadding((int)(4*density), (int)(4*density), (int)(4*density), (int)(4*density));
			// v.setBackgroundDrawable(shape);
			if (this instanceof PresetGroup) {
				v.setBackgroundColor(ContextCompat.getColor(ctx,R.color.dark_grey));
			} else {
				v.setBackgroundColor(ContextCompat.getColor(ctx,R.color.preset_bg));
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
				case AREA: return appliesToArea;
			}
			return true; // should never happen
		}

		/**
		 * Recursively sets the flag indicating that this element is relevant for nodes
		 */
		void setAppliesToNode() {
			if (!appliesToNode) {
				appliesToNode = true;
				if (parent != null) parent.setAppliesToNode();
			}
		}
		
		/**
		 * Recursively sets the flag indicating that this element is relevant for nodes
		 */
		void setAppliesToWay() {
			if (!appliesToWay) {
				appliesToWay = true;
				if (parent != null) parent.setAppliesToWay();
			}
		}
		
		/**
		 * Recursively sets the flag indicating that this element is relevant for nodes
		 */
		void setAppliesToClosedway() {
			if (!appliesToClosedway) {
				appliesToClosedway = true;
				if (parent != null) parent.setAppliesToClosedway();
			}
		}
		
		/**
		 * Recursively sets the flag indicating that this element is relevant for relations
		 */
		void setAppliesToRelation() {
			if (!appliesToRelation) {
				appliesToRelation = true;
				if (parent != null) parent.setAppliesToRelation();
			}
		}
		
		/**
		 * Recursively sets the flag indicating that this element is relevant for an area
		 */
		void setAppliesToArea() {
			if (!appliesToArea) {
				appliesToArea = true;
				if (parent != null) parent.setAppliesToArea();
			}
		}
		
		void setMapFeatures(String url) {
			if (url != null) {
				mapFeatures = url;
			}
		}
		
		public Uri getMapFeatures() {
			return Uri.parse(mapFeatures);
		}
		
		void setNameContext(String context) {
			nameContext = context;
		}
		
		public boolean isDeprecated() {
			return deprecated;
		}

		public void setDeprecated(boolean deprecated) {
			this.deprecated = deprecated;
		}

		public String getRegion() {
			return region;
		}

		public void setRegion(String region) {
			this.region = region;
		}

		@Override
		public String toString() {
			return name + " " + iconpath + " " + mapiconpath + " " + appliesToWay + " " + appliesToNode + " " + appliesToClosedway + " " + appliesToRelation + " " + appliesToArea;
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
			wrappingLayout.setBackgroundColor(ContextCompat.getColor(ctx,android.R.color.transparent)); // make transparent
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
		private static final long serialVersionUID = 10L;

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
		private HashMap<String, String> hints = new HashMap<String, String>();
		
		/**
		 * Default values
		 */
		private HashMap<String, String> defaults = null;
		
		/**
		 * Non standard on values
		 */
		private HashMap<String, String> onValue = null;
		
		/**
		 * Roles
		 */
		private LinkedList<StringWithDescription> roles = null;
		
		/**
		 * Linked names of presets
		 */
		private LinkedList<String> linkedPresetNames = null;
		
		/**
		 * Sort values or not
		 */
		private HashMap<String,Boolean> sort  = null;
		
		/**
		 * Key to key type
		 */
		private HashMap<String,PresetKeyType> keyType = new HashMap<String,PresetKeyType>(); 
		
		/**
		 * Key to match properties
		 */
		private HashMap<String,MatchType> matchType = null; 
		
		/**
		 * Key to combo and multiselect delimiters
		 */
		private HashMap<String,String> delimiters = null; 
		
		/**
		 * Key to combo and multiselect editable property
		 */
		private HashMap<String,Boolean> editable = null; 
		
		/**
		 * Translation contexts
		 */
		private HashMap<String,String> textContext = null;
		private HashMap<String,String> valueContext = null;
		
		/**
		 * Scripts for pre-filling text fields
		 */
		private HashMap<String,String> javascript = null;
		
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
				setAppliesToArea();
			} else {
				String[] typesArray = types.split(",");
				for (String type : typesArray) {
					if (Node.NAME.equals(type)) setAppliesToNode();
					else if (Way.NAME.equals(type)) setAppliesToWay();
					else if ("closedway".equals(type)) setAppliesToClosedway(); // FIXME don't add if it really an area
					else if ("multipolygon".equals(type)) setAppliesToArea();
					else if ("area".equals(type)) setAppliesToArea(); // 
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
			if (appliesTo(ElementType.AREA)) autosuggestAreas.add(key, value.length() > 0 ? new StringWithDescription(value, text) : null);			
		}
		
		/**
		 * Adds a recommended or optional tag to the item and populates autosuggest.
		 * @param optional true if optional, false if recommended
		 * @param key key name of the tag
		 * @param values values string from the XML (comma-separated list of possible values)
		 */
		public void addTag(boolean optional, String key, PresetKeyType type, String values) {
			addTag(optional, key, type, values, null, null, COMBO_DELIMITER, null);
		}
		
		public void addTag(boolean optional, String key, PresetKeyType type, String values, String displayValues, String shortDescriptions, final String delimiter, String valuesContext) {
			String[] valueArray = (values == null) ? new String[0] : values.split(Pattern.quote(delimiter));
			String[] displayValueArray = (displayValues == null) ? new String[0] : displayValues.split(Pattern.quote(delimiter));
			String[] shortDescriptionArray = (shortDescriptions == null) ? new String[0] : shortDescriptions.split(Pattern.quote(delimiter));
			StringWithDescription[] valuesWithDesc = new StringWithDescription[valueArray.length];
			boolean useDisplayValues = valueArray.length == displayValueArray.length;
			boolean useShortDescriptions = !useDisplayValues && valueArray.length == shortDescriptionArray.length;
			for (int i=0;i<valueArray.length;i++){
				String description = null;
				if (useDisplayValues) {  
					description = (po != null && displayValueArray[i] != null) ? (valuesContext != null ? po.t(valuesContext,displayValueArray[i]) : po.t(displayValueArray[i])) : displayValueArray[i];
				} else if (useShortDescriptions) {
					description = (po != null && shortDescriptionArray[i] != null) ? (valuesContext != null ? po.t(valuesContext,shortDescriptionArray[i]) : po.t(shortDescriptionArray[i])):shortDescriptionArray[i];
				}
				valuesWithDesc[i] = new StringWithDescription(valueArray[i], description);
			}
			addTag(optional, key, type, valuesWithDesc, delimiter);
		}
		
		public void addTag(boolean optional, String key, PresetKeyType type, StringWithDescription[] valueArray, final String delimiter) {
	    	if (!chunk){
		    	if (valueArray==null || valueArray.length == 0) {
		    		tagItems.add(key+"\t", this);
		    	} else {
		    		for (StringWithDescription v:valueArray) {
		    			tagItems.add(key+"\t"+v.getValue(), this);
		    		}
		    	}
		    }
			// Log.d(DEBUG_TAG,name + " key " + key + " type " + type);
			keyType.put(key,type);
			if (appliesTo(ElementType.NODE)) autosuggestNodes.add(key, valueArray);
			if (appliesTo(ElementType.WAY)) autosuggestWays.add(key, valueArray);
			if (appliesTo(ElementType.CLOSEDWAY)) autosuggestClosedways.add(key, valueArray);
			if (appliesTo(ElementType.RELATION)) autosuggestRelations.add(key, valueArray);
			if (appliesTo(ElementType.AREA)) autosuggestAreas.add(key, valueArray);
			
			(optional ? optionalTags : recommendedTags).put(key, valueArray);
			
			// only save delimiter if not default
			if ((type == PresetKeyType.MULTISELECT && !MULTISELECT_DELIMITER.equals(delimiter)) 
					|| (type == PresetKeyType.COMBO && !COMBO_DELIMITER.equals(delimiter))) {
				addDelimiter(key,delimiter);
			}
		}
		
		public void addRole(final StringWithDescription value)
		{
			if (roles == null) {
				roles =  new LinkedList<StringWithDescription>();
			}
			roles.add(value);
		}

		public void addAllRoles(LinkedList<StringWithDescription> newRoles)
		{
			if (roles == null) { 
				roles = newRoles; // doesn't matter if newRoles is null
			} else if (newRoles != null){
				roles.addAll(newRoles);
			}
		}
		
		public List<StringWithDescription> getRoles() {
			return roles != null ? Collections.unmodifiableList(roles) : null;
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
			if (defaults == null) {
				defaults = new HashMap<String, String>();
			}
			defaults.put(key, defaultValue);
		}
		
		public void addAllDefaults(HashMap<String, String> newDefaults)
		{
			if (defaults == null) { 
				defaults = newDefaults; // doesn't matter if newDefaults is null
			} else if (newDefaults != null){
				defaults.putAll(newDefaults);
			}
		}
		
		public String getDefault(String key) {
			return defaults != null ? defaults.get(key) : null;
		}
		
		/**
		 * Save non-standard values for the tag
		 * @param key
		 * @param on
		 */
		public void addOnValue(String key, String on) {
			if (onValue == null) {
				onValue = new HashMap<String, String>();
			}
			onValue.put(key, on);
		}
		
		public void addAllOnValues(HashMap<String, String> newOnValues)
		{
			if (onValue == null) { 
				onValue = newOnValues; // doesn't matter if newOnValues is null
			} else if (newOnValues != null){
				onValue.putAll(newOnValues);
			}
		}
		
		/**
		 * Get the value that should be used for a checked check box
		 * @param key the key for the checkbox
		 * @return either default value or what has been set in the preset
		 */
		public String getOnValue(String key) {
			if (onValue != null) {
				return onValue.get(key) != null ? onValue.get(key) : "yes";
			}
			return "yes";
		}
		
		/**
		 * Save non-standard values for the tag
		 * @param key
		 * @param on
		 */
		public void addDelimiter(String key, String delimiter) {
			if (delimiters == null) {
				delimiters = new HashMap<String,String>();
			}
			delimiters.put(key, delimiter);
		}
		
		public void addAllDelimiters(HashMap<String, String> newDelimiters)
		{
			if (delimiters == null) { 
				delimiters = newDelimiters; // doesn't matter if newOnValues is null
			} else if (newDelimiters != null){
				delimiters.putAll(newDelimiters);
			}
		}
		
		public char getDelimiter(String key) {
			return (delimiters != null && delimiters.get(key) != null? delimiters.get(key) : (getKeyType(key) == PresetKeyType.MULTISELECT ? MULTISELECT_DELIMITER : COMBO_DELIMITER)).charAt(0);
		}
		
		public void setMatchType(String key, String match) {
			if (matchType == null) {
				matchType = new HashMap<String,MatchType>();
			}
			MatchType type = null;
			if (match.equals("none")) {
				type = MatchType.NONE;
			} else if (match.equals("key")) {
				type = MatchType.KEY;
			} else if (match.equals("key!")) {
				type = MatchType.KEY_NEG;
			} else if (match.equals("keyvalue")) {
				type = MatchType.KEY_VALUE;
			} else if (match.equals("keyvalue!")) {
				type = MatchType.KEY_VALUE_NEG;
			}
			matchType.put(key, type);
		}
		
		public void setAllMatchTypes(HashMap<String,MatchType> newMatchTypes)
		{
			if (matchType == null) { 
				matchType = newMatchTypes; // doesn't matter if newMatchTypes is null
			} else if (newMatchTypes != null){
				matchType.putAll(newMatchTypes);
			}
		}

		public MatchType getMatchType(String key) {
			return matchType != null ? matchType.get(key) : null;
		}
		
		public void addLinkedPresetName(String presetName) {
			if (linkedPresetNames == null) {
				linkedPresetNames = new LinkedList<String>();
			}
			linkedPresetNames.add(presetName);
		}
		
		public void addAllLinkedPresetNames(LinkedList<String> newLinkedPresetNames) {
			if (linkedPresetNames == null) { 
				linkedPresetNames = newLinkedPresetNames; // doesn't matter if newLinkedPresetNames is null
			} else if (newLinkedPresetNames != null){
				linkedPresetNames.addAll(newLinkedPresetNames);
			}
		}
		
		public LinkedList<String> getLinkedPresetNames() {
			return linkedPresetNames;
		}
		
		public List<PresetItem> getLinkedPresets() {
			ArrayList<PresetItem> result = new ArrayList<PresetItem>();
			if (linkedPresetNames != null) {
				for (String n:linkedPresetNames) {
					Integer index = getItemIndexByName(n); // FIXME this involves a sequential search
					if (index != null) {
						result.add(allItems.get(index.intValue()));
					} else {
						Log.e(DEBUG_TAG,"Couldn't find linked preset " + n);
					}
				}
			}
			return result;
		}
		
		public void setSort(String key, boolean sortIt) {
			if (sort == null) {
				sort = new HashMap<String,Boolean>(); 
			}
			sort.put(key,sortIt);
		}
		
		public void setAllSort(HashMap<String,Boolean> newSort) {
			if (sort == null) { 
				sort = newSort; // doesn't matter if newSort is null
			} else if (newSort != null){
				sort.putAll(newSort);
			}
		}
		
		public boolean sortIt(String key) {
			return (sort == null ||  sort.get(key) == null) ? true : sort.get(key);
		}
		
		public void setJavaScript(String key, String script) {
			if (javascript == null) {
				javascript = new HashMap<String,String>(); 
			}
			javascript.put(key,script);
		}
		
		public void setAllJavaScript(HashMap<String,String> newJavaScript) {
			if (javascript == null) { 
				javascript = newJavaScript; // doesn't matter if newSort is null
			} else if (newJavaScript != null){
				javascript.putAll(newJavaScript);
			}
		}
		
		public String getJavaScript(String key) {
			return javascript == null ?  null : javascript.get(key);
		}
		
		
		public void setEditable(String key, boolean isEditable) {
			if (editable == null) {
				editable = new HashMap<String,Boolean>(); 
			}
			editable.put(key,isEditable);
		}
		
		public void setAllEditable(HashMap<String,Boolean> newEditable) {
			if (editable == null) { 
				editable = newEditable; // doesn't matter if newSort is null
			} else if (newEditable != null){
				editable.putAll(newEditable);
			}
		}
		
		/**
		 * Check is the combo or multiselect should be editable
		 * NOTE: contrary to the definition in JOSM the default is false/no
		 * @param key
		 * @return
		 */
		public boolean isEditable(String key) {
			return (editable == null ||  editable.get(key) == null) ? false : editable.get(key);
		}
		
		public void setTextContext(String key, String textContext) {
			if (this.textContext == null) {
				this.textContext = new HashMap<String, String>();
			}
			this.textContext.put(key, textContext);
		}
		
		public String getTextContext(String key) {
			return textContext.get(key);
		}
		
		public void setValueContext(String key, String valueContext) {
			if (this.valueContext == null) {
				this.valueContext = new HashMap<String, String>();
			}
			this.valueContext.put(key, valueContext);
		}
		
		public String getValueContext(String key) {
			return valueContext.get(key);
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
		
		/**
		 * Return a ist of the values suitable for autocomplete, note vales for fixed tags are not returned
		 * @param key
		 * @return
		 */
		public Collection<StringWithDescription> getAutocompleteValues(String key) {
			Collection<StringWithDescription> result = new LinkedHashSet<StringWithDescription>();
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
			if (name.equals("Addresses")) {
				Log.d(DEBUG_TAG,"matching addresses fixed");
			}
			for (Entry<String, StringWithDescription> tag : fixedTags.entrySet()) { // for each own tag
				String key = tag.getKey();
				if (!tagSet.containsKey(key)) {
					return false;
				}
				MatchType type = getMatchType(key);
				if (type==MatchType.NONE) {
					continue;
				}
				String otherTagValue = tagSet.get(key);		
				if (!tag.getValue().equals(otherTagValue) && type!=MatchType.KEY) {
					return false;
				}
			}
			return true;
		}
		
		/**
		 * Returns the number of matches between the list of recommended tags (really a misnomer) and the provided tags
		 * @param tagSet
		 * @return number of matches
		 */
		public int matchesRecommended(Map<String,String> tagSet) {
			if (name.equals("Addresses")) {
				Log.d(DEBUG_TAG,"matching addresses recommended");
			}
			int matches = 0;
			for (Entry<String, StringWithDescription[]> tag : recommendedTags.entrySet()) { // for each own tag
				String key = tag.getKey();
				if (tagSet.containsKey(key)) { // key could have null value in the set
					// value not empty
					if (getMatchType(key)==MatchType.NONE) {
						// don't count this
						break;
					}
					if (getMatchType(key)==MatchType.KEY) {
						matches++;
						break;
					}
					String otherTagValue = tagSet.get(key);
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

		/**
		 * Return true if the key is contained in this preset
		 * @param key
		 * @return
		 */
		public boolean hasKey(String key) {
			return fixedTags.containsKey(key) || recommendedTags.containsKey(key) || optionalTags.containsKey(key);
		}
		
		/**
		 * Return true if the key and value is contained in this preset taking match attribute in to account
		 * Note mathe="none" is handled the same as "key" in this method
		 * @param key
		 * @return
		 */
		public boolean hasKeyValue(String key, String value) {

			StringWithDescription swd = fixedTags.get(key);
			if (swd!=null) {
				if ("".equals(value) || swd.getValue()==null || swd.equals(value) || "".equals(swd.getValue())) {
					return true;
				}
			}

			MatchType type = getMatchType(key);
			PresetKeyType keyType = getKeyType(key);

			if (recommendedTags.containsKey(key)) {
				if (type==MatchType.KEY || type==MatchType.NONE || keyType==PresetKeyType.MULTISELECT) { // MULTISELECT always editable
					return true;
				}
				StringWithDescription[] swdArray = recommendedTags.get(key);
				if (swdArray != null && swdArray.length > 0) {
					for (StringWithDescription v:swdArray) {
						if ("".equals(value) || v.getValue()==null || v.equals(value) || "".equals(v.getValue())) {
							return true;
						}
					}
				} else {
					return true;
				}
			}

			if (optionalTags.containsKey(key)) { 
				if (type==MatchType.KEY || type==MatchType.NONE || keyType==PresetKeyType.MULTISELECT) { // MULTISELECT always editable
					return true;
				}
				StringWithDescription[] swdArray = optionalTags.get(key);
				if (swdArray != null && swdArray.length > 0) {
					for (StringWithDescription v:swdArray) {
						if ("".equals(value) || v.getValue()==null || v.equals(value) || "".equals(v.getValue())) {
							return true;
						}
					}
				} else {
					return true;
				}
			}
			return false;
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
	
		void setChunk() {
			chunk = true;
		}
		
		boolean isChunk() {
			return chunk;
		}
		
		public String toJSON() {
			String jsonString = "";
			for (String k:fixedTags.keySet()) {
				jsonString = jsonString + tagToJSON(k, fixedTags.get(k).getValue());
			}
			for (String k:recommendedTags.keySet()) {
				// check match attribute
				MatchType match = getMatchType(k);
				PresetKeyType type = getKeyType(k);
				if (isEditable(k) || type==PresetKeyType.TEXT) {
					jsonString = jsonString + tagToJSON(k, null);
				}
				if (!isEditable(k) && type != PresetKeyType.TEXT && (match==null || match == MatchType.KEY_VALUE || match == MatchType.KEY)) {
					for (StringWithDescription v:recommendedTags.get(k)) {
						jsonString = jsonString + tagToJSON(k, v.getValue());
					}
				}
			}
			for (String k:optionalTags.keySet()) {
				// check match attribute
				MatchType match = getMatchType(k);
				PresetKeyType type = getKeyType(k);
				if (isEditable(k) || type==PresetKeyType.TEXT || (match != null && match != MatchType.KEY_VALUE)) {
					jsonString = jsonString + tagToJSON(k, null);
				}
				if (!isEditable(k) && type != PresetKeyType.TEXT && (match==null || match == MatchType.KEY_VALUE || match == MatchType.KEY)) {
					for (StringWithDescription v:optionalTags.get(k)) {
						jsonString = jsonString + tagToJSON(k, v.getValue());
					}
				}
			}
			return jsonString;
		}
		
		/**
		 * For taginfo.openstreetmap.org
		 * @param key
		 * @param value
		 * @return
		 */
		private String tagToJSON(String key, String value) {
			String presetName = name;
			PresetElement p = getParent();
			while (p != null && p != rootGroup && !"".equals(p.getName())){
				presetName = p.getName() + "/" + presetName;
				p = p.getParent();
			}

			String result = "{\"description\":\"" + presetName + "\",\"key\": \"" + key + "\"" + (value == null ? "" : ",\"value\": \"" + value + "\"");
			result = result + ",\"object_types\": [";
			boolean first = true;
			if (appliesToNode) {
				result = result + "\"node\"";
				first = false;
			}
			if (appliesToWay) {
				if (!first) {
					result = result + ",";
				}
				result = result + "\"way\"";
				first = false;
			}	
			if (appliesToRelation) {
				if (!first) {
					result = result + ",";
				}
				result = result + "\"relation\"";
				first = false;
			}
			if (appliesToClosedway || appliesToArea) {
				if (!first) {
					result = result + ",";
				}
				result = result + "\"area\"";
				first = false;
			}
			return  result + "]},\n";
		}

		public void groupI18nKeys() {
			Util.groupI18nKeys(recommendedTags);
			Util.groupI18nKeys(optionalTags);
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
		void onItemClick(PresetItem item);
		boolean onItemLongClick(PresetItem item);
		void onGroupClick(PresetGroup group);
	}

	static public Collection<String> getAutocompleteKeys(Preset[] presets, ElementType type) {
		Collection<String> result = new LinkedHashSet<String>();
		for (Preset p:presets) {
			if (p!=null) {
				switch (type) {
				case NODE: result.addAll(p.autosuggestNodes.getKeys()); break;
				case WAY: result.addAll(p.autosuggestWays.getKeys()); break;
				case CLOSEDWAY: result.addAll(p.autosuggestClosedways.getKeys()); break;
				case RELATION: result.addAll(p.autosuggestRelations.getKeys()); break;
				case AREA: result.addAll(p.autosuggestAreas.getKeys()); break;
				default: return null; // should never happen, all cases are covered
				}
			}
		}
		List<String> r = new ArrayList<String>(result);
		Collections.sort(r);
		return r; 
	}
	
	static public Collection<StringWithDescription> getAutocompleteValues(Preset[] presets, ElementType type, String key) {
		Collection<StringWithDescription> result = new LinkedHashSet<StringWithDescription>();
		for (Preset p:presets) {
			if (p!=null) {
				switch (type) {
				case NODE: result.addAll(p.autosuggestNodes.get(key)); break;
				case WAY: result.addAll(p.autosuggestWays.get(key)); break;
				case CLOSEDWAY: result.addAll(p.autosuggestClosedways.get(key)); break;
				case RELATION: result.addAll(p.autosuggestRelations.get(key)); break;
				case AREA: result.addAll(p.autosuggestAreas.get(key)); break;
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
	
	/**
	 * Build an intent to startup up the correct mapfeatures wiki page
	 * @param ctx
	 * @param p
	 * @return
	 */
	public static Intent getMapFeaturesIntent(Context ctx, PresetItem p) {
		Uri uri = null;
		if (p != null) {
			try {
				uri = p.getMapFeatures();
			} catch (NullPointerException npe) {
				// 
				Log.d(DEBUG_TAG,"Preset " + p.getName() + " has no/invalid map feature uri");
			}
		}
		if (uri == null) {
			uri = Uri.parse(ctx.getString(R.string.link_mapfeatures));
		}
		return new Intent(Intent.ACTION_VIEW, uri);
	}
	
	/**
	 * Split multi select values with the preset defined delimiter character
	 * @param values list of values that can potentially be split
	 * @param preset the preset that sould be used
	 * @param key the key used to determine the delimter value
	 * @return list of split values
	 */
	@Nullable
	public static ArrayList<String> splitValues(ArrayList<String>values, @NonNull PresetItem preset, @NonNull String key) {
		ArrayList<String> result = new ArrayList<String>();
		// String delimiter = Matcher.quoteReplacement("\\Q" + preset.getDelimiter(key)+"\\E"); // always quote to avoid surprises
		// FIXME it is not clear why quoting as above stopped working needs investigation
		String delimiter = String.valueOf(preset.getDelimiter(key));
		if (values==null) {
			return null;
		}
		for (String v:values) {
			if (v==null) {
				continue;
			}
			for (String s:v.split(delimiter)) {
				result.add(s.trim());
			}
		}
		return result;
	}
	
	/**
	 * This is for the taginfo project repo and not really for testing
	 */
	public static boolean generateTaginfoJson(Context ctx, String filename) {
		Preset[] presets = App.getCurrentPresets(ctx);
		
		PrintStream outputStream = null;
		try {
			// String filename = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss", Locale.US).format(new Date())+".json";
			File outfile = new File(FileUtil.getPublicDirectory(), filename);
			outputStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(outfile)));
			
			outputStream.println("{");
			outputStream.println("\"data_format\":1,");
			outputStream.println("\"data_url\":\"https://raw.githubusercontent.com/MarcusWolschon/osmeditor4android/master/taginfo.json\",");
			outputStream.println("\"project\":{");
			outputStream.println("\"name\":\"Vespucci\",");
			outputStream.println("\"description\":\"Offline editor for OSM data on Android.\",");
			outputStream.println("\"project_url\":\"https://github.com/MarcusWolschon/osmeditor4android\",");
			outputStream.println("\"doc_url\":\"http://vespucci.io/\",");
			outputStream.println("\"icon_url\":\"https://raw.githubusercontent.com/MarcusWolschon/osmeditor4android/master/res/drawable/osm_logo.png\",");
			outputStream.println("\"keywords\":[");
			outputStream.println("\"editor\"");
			outputStream.println("]},");
			
			outputStream.println("\"tags\":[");
			for (int i=0;i<presets.length;i++) {
				String json = presets[i].toJSON();
				if (i==presets.length-1) {
					int comma = json.lastIndexOf(',');
					json = json.substring(0, comma);
				}
				outputStream.print(json);
			}
			outputStream.println("]}");
		} catch (Exception e) {
			Log.e(DEBUG_TAG, "Export failed - " + filename);
			e.printStackTrace();
			return false;
		} finally {
			SavingHelper.close(outputStream);
		}
		return true;
	}
}

