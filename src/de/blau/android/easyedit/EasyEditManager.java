package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.acra.ACRA;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.speech.RecognizerIntent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextThemeWrapper;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.blau.android.Application;
import de.blau.android.HelpViewer;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Main.UndoListener;
import de.blau.android.R;
import de.blau.android.dialogs.ElementInfo;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.PrefEditor;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.propertyeditor.Address;
import de.blau.android.tasks.TaskFragment;
import de.blau.android.util.ElementSearch;
import de.blau.android.util.GeoMath;
import de.blau.android.util.MenuUtil;
import de.blau.android.util.NetworkStatus;
import de.blau.android.util.SearchIndexUtils;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * This class handles most of the EasyEdit mode actions, to keep it separate from the main class.
 * @author Jan
 *
 */
public class EasyEditManager {

	private static final String DEBUG_TAG = EasyEditManager.class.getSimpleName();
	
	private final Main main;
	private final Logic logic;
	/** the touch listener from Main */
	
	private ActionMode currentActionMode = null;
	private EasyEditActionModeCallback currentActionModeCallback = null;
	
	private ActionMenuView cabBottomBar;
	
	public final static int GROUP_MODE = 0;
	public final static int GROUP_BASE = 1;
	
	private static final int MENUITEM_HELP = 0;
	
	public EasyEditManager(Main main) {
		this.main = main;
		this.logic = Application.getLogic();
	}
	
	/**
	 * Returns true if a actionmode is currently active
	 * @return
	 */
	public boolean isProcessingAction() {
		return (currentActionModeCallback != null);
	}
	
	
	/**
	 * Check if the actionmode ants its own context menu
	 * @return
	 */
	public boolean needsCustomContextMenu() {
		return isProcessingAction() && currentActionModeCallback.needsCustomContextMenu();
	}
	
	/**
	 * call if you need to abort the current action mode
	 */
	public void finish() {
		if (currentActionMode != null) {
			currentActionMode.finish();
		}
	}
	
	/**
	 * Call to let the action mode (if any) have a first go at the click.
	 * @param x the x coordinate (screen coordinate?) of the click
	 * @param y the y coordinate (screen coordinate?) of the click
	 * @return true if the click was handled
	 */
	public boolean actionModeHandledClick(float x, float y) {
		return (currentActionModeCallback != null && currentActionModeCallback.handleClick(x, y));
	}
	
	/**
	 * Handle case where nothing is touched.
	 * @param doubleTap TODO
	 */
	public void nothingTouched(boolean doubleTap) {
		// User clicked an empty area. If something is selected, deselect it.
		if (!doubleTap && currentActionModeCallback instanceof ExtendSelectionActionModeCallback) {
			return; // don't deselect all just because we didn't hit anything TODO display a toast
		}
		if (currentActionModeCallback instanceof ElementSelectionActionModeCallback || currentActionModeCallback instanceof ExtendSelectionActionModeCallback) {
			currentActionMode.finish();
		}
		logic.setSelectedNode(null);
		logic.setSelectedWay(null);
		logic.setSelectedRelationWays(null);
		logic.setSelectedRelationNodes(null);
	}
	
	/**
	 * Handle editing the given element.
	 * @param element The OSM element to edit.
	 */
	public void editElement(OsmElement element) {
		if (currentActionModeCallback == null || !currentActionModeCallback.handleElementClick(element)) {
			// No callback or didn't handle the click, perform default (select element)
			ActionMode.Callback cb = null;
			if (element instanceof Node) cb = new NodeSelectionActionModeCallback((Node)element);
			if (element instanceof Way ) cb = new  WaySelectionActionModeCallback((Way )element);
			if (element instanceof Relation ) cb = new RelationSelectionActionModeCallback((Relation )element);
			if (cb != null) {
				main.startSupportActionMode(cb);
				String toast = element.getDescription(main);
				if (element.hasProblem(main)) {
					String problem = element.describeProblem();
					toast = !problem.equals("") ? toast + "\n" + problem : toast;
				}
				Toast.makeText(main, toast, Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	/**
	 * Edit currently selected elements.
	 */
	public void editElements() {
		if (currentActionModeCallback == null) {
			// No callback or didn't handle the click, perform default (select element)
			ActionMode.Callback cb = null;
			OsmElement e = null;
			if (logic.getSelectedNodes() != null && logic.getSelectedNodes().size() == 1 && logic.getSelectedWays() == null && logic.getSelectedRelations() == null) {
				e = logic.getSelectedNode();
				cb = new NodeSelectionActionModeCallback((Node) e);
			} else if (logic.getSelectedNodes() == null && logic.getSelectedWays() != null && logic.getSelectedWays().size() == 1 && logic.getSelectedRelations() == null) {
				e = logic.getSelectedWay();
				cb = new  WaySelectionActionModeCallback((Way) e);
			} else if (logic.getSelectedNodes() == null && logic.getSelectedWays() == null && logic.getSelectedRelations() != null && logic.getSelectedRelations().size() == 1) {
				e = logic.getSelectedRelations().get(0);
				cb = new RelationSelectionActionModeCallback((Relation )e);
			} else if (logic.getSelectedNodes() != null || logic.getSelectedWays() != null || logic.getSelectedRelations() != null) {
				ArrayList<OsmElement> selection = new ArrayList<OsmElement>(); 
				if (logic.getSelectedNodes() != null) {
					selection.addAll(logic.getSelectedNodes());
				}
				if (logic.getSelectedWays() != null) {
					selection.addAll(logic.getSelectedWays());
				}
				if (logic.getSelectedRelations() != null) {
					selection.addAll(logic.getSelectedRelations());
				}
				cb = new ExtendSelectionActionModeCallback(selection);
			}
			if (cb != null) {
				main.startSupportActionMode(cb);
				if (e != null) {
					String toast = e.getDescription(main);
					if (e.hasProblem(main)) {
						String problem = e.describeProblem();
						toast = !problem.equals("") ? toast + "\n" + problem : toast;
					}
					Toast.makeText(main, toast, Toast.LENGTH_SHORT).show();
				}
			}
		}
	}
	
	/** This gets called when the map is long-pressed in easy-edit mode */
	public boolean handleLongClick(View v, float x, float y) {

		if ((currentActionModeCallback instanceof PathCreationActionModeCallback)) 
		{
			// we don't do long clicks in the above modes
			Log.d("EasyEditManager", "handleLongClick ignoring long click");
			return false; // this probably should really return true aka click handled
		}
		v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
		// TODO: Need to patch ABS, see https://github.com/JakeWharton/ActionBarSherlock/issues/642
		if (main.startSupportActionMode(new LongClickActionModeCallback(x, y)) == null) {
			main.startSupportActionMode(new PathCreationActionModeCallback(x, y));
		}
		return true;
	}
	
	
	public void startExtendedSelection(OsmElement osmElement) {
		if ((currentActionModeCallback instanceof WaySelectionActionModeCallback)
				|| (currentActionModeCallback instanceof NodeSelectionActionModeCallback)
				|| (currentActionModeCallback instanceof RelationSelectionActionModeCallback))
		{
			// one element already selected
			((ElementSelectionActionModeCallback)currentActionModeCallback).deselect = false; // keep the element visually selected
			main.startSupportActionMode(new ExtendSelectionActionModeCallback(((ElementSelectionActionModeCallback)currentActionModeCallback).element));
			// add 2nd element FIXME may need some checks
			((ExtendSelectionActionModeCallback)currentActionModeCallback).handleElementClick(osmElement);
		} else if (currentActionModeCallback instanceof ExtendSelectionActionModeCallback) {
			// ignore for now
		} else if (currentActionModeCallback != null) {
			// ignore for now
		} else {
			// nothing selected
			main.startSupportActionMode(new ExtendSelectionActionModeCallback(osmElement));
		}
	}
	
	public void invalidate() {
		if (currentActionMode != null) {
			currentActionMode.invalidate();
		}
	}
	
	/**
	 * call the onBackPressed method for the currently active action mode
	 * @return
	 */
	public boolean handleBackPressed() {
		if (currentActionModeCallback != null)
			return currentActionModeCallback.onBackPressed();
		return false;
	}
	
	/**
	 * Takes a parameter for a node and one for a way.
	 * If the way is not null, opens a tag editor for the way.
	 * Otherwise, opens a tag editor for the node
	 * (unless the node is also null, then nothing happens).
	 * @param possibleNode a node that was edited, or null
	 * @param possibleWay a way that was edited, or null
	 * @param select TODO
	 */
	private void tagApplicable(Node possibleNode, Way possibleWay, boolean select) {
		if (possibleWay == null) {
			// Single node was added
			if (possibleNode != null) { // null-check to be sure
				if (select) {
					main.startSupportActionMode(new NodeSelectionActionModeCallback(possibleNode));
				}
				main.performTagEdit(possibleNode, null, false, false);
			}
		} else { // way was added
			if (select) {
				main.startSupportActionMode(new WaySelectionActionModeCallback(possibleWay));
			}
			main.performTagEdit(possibleWay, null, false, false);		
		}
	}
	
	/**
	 * Finds which ways can be merged with a way.
	 * For this, the ways must not be equal, need to share at least one end node,
	 * and either at least one of them must not have tags, or the tags on both ways must be equal.  
	 * 
	 * @param way the way into which other ways may be merged
	 * @return a list of all ways which can be merged into the given way
	 */
	private Set<OsmElement> findMergeableWays(Way way) {
		Set<Way> candidates = new HashSet<Way>();
		Set<OsmElement> result = new HashSet<OsmElement>();
		candidates.addAll(logic.getWaysForNode(way.getFirstNode()));
		candidates.addAll(logic.getWaysForNode(way.getLastNode()));
		for (Way candidate : candidates) {
			if ((way != candidate)
				&& (candidate.isEndNode(way.getFirstNode()) || candidate.isEndNode(way.getLastNode()))
				&& (candidate.getTags().isEmpty() || way.getTags().isEmpty() || 
						way.getTags().entrySet().equals(candidate.getTags().entrySet()) )
						//TODO check for relations too
				) {
				result.add(candidate);
			}
		}
		return result;
	}
	
	/**
	 * Finds which nodes can be append targets.
	 * @param way The way that will be appended to.
	 * @return The set of nodes suitable for appending.
	 */
	private Set<OsmElement> findAppendableNodes(Way way) {
		Set<OsmElement> result = new HashSet<OsmElement>();
		for (Node node : way.getNodes()) {
			if (way.isEndNode(node)) result.add(node);
		}
		// don't allow appending to circular ways
		if (result.size() == 1) result.clear();
		return result;
	}
	
	/**
	 * Finds which ways or nodes can be used as a via element in a restriction relation
	 * 
	 * @param way the from way
	 * @return a list of all applicable objects
	 */
	private Set<OsmElement> findViaElements(Way way) {
		
		Set<OsmElement> result = new HashSet<OsmElement>();
		for (Node n:way.getNodes()) {
			for (Way w:logic.getWaysForNode(n)) {
				if (w.getTagWithKey(Tags.KEY_HIGHWAY) != null) {
					result.add(w);
					result.add(n); // result is a set so we wont get dups
				}
			}
		}
		return result;
	}
	
	/**
	 * Find possible elements for the "to" role of a restriction relation
	 * @param way
	 * @param commonNode
	 * @return
	 */
	private Set<OsmElement> findToElements(OsmElement viaElement) {
		Set<OsmElement> result = new HashSet<OsmElement>();	
		Set<Node> nodes = new HashSet<Node>();
		if (Node.NAME.equals(viaElement.getName())) {
			nodes.add((Node) viaElement);
		} else if (Way.NAME.equals(viaElement.getName())) {
			nodes.addAll(((Way)viaElement).getNodes());
		} else {
			Log.e(DEBUG_TAG, "Unknown element type for via element " + viaElement.getName() + " " + viaElement.getDescription());
		}
		for (Node n:nodes) {
			for (Way w:logic.getWaysForNode(n)) {
				if (w.getTagWithKey(Tags.KEY_HIGHWAY) != null) {
					result.add(w);
				}
			}
		}
		return result;
	}
	
	public void handleActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (currentActionModeCallback instanceof LongClickActionModeCallback) {
			((LongClickActionModeCallback)currentActionModeCallback).handleActivityResult(requestCode, resultCode, data);
		}
	}
	
	public boolean processShortcut(Character c) {
		if (currentActionModeCallback != null) {
			return currentActionModeCallback.processShortcut(c);
		}
		return false;
	}
	
	/**
	 * Replace the menu used by the action mode by our toolbar if necessary
	 * @param menu original menu
	 * @param actionMode the current action mode
	 * @param callback the callback we are currently in
	 * @return
	 */
	protected Menu replaceMenu(Menu menu, final ActionMode actionMode, final ActionMode.Callback callback) {
		if (cabBottomBar!=null) {
			menu = cabBottomBar.getMenu();
			android.support.v7.widget.ActionMenuView.OnMenuItemClickListener listener = new android.support.v7.widget.ActionMenuView.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					return callback.onActionItemClicked(actionMode,item);
				}	
			};
			cabBottomBar.setOnMenuItemClickListener(listener);
		}
		return menu;
	}
	
	/**
	 * Call the per actionmode onCreateContextMenu
	 * @param menu
	 */
	public void createContextMenu(ContextMenu menu) {
		if (currentActionModeCallback != null) {
			currentActionModeCallback.onCreateContextMenu(menu);
		}	
	}
	
	/**
	 * Base class for ActionMode callbacks inside {@link EasyEditManager}.
	 * Derived classes should call {@link #onCreateActionMode(ActionMode, Menu)} and {@link #onDestroyActionMode(ActionMode)}.
	 * It will handle registering and de-registering the action mode callback with the {@link EasyEditManager}.
	 * When the {@link EasyEditManager} receives a click on a node or way, it may pass it to the current action mode callback.
	 * The callback can then swallow it by returning true or allow the default handling to happen by returning false
	 * in the {@link #handleNodeClick(Node)} or {@link #handleWayClick(Way)} methods.
	 * 
	 * @author Jan
	 *
	 */
	public abstract class EasyEditActionModeCallback implements ActionMode.Callback {
		
		int helpTopic = 0;
		MenuUtil menuUtil = new MenuUtil(main);
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			Log.d("EasyEditActionModeCallback", "onCreateActionMode");
			currentActionMode = mode;
			currentActionModeCallback = this;
			main.hideLock();
			
			if (main.getBottomBar() != null) {
				View v = main.findViewById(R.id.cab_stub);
				if (v instanceof ViewStub) { // only need to inflate once
					ViewStub stub = (ViewStub) v;
					stub.setLayoutResource(R.layout.toolbar);
					stub.setInflatedId(R.id.cab_stub);
					cabBottomBar = (ActionMenuView) stub.inflate();
					Preferences prefs = new Preferences(main);
					MenuUtil.setupBottomBar(main, cabBottomBar, main.isFullScreen(), prefs.lightThemeEnabled());
				} else if (v instanceof ActionMenuView) {
					cabBottomBar = (ActionMenuView) v;
					cabBottomBar.setVisibility(View.VISIBLE);
					cabBottomBar.getMenu().clear();
				}
				main.hideBottomBar();
			}
			return false;
		}
		
		/**
		 * Override this is you want to create a custom context menu in onCreateContextMenu
		 * @return
		 */
		public boolean needsCustomContextMenu() {
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			Log.d("EasyEditActionModeCallback", "onDestroyActionMode");
			currentActionMode = null;
			currentActionModeCallback = null;
			logic.hideCrosshairs();
			main.invalidateMap();
			if (cabBottomBar != null) {
				cabBottomBar.setVisibility(View.GONE);
				main.showBottomBar();
			}
			main.showLock();
		}
		
		/**
		 * This method gets called when the map is clicked, before checking for clicked nodes/ways.
		 * The ActionModeCallback can then either return true to indicate that the click was handled (or should be ignored),
		 * or return false to indicate default handling should apply
		 * (which includes checking for node/way clicks and calling the corresponding methods).
		 * 
		 * @param x the x screen coordinate of the click
		 * @param y the y screen coordinate of the click
		 * @return true if the click has been handled, false if default handling should apply
		 */
		public boolean handleClick(float x, float y) {
			return false;
		}
		
		/**
		 * This method gets called when an OsmElement click has to be handled.
		 * The ActionModeCallback can then either return true to indicate that the click was handled (or should be ignored),
		 * or return false to indicate default handling should apply.
		 * @param element the OsmElement that was clicked
		 * @return true if the click has been handled, false if default handling should apply
		 */
		public boolean handleElementClick(OsmElement element) {
			return false;
		}
		
		/** {@inheritDoc} */ // placed here for convenience, allows to avoid unnecessary methods in subclasses
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			menuUtil.reset();
			return false;
		}
		
