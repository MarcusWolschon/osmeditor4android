package de.blau.android.presets;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.AttributeList;
import org.xml.sax.HandlerBase;
import org.xml.sax.SAXException;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.blau.android.R;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.util.MultiHashMap;

/**
 * This class loads and represents JOSM preset files.
 * @author Jan Schejbal
 */
public class Preset {

	// TODO tags to lowercase?
	
	protected Context context;
	
	/** Lists items having a tag. The map key is tagkey+"\t"+tagvalue.
	 * tagItems.get(tagkey+"\t"+tagvalue) will give you all items that have the tag tagkey=tagvalue */
	protected MultiHashMap<String, PresetItem> tagItems = new MultiHashMap<String, PresetItem>();

	protected PresetGroup rootGroup = new PresetGroup(null, null, null);
	
	
	
	@SuppressWarnings("deprecation")
	public Preset(Context ctx) {
		this.context = ctx;
		SAXParser saxParser;
		try {
			saxParser = SAXParserFactory.newInstance().newSAXParser();
			
	        InputStream input = ctx.getResources().openRawResource(R.raw.presets);
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
	            		PresetGroup g = groupstack.peek();
	            		g.addSeparator();
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
		} catch (Exception e) {
			Log.e("PresetParser", "Error parsing preset", e);
			e.printStackTrace();
		}
	}
	
	public PresetGroup getRootGroup() {
		return rootGroup;
	}
	
	/**
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
		 * @param iconpath TODO
		 */
		public PresetElement(PresetGroup parent, String name, String iconpath) {
			this.parent = parent;
			this.name = name;
			this.icon = null; // TODO
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
		 * Returns a basic view representing the current element (i.e. a button with icon and name)
		 * @return the view
		 */
		private View getBaseView() {
			TextView v = new TextView(context);
			v.setText(this.getName());
			v.setCompoundDrawables(this.getIcon(), null, null, null);
			return v;
		}
		
		/**
		 * Returns a view representing this element (i.e. a button with icon and name)
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
	
	public class PresetGroup extends PresetElement {
		
		/** A list of sections.
		 * Each section can contain multiple elements, the sections are separated by separators. */
		private List<List<PresetElement>> sections = new ArrayList<List<PresetElement>>();
		
		/** Reference to the current section. If there is null, a new one will be created. */
		private List<PresetElement> currentSection = null;
		
		
		public PresetGroup(PresetGroup parent, String name, String iconpath) {
			super(parent, name,iconpath);
		}

		public void addElement(PresetElement element) {
			if (currentSection == null) {
				currentSection = new ArrayList<PresetElement>();
				sections.add(currentSection);
			}
			currentSection.add(element);
		}

		public void addSeparator() {
			currentSection = null;
		}
		
		@Override
		public View getView(final PresetClickHandler handler) {
			View v = super.getBaseView();
			v.setBackgroundColor(android.R.color.darker_gray);
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
			LinearLayout grouplayout = new LinearLayout(context);
			grouplayout.setOrientation(LinearLayout.VERTICAL);
			for (List<PresetElement> section : sections) {
				LinearLayout sectionlayout = new LinearLayout(context);
				sectionlayout.setOrientation(LinearLayout.HORIZONTAL);
				for (PresetElement element : section) {
					if (element.appliesTo(type)) {
						sectionlayout.addView(element.getView(handler));
					}
				}
				grouplayout.addView(sectionlayout);
			}
			return grouplayout;
		}

	}
	
	public class PresetItem extends PresetElement {
		private HashMap<String, String> tags = new HashMap<String, String>();

		public PresetItem(PresetGroup parent, String name, String iconpath, String types) {
			super(parent, name, iconpath);
			String[] typesArray = types.split(",");
			for (String type : typesArray) {
				if (type.equals("node")) setAppliesToNode();
				else if (type.equals("way")) setAppliesToWay();
				else if (type.equals("closedway")) setAppliesToClosedway();
			}
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
	}
	
	public interface PresetClickHandler {
		public void onItemClick(PresetItem item);
		public void onGroupClick(PresetGroup group);
	}
}
