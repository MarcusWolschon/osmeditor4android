package de.blau.android.presets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.DigestInputStream;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import de.blau.android.R;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.util.Hash;
import de.blau.android.util.MultiHashMap;
import de.blau.android.views.WrappingLayout;

/**
 * This class loads and represents JOSM preset files.
 * @author Jan Schejbal
 */
public class Preset {

	// TODO tags to lowercase?
	
	/** name of the preset XML file in a preset directory */
	public static final String PRESETXML = "preset.xml";
	/** name of the MRU serialization file in a preset directory */
	private static final String MRUFILE = "mru.dat";

	protected final Context context;
	
	/** The directory containing all data (xml, MRU data, images) about this preset */
	private File directory;

	/** Lists items having a tag. The map key is tagkey+"\t"+tagvalue.
	 * tagItems.get(tagkey+"\t"+tagvalue) will give you all items that have the tag tagkey=tagvalue */
	protected final MultiHashMap<String, PresetItem> tagItems = new MultiHashMap<String, PresetItem>();

	protected PresetGroup rootGroup;

	protected final PresetIconManager iconManager;	
	
	/** all known preset items in order of loading */
	protected ArrayList<PresetItem> allItems = new ArrayList<PresetItem>();

	/**
	 * Serializable class for storing Most Recently Used information.
	 * Hash is used to check compatibility.
	 */
	protected class PresetMRUInfo implements Serializable {
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
	
