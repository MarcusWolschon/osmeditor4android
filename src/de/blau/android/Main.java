package de.blau.android;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.Bundle;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;
import android.widget.RelativeLayout.LayoutParams;
import de.blau.android.Logic.CursorPaddirection;
import de.blau.android.Logic.Mode;
import de.blau.android.exception.FollowGpsException;
import de.blau.android.exception.OsmException;
import de.blau.android.osb.Bug;
import de.blau.android.osb.CommitTask;
import de.blau.android.osb.Database;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Server;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;
import de.blau.android.presets.TagKeyAutocompletionAdapter;
import de.blau.android.presets.TagValueAutocompletionAdapter;
import de.blau.android.resources.Paints;
import de.blau.android.views.overlay.OpenStreetBugsOverlay;
import de.blau.android.views.overlay.OpenStreetMapViewOverlay;

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
		Application.mainActivity = this;
		LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(
			Context.LOCATION_SERVICE);
		getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
		getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		getWindow().requestFeature(Window.FEATURE_RIGHT_ICON);

		RelativeLayout rl = new RelativeLayout(getApplicationContext());
		if (map != null) {
			map.onDestroy();
		}
		map = new Map(getApplicationContext());
		dialogFactory = new DialogFactory(this);

		//Register some Listener
		MapTouchListener mapTouchListener = new MapTouchListener();
		map.setOnTouchListener(mapTouchListener);
		map.setOnCreateContextMenuListener(mapTouchListener);
		map.setOnKeyListener(new MapKeyListener());
		
		rl.addView(map);
		
		// Set up the zoom in/out controls
		final ZoomControls zc = new ZoomControls(getApplicationContext());
		zc.setOnZoomInClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				logic.zoom(Logic.ZOOM_IN);
				zc.setIsZoomInEnabled(logic.canZoom(Logic.ZOOM_IN));
				zc.setIsZoomOutEnabled(logic.canZoom(Logic.ZOOM_OUT));
			}
		});
		zc.setOnZoomOutClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				logic.zoom(Logic.ZOOM_OUT);
				zc.setIsZoomInEnabled(logic.canZoom(Logic.ZOOM_IN));
				zc.setIsZoomOutEnabled(logic.canZoom(Logic.ZOOM_OUT));
			}
		});
		RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		rlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rl.addView(zc, rlp);
		
		setContentView(rl);

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
		Resources r = getResources();
		Database.setUserAgent(r.getString(R.string.app_name) + "/" + r.getString(R.string.app_version));
		prefs = new Preferences(sharedPrefs, r);
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
		if (!prefs.isOpenStreetBugsEnabled()) {
			menu.removeItem(R.id.menu_openstreetbug);
		}
		return true;
	}

	/**
	 * Creates the menu from the XML file "main_menu.xml".<br> {@inheritDoc}
	 */
	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		if (!prefs.isOpenStreetBugsEnabled()) {
			menu.removeItem(R.id.menu_openstreetbug);
		} else {
			if(menu.findItem(R.id.menu_openstreetbug) == null) {
				// restore missing menu item
				menu.clear();
				final MenuInflater inflater = getMenuInflater();
				inflater.inflate(R.menu.main_menu, menu);
			}
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_move:
			logic.setMode(Logic.Mode.MODE_MOVE);
			getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.menu_move);
			return true;

		case R.id.menu_edit:
			logic.setMode(Logic.Mode.MODE_EDIT);
			getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.menu_edit);
			return true;

		case R.id.menu_tag:
			logic.setMode(Logic.Mode.MODE_TAG_EDIT);
			getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.menu_tag);
			return true;

		case R.id.menu_add:
			logic.setMode(Logic.Mode.MODE_ADD);
			getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.menu_add);
			return true;

		case R.id.menu_erase:
			logic.setMode(Logic.Mode.MODE_ERASE);
			getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.menu_erase);
			return true;

		case R.id.menu_split:
			logic.setMode(Logic.Mode.MODE_SPLIT);
			getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.menu_split);
			return true;

		case R.id.menu_openstreetbug:
			logic.setMode(Logic.Mode.MODE_OPENSTREETBUG);
			getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.menu_openstreetbug);
			Toast.makeText(this, R.string.toast_file_openstreetbug, Toast.LENGTH_SHORT).show();
			return true;

		case R.id.menu_append:
			logic.setMode(Logic.Mode.MODE_APPEND);
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
			logic.save(true);
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
		Dialog dialog = dialogFactory.create(id);
		if (dialog != null) {
			return dialog;
		}
		return super.onCreateDialog(id);
	}
	
	/**
	 * Prepare the fields of dialogs before they are shown. Only some need this special
	 * handling.
	 * @param id Dialog ID number.
	 * @param dialog Dialog object.
	 */
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		if (dialog instanceof AlertDialog) {
			AlertDialog ad = (AlertDialog)dialog;
			switch (id) {
			case DialogFactory.CONFIRM_UPLOAD:
				TextView changes = (TextView)ad.findViewById(R.id.upload_changes);
				changes.setText(getString(R.string.confirm_upload_text, getPendingChanges()));
				break;
			case DialogFactory.OPENSTREETBUG_EDIT:
				Bug bug = logic.getSelectedBug();
				ad.setTitle(getString((bug.getId() == 0) ? R.string.openstreetbug_new_title : R.string.openstreetbug_edit_title));
				TextView comments = (TextView)ad.findViewById(R.id.openstreetbug_comments);
				comments.setText(bug.getComment().replaceAll("<hr />", "\n"));
				EditText comment = (EditText)ad.findViewById(R.id.openstreetbug_comment);
				comment.setText("");
				comment.setFocusable(!bug.isClosed());
				comment.setFocusableInTouchMode(!bug.isClosed());
				comment.setEnabled(!bug.isClosed());
				CheckBox close = (CheckBox)ad.findViewById(R.id.openstreetbug_close);
				close.setChecked(bug.isClosed());
				close.setEnabled(!bug.isClosed() && bug.getId() != 0);
				Button commit = ad.getButton(AlertDialog.BUTTON_POSITIVE);
				commit.setEnabled(!bug.isClosed());
				break;
			}
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
		map.onDestroy();
		logic.disableGpsUpdates();
		logic.save(false);
		super.onDestroy();
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		map.onLowMemory();
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
		logic.loadFromFile();
	}

	public void performCurrentViewHttpLoad() {
		logic.downloadCurrent();
	}

	private void performHttpLoad(final BoundingBox box) {
		logic.downloadBox(box);
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
				logic.upload(comment);
			} else {
				Toast.makeText(getApplicationContext(), R.string.toast_no_changes, Toast.LENGTH_LONG).show();
			}
		} else {
			showDialog(DialogFactory.NO_LOGIN_DATA);
		}
	}
	
	/**
	 * Commit changes to the currently selected OpenStreetBug.
	 * @param comment Comment to add to the bug.
	 * @param close Flag to indicate if the bug is to be closed.
	 */
	public void performOpenStreetBugCommit(final String comment, final boolean close) {
		Log.d("Vespucci", "Main.performOpenStreetBugCommit");
		dismissDialog(DialogFactory.OPENSTREETBUG_EDIT);
		new CommitTask(logic.getSelectedBug(), comment, close) {
			
			/** Flag to track if the bug is new. */
			private boolean newBug;
			
			@Override
			protected void onPreExecute() {
				newBug = (bug.getId() == 0);
				setProgressBarIndeterminateVisibility(true);
			}
			
			@Override
			protected Boolean doInBackground(String... args) {
				// execute() is called below with no arguments (args will be empty)
				// getDisplayName() is deferred to here in case a lengthy OSM query
				// is required to determine the nickname
				String nickname = null;
				Server server = prefs.getServer();
				if (server.isLoginSet()) {
					nickname = server.getDisplayName();
				}
				return super.doInBackground(nickname);
			}
			
			@Override
			protected void onPostExecute(Boolean result) {
				if (result && newBug) {
					for (OpenStreetMapViewOverlay o : map.getOverlays()) {
						if (o instanceof OpenStreetBugsOverlay) {
							((OpenStreetBugsOverlay)o).addBug(bug);
						}
					}
				}
				setProgressBarIndeterminateVisibility(false);
				Toast.makeText(getApplicationContext(), result ? R.string.openstreetbug_commit_ok : R.string.openstreetbug_commit_fail, Toast.LENGTH_SHORT).show();
				map.invalidate();
			}
			
		}.execute();
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
		private List<Bug> clickedBugs;
		
		private boolean hasMoved;

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
			return v.onTouchEvent(m);
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
			clickedBugs = null;
			clickedNodesAndWays = null;
			hasMoved = false;
			logic.handleTouchEventDown(x, y);
		}

		private void touchEventMove(final float x, final float y) {
			if (hasMoved(x, y)) hasMoved = true;
			logic.handleTouchEventMove(x, y, oldPosX - x, y - oldPosY, hasMoved);
			oldPosX = x;
			oldPosY = y;
		}

		/**
		 * @param v
		 * @param x
		 * @param y
		 */
		private void touchEventUp(final View v, final float x, final float y) {
			if (!hasMoved) {
				OpenStreetBugsOverlay osbo = map.getOpenStreetBugsOverlay();
				if (osbo != null) {
					clickedBugs = osbo.getClickedBugs(x, y, map.getViewBox());
				}
				
				Mode mode = logic.getMode();
				boolean isInEditZoomRange = logic.isInEditZoomRange();

				if (isInEditZoomRange) {
					switch (mode) {
					case MODE_MOVE:
						break;
					case MODE_OPENSTREETBUG:
						switch ((clickedBugs == null) ? 0 : clickedBugs.size()) {
						case 0:
							performBugEdit(logic.makeNewBug(x, y));
							break;
						case 1:
							performBugEdit(clickedBugs.get(0));
							break;
						default:
							v.showContextMenu();
							break;
						}
						break;
					case MODE_ADD:
						logic.performAdd(x, y);
						break;
					case MODE_TAG_EDIT:
						selectElementForTagEdit(v, x, y);
						break;
					case MODE_ERASE:
						selectElementForErase(v, x, y);
						break;
					case MODE_SPLIT:
						selectElementForSplit(v, x, y);
						break;
					case MODE_APPEND:
						performAppend(v, x, y);
						break;
					}
					map.invalidate();
				} else {
					switch ((clickedBugs == null) ? 0 : clickedBugs.size()) {
					case 0:
						if (!isInEditZoomRange) {
							int res;
							switch (mode) {
							case MODE_ADD:
							case MODE_EDIT:
							case MODE_APPEND:
							case MODE_ERASE:
							case MODE_SPLIT:
							case MODE_TAG_EDIT:
								res = R.string.toast_not_in_edit_range;
								break;
							case MODE_OPENSTREETBUG:
								res = R.string.toast_not_in_bug_range;
								break;
							case MODE_MOVE:
							default:
								res = 0;
								break;
							}
							if (res != 0) {
								Toast.makeText(getApplicationContext(), res, Toast.LENGTH_LONG).show();
							}
						}
						break;
					case 1:
						performBugEdit(clickedBugs.get(0));
						break;
					default:
						v.showContextMenu();
						break;
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
			switch (((clickedBugs == null) ? 0 : clickedBugs.size()) + clickedNodesAndWays.size()) {
			case 0:
				// no elements were touched, ignore
				break;
			case 1:
				// exactly one element touched
				if (clickedBugs != null && clickedBugs.size() == 1) {
					performBugEdit(clickedBugs.get(0));
				} else {
					performTagEdit(clickedNodesAndWays.get(0));
				}
				break;
			default:
				// multiple possible elements touched - show menu
				v.showContextMenu();
				break;
			}
		}

		private void selectElementForErase(final View v, final float x, final float y) {
			clickedNodesAndWays = logic.getClickedNodes(x, y);
			switch (clickedNodesAndWays.size()) {
			case 0:
				// no elements were touched, ignore
				break;
			case 1:
				logic.performErase((Node)clickedNodesAndWays.get(0));
				break;
			default:
				v.showContextMenu();
				break;
			}
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

			switch (clickedNodesAndWays.size()) {
			case 0:
				// no elements were touched, ignore
				break;
			case 1:
				logic.performSplit((Node)clickedNodesAndWays.get(0));
				break;
			default:
				v.showContextMenu();
				break;
			}
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
				switch (clickedNodesAndWays.size()) {
				case 0:
					// no elements touched, ignore
					break;
				case 1:
					logic.performAppendStart(clickedNodesAndWays.get(0));
					break;
				default:
					appendMode = AppendMode.APPEND_START;
					v.showContextMenu();
					break;
				}
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
		 * Edit an OpenStreetBug.
		 * @param bug The bug to edit.
		 */
		private void performBugEdit(final Bug bug) {
			logic.setSelectedBug(bug);
			showDialog(DialogFactory.OPENSTREETBUG_EDIT);
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
			int id = 0;
			if (clickedBugs != null) {
				for (Bug b : clickedBugs) {
					menu.add(Menu.NONE, id++, Menu.NONE, b.getDescription()).setOnMenuItemClickListener(this);
				}
			}
			if (clickedNodesAndWays != null) {
				for (OsmElement e : clickedNodesAndWays) {
					menu.add(Menu.NONE, id++, Menu.NONE, e.getDescription()).setOnMenuItemClickListener(this);
				}
			}
		}

		@Override
		public boolean onMenuItemClick(final MenuItem item) {
			int itemId = item.getItemId();
			if (clickedBugs != null && itemId >= 0 && itemId < clickedBugs.size()) {
				performBugEdit(clickedBugs.get(itemId));
			} else {
				if (clickedBugs != null) {
					itemId -= clickedBugs.size();
				}
				if (itemId >= 0 && itemId < clickedNodesAndWays.size()) {
					OsmElement element = clickedNodesAndWays.get(itemId);
					switch (logic.getMode()) {
					case MODE_TAG_EDIT:
						performTagEdit(element);
						break;
					case MODE_ERASE:
						logic.performErase((Node) element);
						break;
					case MODE_SPLIT:
						logic.performSplit((Node) element);
						break;
					case MODE_APPEND:
						switch (appendMode) {
						case APPEND_START:
							logic.performAppendStart(element);
							break;
						case APPEND_APPEND:
							// TODO
						}
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
						translate(Logic.CursorPaddirection.DIRECTION_UP);
						return true;
						
					case KeyEvent.KEYCODE_DPAD_DOWN:
						translate(Logic.CursorPaddirection.DIRECTION_DOWN);
						return true;
						
					case KeyEvent.KEYCODE_DPAD_LEFT:
						translate(Logic.CursorPaddirection.DIRECTION_LEFT);
						return true;
						
					case KeyEvent.KEYCODE_DPAD_RIGHT:
						translate(Logic.CursorPaddirection.DIRECTION_RIGHT);
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
		
		private void translate(final CursorPaddirection direction) {
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
		List<String> changes = logic.getPendingChanges(this);
		StringBuilder retval = new StringBuilder();
		for (String change : changes) {
			retval.append(change).append('\n');
		}
		return retval.toString();
	}
}