		/** {@inheritDoc} */ // placed here for convenience, allows to avoid unnecessary methods in subclasses
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			Log.d("EasyEditActionModeCallback", "onActionItemClicked");
			if (item.getItemId() == MENUITEM_HELP) {
				if (helpTopic != 0) {
					HelpViewer.start(main, helpTopic);
				} else {
					Toast.makeText(main, R.string.toast_nohelp, Toast.LENGTH_LONG).show(); // this is essentially just an error message
				}
			}
			return false;
		}
		
		/**
		 * modify behavior of back button in action mode
		 * @return
		 */
		public boolean onBackPressed() {
			return false;
		}
		
		public boolean processShortcut(Character c) {
			return false;
		}
		
		protected void arrangeMenu(Menu menu) {
			menuUtil.setShowAlways(menu);
		}
		
		public void onCreateContextMenu(ContextMenu menu) {
		}
	}
	
	private class LongClickActionModeCallback extends EasyEditActionModeCallback implements android.view.MenuItem.OnMenuItemClickListener {
		private static final int MENUITEM_OSB = 1;
		private static final int MENUITEM_NEWNODEWAY = 2;
		private static final int MENUITEM_SPLITWAY = 3;
		private static final int MENUITEM_PASTE = 4;
		private static final int MENUITEM_NEWNODE_GPS = 5;
		private static final int MENUITEM_NEWNODE_ADDRESS = 6;
		private static final int MENUITEM_NEWNODE_PRESET = 7;
		private static final int MENUITEM_NEWNODE_VOICE = 8;
		private float startX;
		private float startY;
		private int startLon;
		private int startLat;
		private float x;
		private float y;
		LocationManager locationManager = null;
		private List<OsmElement> clickedNodes;
		private List<Way>clickedNonClosedWays;
		
		public LongClickActionModeCallback(float x, float y) {
			super();
			this.x = x;
			this.y = y;
			clickedNodes = logic.getClickedNodes(x, y);
			clickedNonClosedWays = logic.getClickedWays(false, x, y); // 
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			helpTopic = R.string.help_longclick;
			super.onCreateActionMode(mode, menu);
			mode.setTitle(R.string.menu_add);
			mode.setSubtitle(null);
			// mode.setTitleOptionalHint(true);
			// show crosshairs 
			logic.showCrosshairs(x, y);
			startX = x;
			startY = y;
			startLon = logic.xToLonE7(x);
			startLat = logic.yToLatE7(y);
			// return isNeeded();
			// always required for paste
			return true;
		}
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			menu = replaceMenu(menu, mode, this);
			super.onPrepareActionMode(mode, menu);
			menu.clear();
			menuUtil.reset();
			Preferences prefs = new Preferences(main);
			if (prefs.voiceCommandsEnabled()) {
				menu.add(Menu.NONE, MENUITEM_NEWNODE_VOICE, Menu.NONE, R.string.menu_voice_commands).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.mic)).setEnabled(NetworkStatus.isConnected(main));
			}			
			menu.add(Menu.NONE, MENUITEM_NEWNODE_ADDRESS, Menu.NONE, R.string.tag_menu_address).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_address));
			menu.add(Menu.NONE, MENUITEM_NEWNODE_PRESET, Menu.NONE, R.string.tag_menu_preset).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_preset));
			menu.add(Menu.NONE, MENUITEM_OSB, Menu.NONE, R.string.openstreetbug_new_bug).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_bug));
			if ((clickedNonClosedWays != null && clickedNonClosedWays.size() > 0) && (clickedNodes == null || clickedNodes.size()==0) ) {
				menu.add(Menu.NONE, MENUITEM_SPLITWAY, Menu.NONE, R.string.menu_split).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_split));
			}
			menu.add(Menu.NONE, MENUITEM_NEWNODEWAY, Menu.NONE, R.string.openstreetbug_new_nodeway).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_append));
			if (!logic.clipboardIsEmpty()) {
				menu.add(Menu.NONE, MENUITEM_PASTE, Menu.NONE, R.string.menu_paste).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_paste)).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_paste));
			}
			// check if GPS is enabled
			locationManager = (LocationManager)Application.mainActivity.getSystemService(android.content.Context.LOCATION_SERVICE);
			if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				menu.add(Menu.NONE, MENUITEM_NEWNODE_GPS, Menu.NONE, R.string.menu_newnode_gps).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_gps));
			}
			menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM|10, R.string.menu_help).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_help)).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_help));
			arrangeMenu(menu);
			return true;
		}
		
		/**
		 * if we get a short click go to path creation mode
		 */
		@Override
		public boolean handleClick(float x, float y) {
			PathCreationActionModeCallback pcamc = new PathCreationActionModeCallback(logic.lonE7ToX(startLon), logic.latE7ToY(startLat));
			main.startSupportActionMode(pcamc);
			pcamc.handleClick(x, y);
			logic.hideCrosshairs();
			return true;
		}
		
		@Override
		public boolean needsCustomContextMenu() {
			return true;
		}
		
		@Override
		public void onCreateContextMenu(ContextMenu menu) {
			if (clickedNonClosedWays != null && clickedNonClosedWays.size() > 0) { 
				int id = 0;
				menu.add(Menu.NONE, id++, Menu.NONE, R.string.split_all_ways).setOnMenuItemClickListener(this);
				for (Way w:clickedNonClosedWays) {
					menu.add(Menu.NONE, id++, Menu.NONE, w.getDescription(Application.mainActivity)).setOnMenuItemClickListener(this);
				}
			}
		}	

		@Override
		public boolean onMenuItemClick(MenuItem item) {
			int itemId = item.getItemId();
			
			List<Way>ways = new ArrayList<Way>();
			if (itemId==0) {
				ways = clickedNonClosedWays;
			} else { 
				ways.add(clickedNonClosedWays.get(itemId -1));
			}
			try {
				if (logic.performAddOnWay(ways,startX, startY)) {
					Node splitPosition = logic.getSelectedNode();
					for (Way way:ways) {
						if (way.hasNode(splitPosition)) {
							logic.performSplit(way,logic.getSelectedNode());
						}
					}
				}
			} catch (OsmIllegalOperationException e) {
				e.printStackTrace();// FIXME toast or so
			}
			currentActionMode.finish();
			return false;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			super.onActionItemClicked(mode, item);
			switch (item.getItemId()) {
			case MENUITEM_OSB:
				// 
				mode.finish();
				logic.setSelectedBug(logic.makeNewBug(x, y));
				FragmentManager fm = main.getSupportFragmentManager();
				FragmentTransaction ft = fm.beginTransaction();
			    Fragment prev = fm.findFragmentByTag("fragment_bug");
			    if (prev != null) {
			        ft.remove(prev);
			    }
			    ft.commit();
		        TaskFragment bugDialog = TaskFragment.newInstance(logic.getSelectedBug());
		        bugDialog.show(fm, "fragment_bug");
				logic.hideCrosshairs();
				return true;
			case MENUITEM_NEWNODEWAY:
				main.startSupportActionMode(new PathCreationActionModeCallback(x, y));
				logic.hideCrosshairs();
				return true;
			case MENUITEM_SPLITWAY:
				if (clickedNonClosedWays.size() > 1) {
					main.getMap().showContextMenu();
				} else {
					Way way = clickedNonClosedWays.get(0);
					ArrayList<Way>ways = new ArrayList<Way>();
					ways.add(way);
					try {
						if (logic.performAddOnWay(ways,startX, startY)) {
							logic.performSplit(way,logic.getSelectedNode());
						}
					} catch (OsmIllegalOperationException e) {
						e.printStackTrace();// FIXME toast
					}
					currentActionMode.finish();
				}
				return true;
			case MENUITEM_NEWNODE_ADDRESS:
			case MENUITEM_NEWNODE_PRESET:
				logic.hideCrosshairs();
				try {
					logic.setSelectedNode(null);
					logic.performAdd(x, y);
				} catch (OsmIllegalOperationException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				Node lastSelectedNode = logic.getSelectedNode();
				if (lastSelectedNode != null) {
					main.startSupportActionMode(new NodeSelectionActionModeCallback(lastSelectedNode));
					main.performTagEdit(lastSelectedNode, null, item.getItemId() == MENUITEM_NEWNODE_ADDRESS, item.getItemId() == MENUITEM_NEWNODE_PRESET); // show preset screen or add addresses
				}
				return true;
			case MENUITEM_PASTE:
				logic.pasteFromClipboard(startX, startY);
				logic.hideCrosshairs();
				mode.finish();
				return true;
			case MENUITEM_NEWNODE_GPS:
				logic.hideCrosshairs();
				try {
					logic.setSelectedNode(null);
					logic.performAdd(x, y);
					Node node = logic.getSelectedNode();
					if (locationManager != null && node != null) {
						Location location = null;
						try {
							location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
						} catch (SecurityException sex) {
							// can be safely ignored, this is only called when GPS is enabled
						}
						if (location != null) {
							double lon = location.getLongitude();
							double lat = location.getLatitude();
							if (lon >= -180 && lon <= 180 && lat >= -GeoMath.MAX_LAT && lat <= GeoMath.MAX_LAT) {
								logic.performSetPosition(node,lon,lat);
								TreeMap<String, String> tags = new TreeMap<String, String>(node.getTags());
								if (location.hasAltitude()) {
									tags.put(Tags.KEY_ELE, String.format(Locale.US,"%.1f",location.getAltitude()));
									tags.put(Tags.KEY_ELE_MSL, String.format(Locale.US,"%.1f",location.getAltitude()));
									tags.put(Tags.KEY_SOURCE_ELE, Tags.VALUE_GPS);
								}
								tags.put(Tags.KEY_SOURCE, Tags.VALUE_GPS);
								logic.setTags(Node.NAME, node.getOsmId(), tags);
							}
						}
					}
					currentActionMode.finish();
				} catch (OsmIllegalOperationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return true;
			case MENUITEM_NEWNODE_VOICE:
				logic.hideCrosshairs();
				logic.setSelectedNode(null);
				Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
				intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
				try {
					main.startActivityForResult(intent, Main.VOICE_RECOGNITION_REQUEST_CODE);
				} catch (Exception ex) {
					Log.d("EasyEdit","Caught exception " + ex);
					Toast.makeText(main,"No voice recognition facility present", Toast.LENGTH_LONG).show();
					logic.showCrosshairs(startX, startY);
				}
				return true;
			default:
				Log.e("LongClickActionModeCallback", "Unknown menu item");
				break;
			}
			return false;
		}
		
		/**
		 * Path creation action mode is ending
		 */
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			logic.setSelectedNode(null);
			super.onDestroyActionMode(mode);
		}
		
		/**
		 * FIXME This is still very hackish with lots of code duplication
		 * @param requestCode
		 * @param resultCode
		 * @param data
		 */
		void handleActivityResult(final int requestCode, final int resultCode, final Intent data) {
			ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

			// 
			StorageDelegator storageDelegator = Application.getDelegator();
			for (String v:matches) {
				String[] words = v.split("\\s+", 2);
				if (words.length > 0) {
					// 
					String first = words[0];
					try {
						int number = Integer.parseInt(first);
						// worked if there is a further word(s) simply add it/them
						Toast.makeText(main,+ number  + (words.length == 2?words[1]:""), Toast.LENGTH_LONG).show();
						Node node = logic.performAddNode(startLon/1E7D, startLat/1E7D);
						if (node != null) {
							TreeMap<String, String> tags = new TreeMap<String, String>(node.getTags());
							tags.put(Tags.KEY_ADDR_HOUSENUMBER, "" + number  + (words.length == 3?words[2]:""));
							tags.put("source:original_text", v);
							LinkedHashMap<String, ArrayList<String>> map = Address.predictAddressTags(main, Node.NAME, node.getOsmId(), 
									new ElementSearch(new int[]{node.getLon(),node.getLat()}, true), 
									Util.getArrayListMap(tags), Address.NO_HYSTERESIS);
							tags = new TreeMap<String, String>();
							for (String key:map.keySet()) {
								tags.put(key, map.get(key).get(0));
							}
							logic.setTags(Node.NAME, node.getOsmId(), tags);
							main.startSupportActionMode(new NodeSelectionActionModeCallback(node));
							return;
						}
					} catch (Exception ex) {
						// ok wasn't a number
					}

					List<PresetItem> presetItems = SearchIndexUtils.searchInPresets(main, first,ElementType.NODE,2,1);
					
					if (presetItems != null && presetItems.size()==1) {		
						Node node = addNode(logic.performAddNode(startLon/1E7D, startLat/1E7D), words.length == 2? words[1]:null, presetItems.get(0), logic, v);
						if (node != null) {
							main.startSupportActionMode(new NodeSelectionActionModeCallback(node));
							return;
						} 
					}
				
					Map<String, NameAndTags> namesSearchIndex = Application.getNameSearchIndex(main);
					if (namesSearchIndex == null) {
						return;
					}
					// search in names
					NameAndTags nt = SearchIndexUtils.searchInNames(main, v, 2);
					if (nt != null) {
						HashMap<String, String> map = new HashMap<String, String>();
						map.putAll(nt.getTags());
						PresetItem pi = Preset.findBestMatch(Application.getCurrentPresets(main), map);
						if (pi != null) {
							Node node = addNode(logic.performAddNode(startLon/1E7D, startLat/1E7D), nt.getName(), pi, logic, v);
							if (node != null) {
								// set tags from name suggestions
								Map<String,String> tags = new TreeMap<String, String>(node.getTags());
								for (String k:map.keySet()) {
									tags.put(k, map.get(k));
								}
								storageDelegator.setTags(node,tags); // note doesn't create a new undo checkpoint
								main.startSupportActionMode(new NodeSelectionActionModeCallback(node));
								return;
							}
						}
					}
				}
			}
			logic.showCrosshairs(startX, startY); // re-show the cross hairs nothing found/something went wrong
		}
		
		Node addNode(Node node, String name, PresetItem pi, Logic logic, String original) {
			if (node != null) {
				Toast.makeText(main, pi.getName()  + (name != null? " name: " + name:""), Toast.LENGTH_LONG).show();
				if (node != null) {
					TreeMap<String, String> tags = new TreeMap<String, String>(node.getTags());
					for (Entry<String, StringWithDescription> tag : pi.getFixedTags().entrySet()) {
						tags.put(tag.getKey(), tag.getValue().getValue());
					}
					if (name != null) {
						tags.put(Tags.KEY_NAME, name);
					}
					tags.put("source:original_text", original);
					logic.setTags(Node.NAME, node.getOsmId(), tags);
					logic.setSelectedNode(node);
					return node;
				}
			}
			return null;
		}
		
		public boolean processShortcut(Character c) {
			if (c == Util.getShortCut(main, R.string.shortcut_paste)) {
				logic.pasteFromClipboard(startX, startY);
				logic.hideCrosshairs();
				if (currentActionMode != null) {
					currentActionMode.finish();
				}
				return true;
			}
			return false;
		}
	}
	
	/**
	 * This callback handles path creation. It is started after a long-press.
	 * During this action mode, clicks are handled by custom code.
	 * The node and way click handlers are thus never called.
	 */
	private class PathCreationActionModeCallback extends EasyEditActionModeCallback {
		private static final int MENUITEM_UNDO = 1;
		private static final int MENUITEM_NEWWAY_PRESET = 2;
		
		/** x coordinate of first node */
		private float x;
		/** y coordinate of first node */
		private float y;
		/** Node to append to */
		private Node appendTargetNode;
		/** Way to append to */
		private Way appendTargetWay;
		
		/** contains a pointer to the created way if one was created. used to fix selection after undo. */
		private Way createdWay = null;
		/** contains a list of created nodes. used to fix selection after undo. */
		private ArrayList<Node> createdNodes = new ArrayList<Node>();
		
		public PathCreationActionModeCallback(float x, float y) {
			super();
			this.x = x;
			this.y = y;
			appendTargetNode = null;
			appendTargetWay = null;
		}
		
		public PathCreationActionModeCallback(Node node) {
			super();
			appendTargetNode = node;
			appendTargetWay = null;
		}
		
		public PathCreationActionModeCallback(Way way, Node node) {
			super();
			appendTargetNode = node;
			appendTargetWay = way;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			helpTopic = R.string.help_pathcreation;
			super.onCreateActionMode(mode, menu);
			mode.setSubtitle(R.string.actionmode_createpath);
			logic.setSelectedWay(null);
			logic.setSelectedNode(appendTargetNode);
			if (appendTargetNode != null) {
				if (appendTargetWay != null) {
					logic.performAppendStart(appendTargetWay, appendTargetNode);
				} else {
					logic.performAppendStart(appendTargetNode);
				}
			} else {
				try {
					pathCreateNode(x, y);
				} catch (OsmIllegalOperationException e) {
					Toast.makeText(main, e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
			logic.hideCrosshairs();
			return true;
		}
		
		@Override
		public boolean handleClick(float x, float y) {
			super.handleClick(x, y);
			try {
				pathCreateNode(x, y);
			} catch (OsmIllegalOperationException e) {
				Toast.makeText(main, e.getMessage(), Toast.LENGTH_LONG).show();
			}
			return true;
		}
		
		/**
		 * Creates/adds a node into a path during path creation
		 * @param x x screen coordinate
		 * @param y y screen coordinate
		 * @throws OsmIllegalOperationException 
		 */
		private void pathCreateNode(float x, float y) throws OsmIllegalOperationException {
			Node lastSelectedNode = logic.getSelectedNode();
			Way lastSelectedWay = logic.getSelectedWay();
			if (appendTargetNode != null) {
				logic.performAppendAppend(x, y);
			} else {
				logic.performAdd(x, y);
			}
			if (logic.getSelectedNode() == null) {
				// user clicked last node again -> finish adding
				if (currentActionMode != null) // TODO for unknown reasons this now and then seems to be null
					currentActionMode.finish();
				tagApplicable(lastSelectedNode, lastSelectedWay, true); 
			} else { // update cache for undo
				createdWay = logic.getSelectedWay();
				if (createdWay == null) {	
					createdNodes = new ArrayList<Node>();
				}
				createdNodes.add(logic.getSelectedNode());		
			}
			main.invalidateMap();
		}
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			menu = replaceMenu(menu, mode, this);
			super.onPrepareActionMode(mode, menu);
			menu.clear();
			menuUtil.reset();
			menu.add(Menu.NONE, MENUITEM_UNDO, Menu.NONE, R.string.undo).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_undo)).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_undo));
			menu.add(Menu.NONE, MENUITEM_NEWWAY_PRESET, Menu.NONE, R.string.tag_menu_preset).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_preset));
			menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM|10, R.string.menu_help).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_help)).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_help));
			arrangeMenu(menu);
			return true;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			super.onActionItemClicked(mode, item);
			switch (item.getItemId()) {
			case MENUITEM_UNDO:
				handleUndo();
				break;
			case MENUITEM_NEWWAY_PRESET:
				logic.hideCrosshairs();
				try {
					logic.setSelectedNode(null);
					logic.performAdd(x, y);
				} catch (OsmIllegalOperationException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				Way lastSelectedWay = logic.getSelectedWay();
				if (lastSelectedWay != null) {
					main.startSupportActionMode(new WaySelectionActionModeCallback(lastSelectedWay));
					main.performTagEdit(lastSelectedWay, null, false, item.getItemId() == MENUITEM_NEWWAY_PRESET); // show preset screen
				}
				return true;
			default:
				Log.e("PathCreationActionModeCallback", "Unknown menu item");
				break;
			}
			return false;
		}
		
		private void handleUndo() {
			logic.undo();
			if (logic.getSelectedNode() == null) { // should always happen when we added a new node and removed it
				Iterator<Node> nodeIterator = createdNodes.iterator();
				while (nodeIterator.hasNext()) { // remove nodes that do not exist anymore
					if (!logic.exists(nodeIterator.next())) nodeIterator.remove();
				}
			} else {
				// remove existing node from list
				createdNodes.remove(logic.getSelectedNode());
			}
			// exit or select the previous node
			if (createdNodes.isEmpty()) {
				logic.setSelectedNode(null);
				// all nodes have been deleted, cancel action mode
				if (currentActionMode != null) { //TODO shouldn't happen but does
					currentActionMode.finish();
				}
			} else {
				// select last node
				logic.setSelectedNode(createdNodes.get(createdNodes.size()-1));
			}

			createdWay = logic.getSelectedWay(); // will be null if way was deleted by undo
			main.invalidateMap();
		}
		
		/**
		 * Path creation action mode is ending
		 */
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			Node lastSelectedNode = logic.getSelectedNode();
			Way lastSelectedWay = logic.getSelectedWay();
			logic.setSelectedWay(null);
			logic.setSelectedNode(null);
			super.onDestroyActionMode(mode);
			if (appendTargetNode == null) { // doesn't work as intended element selected modes get zapped, don't try to select because of this
				tagApplicable(lastSelectedNode, lastSelectedWay, false); 
			}
		}
	}
	
	/**
	 * This action mode handles element selection. When a node or way should be selected, just start this mode.
	 * The element will be automatically selected, and a second click on the same element will open the tag editor.
	 * @author Jan
	 *
	 */
	private abstract class ElementSelectionActionModeCallback extends EasyEditActionModeCallback {
		private static final int MENUITEM_UNDO = 0;
		private static final int MENUITEM_TAG = 1;
		private static final int MENUITEM_DELETE = 2;
		private static final int MENUITEM_HISTORY = 3; 
		private static final int MENUITEM_COPY = 4;
		private static final int MENUITEM_CUT = 5;
		private static final int MENUITEM_RELATION = 6;
		private static final int MENUITEM_EXTEND_SELECTION = 7;
		private static final int MENUITEM_ELEMENT_INFO = 8;
		
		private static final int MENUITEM_TAG_LAST = 21;
		private static final int MENUITEM_ZOOM_TO_SELECTION = 22;
		private static final int MENUITEM_PREFERENCES = 23;
		
		protected OsmElement element = null;
		
		protected boolean deselect = true;
		
		protected UndoListener undoListener; 
		
		public ElementSelectionActionModeCallback(OsmElement element) {
			super();
			this.element = element;
			undoListener = main.new UndoListener(); 
		}
		
		/**
		 * Internal helper to avoid duplicate code in {@link #handleNodeClick(Node)} and {@link #handleWayClick(Way)}.
		 * @param element clicked element
		 * @return true if handled, false if default handling should apply
		 */
		@Override
		public boolean handleElementClick(OsmElement element) {
			super.handleElementClick(element);
			if (element == this.element) {
				main.performTagEdit(element, null, false, false);
				return true;
			}
			return false;
		}
		
		@SuppressLint("InflateParams")
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			menu = replaceMenu(menu, mode, this);
			super.onPrepareActionMode(mode, menu);
			menu.clear();
			menuUtil.reset();
			
			main.getMenuInflater().inflate(R.menu.undo_action, menu);
			MenuItem undo = menu.findItem(R.id.undo_action);
			if (logic.getUndo().canUndo() || logic.getUndo().canRedo()) {
				undo.setVisible(true);
				undo.setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_undo));
			} else {
				undo.setVisible(false);
			}
			View undoView = MenuItemCompat.getActionView(undo);
			if (undoView == null) { // FIXME this is a temp workaround for pre-11 Android, we could probably simply always do the following 
				Preferences prefs = new Preferences(main);
				Context context =  new ContextThemeWrapper(main, prefs.lightThemeEnabled() ? R.style.Theme_customMain_Light : R.style.Theme_customMain);
				undoView =  ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.undo_action_view, null);
			}
			undoView.setOnClickListener(undoListener);
			undoView.setOnLongClickListener(undoListener);
			
			menu.add(Menu.NONE, MENUITEM_TAG, Menu.NONE, R.string.menu_tags).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_tagedit)).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_tags));
			menu.add(Menu.NONE, MENUITEM_DELETE, Menu.CATEGORY_SYSTEM, R.string.delete).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_delete));
			// disabled for now menu.add(Menu.NONE, MENUITEM_TAG_LAST, Menu.NONE, R.string.tag_menu_repeat).setIcon(R.drawable.tag_menu_repeat);
			if (!(element instanceof Relation)) {
				menu.add(Menu.NONE, MENUITEM_COPY, Menu.CATEGORY_SECONDARY, R.string.menu_copy).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_copy)).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_copy));
				menu.add(Menu.NONE, MENUITEM_CUT, Menu.CATEGORY_SECONDARY, R.string.menu_cut).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_cut)).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_cut));
			}
			menu.add(GROUP_BASE, MENUITEM_EXTEND_SELECTION, Menu.CATEGORY_SYSTEM, R.string.menu_extend_selection).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_multi_select));
			menu.add(Menu.NONE, MENUITEM_RELATION, Menu.CATEGORY_SYSTEM, R.string.menu_relation).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_relation));
			if (element.getOsmId() > 0) {
				menu.add(GROUP_BASE, MENUITEM_HISTORY, Menu.CATEGORY_SYSTEM, R.string.menu_history).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_history)).setEnabled(NetworkStatus.isConnected(Application.mainActivity));
			}
			menu.add(GROUP_BASE, MENUITEM_ELEMENT_INFO, Menu.CATEGORY_SYSTEM, R.string.menu_information).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_info)).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_information));
			// menu.add(GROUP_BASE, MENUITEM_ZOOM_TO_SELECTION,  Menu.CATEGORY_SYSTEM|10, "Zoom to selection");
			menu.add(GROUP_BASE, MENUITEM_PREFERENCES, Menu.CATEGORY_SYSTEM|10, R.string.menu_config).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_config));
			menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM|10, R.string.menu_help).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_help)).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_help));
			return true;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			super.onActionItemClicked(mode, item);
			switch (item.getItemId()) {
			case MENUITEM_TAG: main.performTagEdit(element, null, false, false); break;
			case MENUITEM_TAG_LAST: main.performTagEdit(element, null, true, false); break;
			case MENUITEM_DELETE: menuDelete(mode); break;
			case MENUITEM_HISTORY: showHistory(); break;
			case MENUITEM_COPY: logic.copyToClipboard(element); currentActionMode.finish(); break;
			case MENUITEM_CUT: logic.cutToClipboard(element); currentActionMode.finish(); break;
			case MENUITEM_RELATION: main.startSupportActionMode(new  AddRelationMemberActionModeCallback(element)); break;
			case MENUITEM_EXTEND_SELECTION: deselect = false; main.startSupportActionMode(new  ExtendSelectionActionModeCallback(element)); break;
			case MENUITEM_ELEMENT_INFO: ElementInfo.showDialog(main,element); break;
			case MENUITEM_PREFERENCES: 	PrefEditor.start(main); break;
			case R.id.undo_action:
				// should not happen
				Log.d("EasyEditManager.ElementSelectionActionModeCallback","menu undo clicked");
				undoListener.onClick(null);
				break;
			default: return false;
			}
			return true;
		}
		
		protected abstract void menuDelete(ActionMode mode);
		
		/**
		 * Opens the history page of the selected element in a browser
		 */
		private void showHistory() {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(main.getBaseURL()+"browse/"+element.getName()+"/"+element.getOsmId()+"/history"));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			main.startActivity(intent);
		}
		
		/**
		 * Element selection action mode is ending
		 */
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			logic.setClickableElements(null);
			logic.setReturnRelations(true);			
			if (deselect) {
				Log.d("EasyEditManager.ElementSelectionCallback","deselecting");
				logic.setSelectedNode(null);
				logic.setSelectedWay(null);
				logic.setSelectedRelation(null);
				logic.setSelectedRelationWays(null);
				logic.setSelectedRelationNodes(null);
				logic.setSelectedRelationRelations(null);
			}
			super.onDestroyActionMode(mode);
		}
		
		public boolean processShortcut(Character c) {
			if (c == Util.getShortCut(main, R.string.shortcut_copy)) {
				logic.copyToClipboard(element); currentActionMode.finish();
				return true;
			} else if (c == Util.getShortCut(main, R.string.shortcut_cut)) {
				logic.cutToClipboard(element); currentActionMode.finish();
				return true;
			} else if (c == Util.getShortCut(main, R.string.shortcut_info)) {
				ElementInfo.showDialog(main,element); 
				return true;
			}  else if (c == Util.getShortCut(main, R.string.shortcut_tagedit)) {
				main.performTagEdit(element, null, false, false);
				return true;
			}
			return false;
		}
	}
	
	private class NodeSelectionActionModeCallback extends ElementSelectionActionModeCallback {
		private static final int MENUITEM_APPEND = 9;
		private static final int MENUITEM_JOIN = 10;
		private static final int MENUITEM_UNJOIN = 11;
		private static final int MENUITEM_EXTRACT = 12;
		
		private static final int MENUITEM_SET_POSITION = 15;
		private static final int MENUITEM_ADDRESS = 16;
		
		private OsmElement joinableElement = null;
		
		private NodeSelectionActionModeCallback(Node node) {
			super(node);
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			helpTopic = R.string.help_nodeselection;
			super.onCreateActionMode(mode, menu);
			logic.setSelectedNode((Node)element);
			logic.setSelectedWay(null);
			logic.setSelectedRelationWays(null);
			logic.setSelectedRelationNodes(null);
			main.invalidateMap();
			mode.setTitle(R.string.actionmode_nodeselect);
			mode.setSubtitle(null);
			// mode.setTitleOptionalHint(true); // no need to display the title, only available in 4.1 up
			return true;
		}
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			menu = replaceMenu(menu, mode, this);
			
			super.onPrepareActionMode(mode, menu);
			if (((Node)element).getTags().containsKey(Tags.KEY_ENTRANCE) && !((Node)element).getTags().containsKey(Tags.KEY_ADDR_HOUSENUMBER)) {
				menu.add(Menu.NONE, MENUITEM_ADDRESS, Menu.NONE, R.string.tag_menu_address).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_address));
			}
			if (logic.isEndNode((Node)element)) {
				menu.add(Menu.NONE, MENUITEM_APPEND, Menu.NONE, R.string.menu_append).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_append));
			}
			joinableElement = logic.findJoinableElement((Node)element);
			if (joinableElement != null) {
				menu.add(Menu.NONE, MENUITEM_JOIN, Menu.NONE, R.string.menu_join).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_merge)).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_merge));
			}
			int wayMembershipCount = logic.getWaysForNode((Node)element).size();
			if (wayMembershipCount > 1) {
				menu.add(Menu.NONE, MENUITEM_UNJOIN, Menu.NONE, R.string.menu_unjoin).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_split));
			}
			if (wayMembershipCount > 0) {
				menu.add(Menu.NONE, MENUITEM_EXTRACT, Menu.NONE, R.string.menu_extract).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_extract_node));
			}
			menu.add(Menu.NONE, MENUITEM_SET_POSITION, Menu.CATEGORY_SYSTEM, R.string.menu_set_position).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_gps));
			arrangeMenu(menu);
			return true;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if (!super.onActionItemClicked(mode, item)) {
				switch (item.getItemId()) {
				case MENUITEM_APPEND:
					main.startSupportActionMode(new PathCreationActionModeCallback((Node)element));
					break;
				case MENUITEM_JOIN:
					try {
						if (!logic.performJoin(joinableElement, (Node) element)) {
							Toast.makeText(main,
									R.string.toast_merge_tag_conflict,
									Toast.LENGTH_LONG).show();
							main.performTagEdit(element, null, false, false);
						} else {
							mode.finish();
						}
					} catch (OsmIllegalOperationException e) {
						Toast.makeText(main, e.getMessage(), Toast.LENGTH_LONG).show();
					}
					break;
				case MENUITEM_UNJOIN:
					logic.performUnjoin((Node)element);
					mode.finish();
					break;
				case MENUITEM_EXTRACT:
					logic.performExtract((Node)element);
					invalidate();
					break;
				case MENUITEM_SET_POSITION: 
					setPosition(); 
					break;
				case MENUITEM_ADDRESS: main.performTagEdit(element, null, true, false); break;
				default: return false;
				}
			}
			return true;
		}
		
		@Override
		protected void menuDelete(ActionMode mode) {
			if (element.hasParentRelations()) {
				new AlertDialog.Builder(main)
					.setTitle(R.string.delete)
					.setMessage(R.string.deletenode_relation_description)
					.setPositiveButton(R.string.deletenode,
						new DialogInterface.OnClickListener() {	
							@Override
							public void onClick(DialogInterface dialog, int which) {
								logic.performEraseNode((Node)element, true);
								currentActionMode.finish();
							}
						})
					.show();
			} else {
				logic.performEraseNode((Node)element, true);
				mode.finish();
			}
		}
		
		private void setPosition() {
			if (element instanceof Node) {
				// show dialog to set lon/lat
				createSetPositionDialog(((Node)element).getLon(), ((Node)element).getLat()).show();
			}
		}
		
		@SuppressLint("InflateParams")
		AppCompatDialog 	createSetPositionDialog(int lonE7, int latE7) {
			final LayoutInflater inflater = ThemeUtils.getLayoutInflater(Application.mainActivity);
			Builder dialog = new AlertDialog.Builder(Application.mainActivity);
			dialog.setTitle(R.string.menu_set_position);
			
			View layout = inflater.inflate(R.layout.set_position, null);
			dialog.setView(layout);
			TextView datum = (TextView) layout.findViewById(R.id.set_position_datum); // TODO add conversion to/from other datums
			datum.setText("WGS84");
			EditText lon = (EditText) layout.findViewById(R.id.set_position_lon);
			lon.setText(String.format(Locale.US,"%.7f", lonE7/1E7d));
			EditText lat = (EditText) layout.findViewById(R.id.set_position_lat);
			lat.setText(String.format(Locale.US,"%.7f", latE7/1E7d));
			
			dialog.setPositiveButton(R.string.set, createSetButtonListener(lon, lat, (Node)element));		
			dialog.setNegativeButton(R.string.cancel, null);
	
			return dialog.create();
		}
		
		/**
		 * Create an onClick listener that sets the coordnaties in the node 
		 * @return the OnClickListnener
		 */
		private OnClickListener createSetButtonListener(final EditText lonField, final EditText latField, final Node node) {
			return new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					double lon = Double.valueOf(lonField.getText().toString());
					double lat = Double.valueOf(latField.getText().toString());
					if (lon >= -180 && lon <= 180 && lat >= -GeoMath.MAX_LAT && lat <= GeoMath.MAX_LAT) {
						logic.performSetPosition(node,lon,lat);
						invalidate();
					} else {
						createSetPositionDialog((int)(lon*1E7), (int)(lat*1E7)).show();
						Toast.makeText(main, R.string.coordinates_out_of_range, Toast.LENGTH_LONG).show();
					}
				}
			};
		}
	}
	
	private class WaySelectionActionModeCallback extends ElementSelectionActionModeCallback {
		private static final int MENUITEM_SPLIT = 9;
		private static final int MENUITEM_MERGE = 10;
		private static final int MENUITEM_REVERSE = 11;
		private static final int MENUITEM_APPEND = 12;
		private static final int MENUITEM_RESTRICTION = 13;
		private static final int MENUITEM_ROTATE = 14;
		private static final int MENUITEM_ORTHOGONALIZE = 15;
		private static final int MENUITEM_CIRCULIZE = 16;
		private static final int MENUITEM_SPLIT_POLYGON = 17;
		private static final int MENUITEM_ADDRESS = 18;
		
		private Set<OsmElement> cachedMergeableWays;
		private Set<OsmElement> cachedAppendableNodes;
		private Set<OsmElement> cachedViaElements;
		
		private WaySelectionActionModeCallback(Way way) {
			super(way);
			Log.d("WaySelectionActionCallback", "constructor");
			cachedMergeableWays = findMergeableWays(way);
			cachedAppendableNodes = findAppendableNodes(way);
			cachedViaElements = findViaElements(way);
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			helpTopic = R.string.help_wayselection;
			super.onCreateActionMode(mode, menu);
			Log.d("WaySelectionActionCallback", "onCreateActionMode");
			logic.setSelectedNode(null);
			logic.setSelectedRelationWays(null);
			logic.setSelectedRelationNodes(null);
			logic.setSelectedWay((Way)element);
			main.invalidateMap();
			mode.setTitle(R.string.actionmode_wayselect);
			mode.setSubtitle(null);
			return true;
		}
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			menu = replaceMenu(menu, mode, this);
			super.onPrepareActionMode(mode, menu);
			Log.d("WaySelectionActionCallback", "onPrepareActionMode");
			if (((Way)element).getTags().containsKey(Tags.KEY_BUILDING) && !((Way)element).getTags().containsKey(Tags.KEY_ADDR_HOUSENUMBER)) {
				menu.add(Menu.NONE, MENUITEM_ADDRESS, Menu.NONE, R.string.tag_menu_address).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_address));
			}
			menu.add(Menu.NONE, MENUITEM_REVERSE, Menu.NONE, R.string.menu_reverse).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_reverse));
			if (((Way)element).getNodes().size() > 2) {
				menu.add(Menu.NONE, MENUITEM_SPLIT, Menu.NONE, R.string.menu_split).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_split));
			}
			if (cachedMergeableWays.size() > 0) {
				menu.add(Menu.NONE, MENUITEM_MERGE, Menu.NONE, R.string.menu_merge).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_merge));
			}
			if (cachedAppendableNodes.size() > 0) {
				menu.add(Menu.NONE, MENUITEM_APPEND, Menu.NONE, R.string.menu_append).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_append));
			}
			if (((Way)element).getTagWithKey(Tags.KEY_HIGHWAY) != null && (cachedViaElements.size() > 0)) {
				menu.add(Menu.NONE, MENUITEM_RESTRICTION, Menu.NONE, R.string.menu_restriction).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_add_restriction));	
			}
			if (((Way)element).getNodes().size() > 2) {
				menu.add(Menu.NONE, MENUITEM_ORTHOGONALIZE, Menu.NONE, R.string.menu_orthogonalize).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_ortho));
			}
			menu.add(Menu.NONE, MENUITEM_ROTATE, Menu.NONE, R.string.menu_rotate).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_rotate));
			if (((Way)element).getNodes().size() > 3 && ((Way)element).isClosed()) {
				menu.add(Menu.NONE, MENUITEM_CIRCULIZE, Menu.NONE, R.string.menu_circulize);
				if (((Way)element).getNodes().size() > 4) { // 5 nodes is the minimum required to be able to split in to two polygons
					menu.add(Menu.NONE, MENUITEM_SPLIT_POLYGON, Menu.NONE, R.string.menu_split_polygon);
				}
			}
			arrangeMenu(menu);
			return true;
		}
		
		private void reverseWay() {
			final Way way = (Way) element;
			if (way.notReversable()) {
				new AlertDialog.Builder(main)
				.setTitle(R.string.menu_reverse)
				.setMessage(R.string.notreversable_description)
				.setPositiveButton(R.string.reverse_anyway,
					new DialogInterface.OnClickListener() {	
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (logic.performReverse(way)) { // true if it had oneway tag
								Toast.makeText(main, R.string.toast_oneway_reversed, Toast.LENGTH_LONG).show();
								main.performTagEdit(way, null, false, false);
							}
						}
					})
				.show();		
			} else if (logic.performReverse(way)) { // true if it had oneway tag
				Toast.makeText(main, R.string.toast_oneway_reversed, Toast.LENGTH_LONG).show();
				main.performTagEdit(way, null, false, false);
			} else {
				invalidate(); // sucessful reverseal update menubar 
			}
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if (!super.onActionItemClicked(mode, item)) {
				switch (item.getItemId()) {
				case MENUITEM_SPLIT: main.startSupportActionMode(new WaySplittingActionModeCallback((Way)element, false)); break;
				case MENUITEM_MERGE: main.startSupportActionMode(new WayMergingActionModeCallback((Way)element, cachedMergeableWays)); break;
				case MENUITEM_REVERSE: reverseWay(); break;
				case MENUITEM_APPEND: main.startSupportActionMode(new WayAppendingActionModeCallback((Way)element, cachedAppendableNodes)); break;
				case MENUITEM_RESTRICTION: main.startSupportActionMode(new  RestrictionFromElementActionModeCallback((Way)element, cachedViaElements)); break;
				case MENUITEM_ROTATE: logic.setRotationMode(); logic.showCrosshairsForCentroid(); break;
				case MENUITEM_ORTHOGONALIZE: logic.performOrthogonalize((Way)element); invalidate(); break;
				case MENUITEM_CIRCULIZE: logic.performCirculize((Way)element); invalidate(); break;
				case MENUITEM_SPLIT_POLYGON: main.startSupportActionMode(new WaySplittingActionModeCallback((Way)element, true)); break;
				case MENUITEM_ADDRESS: main.performTagEdit(element, null, true, false); break;
				default: return false;
				}
			}
			return true;
		}
		
		@Override
		protected void menuDelete(ActionMode mode) {
			boolean isRelationMember = element.hasParentRelations();
			boolean allNodesDownloaded = logic.isInDownload((Way)element);
			
			if ( allNodesDownloaded) {
				new AlertDialog.Builder(main)
					.setTitle(R.string.delete)
					.setMessage(isRelationMember ? R.string.deleteway_relation_description : R.string.deleteway_description)
					.setPositiveButton(R.string.deleteway_wayonly,
						new DialogInterface.OnClickListener() {	
							@Override
							public void onClick(DialogInterface dialog, int which) {
								logic.performEraseWay((Way)element, false, true);
								currentActionMode.finish();
							}
						})
					.setNeutralButton(R.string.deleteway_wayandnodes,
						new DialogInterface.OnClickListener() {	
							@Override
							public void onClick(DialogInterface dialog, int which) {
								logic.performEraseWay((Way)element, true, true);
								currentActionMode.finish();
							}
						})
					.show();
			} else {
				new AlertDialog.Builder(main)
				.setTitle(R.string.delete)
				.setMessage(R.string.deleteway_nodesnotdownloaded_description)
				.setPositiveButton(R.string.okay, null)
				.show();
			}
		}	
	}
	
	private class WaySplittingActionModeCallback extends EasyEditActionModeCallback {
		private Way way;
		private List<OsmElement> nodes = new ArrayList<OsmElement>();
		private boolean createPolygons = false;
		
		public WaySplittingActionModeCallback(Way way, boolean createPolygons) {
			super();
			this.way = way;
			nodes.addAll(way.getNodes());
			if (!way.isClosed()) { 
				// remove first and last node
				nodes.remove(0);
				nodes.remove(nodes.size()-1);
			} else {
				this.createPolygons = createPolygons;
			}
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			helpTopic = R.string.help_waysplitting;
			super.onCreateActionMode(mode, menu);
			if (way.isClosed())
				mode.setSubtitle(R.string.menu_closed_way_split_1);
			else
				mode.setSubtitle(R.string.menu_split);
			logic.setClickableElements(new HashSet<OsmElement>(nodes));
			logic.setReturnRelations(false);
			return true;
		}
		
		@Override
		public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid nodes can be clicked
			super.handleElementClick(element);
			// protect against race conditions
			if (!(element instanceof Node)) {
				// TODO fix properly
				return false;
			}
			if (way.isClosed())
				main.startSupportActionMode(new ClosedWaySplittingActionModeCallback(way, (Node) element, createPolygons));
			else {
				logic.performSplit(way, (Node)element);
				currentActionMode.finish();
			}
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			logic.setClickableElements(null);
			logic.setReturnRelations(true);
			super.onDestroyActionMode(mode);
		}	
	}
	
	private class ClosedWaySplittingActionModeCallback extends EasyEditActionModeCallback {
		private Way way;
		private Node node;
		private Set<OsmElement> nodes = new HashSet<OsmElement>();
		private boolean createPolygons = false;
		
		public ClosedWaySplittingActionModeCallback(Way way, Node node, boolean createPolygons) {
			super();
			this.way = way;
			this.node = node;
			this.createPolygons = createPolygons;
			List<Node> allNodes = way.getNodes();
			nodes.addAll(allNodes);
			if (createPolygons) { // remove neighbouring nodes
				if (way.isEndNode(node)) { // we have at least 4 nodes so this will not cause problems
					nodes.remove(allNodes.get(1)); // remove 2nd element
					nodes.remove(allNodes.get(allNodes.size()-2)); // remove 2nd last element
				} else {
					int nodeIndex = allNodes.indexOf(node);
					nodes.remove(allNodes.get(nodeIndex-1));
					nodes.remove(allNodes.get(nodeIndex+1));
				}
			}
			nodes.remove(node);	
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			helpTopic = R.string.help_closedwaysplitting;
			super.onCreateActionMode(mode, menu);
			mode.setSubtitle(R.string.menu_closed_way_split_2);
			logic.setClickableElements(nodes);
			logic.setReturnRelations(false);
			return true;
		}
		
		@Override
		public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid nodes can be clicked
			super.handleElementClick(element);
			Way[] result = logic.performClosedWaySplit(way, node, (Node)element, createPolygons);
			if (result!= null && result.length == 2) {
				logic.setSelectedNode(null);
				logic.setSelectedRelation(null);
				logic.setSelectedWay(result[0]);
				logic.addSelectedWay(result[1]);
				ArrayList<OsmElement> selection = new ArrayList<OsmElement>(); 
				selection.addAll(logic.getSelectedWays());
				main.startSupportActionMode(new ExtendSelectionActionModeCallback(selection));
			} else { //FIXME toast here?
				Log.d(DEBUG_TAG,"split failed");
				currentActionMode.finish();
			}
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			logic.setClickableElements(null);
			logic.setReturnRelations(true);
			super.onDestroyActionMode(mode);
		}	
	}
	
	private class WayMergingActionModeCallback extends EasyEditActionModeCallback {
		private Way way;
		private Set<OsmElement> ways;
		
		public WayMergingActionModeCallback(Way way, Set<OsmElement> mergeableWays) {
			super();
			this.way = way;
			ways = mergeableWays;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			helpTopic = R.string.help_mergingways;
			mode.setSubtitle(R.string.menu_merge);
			logic.setClickableElements(ways);
			logic.setReturnRelations(false);
			super.onCreateActionMode(mode, menu);
			return true;
		}
		
		@Override
		public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid ways can be clicked
			super.handleElementClick(element);
			// race conditions with touch events seem to make the impossible possible
			//TODO fix properly
			if (!(element instanceof Way)) {
				return false;
			}
			if (!findMergeableWays(way).contains((Way)element)) {
				return false;
			}
			try {
				if (!logic.performMerge(way, (Way)element)) {
					Toast.makeText(main, R.string.toast_merge_tag_conflict, Toast.LENGTH_LONG).show();
					if (way.getState() != OsmElement.STATE_DELETED)
						main.performTagEdit(way, null, false, false);
					else
						main.performTagEdit(element, null, false, false);
				} else {
					if (way.getState() != OsmElement.STATE_DELETED)
						main.startSupportActionMode(new WaySelectionActionModeCallback(way));
					else
						main.startSupportActionMode(new WaySelectionActionModeCallback((Way)element));
				}
			} catch (OsmIllegalOperationException e) {
				Toast.makeText(main, e.getMessage(), Toast.LENGTH_LONG).show();
			} catch (NotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			logic.setClickableElements(null);
			logic.setReturnRelations(true);
			super.onDestroyActionMode(mode);
		}
	}
	
	private class WayAppendingActionModeCallback extends EasyEditActionModeCallback {
		private Way way;
		private Set<OsmElement> nodes;
		public WayAppendingActionModeCallback(Way way, Set<OsmElement> appendNodes) {
			super();
			this.way = way;
			nodes = appendNodes;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			helpTopic = R.string.help_appendtoway;
			mode.setSubtitle(R.string.menu_append);
			logic.setClickableElements(nodes);
			logic.setReturnRelations(false);
			super.onCreateActionMode(mode, menu);
			return true;
		}
		
		@Override
		public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid nodes can be clicked
			super.handleElementClick(element);
			main.startSupportActionMode(new PathCreationActionModeCallback(way, (Node)element));
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			logic.setClickableElements(null);
			logic.setReturnRelations(true);
			super.onDestroyActionMode(mode);
		}
	}
	
	private class RelationSelectionActionModeCallback extends ElementSelectionActionModeCallback {
	
		private static final int MENUITEM_ADD_RELATION_MEMBERS = 9;
		private static final int MENUITEM_SELECT_RELATION_MEMBERS = 10;
		
		private RelationSelectionActionModeCallback(Relation relation) {
			super(relation);
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			helpTopic = R.string.help_relationselection;
			super.onCreateActionMode(mode, menu);
			logic.setSelectedNode(null);
			logic.setSelectedWay(null);
			if (element != null && (((Relation)element).getMembers()==null || ((Relation)element).getMembers().size()==0)) {
				// we can only select an empty relation if there is a reference from another object, this is always a bug 
				Log.e(DEBUG_TAG,"relation " + element.getOsmId() + " is empty ");
				Toast.makeText(main, R.string.toast_rmpty_relation, Toast.LENGTH_LONG).show();
				ACRA.getErrorReporter().handleException(null);
				super.onDestroyActionMode(mode);
				return false;
			}
			logic.selectRelation((Relation) element);
			mode.setTitle(R.string.actionmode_relationselect);	
			mode.setSubtitle(null);
			main.invalidateMap();
			return true;
		}
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			menu = replaceMenu(menu, mode, this);
			super.onPrepareActionMode(mode, menu);
			menu.add(Menu.NONE, MENUITEM_ADD_RELATION_MEMBERS, Menu.NONE, R.string.menu_add_relation_member).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_relation_add_member));
			if (((Relation)element).getMembers() != null) {
				menu.add(Menu.NONE, MENUITEM_SELECT_RELATION_MEMBERS, Menu.NONE, R.string.menu_select_relation_members).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_relation_members));
			}
			arrangeMenu(menu);
			return true;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if (!super.onActionItemClicked(mode, item)) {
				switch (item.getItemId()) {
				case MENUITEM_ADD_RELATION_MEMBERS: main.startSupportActionMode(new  AddRelationMemberActionModeCallback((Relation)element, null)); break;
				case MENUITEM_SELECT_RELATION_MEMBERS:
					ArrayList<OsmElement> selection = new ArrayList<OsmElement>();
					if (((Relation)element).getMembers() != null) {
						for (RelationMember rm : ((Relation)element).getMembers()) {
							selection.add(rm.getElement());
						}
					}
					if (selection.size() > 0) {
						deselect = false;
						main.startSupportActionMode(new  ExtendSelectionActionModeCallback(selection));
					} 
					break;
				default: return false;
				}
			}
			return true;
		}
		
		@Override
		protected void menuDelete(ActionMode mode) {
			if (element.hasParentRelations()) {
				new AlertDialog.Builder(main)
					.setTitle(R.string.delete)
					.setMessage(R.string.deleterelation_relation_description)
					.setPositiveButton(R.string.deleterelation,
						new DialogInterface.OnClickListener() {	
							@Override
							public void onClick(DialogInterface dialog, int which) {
								logic.performEraseRelation((Relation)element, true);
								currentActionMode.finish();
							}
						})
					.show();
			} else {
				logic.performEraseRelation((Relation)element, true);
				mode.finish();
			}
		}
	}
	private class RestartRestrictionFromElementActionModeCallback extends EasyEditActionModeCallback {
		private Set<OsmElement> fromElements;
		private Set<OsmElement> viaElements;
		
		public RestartRestrictionFromElementActionModeCallback(Set<OsmElement> froms, Set<OsmElement> vias) {
			super();
			fromElements = froms;
			viaElements = vias;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			helpTopic = R.string.help_addingrestriction;
			mode.setTitle(R.string.menu_restriction_restart_from);
			logic.setClickableElements(fromElements);
			logic.setReturnRelations(false);
			logic.setSelectedRelationWays(null); // just to be safe
			logic.addSelectedRelationWay(null);
			logic.setSelectedNode(null);
			logic.setSelectedWay(null);
			super.onCreateActionMode(mode, menu);
			return true;
		}
		
		@Override
		/**
		 */
		public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid nodes can be clicked
			super.handleElementClick(element);
			if (viaElements.size() > 1) {
				// redo via selection, this time with pre-split way
				main.startSupportActionMode(new RestrictionFromElementActionModeCallback((Way)element, viaElements));
				return true;
			} else if (viaElements.size() == 1) {
				main.startSupportActionMode(new RestrictionViaElementActionModeCallback((Way)element, viaElements.iterator().next()));
				return true;
			} 
			Log.e(DEBUG_TAG, "viaElements size " + viaElements.size());
			return false;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			logic.setClickableElements(null);
			logic.setReturnRelations(true);
			logic.setSelectedNode(null);
			logic.setSelectedWay(null);
			logic.setSelectedRelationWays(null);
			logic.setSelectedRelationNodes(null);
			
			super.onDestroyActionMode(mode);
		}
	}
	
	private class RestrictionFromElementActionModeCallback extends EasyEditActionModeCallback {
		private Way fromWay;
		private Set<OsmElement> viaElements;
		private boolean viaSelected = false;
		
		public RestrictionFromElementActionModeCallback(Way way, Set<OsmElement> vias) {
			super();
			this.fromWay = way;
			viaElements = vias;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			helpTopic = R.string.help_addingrestriction;
			mode.setTitle(R.string.menu_restriction_via);
			logic.setClickableElements(viaElements);
			logic.setReturnRelations(false);
			logic.setSelectedRelationWays(null); // just to be safe
			logic.addSelectedRelationWay(fromWay);
			logic.setSelectedNode(null);
			logic.setSelectedWay(null);
			super.onCreateActionMode(mode, menu);
			return true;
		}
		
		@Override
		/**
		 * In the simplest case this selects the next step in creating the restriction, in the worst it splits both the via and from way and
		 * restarts the process.
		 */
		public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid nodes can be clicked
			super.handleElementClick(element);
			// check if we have to split from or via
			Node viaNode = null;
			Way viaWay = null;
			if (Node.NAME.equals(element.getName())) {
				viaNode = (Node) element;
			} else if (Way.NAME.equals(element.getName())) {
				viaWay = (Way) element;
				viaNode = fromWay.getCommonNode(viaWay);
			} else {
				// ABORT
			}
			Way newFromWay = null;
			if (!fromWay.getFirstNode().equals(viaNode) && !fromWay.getLastNode().equals(viaNode)) {
				// split from at node
				newFromWay = logic.performSplit(fromWay,viaNode);
			}
			Way newViaWay = null;
			if (viaWay != null && !viaWay.getFirstNode().equals(viaNode) && !viaWay.getLastNode().equals(viaNode)) {
				newViaWay = logic.performSplit(viaWay,viaNode);
			}
			Set<OsmElement> viaElements = new HashSet<OsmElement>();
			viaElements.add(element);
			if (newViaWay != null) {
				viaElements.add(newViaWay);
			}
			if (newFromWay != null) {
				Set<OsmElement> fromElements = new HashSet<OsmElement>();
				fromElements.add(fromWay);
				fromElements.add(newFromWay);
				Toast.makeText(main, newViaWay == null ? R.string.toast_split_from:R.string.toast_split_from_and_via, Toast.LENGTH_LONG).show();
				main.startSupportActionMode(new RestartRestrictionFromElementActionModeCallback(fromElements, viaElements));
				return true;
			}
			if (newViaWay != null) {
				// restart via selection
				Toast.makeText(main, R.string.toast_split_via, Toast.LENGTH_LONG).show();
				main.startSupportActionMode(new RestrictionFromElementActionModeCallback(fromWay, viaElements));
				return true;
			}
			viaSelected = true;
			main.startSupportActionMode(new RestrictionViaElementActionModeCallback(fromWay, element));
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			logic.setClickableElements(null);
			logic.setReturnRelations(true);
			logic.setSelectedNode(null);
			logic.setSelectedWay(null);
			if (!viaSelected) { // back button or done pressed early
				logic.setSelectedRelationWays(null);
				logic.setSelectedRelationNodes(null);
			}
			super.onDestroyActionMode(mode);
		}
	}
	
	private class RestrictionViaElementActionModeCallback extends EasyEditActionModeCallback {
		private Way fromWay;
		private OsmElement viaElement;
		private Set<OsmElement> cachedToElements;
		private boolean toSelected = false;

		public RestrictionViaElementActionModeCallback(Way from, OsmElement via) {
			super();
			fromWay = from;
			viaElement = via;
			cachedToElements = findToElements(viaElement);
		}
		
		public RestrictionViaElementActionModeCallback(Way from, OsmElement via, Set<OsmElement>toElements) {
			super();
			fromWay = from;
			viaElement = via;
			cachedToElements = toElements;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			helpTopic = R.string.help_addingrestriction;
			mode.setTitle(R.string.menu_restriction_to);
			logic.setClickableElements(cachedToElements);
			logic.setReturnRelations(false);
			if (Node.NAME.equals(viaElement.getName())) {
				logic.addSelectedRelationNode((Node) viaElement);
			} else {
				logic.addSelectedRelationWay((Way) viaElement);
			}
			super.onCreateActionMode(mode, menu);
			return true;
		}
		
		@Override
		public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid elements can be clicked
			super.handleElementClick(element);
			Node viaNode = null;
			Way toWay = (Way) element;
			if (Node.NAME.equals(viaElement.getName())) {
				viaNode = (Node) viaElement;
			} else if (Way.NAME.equals(viaElement.getName())) {
				Way viaWay = (Way) viaElement;
				viaNode = ((Way)viaElement).getCommonNode(toWay);
				if (!viaWay.getFirstNode().equals(viaNode) && !viaWay.getLastNode().equals(viaNode)) {
					// split via way and use appropriate segment
					Way newViaWay = logic.performSplit(viaWay, viaNode);
					Toast.makeText(main, R.string.toast_split_via, Toast.LENGTH_LONG).show();
					if (fromWay.hasNode(newViaWay.getFirstNode()) || fromWay.hasNode(newViaWay.getFirstNode())) {
						viaElement = newViaWay;
					}
				}
			}
			// now check if we need to split the toWay
			if (!toWay.getFirstNode().equals(viaNode) && !toWay.getLastNode().equals(viaNode)) {
				Way newToWay = logic.performSplit(toWay, viaNode);
				Toast.makeText(main, R.string.toast_split_to, Toast.LENGTH_LONG).show();
				Set<OsmElement> toCandidates = new HashSet<OsmElement>();
				toCandidates.add(toWay);
				toCandidates.add(newToWay);
				main.startSupportActionMode(new RestrictionViaElementActionModeCallback(fromWay, viaElement, toCandidates));
				return true;
			}
			
			toSelected = true;
			main.startSupportActionMode(new RestrictionToElementActionModeCallback(fromWay, viaElement, (Way) element));
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			logic.setClickableElements(null);
			logic.setReturnRelations(true);
			logic.setSelectedNode(null);
			logic.setSelectedWay(null);
			if (!toSelected) {
				// back button or done pressed early
				logic.setSelectedRelationWays(null);
				logic.setSelectedRelationNodes(null);
			}
			super.onDestroyActionMode(mode);
		}
	}
	
	private class RestrictionToElementActionModeCallback extends EasyEditActionModeCallback {
		
		private Way fromWay;
		private OsmElement viaElement;
		private Way toWay;
		
		public RestrictionToElementActionModeCallback(Way from, OsmElement via, Way to) {
			super();
			fromWay = from;
			viaElement = via;
			toWay = to;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			helpTopic = R.string.help_addingrestriction;
			mode.setTitle(R.string.menu_restriction);
			super.onCreateActionMode(mode, menu);
			logic.addSelectedRelationWay(toWay);
			Relation restriction = logic.createRestriction(fromWay, viaElement, toWay, fromWay == toWay ? Tags.VALUE_NO_U_TURN : null);
			Log.i("EasyEdit", "Created restriction");
			main.performTagEdit(restriction, Tags.VALUE_RESTRICTION, false, false);
			main.startSupportActionMode(new RelationSelectionActionModeCallback(restriction));
			return false; // we are actually already finished
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) { // note never called
			logic.setClickableElements(null);
			logic.setReturnRelations(true);
			logic.setSelectedRelationWays(null);
			logic.setSelectedRelationNodes(null);
			logic.setSelectedNode(null);
			logic.setSelectedWay(null);
			super.onDestroyActionMode(mode);
		}
	}
	
	private class AddRelationMemberActionModeCallback extends EasyEditActionModeCallback {
		private static final int MENUITEM_REVERT = 1;

		private ArrayList<OsmElement> members;
		private Relation relation = null;
		private MenuItem revert = null;
		private boolean backPressed = false;
		
		
		public AddRelationMemberActionModeCallback(ArrayList<OsmElement> selection) {
			super();
			members = new ArrayList<OsmElement>(selection);
		}
		
		public AddRelationMemberActionModeCallback(OsmElement element) {
			super();
			members = new ArrayList<OsmElement>();
			addElement(element);
		}
		
		public AddRelationMemberActionModeCallback(Relation relation, OsmElement element) {
			super();
			members = new ArrayList<OsmElement>();
			if (element != null)
				addElement(element);
			this.relation = relation;
		}
		
		private void addElement(OsmElement element) {
			members.add(element);
			if (element.getName().equals(Way.NAME)) {
				logic.addSelectedRelationWay((Way)element);
			} else if (element.getName().equals(Node.NAME)) {
				logic.addSelectedRelationNode((Node)element);
			} else if (element.getName().equals(Relation.NAME)) {
				logic.addSelectedRelationRelation((Relation)element);
			}
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			helpTopic = R.string.help_addrelationmember;
			mode.setTitle(R.string.menu_relation);
			mode.setSubtitle(R.string.menu_add_relation_member);
			super.onCreateActionMode(mode, menu);
			logic.setReturnRelations(true); // can add relations

			menu.add(Menu.NONE, MENUITEM_REVERT, Menu.NONE, R.string.tag_menu_revert).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_undo)).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_undo));
			revert = menu.findItem(MENUITEM_REVERT);
			revert.setVisible(false);
			setClickableElements();
			return true;
		}
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			if (members.size() > 0)
				revert.setVisible(true);
			arrangeMenu(menu); 
			return true;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if (!super.onActionItemClicked(mode, item)) {
				switch (item.getItemId()) {
				case MENUITEM_REVERT: // remove last item in list
					if(members.size() > 0) {
						OsmElement element = members.get(members.size()-1);
						if (element.getName().equals(Way.NAME))
							logic.removeSelectedRelationWay((Way)element);
						else if (element.getName().equals(Node.NAME))
							logic.removeSelectedRelationNode((Node)element);
						members.remove(members.size()-1);
						setClickableElements();
						main.invalidateMap();
						if (members.size() == 0)
							item.setVisible(false);
					}
					break;
				default: return false;
				}
			}
			return true;
		}
		
		@Override
		public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid elements can be clicked
			super.handleElementClick(element);
			addElement(element);
			setClickableElements();
			if (members.size() > 0)
				revert.setVisible(true);
			main.invalidateMap();
			return true;
		}
		
		private void setClickableElements() {
			ArrayList<OsmElement> excludes = new ArrayList<OsmElement>(members);
			if (relation != null) {
				logic.selectRelation(relation);
				excludes.addAll(relation.getMemberElements());
			}
			logic.setClickableElements(logic.findClickableElements(excludes));
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			super.onDestroyActionMode(mode);
			logic.setClickableElements(null);
			logic.setReturnRelations(true);
			logic.setSelectedNode(null);
			logic.setSelectedWay(null);
			logic.setSelectedRelation(null);
			if (!backPressed) {
				if (members.size() > 0) { // something was actually added
					if (relation == null)
						main.performTagEdit(logic.createRelation(null, members),"type", false, false);
					else {
						logic.addMembers(relation, members);
						main.performTagEdit(relation, null, false, false);
					}
				}
			}
			logic.setSelectedRelationWays(null);
			logic.setSelectedRelationNodes(null);
			logic.setSelectedRelationRelations(null);
		}
		
		/**
		 * back button should abort relation creation
		 */
		@Override
		public boolean onBackPressed() {
			backPressed = true;
			return false; // call the normal stuff
		}
	}	
	
	private class ExtendSelectionActionModeCallback extends EasyEditActionModeCallback {
		
		private static final int MENUITEM_TAG = 2;
		private static final int MENUITEM_DELETE = 3;
		private static final int MENUITEM_COPY = 4;
		private static final int MENUITEM_CUT = 5;
		private static final int MENUITEM_MERGE = 6;
		private static final int MENUITEM_RELATION = 7;
		private static final int MENUITEM_ORTHOGONALIZE = 8;
		private static final int MENUITEM_MERGE_POLYGONS = 9;
		
		private static final int MENUITEM_PREFERENCES = 10;

		private ArrayList<OsmElement> selection;
		private List<OsmElement> sortedWays;
		
		protected UndoListener undoListener; 
		
		private boolean backPressed = false;
		private boolean deselect = true;
				
		public ExtendSelectionActionModeCallback(ArrayList<OsmElement> elements) {
			super();
			selection = new ArrayList<OsmElement>();
			for (OsmElement e: elements) {
				if (e != null) {
					addOrRemoveElement(e);
				}
			}
			undoListener = main.new UndoListener(); 
		}
		
		public ExtendSelectionActionModeCallback(OsmElement element) {
			super();
			Log.d("EasyEditMangager","Multi-Select create mode with " + element);
			selection = new ArrayList<OsmElement>();
			if (element != null) {
				addOrRemoveElement(element);
			}
			undoListener = main.new UndoListener(); 
		}
		
		private void addOrRemoveElement(OsmElement element) {
			if (!selection.contains(element)) {
				selection.add(element);
				if (element.getName().equals(Way.NAME)) {
					logic.addSelectedWay((Way)element);
				} else if (element.getName().equals(Node.NAME)) {
					logic.addSelectedNode((Node)element);
				} else if (element.getName().equals(Relation.NAME)) {
					logic.addSelectedRelation((Relation)element);
				}
			} else {
				selection.remove(element);
				if (element.getName().equals(Way.NAME)) {
					logic.removeSelectedWay((Way)element);
				} else if (element.getName().equals(Node.NAME)) {
					logic.removeSelectedNode((Node)element);
				} else if (element.getName().equals(Relation.NAME)) {
					logic.removeSelectedRelation((Relation)element);
				}
			}
			if (selection.size() == 0) {
				// nothing slected more .... stop
				currentActionMode.finish();
			} else {
				sortedWays = Util.sortWays(selection);
				invalidate();
			}
			main.invalidateMap();
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			helpTopic = R.string.help_multiselect;
			mode.setTitle(R.string.actionmode_multiselect);
			mode.setSubtitle("");
			super.onCreateActionMode(mode, menu);
			logic.setReturnRelations(true); // can add relations
			setClickableElements();
			return true;
		}
		
		@SuppressLint("InflateParams")
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			menu = replaceMenu(menu, mode, this);
			menu.clear();
			menuUtil.reset();
			main.getMenuInflater().inflate(R.menu.undo_action, menu);
			MenuItem undo = menu.findItem(R.id.undo_action);
			if (logic.getUndo().canUndo() || logic.getUndo().canRedo()) {
				undo.setVisible(true);
				undo.setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_undo));
			}
			View undoView = MenuItemCompat.getActionView(undo);
			if (undoView == null) { // FIXME this is a temp workaround for pre-11 Android
				Preferences prefs = new Preferences(main);
				Context context =  new ContextThemeWrapper(main, prefs.lightThemeEnabled() ? R.style.Theme_customMain_Light : R.style.Theme_customMain);
				undoView =  ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.undo_action_view, null);
			}
			undoView.setOnClickListener(undoListener);
			undoView.setOnLongClickListener(undoListener);
			
			menu.add(Menu.NONE, MENUITEM_TAG, Menu.NONE, R.string.menu_tags).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_tags));
			menu.add(Menu.NONE, MENUITEM_DELETE, Menu.CATEGORY_SYSTEM, R.string.delete).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_delete));
			// disabled for now menu.add(Menu.NONE, MENUITEM_TAG_LAST, Menu.NONE, R.string.tag_menu_repeat).setIcon(R.drawable.tag_menu_repeat);
			// if (!(element instanceof Relation)) {
			//	menu.add(Menu.NONE, MENUITEM_COPY, Menu.CATEGORY_SECONDARY, R.string.menu_copy).setIcon(ThemeUtils.getResIdFromAttribute(caller.getActivity(),R.attr.menu_copy)).setShowAsAction(menuSize.showAlways());
			//	menu.add(Menu.NONE, MENUITEM_CUT, Menu.CATEGORY_SECONDARY, R.string.menu_cut).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_cut)).setShowAsAction(menuSize.showAlways());
			//}
			if (sortedWays != null) {
				 menu.add(Menu.NONE, MENUITEM_MERGE, Menu.NONE, R.string.menu_merge).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_merge));
			}
			menu.add(Menu.NONE, MENUITEM_RELATION, Menu.CATEGORY_SYSTEM, R.string.menu_relation).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_relation));
			
			List<Way> selectedWays = logic.getSelectedWays();
			if (selectedWays != null && selectedWays.size() >0) {
				 menu.add(Menu.NONE, MENUITEM_ORTHOGONALIZE, Menu.NONE, R.string.menu_orthogonalize).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_ortho));
			}
			
