package de.blau.android;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.widget.Toast;
import de.blau.android.exception.FollowGpsException;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Server;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;
import de.blau.android.presets.TagKeyAutocompletionAdapter;
import de.blau.android.presets.TagValueAutocompletionAdapter;
import de.blau.android.resources.Paints;

/**
 * This is the main Activity from where other Activities will be started.
 * 
 * @author mb
 */
public class Main extends Activity {

	/**
	 * Tag used for Android-logging.
	 */
	private static final String DEBUG_TAG = Main.class.getName();

	/**
	 * Requests a {@link BoundingBox} as an activity-result.
	 */
	public static final int REQUEST_BOUNDINGBOX = 0;

	/**
	 * Requests a list of {@link Tag Tags} as an activity-result.
	 */
	public static final int REQUEST_EDIT_TAG = 1;

	/**
	 * List of Exceptions for error reporting.
	 */
	//TODO: Put this in ErrorMailer!
	private final ArrayList<Exception> exceptions = new ArrayList<Exception>();

	private final Handler handler = new Handler();

	private DialogFactory dialogFactory;

	/**
	 * The map View.
	 */
	private Map map;

	/**
	 * Our user-preferences.
	 */
	private Preferences prefs;

	/**
	 * The logic that manipulates the model. (non-UI)<br/>
	 * This is created in {@link #onCreate(Bundle)} and never changed afterwards.<br/>
	 * If may be null or not reflect the current state if accessed from outside this activity.
	 */
	protected static Logic logic;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(
			Context.LOCATION_SERVICE);
		getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
		getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		getWindow().requestFeature(Window.FEATURE_RIGHT_ICON);

		map = new Map(getApplicationContext());
		dialogFactory = new DialogFactory(this);

		//Register some Listener
		MapTouchListener mapTouchListener = new MapTouchListener();
		map.setOnTouchListener(mapTouchListener);
		map.setOnCreateContextMenuListener(mapTouchListener);
		map.setOnKeyListener(new MapKeyListener());

		setContentView(map);

		//Load previous logic (inkl. StorageDelegator)
		logic = (Logic) getLastNonConfigurationInstance();
		if (logic == null) {
			logic = new Logic(locationManager, map, new Paints(getApplicationContext().getResources()));
			if (isLastActivityAvailable()) {
				resumeLastActivity();
			} else {
				gotoBoxPicker();
			}
		} else {
			logic.setMap(map);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return logic;
	}

	/**
	 * Loads the preferences into {@link #map} and {@link #logic}, triggers new {@inheritDoc}
	 */
	@Override
	protected void onStart() {
		super.onStart();
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		prefs = new Preferences(sharedPrefs, getResources());
		map.setPrefs(prefs);
		logic.setPrefs(prefs);
		map.requestFocus();

		// cache some values (optional)
		TagValueAutocompletionAdapter.fillCache(this);
		TagKeyAutocompletionAdapter.fillCache(this);
	}

	/**
	 * Creates the menu from the XML file "main_menu.xml".<br> {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_move:
			logic.setMode(Logic.MODE_MOVE);
			getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.menu_move);
			return true;

		case R.id.menu_edit:
			logic.setMode(Logic.MODE_EDIT);
			getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.menu_edit);
			return true;

		case R.id.menu_tag:
			logic.setMode(Logic.MODE_TAG_EDIT);
			getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.menu_tag);
			return true;

		case R.id.menu_add:
			logic.setMode(Logic.MODE_ADD);
			getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.menu_add);
			return true;

		case R.id.menu_erase:
			logic.setMode(Logic.MODE_ERASE);
			getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.menu_erase);
			return true;

		case R.id.menu_split:
			logic.setMode(Logic.MODE_SPLIT);
			//TODO: no icon yet getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.menu_split);
			return true;

		case R.id.menu_append:
			logic.setMode(Logic.MODE_APPEND);
			getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.menu_append);
			return true;

		case R.id.menu_confing:
			startActivity(new Intent(getApplicationContext(), PrefEditor.class));
			return true;

		case R.id.menu_gps_start_pause:
			logic.setTrackingState(Tracker.STATE_PAUSE);
			return true;

		case R.id.menu_gps_follow:
			followGps();
			return true;

		case R.id.menu_gps_stop:
			logic.setTrackingState(Tracker.STATE_STOP);
			return true;

		case R.id.menu_transfer_download_current:
			onMenuDownloadCurrent();
			return true;

		case R.id.menu_transfer_download_other:
			gotoBoxPicker();
			return true;

		case R.id.menu_transfer_upload:
			confirmUpload();
			return true;

		case R.id.menu_save:
			logic.save(this, handler, true);
			return true;
		}

		return false;
	}

	/**
	 * 
	 */
	private void followGps() {
		try {
			logic.setFollowGps(true);
		} catch (FollowGpsException e) {
			showToastWaitingForGps();
		}
	}

