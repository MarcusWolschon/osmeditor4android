package de.blau.android.presets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.AttributeList;
import org.xml.sax.HandlerBase;
import org.xml.sax.SAXException;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ScrollView;
import android.widget.TextView;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.PresetEditorActivity;
import de.blau.android.util.Hash;
import de.blau.android.util.MultiHashMap;
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
public class Preset {
	
	/** name of the preset XML file in a preset directory */
	public static final String PRESETXML = "preset.xml";
	/** name of the MRU serialization file in a preset directory */
	private static final String MRUFILE = "mru.dat";
	public static final String APKPRESET_URLPREFIX = "apk:";
	
	/** The directory containing all data (xml, MRU data, images) about this preset */
	private File directory;

	/** Lists items having a tag. The map key is tagkey+"\t"+tagvalue.
	 * tagItems.get(tagkey+"\t"+tagvalue) will give you all items that have the tag tagkey=tagvalue */
	protected final MultiHashMap<String, PresetItem> tagItems = new MultiHashMap<String, PresetItem>();

	/** The root group of the preset, containing all top-level groups and items */
	protected PresetGroup rootGroup;

	/** {@link PresetIconManager} used for icon loading */
	protected final PresetIconManager iconManager;	
	
	/** all known preset items in order of loading */
	protected ArrayList<PresetItem> allItems = new ArrayList<PresetItem>();

	/** Maps all possible keys to the respective values for autosuggest (only key/values applying to nodes) */
	protected final MultiHashMap<String, String> autosuggestNodes = new MultiHashMap<String, String>(true);
	/** Maps all possible keys to the respective values for autosuggest (only key/values applying to ways) */
	protected final MultiHashMap<String, String> autosuggestWays = new MultiHashMap<String, String>(true);
	/** Maps all possible keys to the respective values for autosuggest (only key/values applying to closed ways) */
	protected final MultiHashMap<String, String> autosuggestClosedways = new MultiHashMap<String, String>(true);
	
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
	