	public Preset(Context ctx, File directory) throws Exception {
		this.context = ctx;
		this.iconManager = new PresetIconManager(ctx, null);
		this.directory = directory;
		rootGroup = new PresetGroup(null, "", null);
		
		InputStream fileStream;
		if (directory.getName().equals(AdvancedPrefDatabase.ID_DEFAULT)) {
			fileStream = ctx.getResources().openRawResource(R.raw.presets);
		} else {
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
        	
        	private Stack<PresetGroup> groupstack = new Stack<PresetGroup>();
        	private PresetItem currentItem = null;
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
            		}
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
        try {
        	ObjectInputStream mruReader = 
        		new ObjectInputStream(new FileInputStream(new File(directory, MRUFILE)));
        	tmpMRU = (PresetMRUInfo) mruReader.readObject();
        	if (!tmpMRU.presetHash.equals(hashValue)) throw new InvalidObjectException("hash mismatch");
        } catch (Exception e) {
        	tmpMRU = new PresetMRUInfo(hashValue);
        	// Unserialization failed for whatever reason (missing file, wrong version, ...) - use empty list
        	Log.i("Preset", "No usable old MRU list, creating new one ("+e.toString()+")");
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
	
	public PresetGroup getRootGroup() {
		return rootGroup;
	}
	
	public View getRecentPresetView(PresetClickHandler handler, ElementType type) {
		PresetGroup recent = new PresetGroup(null, "recent", null);
		for (int index : mru.recentPresets) {
			recent.addElement(allItems.get(index));
		}
		return recent.getGroupView(handler, type);
	}
	
	public void putRecentlyUsed(PresetItem item) {
		Integer id = item.getItemIndex();
		// prevent duplicates
		mru.recentPresets.remove(id); // calling remove(Object), i.e. removing the number if it is in the list, not the i-th item
		mru.recentPresets.addFirst(id);
		if (mru.recentPresets.size() > 50) mru.recentPresets.removeLast();
		mru.changed  = true;
	}

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
	 * Finds the item best matching a certain tag set, or null if no item matches.
	 * To match, all (mandatory) tags of the preset item need to be in the tag set.
	 * The item does NOT need to have all tags from the tag set, but the tag set needs
	 * to have all tags from the item.
	 * 
	 * If multiple items match, the most specific one (i.e. having most tags) wins.
	 * If there is a draw, no guarantees are made.
	 * @param tags tags to check against (i.e. tags of a map element)
	 * @return null, or the "best" matching item for the given tag set
	 */
	public PresetItem findBestMatch(HashMap<String,String> tags) {
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
			this.icon = iconManager.getDrawableOrPlaceholder(iconpath, 48);
			if (parent != null)	parent.addElement(this);
		}		
		
		public String getName() {
			return name;
		}

		public Drawable getIcon() {
			return icon;
		}
		
		public PresetGroup getParent() {
			return parent;
		}
		
		/**
		 * Returns a basic view representing the current element (i.e. a button with icon and name).
		 * Can (and should) be used when implementing {@link #getView(PresetClickHandler)}.
		 * @return the view
		 */
		private final TextView getBaseView() {
			TextView v = new TextView(context);
			float density = context.getResources().getDisplayMetrics().density;
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
		public abstract View getView(final PresetClickHandler handler);
		
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
	
	public class PresetSeparator extends PresetElement {
		public PresetSeparator(PresetGroup parent) {
			super(parent, "", null);
		}

		@Override
		public View getView(PresetClickHandler handler) {
			View v = new View(context);
			v.setMinimumHeight(1);
			v.setMinimumWidth(99999);
			return v;
		}
		
	}
	
	public class PresetGroup extends PresetElement {
		
		/** Elements in this group */
		private ArrayList<PresetElement> elements = new ArrayList<PresetElement>();
		
		
		public PresetGroup(PresetGroup parent, String name, String iconpath) {
			super(parent, name,iconpath);
		}

		public void addElement(PresetElement element) {
			elements.add(element);
		}
		
		@Override
		public View getView(final PresetClickHandler handler) {
			TextView v = super.getBaseView();
			v.setBackgroundColor(android.R.color.darker_gray);
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
		public View getGroupView(PresetClickHandler handler, ElementType type) {
			ScrollView scrollView = new ScrollView(context);
			scrollView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			WrappingLayout wrappingLayout = new WrappingLayout(context);
			float density = context.getResources().getDisplayMetrics().density;
			wrappingLayout.setHorizontalSpacing((int)(10*density));
			wrappingLayout.setVerticalSpacing((int)(10*density));
			ArrayList<PresetElement> filteredElements = filterElements(elements, type);
			ArrayList<View> childViews = new ArrayList<View>();
			for (PresetElement element : filteredElements) {
				childViews.add(element.getView(handler));
			}
			wrappingLayout.setWrappedChildren(childViews);
			scrollView.addView(wrappingLayout);
			return scrollView;
		}

	}
	
	public class PresetItem extends PresetElement {
		private HashMap<String, String> tags = new HashMap<String, String>();
		private int itemIndex;

		public PresetItem(PresetGroup parent, String name, String iconpath, String types) {
			super(parent, name, iconpath);
			String[] typesArray = types.split(",");
			for (String type : typesArray) {
				if (type.equals("node")) setAppliesToNode();
				else if (type.equals("way")) setAppliesToWay();
				else if (type.equals("closedway")) setAppliesToClosedway();
			}
			itemIndex = allItems.size();
			allItems.add(this);
		}

		/**
		 * Adds a tag to the item, and registers the item in the tagItems map
		 * @param key key name of the tag
		 * @param value value of the tag
		 */
		public void addTag(String key, String value) {
			if (key == null) throw new NullPointerException("null key not supported");
			if (value == null) value = "";
			tags.put(key, value);
			tagItems.add(key+"\t"+value, this);
		}
		
		/**
		 * @return the tags belonging to this item (unmodifiable)
		 */
		public Map<String,String> getTags() {
			return Collections.unmodifiableMap(tags);
		}
		
		public int getTagCount() {
			return tags.size();
		}
		
		public boolean matches(HashMap<String,String> tagSet) {
			for (Entry<String, String> tag : this.tags.entrySet()) { // for each own tag
				String otherTagValue = tagSet.get(tag.getKey());
				if (otherTagValue == null || !tag.getValue().equals(otherTagValue)) return false;
			}
			return true;
		}

		@Override
		public View getView(final PresetClickHandler handler) {
			View v = super.getBaseView();
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
		
		private PresetGroupAdapter(ArrayList<PresetElement> content, ElementType type,
				PresetClickHandler handler) {
			this.handler = handler;
			
			elements = filterElements(content, type);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getItem(position).getView(handler);
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

	public interface PresetClickHandler {
		public void onItemClick(PresetItem item);
		public void onGroupClick(PresetGroup group);
	}
}