	/**
	 * Handles the menu click on "download current view".<br>
	 * When no {@link #delegator} is set, the user will be redirected to AreaPicker.<br>
	 * When the user made some changes, {@link #DIALOG_TRANSFER_DOWNLOAD_CURRENT_WITH_CHANGES} will be shown.<br>
	 * Otherwise the current viewBox will be re-downloaded from the server.
	 */
	private void onMenuDownloadCurrent() {
		if (logic.hasChanges()) {
			showDialog(DialogFactory.DOWNLOAD_CURRENT_WITH_CHANGES);
		} else {
			performCurrentViewHttpLoad();
		}
	}

	/**
	 * Uses {@link DialogFactory} to create Dialogs<br> {@inheritDoc}
	 */
	@Override
	protected Dialog onCreateDialog(final int id) {
		Dialog dialog = dialogFactory.create(this, id);
		if (dialog != null) {
			return dialog;
		}
		return super.onCreateDialog(id);
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		if (id == DialogFactory.CONFIRM_UPLOAD) {
			AlertDialog ad = (AlertDialog)dialog;
			ad.setMessage(getString(R.string.confirm_upload_text, getPendingChanges()));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_BOUNDINGBOX && data != null) {
			handleBoxPickerResult(resultCode, data);
		} else if (requestCode == REQUEST_EDIT_TAG && resultCode == RESULT_OK && data != null) {
			handleTagEditorResult(data);
		}
	}

	/**
	 * @param resultCode
	 * @param data
	 */
	private void handleBoxPickerResult(final int resultCode, final Intent data) {
		Bundle b = data.getExtras();
		int left = b.getInt(BoxPicker.RESULT_LEFT);
		int bottom = b.getInt(BoxPicker.RESULT_BOTTOM);
		int right = b.getInt(BoxPicker.RESULT_RIGHT);
		int top = b.getInt(BoxPicker.RESULT_TOP);

		try {
			BoundingBox box = new BoundingBox(left, bottom, right, top);
			if (resultCode == RESULT_OK) {
				performHttpLoad(box);
			} else if (resultCode == RESULT_CANCELED) {
				openEmptyMap(box);
			}
		} catch (OsmException e) {
			//Values should be done checked in LocationPciker.
			Log.e(DEBUG_TAG, "OsmException", e);
		}
	}

	/**
	 * @param data
	 */
	@SuppressWarnings("unchecked")
	private void handleTagEditorResult(final Intent data) {
		Bundle b = data.getExtras();
		// Read data from extras
		// Intents can't hold a Map in the extras, so
		// it is converted to an ArrayList
		ArrayList<String> tagList = (ArrayList<String>) b.getSerializable(TagEditor.TAGS);
		String type = b.getString(TagEditor.TYPE);
		long osmId = b.getLong(TagEditor.OSM_ID);

		int size = tagList.size();
		HashMap<String, String> tags = new HashMap<String, String>(size / 2);
		for (int i = 0; i < size; i += 2) {
			tags.put(tagList.get(i), tagList.get(i + 1));
		}

		logic.insertTags(type, osmId, tags);
		map.invalidate();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		logic.disableGpsUpdates();
		logic.save(this, handler, false);
	}

	/**
	 * TODO: put this in Logic!!! Checks if a serialized {@link StorageDelegator} file is available.
	 * 
	 * @return true, when the file is available, otherwise false.
	 */
	private boolean isLastActivityAvailable() {
		FileInputStream in = null;
		try {
			in = openFileInput(StorageDelegator.FILENAME);
			return true;
		} catch (final FileNotFoundException e) {
			return false;
		} finally {
			Server.close(in);
		}
	}

	/**
	 * Loads the last activities storage, give it to the View and activate it.
	 * 
	 * @throws IOException when the file could not be read.
	 * @throws ClassNotFoundException
	 */
	public void resumeLastActivity() {
		showDialog(DialogFactory.PROGRESS_LOADING);
		logic.loadFromFile(this, handler);
	}