	/**
	 * Creates a preset object.
	 * @param ctx context (used for preset loading)
	 * @param directory directory to load/store preset data (XML, icons, MRUs)
	 * @param name of external package containing preset assets for APK presets, null for other presets
	 * @throws Exception
	 */
	public Preset(Context ctx, File directory, String externalPackage) throws Exception {
		this.directory = directory;
		rootGroup = new PresetGroup(null, "", null);
		
		directory.mkdir();
		
		InputStream fileStream;
		if (directory.getName().equals(AdvancedPrefDatabase.ID_DEFAULT)) {
			Log.i("Preset", "Loading default preset");
			this.iconManager = new PresetIconManager(ctx, null, null);
			fileStream = iconManager.openAsset(PRESETXML, true);
		} else if (externalPackage != null) {
			Log.i("Preset", "Loading APK preset, package=" + externalPackage + ", directory="+directory.toString());
			this.iconManager = new PresetIconManager(ctx, directory.toString(), externalPackage);
			fileStream = iconManager.openAsset(PRESETXML, false);
		} else {
			Log.i("Preset", "Loading downloaded preset, directory="+directory.toString());
			this.iconManager = new PresetIconManager(ctx, directory.toString(), null);
			fileStream = new FileInputStream(new File(directory, PRESETXML));
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
        
        mru = initMRU(directory, hashValue);
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

        	{
        		groupstack.push(rootGroup);
        	}
        	
            /** 
             * ${@inheritDoc}.
             */
			@Override
            public void startElement(String name, AttributeList attr) throws SAXException {
            	if (name.equals("group")) {
            		PresetGroup parent = groupstack.peek();
            		PresetGroup g = new PresetGroup(parent, attr.getValue("name"), attr.getValue("icon"));
            		groupstack.push(g);
            	} else if (name.equals("item")) {
            		if (currentItem != null) throw new SAXException("Nested items are not allowed");
            		PresetGroup parent = groupstack.peek();
            		currentItem = new PresetItem(parent, attr.getValue("name"), attr.getValue("icon"), attr.getValue("type"));
            	} else if (name.equals("separator")) {
            		new PresetSeparator(groupstack.peek());
            	} else if (name.equals("optional")) {
            		inOptionalSection = true;
            	} else if (name.equals("key")) {
            		if (!inOptionalSection) {
            			currentItem.addTag(attr.getValue("key"), attr.getValue("value"));
            		} else {
            			// Optional fixed tags should not happen, their values will NOT be automatically inserted.
            			currentItem.addTag(true, attr.getValue("key"), attr.getValue("value"));
            		}
            	} else if (name.equals("text")) {
            		currentItem.addTag(inOptionalSection, attr.getValue("key"), null);
            	} else if (name.equals("check")) {
            		currentItem.addTag(inOptionalSection, attr.getValue("key"), "yes,no");            		
            	} else if (name.equals("combo")) {
            		currentItem.addTag(inOptionalSection, attr.getValue("key"), attr.getValue("values"));            		
            	} else if (name.equals("multiselect")) {
            		currentItem.addTag(inOptionalSection, attr.getValue("key"), null); // TODO full multiselect parsing/support?
            	}
            }
            
            
            @Override
            public void endElement(String name) throws SAXException {
            	if (name.equals("group")) {
            		groupstack.pop();
            	} else if (name.equals("optional")) {
            		inOptionalSection = false;
            	} else if (name.equals("item")) {
            		currentItem = null;
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
        	// Unserialization failed for whatever reason (missing file, wrong version, ...) - use empty list
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
	            	if (name.equals("group") || name.equals("item")) {
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
	
	/**
	 * Returns a view showing the most recently used presets
	 * @param handler the handler which will handle clicks on the presets
	 * @param type filter to show only presets applying to this type
	 * @return the view
	 */
	public View getRecentPresetView(Context ctx, PresetClickHandler handler, ElementType type) {
		PresetGroup recent = new PresetGroup(null, "recent", null);
		for (int index : mru.recentPresets) {
			recent.addElement(allItems.get(index));
		}
		return recent.getGroupView(ctx, handler, type);
	}
	
	/**
	 * Add a preset to the front of the MRU list (removing old duplicates and limiting the list to 50 entries if needed)
	 * @param item the item to add
	 */
	public void putRecentlyUsed(PresetItem item) {
		Integer id = item.getItemIndex();
		// prevent duplicates
		mru.recentPresets.remove(id); // calling remove(Object), i.e. removing the number if it is in the list, not the i-th item
		mru.recentPresets.addFirst(id);
		if (mru.recentPresets.size() > 50) mru.recentPresets.removeLast();
		mru.changed  = true;
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

	/**
	 * WARNING - UNTESTED
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
	public PresetItem findBestMatch(Map<String,String> tags) {
		int bestMatchStrength = 0;
		PresetItem bestMatch = null;
		
		// Build candidate list
		LinkedHashSet<PresetItem> possibleMatches = new LinkedHashSet<PresetItem>();
		for (Entry<String, String> tag : tags.entrySet()) {
			String tagString = tag.getKey()+"\t"+tag.getValue();
			possibleMatches.addAll(tagItems.get(tagString));
		}
		
		// Find best
		for (PresetItem possibleMatch : possibleMatches) {
			if (possibleMatch.getTagCount() <= bestMatchStrength) continue; // isn't going to help
			if (possibleMatch.matches(tags)) {
				bestMatch = possibleMatch;
				bestMatchStrength = bestMatch.getTagCount();
			}
		}
		
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
	public abstract class PresetElement {
		private String name;
		private Drawable icon;
		private BitmapDrawable mapIcon;
		private PresetGroup parent;
		private boolean appliesToWay, appliesToNode, appliesToClosedway; //appliesToRelation

		/**
		 * Creates the element, setting parent, name and icon, and registers with the parent
		 * @param parent parent group (or null if this is the root group)
		 * @param name name of the element
		 * @param iconpath The icon path (either "http://" URL or "presets/" local image reference)
		 */
		public PresetElement(PresetGroup parent, String name, String iconpath) {
			this.parent = parent;
			this.name = name;
			if (iconpath != null) {
				this.icon = iconManager.getDrawableOrPlaceholder(iconpath, 48);
				this.mapIcon = iconManager.getDrawable(iconpath, de.blau.android.Map.ICON_SIZE_DP);
			}
			if (parent != null)	parent.addElement(this);
		}		
		
		public String getName() {
			return name;
		}

		public Drawable getIcon() {
			return icon;
		}
		
		public BitmapDrawable getMapIcon() {
			return mapIcon;
		}
		
		public PresetGroup getParent() {
			return parent;
		}
		
		/**
		 * Returns a basic view representing the current element (i.e. a button with icon and name).
		 * Can (and should) be used when implementing {@link #getView(PresetClickHandler)}.
		 * @return the view
		 */
		private final TextView getBaseView(Context ctx) {
			TextView v = new TextView(ctx);
			float density = ctx.getResources().getDisplayMetrics().density;
			v.setText(this.getName());
			v.setCompoundDrawables(null, this.getIcon(), null, null);
			v.setCompoundDrawablePadding((int)(8*density));
			v.setWidth((int)(100*density));
			v.setHeight((int)(100*density));
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
	}
	
	/**
	 * Represents a separator in a preset group
	 */
	public class PresetSeparator extends PresetElement {
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
		
		/** Elements in this group */
		private ArrayList<PresetElement> elements = new ArrayList<PresetElement>();
		
		
		public PresetGroup(PresetGroup parent, String name, String iconpath) {
			super(parent, name,iconpath);
		}

		public void addElement(PresetElement element) {
			elements.add(element);
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
			scrollView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			WrappingLayout wrappingLayout = new WrappingLayout(ctx);
			float density = ctx.getResources().getDisplayMetrics().density;
			wrappingLayout.setHorizontalSpacing((int)(10*density));
			wrappingLayout.setVerticalSpacing((int)(10*density));
			ArrayList<PresetElement> filteredElements = filterElements(elements, type);
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
		/** "fixed" tags, i.e. the ones that have a fixed key-value pair */
		private LinkedHashMap<String, String> tags = new LinkedHashMap<String, String>();
		
		/** Tags that are not in the optional section, but do not have a fixed key-value-pair.
		 *  The map key provides the key, while the map value (String[]) provides the possible values. */
		private LinkedHashMap<String, String[]> recommendedTags = new LinkedHashMap<String, String[]>();
		
		/** Tags that are in the optional section.
		 *  The map key provides the key, while the map value (String[]) provides the possible values. */
		private LinkedHashMap<String, String[]> optionalTags = new LinkedHashMap<String, String[]>();
		
		
		private int itemIndex;

		public PresetItem(PresetGroup parent, String name, String iconpath, String types) {
			super(parent, name, iconpath);
			if (types == null) {
				// Type not specified, assume all types
				setAppliesToNode();
				setAppliesToWay();
				setAppliesToClosedway();
			} else {
				String[] typesArray = types.split(",");
				for (String type : typesArray) {
					if (type.equals("node")) setAppliesToNode();
					else if (type.equals("way")) setAppliesToWay();
					else if (type.equals("closedway")) setAppliesToClosedway();
				}
			}	
			itemIndex = allItems.size();
			allItems.add(this);
		}

		/**
		 * Adds a fixed tag to the item, registers the item in the tagItems map and populates autosuggest.
		 * @param key key name of the tag
		 * @param value value of the tag
		 */
		public void addTag(String key, String value) {
			if (key == null) throw new NullPointerException("null key not supported");
			if (value == null) value = "";
			tags.put(key, value);
			tagItems.add(key+"\t"+value, this);
			if (appliesTo(ElementType.NODE)) autosuggestNodes.add(key, value.length() > 0 ? value : null);
			if (appliesTo(ElementType.WAY)) autosuggestWays.add(key, value.length() > 0 ? value : null);
			if (appliesTo(ElementType.CLOSEDWAY)) autosuggestClosedways.add(key, value.length() > 0 ? value : null);
		}
		
		/**
		 * Adds a recommended or optional tag to the item and populates autosuggest.
		 * @param optional true if optional, false if recommended
		 * @param key key name of the tag
		 * @param values values string from the XML (comma-separated list of possible values)
		 */
		public void addTag(boolean optional, String key, String values) {
			String[] valueArray;
			if (values == null || values.length() == 0) {
				valueArray = new String[0];
			} else {
				valueArray = values.split(",");
			}
			
			if (appliesTo(ElementType.NODE)) autosuggestNodes.add(key, valueArray);
			if (appliesTo(ElementType.WAY)) autosuggestWays.add(key, valueArray);
			if (appliesTo(ElementType.CLOSEDWAY)) autosuggestClosedways.add(key, valueArray);
			
			if (optional) {
				optionalTags.put(key, valueArray);
			} else {
				recommendedTags.put(key, valueArray);
			}
		}

		
		/**
		 * @return the fixed tags belonging to this item (unmodifiable)
		 */
		public Map<String,String> getTags() {
			return Collections.unmodifiableMap(tags);
		}
		
		public int getTagCount() {
			return tags.size();
		}
		
		public Map<String,String[]> getRecommendedTags() {
			return Collections.unmodifiableMap(recommendedTags);
		}

		public Map<String,String[]> getOptionalTags() {
			return Collections.unmodifiableMap(optionalTags);
		}
		
		/**
		 * Checks if all tags belonging to this item exist in the given tagSet,
		 * i.e. the node to which the tagSet belongs could be what this preset specifies.
		 * @param tagSet the tagSet to compare against this item
		 * @return
		 */
		public boolean matches(Map<String,String> tagSet) {
			for (Entry<String, String> tag : this.tags.entrySet()) { // for each own tag
				String otherTagValue = tagSet.get(tag.getKey());
				if (otherTagValue == null || !tag.getValue().equals(otherTagValue)) return false;
			}
			return true;
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
			}
			return v;
		}

		public int getItemIndex() {
			return itemIndex;
		}
	}
	
	
	/**
	 * Adapter providing the preset elements in this group
	 * currently unused, left here in case it is later needed
	 */
	private class PresetGroupAdapter extends BaseAdapter {
	
		private final ArrayList<PresetElement> elements;
		private PresetClickHandler handler;
		private final Context context;
		
		private PresetGroupAdapter(Context ctx, ArrayList<PresetElement> content, ElementType type,
				PresetClickHandler handler) {
			this.handler = handler;
			this.context = ctx;
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
		public void onGroupClick(PresetGroup group);
	}

	public Collection<String> getAutocompleteKeys(ElementType type) {
		switch (type) {
		case NODE: return autosuggestNodes.getKeys();
		case WAY: return autosuggestWays.getKeys();
		case CLOSEDWAY: return autosuggestClosedways.getKeys();
		}
		return null; // should never happen, all cases are covered
	}
	
	public Collection<String> getAutocompleteValues(ElementType type, String key) {
		Collection<String> source = null;
		switch (type) {
		case NODE: source = autosuggestNodes.get(key); break;
		case WAY: source = autosuggestWays.getKeys(); break;
		case CLOSEDWAY:source = autosuggestClosedways.getKeys(); break;
		}
		if (source != null) {
			return source;
		} else {
			return Collections.emptyList();
		}
	}
	
}