//			// for now just two
//			if (selection.size() == 2 && canMerge(selection)) {
//				menu.add(Menu.NONE,MENUITEM_MERGE_POLYGONS, Menu.NONE, "Merge polygons");
//			}
			menu.add(GROUP_BASE, MENUITEM_PREFERENCES, Menu.CATEGORY_SYSTEM|10, R.string.menu_config).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_config));
			menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM|10, R.string.menu_help).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_help)).setIcon(ThemeUtils.getResIdFromAttribute(main,R.attr.menu_help));
			arrangeMenu(menu);
			return true;
		}
		
//		private boolean canMerge(ArrayList<OsmElement> selection) {
//			for (OsmElement e:selection) {
//				if (!(e.getName().equals(Way.NAME) && ((Way)e).isClosed())) {
//					return false;
//				}
//			}
//			
//			return true;
//		}
//		
//		private ArrayList<OsmElement> merge(ArrayList<OsmElement> selection) {
//			if (selection.size() > 1) {
//				Way first = (Way) selection.get(0);
//				ArrayList<OsmElement> rest = (ArrayList<OsmElement>) selection.subList(1,selection.size());
//				ArrayList<OsmElement> newSelection = new ArrayList<OsmElement>();
//				for (OsmElement w:rest) {
//					Way n = logic.mergeSimplePolygons(first, (Way)w);
//					if (n!=null) {
//						first = n;
//					} else {
//						newSelection.add(first);
//						first = (Way)w;
//					}
//				}
//				newSelection.add(first);
//				return 
//			}
//		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if (!super.onActionItemClicked(mode, item)) {
				switch (item.getItemId()) {
				
				case MENUITEM_TAG: main.performTagEdit(selection, false, false); break;
				// case MENUITEM_TAG_LAST: main.performTagEdit(element, null, true); break;
				case MENUITEM_DELETE: menuDelete(false); break;
				
				// case MENUITEM_COPY: logic.copyToClipboard(element); currentActionMode.finish(); break;
				// case MENUITEM_CUT: logic.cutToClipboard(element); currentActionMode.finish(); break;
				case MENUITEM_RELATION: main.startSupportActionMode(new  AddRelationMemberActionModeCallback(selection)); break;
				case MENUITEM_ORTHOGONALIZE: 
					List<Way> selectedWays = logic.getSelectedWays();
					if (selectedWays != null && selectedWays.size() >0) {
						logic.performOrthogonalize(selectedWays);
					}
					break;
				case MENUITEM_MERGE:
					// check if the tags are the same for all ways first ... ignores direction dependent stuff
					Map<String,String> firstTags = selection.get(0).getTags();
					boolean ok = true;
					for (int i=1;i<selection.size();i++) {
						if ((firstTags.isEmpty() && !selection.get(i).getTags().isEmpty())
								|| !firstTags.entrySet().equals(selection.get(i).getTags().entrySet())) {
							ok = false;
						}
					}
					if (!ok) {
						Toast.makeText(main, R.string.toast_potential_merge_tag_conflict, Toast.LENGTH_LONG).show();
						main.performTagEdit(selection, false, false);
					} else {
						try {
							boolean result = logic.performMerge(sortedWays);
							// find the remaing way
							Way remaining = null;
							for (OsmElement w:selection) {
								if (!(w.getState()==OsmElement.STATE_DELETED)) {
									remaining = (Way) w;
								}
							}
							if (remaining != null) {
								main.startSupportActionMode(new WaySelectionActionModeCallback(remaining));
								if (!result) { // merge conflict
									Toast.makeText(main, R.string.toast_merge_tag_conflict, Toast.LENGTH_LONG).show();
									main.performTagEdit(remaining, null, false, false);
								} else {
									invalidate(); // update menubar
								}
							} else {
								Log.e("EasyEditManager.ExtendSelectionActionModeCallback","no merged way");
							}
						} catch (OsmIllegalOperationException e) {
							Toast.makeText(main, e.getMessage(), Toast.LENGTH_LONG).show();
						}	
					}
					break;
				case MENUITEM_PREFERENCES: 	PrefEditor.start(main); break; 
				case R.id.undo_action:
					// should not happen
					Log.d("EasyEditManager.ExtendSelectionActionModeCallback","menu undo clicked");
					undoListener.onClick(null);
					break;
				default: return false;
				}
			}
			return true;
		}
		
		@Override
		public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid elements can be clicked
			Log.d("EasyEditMangager","Multi-Select add/remove " + element);
			addOrRemoveElement(element);
			setClickableElements();
			main.invalidateMap();
			return true;
		}
		
		private void setClickableElements() {
//			ArrayList<OsmElement> excludes = new ArrayList<OsmElement>(selection);
//			logic.setClickableElements(logic.findClickableElements(excludes));
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			super.onDestroyActionMode(mode);
			logic.setClickableElements(null);
			logic.setReturnRelations(true);
			if (deselect) {
				logic.setSelectedRelationWays(null);
				logic.setSelectedRelationNodes(null);
				logic.setSelectedRelationRelations(null);
				logic.setSelectedWay(null);
				logic.setSelectedNode(null);
				logic.setSelectedRelation(null);
				main.invalidateMap();
			}
		}
		
		private void menuDelete(boolean deleteFromRelations) {		
			Log.d("EasyEditManager","Multi-select menuDelete " + deleteFromRelations + " " + selection);
			
			// check for relation membership
			if (!deleteFromRelations) {
				for (OsmElement e:selection) {
					if (e.hasParentRelations()) {
						new AlertDialog.Builder(main)
						.setTitle(R.string.delete)
						.setMessage(R.string.delete_from_relation_description)
						.setPositiveButton(R.string.delete,
							new DialogInterface.OnClickListener() {	
								@Override
								public void onClick(DialogInterface dialog, int which) {
									menuDelete(true);
								}
							})
						.show();
						return;
					}
				}
			}
			
			logic.performEraseMultipleObjects(selection);
			
			currentActionMode.finish();
		}	
		
		/**
		 * back button should abort relation creation
		 */
		@Override
		public boolean onBackPressed() {
			backPressed = true;
			return false; // call the normal stuff
		}
	}
}