	public void performCurrentViewHttpLoad() {
		try {
			showDialog(DialogFactory.PROGRESS_LOADING);
			logic.downloadCurrent(this, handler);
		} catch (OsmServerException e) {
			showDialog(DialogFactory.UNDEFINED_ERROR);
			e.printStackTrace();
			exceptions.add(e);
		} catch (IOException e) {
			showDialog(DialogFactory.NO_CONNECTION);
			e.printStackTrace();
			exceptions.add(e);
		}
	}

	private void performHttpLoad(final BoundingBox box) {
		try {
			showDialog(DialogFactory.PROGRESS_LOADING);
			logic.downloadBox(this, handler, box);
		} catch (OsmException e) {
			showDialog(DialogFactory.UNDEFINED_ERROR);
			Log.e(getClass().getName() + ":performHttpLoad()", "OsmException received", e);
			exceptions.add(e);
		} catch (IOException e) {
			showDialog(DialogFactory.NO_CONNECTION);
			Log.e(getClass().getName() + ":performHttpLoad()", "IOException received", e);
			exceptions.add(e);
		}
	}

	private void openEmptyMap(final BoundingBox box) {
		logic.newEmptyMap(box);
	}

	/**
	 * 
	 */
	public void performUpload(final String comment) {
		dismissDialog(DialogFactory.CONFIRM_UPLOAD);
		final Server server = prefs.getServer();

		if (server != null && server.isLoginSet()) {
			if (logic.hasChanges()) {
				logic.upload(this, handler, comment);
			} else {
				Toast.makeText(getApplicationContext(), R.string.toast_no_changes, Toast.LENGTH_LONG).show();
			}
		} else {
			showDialog(DialogFactory.NO_LOGIN_DATA);
		}
	}

	/**
	 * 
	 */
	public void confirmUpload() {
		final Server server = prefs.getServer();

		if (server != null && server.isLoginSet()) {
			if (logic.hasChanges()) {
				showDialog(DialogFactory.CONFIRM_UPLOAD);
			} else {
				Toast.makeText(getApplicationContext(), R.string.toast_no_changes, Toast.LENGTH_LONG).show();
			}
		} else {
			showDialog(DialogFactory.NO_LOGIN_DATA);
		}
	}

	/**
	 * Starts the LocationPicker activity for requesting a location.
	 */
	public void gotoBoxPicker() {
		startActivityForResult(new Intent(getApplicationContext(), BoxPicker.class), REQUEST_BOUNDINGBOX);
	}

	public List<Exception> getExceptions() {
		return exceptions;
	}

	/**
	 * 
	 */
	private void showToastWaitingForGps() {
		Toast.makeText(getApplicationContext(), R.string.toast_waiting_for_gps, Toast.LENGTH_SHORT).show();
	}

	private enum AppendMode {
		APPEND_START, APPEND_APPEND
	}

	/**
	 * A TouchListener for all gestures made on the touchscreen.
	 * 
	 * @author mb
	 */
	private class MapTouchListener implements OnTouchListener, OnCreateContextMenuListener, OnMenuItemClickListener {

		private static final int INVALID_POS = -1;

		private float firstPosX = INVALID_POS;

		private float firstPosY = INVALID_POS;

		private float oldPosX = INVALID_POS;

		private float oldPosY = INVALID_POS;

		private final static float CLICK_TOLERANCE = 20f;

		private AppendMode appendMode;

		private List<OsmElement> clickedNodesAndWays;

		@Override
		public boolean onTouch(final View v, final MotionEvent m) {
			float x = m.getX();
			float y = m.getY();

			switch (m.getAction()) {
			case MotionEvent.ACTION_DOWN:
				touchEventDown(x, y);
				break;

			case MotionEvent.ACTION_MOVE:
				touchEventMove(x, y);
				break;

			case MotionEvent.ACTION_UP:
				touchEventUp(v, x, y);
				break;
			}
			return true;
		}

		/**
		 * @param x
		 * @param y
		 */
		private void touchEventDown(final float x, final float y) {
			firstPosX = x;
			firstPosY = y;
			oldPosX = x;
			oldPosY = y;

			logic.handleTouchEventDown(x, y);
		}

		private void touchEventMove(final float x, final float y) {
			logic.handleTouchEventMove(x, y, oldPosX - x, y - oldPosY, hasMoved(x, y));

			oldPosX = x;
			oldPosY = y;
		}

		/**
		 * @param v
		 * @param x
		 * @param y
		 */
		private void touchEventUp(final View v, final float x, final float y) {
			boolean hasMoved = hasMoved(x, y);
			if (!hasMoved) {
				byte mode = logic.getMode();
				boolean isInEditZoomRange = logic.isInEditZoomRange();

				if (isInEditZoomRange) {
					switch (mode) {
					case Logic.MODE_ADD:
						logic.performAdd(x, y);
						break;
					case Logic.MODE_TAG_EDIT:
						selectElementForTagEdit(v, x, y);
						break;
					case Logic.MODE_ERASE:
						selectElementForErase(v, x, y);
						break;
					case Logic.MODE_SPLIT:
						selectElementForSplit(v, x, y);
						break;
					case Logic.MODE_APPEND:
						performAppend(v, x, y);
						break;
					}
					map.invalidate();
				} else {
					if (mode != Logic.MODE_MOVE && !isInEditZoomRange && !hasMoved) {
						Toast.makeText(getApplicationContext(), R.string.toast_not_in_edit_range, Toast.LENGTH_LONG)
								.show();
					}
				}
			}

			firstPosX = INVALID_POS;
			firstPosY = INVALID_POS;
			oldPosX = INVALID_POS;
			oldPosY = INVALID_POS;
		}

		private void selectElementForTagEdit(final View v, final float x, final float y) {
			clickedNodesAndWays = logic.getClickedNodesAndWays(x, y);
			int size = clickedNodesAndWays.size();
			if (size == 1) {
				performTagEdit(clickedNodesAndWays.get(0));
			} else if (size > 1) {
				v.showContextMenu();
			} /* else {} */// If no elements where touched, ignore
		}

		private void selectElementForErase(final View v, final float x, final float y) {
			clickedNodesAndWays = logic.getClickedNodes(x, y);
			int size = clickedNodesAndWays.size();
			if (size == 1) {
				logic.performErase((Node) clickedNodesAndWays.get(0));
			} else if (size > 1) {
				v.showContextMenu();
			} /* else {} */// If no elements where touched, ignore
		}

		private void selectElementForSplit(final View v, final float x, final float y) {
			clickedNodesAndWays = logic.getClickedNodes(x, y);

			//TOOD remove nodes with no ways from list
//			for (Iterator iterator = clickedNodesAndWays.iterator(); iterator.hasNext();) {
//				Node node = (Node) iterator.next();
//				if (node.getWaysCount() < 1) {
//					iterator.remove();
//				}
//			}

			int size = clickedNodesAndWays.size();
			if (size == 1) {
				logic.performSplit((Node) clickedNodesAndWays.get(0));
			} else if (size > 1) {
				v.showContextMenu();
			} /* else {} */// If no elements where touched, ignore
		}

		/**
		 * Appends a new Node to a selected Way. If any Way was yet selected, the user have to select one end-node
		 * first. When the user clicks on an empty area, a new node will generated. When he clicks on a existing way,
		 * the new node will be generated on that way. when he selects a different node, this one will be used. when he
		 * selects the previous selected node, it will be de-selected.
		 * 
		 * @param x the click-position on the display.
		 * @param y the click-position on the display.
		 */
		public void performAppend(final View v, final float x, final float y) {
			Node lSelectedNode = logic.getSelectedNode();
			Way lSelectedWay = logic.getSelectedWay();

			if (lSelectedWay == null) {

				clickedNodesAndWays = logic.getClickedEndNodes(x, y);
				int size = clickedNodesAndWays.size();
				if (size == 1) {
					logic.performAppendStart(clickedNodesAndWays.get(0));
				} else if (size > 1) {
					appendMode = AppendMode.APPEND_START;
					v.showContextMenu();
				} /* else {} */// If no elements where touched, ignore

			} else if (lSelectedWay.isEndNode(lSelectedNode)) {
				// TODO Resolve multiple possible selections
				logic.performAppendAppend(x, y);
			}
		}

		/**
		 * @param selectedElement
		 */
		private void performTagEdit(final OsmElement selectedElement) {
			if (selectedElement instanceof Node) {
				logic.setSelectedNode((Node) selectedElement);
			} else if (selectedElement instanceof Way) {
				logic.setSelectedWay((Way) selectedElement);
			}

			if (selectedElement != null) {
				Intent startTagEditor = new Intent(getApplicationContext(), TagEditor.class);

				// convert tag-list to string-lists for Bundle-compatibility
				ArrayList<String> tagList = new ArrayList<String>();
				for (Entry<String, String> tag : selectedElement.getTags().entrySet()) {
					tagList.add(tag.getKey());
					tagList.add(tag.getValue());
				}

				//insert Bundles
				startTagEditor.putExtra(TagEditor.TAGS, tagList);
				startTagEditor.putExtra(TagEditor.TYPE, selectedElement.getName());
				startTagEditor.putExtra(TagEditor.OSM_ID, selectedElement.getOsmId());

				Main.this.startActivityForResult(startTagEditor, Main.REQUEST_EDIT_TAG);
			}
		}

		/**
		 * @param x
		 * @param y
		 * @return
		 */
		private boolean hasMoved(final float x, final float y) {
			return Math.abs(firstPosX - x) > CLICK_TOLERANCE || Math.abs(firstPosY - y) > CLICK_TOLERANCE;
		}

		@Override
		public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
			for (int i = 0, len = clickedNodesAndWays.size(); i < len; i++) {
				OsmElement osmElement = clickedNodesAndWays.get(i);
				menu.add(Menu.NONE, i, Menu.NONE, osmElement.getDescription()).setOnMenuItemClickListener(this);
			}
		}

		@Override
		public boolean onMenuItemClick(final MenuItem item) {
			int itemId = item.getItemId();
			if (itemId >= 0 && itemId < clickedNodesAndWays.size()) {
				OsmElement element = clickedNodesAndWays.get(itemId);
				switch (logic.getMode()) {
				case Logic.MODE_TAG_EDIT:
					performTagEdit(element);
					break;
				case Logic.MODE_ERASE:
					logic.performErase((Node) element);
					break;
				case Logic.MODE_SPLIT:
					logic.performSplit((Node) element);
					break;
				case Logic.MODE_APPEND:
					switch (appendMode) {
					case APPEND_START:
						logic.performAppendStart(element);
						break;
					case APPEND_APPEND:
						// TODO
					}
				}
			}
			return true;
		}
	}

	/**
	 * A KeyListener for all key events.
	 * 
	 * @author mb
	 */
	public class MapKeyListener implements OnKeyListener {

		@Override
		public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
			switch (event.getAction()) {
			case KeyEvent.ACTION_UP:
				if (!v.onKeyUp(keyCode, event)) {
					switch (keyCode) {
					case KeyEvent.KEYCODE_VOLUME_UP:
					case KeyEvent.KEYCODE_VOLUME_DOWN:
						// this stops the piercing beep related to volume adjustments
						return true;
					}
				}
				break;
			case KeyEvent.ACTION_DOWN:
				if (!v.onKeyDown(keyCode, event)) {
					switch (keyCode) {
					case KeyEvent.KEYCODE_DPAD_CENTER:
						setFollowGps();
						return true;
						
					case KeyEvent.KEYCODE_DPAD_UP:
						translate(Logic.DIRECTION_UP);
						return true;
						
					case KeyEvent.KEYCODE_DPAD_DOWN:
						translate(Logic.DIRECTION_DOWN);
						return true;
						
					case KeyEvent.KEYCODE_DPAD_LEFT:
						translate(Logic.DIRECTION_LEFT);
						return true;
						
					case KeyEvent.KEYCODE_DPAD_RIGHT:
						translate(Logic.DIRECTION_RIGHT);
						return true;
						
					case KeyEvent.KEYCODE_VOLUME_UP:
					case KeyEvent.KEYCODE_SEARCH:
						logic.zoom(Logic.ZOOM_IN);
						return true;
						
					case KeyEvent.KEYCODE_VOLUME_DOWN:
					case KeyEvent.KEYCODE_SHIFT_LEFT:
					case KeyEvent.KEYCODE_SHIFT_RIGHT:
						logic.zoom(Logic.ZOOM_OUT);
						return true;
					}
				}
				break;
			}
			return false;
		}
		
		private void translate(final byte direction) {
			logic.translate(direction);
		}
		
		/**
		 * 
		 */
		private void setFollowGps() {
			try {
				logic.setFollowGps(true);
			} catch (FollowGpsException e) {
				showToastWaitingForGps();
			}
		}
	}

	/**
	 * @return a list of all pending changes to upload (contains newlines)
	 */
	public String getPendingChanges() {
		Set<String> changes = logic.getPendingChanges(this);
		StringBuilder retval = new StringBuilder();
		for (String change : changes) {
			retval.append(change).append('\n');
		}
		return retval.toString();
	}
}
