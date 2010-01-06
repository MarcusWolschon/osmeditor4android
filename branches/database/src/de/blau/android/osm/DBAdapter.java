package de.blau.android.osm;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import de.blau.android.exception.OsmException;

public class DBAdapter {

	private static final String DATABASE_NAME = "OSMEditor.db";
	private static final int DATABASE_VERSION = 2;

	private static final String DATABASE_TABLE_BOUNDINGBOXES = "bounding_boxes";

	private static final String KEY_BOUNDINGBOXES_ID = "_id";
	private static final String KEY_BOUNDINGBOXES_TOP = "TOP";
	private static final String KEY_BOUNDINGBOXES_LEFT = "LEFT";
	private static final String KEY_BOUNDINGBOXES_BOTTOM = "BOTTOM";
	private static final String KEY_BOUNDINGBOXES_RIGHT = "RIGHT";

	@SuppressWarnings("unused")
	private static final int BOUNDINGBOXES_ID_COLUMN = 0;
	private static final int BOUNDINGBOXES_TOP_COLUMN = 1;
	private static final int BOUNDINGBOXES_LEFT_COLUMN = 2;
	private static final int BOUNDINGBOXES_BOTTOM_COLUMN = 3;
	private static final int BOUNDINGBOXES_RIGHT_COLUMN = 4;

	private static final String DATABASE_TABLE_NODES = "nodes";

	private static final String KEY_NODES_OSMID = "OSM_ID";
	private static final String KEY_NODES_LATITUDE = "LATITUDE";
	private static final String KEY_NODES_LONGITUDE = "LONGITUDE";
	private static final String KEY_NODES_STATE = "STATE";
	private static final String KEY_NODES_VERSION = "VERSION";

	private static final int NODES_OSMID_COLUMN = 0;
	private static final int NODES_LATITUTE_COLUMN = 1;
	private static final int NODES_LONGITUDE_COLUMN = 2;
	private static final int NODES_STATE_COLUMN = 3;
	private static final int NODES_VERSION_COLUMN = 4;

	private static final String DATABASE_TABLE_WAYS = "ways";

	private static final String KEY_WAYS_OSMID = "OSM_ID";
	private static final String KEY_WAYS_STATE = "STATE";
	private static final String KEY_WAYS_VERSION = "VERSION";

	private static final int WAYS_OSMID_COLUMN = 0;
	private static final int WAYS_STATE_COLUMN = 1;
	private static final int WAYS_VERSION_COLUMN = 2;

	private static final String DATABASE_TABLE_WAYNODES = "waynodes";

	private static final String KEY_WAYNODES_WAYID = "WAY_ID";
	private static final String KEY_WAYNODES_POSITION = "POSITION";
	private static final String KEY_WAYNODES_NODEID = "NODE_ID";

	private static final int WAYNODES_WAYID_COLUMN = 0;
	@SuppressWarnings("unused")
	private static final int WAYNODES_POSITION_COLUMN = 1;
	private static final int WAYNODES_NODEID_COLUMN = 2;

	private static final String DATABASE_TABLE_TAGS = "tags";

	private static final String KEY_TAGS_OSMID = "OSM_ID";
	private static final String KEY_TAGS_TYPE = "ELEMENT_TYPE";
	private static final String KEY_TAGS_KEY = "KEY";
	private static final String KEY_TAGS_VALUE = "VALUE";

	private static final int TAGS_OSMID_COLUMN = 0;
	@SuppressWarnings("unused")
	private static final int TAGS_TYPE_COLUMN = 1;
	private static final int TAGS_KEY_COLUMN = 2;
	private static final int TAGS_VALUE_COLUMN = 3;

	private final Context context;
	private final DBHelper dbHelper;
	private SQLiteDatabase db;

	public DBAdapter(final Context _context) {
		context = _context;
		dbHelper = new DBHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public DBAdapter open() {
		try {
			db = dbHelper.getWritableDatabase();
		} catch (final SQLiteException ex) {
			db = dbHelper.getReadableDatabase();
		}
		return this;
	}

	public void close() {
		db.close();
	}

	public BoundingBox loadBoundingBox() {
		final Cursor q = db.query(DATABASE_TABLE_BOUNDINGBOXES, null,
				KEY_BOUNDINGBOXES_ID + " = 1", null, null, null, null);
		try {
			if (q.moveToNext()) {
				try {
					return new BoundingBox(q.getInt(BOUNDINGBOXES_LEFT_COLUMN),
							q.getInt(BOUNDINGBOXES_BOTTOM_COLUMN), q
									.getInt(BOUNDINGBOXES_RIGHT_COLUMN), q
									.getInt(BOUNDINGBOXES_TOP_COLUMN));
				} catch (final OsmException e) {
					return null;
				}
			} else {
				return null;
			}
		} finally {
			q.close();
		}
	}

	public void loadNodes(final Map<Long, Node> nodes,
			final Set<Node> modifiedNodes) {
		nodes.clear();
		modifiedNodes.clear();
		final Cursor q = db.query(DATABASE_TABLE_NODES, null, null, null, null,
				null, null);
		try {
			while (q.moveToNext()) {
				final Node node = new Node(q.getLong(NODES_OSMID_COLUMN), q
						.getLong(NODES_VERSION_COLUMN), (byte) q
						.getInt(NODES_STATE_COLUMN), q
						.getInt(NODES_LATITUTE_COLUMN), q
						.getInt(NODES_LONGITUDE_COLUMN));
				nodes.put(node.getOsmId(), node);
				if (node.getState() != OsmElement.STATE_UNCHANGED)
					modifiedNodes.add(node);
			}
		} finally {
			q.close();
		}
		loadTags(nodes, OsmElement.TYPE_NODE);
	}

	public void loadWays(final Map<Long, Node> nodes,
			final Map<Long, Way> ways, final Set<Way> modifiedWays) {
		ways.clear();
		modifiedWays.clear();
		Cursor q = db.query(DATABASE_TABLE_WAYS, null, null, null, null, null,
				null);
		try {
			while (q.moveToNext()) {
				final Way way = new Way(q.getLong(WAYS_OSMID_COLUMN), q
						.getLong(WAYS_VERSION_COLUMN), (byte) q
						.getInt(WAYS_STATE_COLUMN));
				ways.put(way.getOsmId(), way);
				if (way.getState() != OsmElement.STATE_UNCHANGED)
					modifiedWays.add(way);
			}
		} finally {
			q.close();
		}

		q = db.query(DATABASE_TABLE_WAYNODES, null, null, null, null, null,
				KEY_WAYNODES_WAYID + ", " + KEY_WAYNODES_POSITION);
		try {
			while (q.moveToNext()) {
				final Way way = ways.get(q.getLong(WAYNODES_WAYID_COLUMN));
				final Node node = nodes.get(q.getLong(WAYNODES_NODEID_COLUMN));
				if (way == null || node == null) {
					// Referred Way or Node not in Storage. This shouldn't
					// happen,
					// but it did during development...
					if (way != null) {
						// If it's the Node that missing, remove Way, too.
						ways.values().remove(way);
						modifiedWays.remove(way);
					}

				} else {
					way.addNode(node);
				}
			}
		} finally {
			q.close();
		}

		loadTags(ways, OsmElement.TYPE_WAY);
	}

	private void loadTags(final Map<Long, ? extends OsmElement> elements,
			final byte type) {
		final Cursor q = db.query(DATABASE_TABLE_TAGS, null, KEY_TAGS_TYPE
				+ " = " + type, null, null, null, null);
		try {
			while (q.moveToNext()) {
				elements.get(q.getLong(TAGS_OSMID_COLUMN)).addOrUpdateTag(
						q.getString(TAGS_KEY_COLUMN),
						q.getString(TAGS_VALUE_COLUMN));
			}
		} finally {
			q.close();
		}
	}

	public void updateBoundingBox(final BoundingBox boundingBox) {
		final ContentValues contentValues = new ContentValues();
		contentValues.put(KEY_BOUNDINGBOXES_ID, 1);
		contentValues.put(KEY_BOUNDINGBOXES_TOP, boundingBox.getTop());
		contentValues.put(KEY_BOUNDINGBOXES_LEFT, boundingBox.getLeft());
		contentValues.put(KEY_BOUNDINGBOXES_BOTTOM, boundingBox.getBottom());
		contentValues.put(KEY_BOUNDINGBOXES_RIGHT, boundingBox.getRight());
		db.insert(DATABASE_TABLE_BOUNDINGBOXES, null, contentValues);
	}

	public void insertNode(final Node node) {
		final ContentValues contentValues = new ContentValues();
		contentValues.put(KEY_NODES_OSMID, node.getOsmId());
		contentValues.put(KEY_NODES_LATITUDE, node.getLat());
		contentValues.put(KEY_NODES_LONGITUDE, node.getLon());
		contentValues.put(KEY_NODES_STATE, node.getState());
		contentValues.put(KEY_NODES_VERSION, node.osmVersion);
		db.insert(DATABASE_TABLE_NODES, null, contentValues);

		insertTags(node);
	}

	public void updateNode(final Node node) {
		final ContentValues contentValues = new ContentValues();
		contentValues.put(KEY_NODES_LATITUDE, node.getLat());
		contentValues.put(KEY_NODES_LONGITUDE, node.getLon());
		db.update(DATABASE_TABLE_NODES, contentValues, KEY_NODES_OSMID + " = "
				+ node.getOsmId(), null);
	}

	public void deleteNode(final Node node) {
		deleteTags(node);
		db.delete(DATABASE_TABLE_NODES, KEY_NODES_OSMID + " = "
				+ node.getOsmId(), null);
	}

	public void insertWay(final Way way) {
		final ContentValues contentValues = new ContentValues();
		contentValues.put(KEY_WAYS_OSMID, way.getOsmId());
		contentValues.put(KEY_WAYS_STATE, way.getState());
		contentValues.put(KEY_WAYS_VERSION, way.osmVersion);
		db.insert(DATABASE_TABLE_WAYS, null, contentValues);

		int i = 0;
		final List<Node> nodes = way.getNodes();
		for (final Node node : nodes) {
			contentValues.clear();
			contentValues.put(KEY_WAYNODES_POSITION, i++);
			contentValues.put(KEY_WAYNODES_WAYID, way.getOsmId());
			contentValues.put(KEY_WAYNODES_NODEID, node.getOsmId());
			db.insert(DATABASE_TABLE_WAYNODES, null, contentValues);
		}

		insertTags(way);
	}

	public void deleteWay(final Way way) {
		deleteTags(way);
		db.delete(DATABASE_TABLE_WAYNODES, KEY_WAYNODES_WAYID + " = "
				+ way.getOsmId(), null);
		db.delete(DATABASE_TABLE_WAYS, KEY_WAYS_OSMID + " = " + way.getOsmId(),
				null);
	}

	public void addNodeToWay(final Way way, final Node node, final int position) {
		db.execSQL("update " + DATABASE_TABLE_WAYNODES + " set "
				+ KEY_WAYNODES_POSITION + " = " + KEY_WAYNODES_POSITION
				+ " + 1 where " + KEY_WAYNODES_WAYID + " = " + way.getOsmId()
				+ " and " + KEY_WAYNODES_POSITION + " >= " + position);

		final ContentValues contentValues = new ContentValues();
		contentValues.put(KEY_WAYNODES_POSITION, position);
		contentValues.put(KEY_WAYNODES_WAYID, way.getOsmId());
		contentValues.put(KEY_WAYNODES_NODEID, node.getOsmId());
		db.insert(DATABASE_TABLE_WAYNODES, null, contentValues);
	}

	public void updateState(final OsmElement element) {
		switch (element.getType()) {
		case OsmElement.TYPE_NODE:
			updateState(DATABASE_TABLE_NODES, KEY_NODES_STATE, KEY_NODES_OSMID,
					element.getOsmId(), element.getState());
			break;
		case OsmElement.TYPE_WAY:
			updateState(DATABASE_TABLE_WAYS, KEY_WAYS_STATE, KEY_WAYS_OSMID,
					element.getOsmId(), element.getState());
			break;
		case OsmElement.TYPE_RELATION:
			// TODO
			break;
		}
	}

	private void updateState(final String table, final String stateField,
			final String osmIdField, final long osmId, final int state) {
		final ContentValues contentValues = new ContentValues();
		contentValues.put(stateField, state);
		db.update(table, contentValues, osmIdField + " = "
				+ Long.toString(osmId), null);
	}

	public void insertTags(final OsmElement element) {
		final ContentValues contentValues = new ContentValues();

		final Set<Entry<String, String>> tagSet = element.getTagSet();
		for (final Entry<String, String> tag : tagSet) {
			contentValues.clear();
			contentValues.put(KEY_TAGS_OSMID, element.getOsmId());
			contentValues.put(KEY_TAGS_TYPE, element.getType());
			contentValues.put(KEY_TAGS_KEY, tag.getKey());
			contentValues.put(KEY_TAGS_VALUE, tag.getValue());
			db.insert(DATABASE_TABLE_TAGS, null, contentValues);
		}
	}

	private void deleteTags(final OsmElement element) {
		db.delete(DATABASE_TABLE_TAGS, KEY_TAGS_OSMID + " = ? and "
				+ KEY_TAGS_TYPE + " = ?", new String[] {
				Long.toString(element.getOsmId()),
				Integer.toString(element.getType()) });
	}

	public void updateTags(final OsmElement element) {
		deleteTags(element);
		insertTags(element);
	}

	public void updateOsmId(final OsmElement element, final int osmId) {
		switch (element.getType()) {
		case OsmElement.TYPE_NODE:
			updateOsmId(DATABASE_TABLE_NODES, KEY_NODES_OSMID, KEY_NODES_OSMID
					+ " = " + Long.toString(element.getOsmId()), osmId);
			updateOsmId(DATABASE_TABLE_TAGS, KEY_TAGS_OSMID, KEY_TAGS_OSMID
					+ " = " + Long.toString(element.getOsmId()) + " and "
					+ KEY_TAGS_TYPE + " = "
					+ Integer.toString(element.getState()), osmId);
			break;
		case OsmElement.TYPE_WAY:
			// TODO
			break;
		case OsmElement.TYPE_RELATION:
			// TODO
			break;
		}
	}

	private void updateOsmId(final String table, final String osmIdField,
			final String where, final int newOsmId) {
		final ContentValues contentValues = new ContentValues();
		contentValues.put(osmIdField, newOsmId);
		db.update(table, contentValues, where, null);
	}

	public void updateVersion(final OsmElement element) {
		switch (element.getType()) {
		case OsmElement.TYPE_NODE:
			updateState(DATABASE_TABLE_NODES, KEY_NODES_STATE, KEY_NODES_OSMID,
					element.getOsmId(), element.getState());
			break;
		case OsmElement.TYPE_WAY:
			updateState(DATABASE_TABLE_WAYS, KEY_WAYS_STATE, KEY_WAYS_OSMID,
					element.getOsmId(), element.getState());
			break;
		case OsmElement.TYPE_RELATION:
			// TODO
			break;
		}
	}

	public void deleteAll() {
		db.beginTransaction();
		try {
			clearTable(DATABASE_TABLE_TAGS);
			clearTable(DATABASE_TABLE_NODES);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		db.execSQL("vacuum;");
	}

	private void clearTable(final String table) {
		db.delete(table, null, null);
	}

	public void deleteElement(final OsmElement element) {
		switch (element.getType()) {
		case OsmElement.TYPE_NODE:
			deleteNode((Node) element);
			break;
		case OsmElement.TYPE_WAY:
			deleteWay((Way) element);
			break;
		case OsmElement.TYPE_RELATION:
			// TODO
			break;
		}
	}

	public boolean storageEmpty() {
		Cursor query = db.query(DATABASE_TABLE_NODES, new String[] { "count("
				+ KEY_NODES_OSMID + ")" }, null, null, null, null, null);
		try {
			if (query.moveToFirst() && query.getInt(0) > 0)
				return false;
		} finally {
			query.close();
		}

		query = db.query(DATABASE_TABLE_WAYS, new String[] { "count("
				+ KEY_WAYS_OSMID + ")" }, null, null, null, null, null);
		try {
			if (query.moveToFirst() && query.getInt(0) > 0)
				return false;
		} finally {
			query.close();
		}

		// TODO Relation tables

		return true;
	}

	private static class DBHelper extends SQLiteOpenHelper {
		public DBHelper(final Context context, final String name,
				final CursorFactory factory, final int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(final SQLiteDatabase _db) {
			_db
					.execSQL("create table "
							+ DATABASE_TABLE_BOUNDINGBOXES
							+ " ("
							+ KEY_BOUNDINGBOXES_ID
							+ " integer not null constraint BOUNDINGBOXES_PK primary key on conflict replace, "
							+ KEY_BOUNDINGBOXES_TOP + " integer not null, "
							+ KEY_BOUNDINGBOXES_LEFT + " integer not null, "
							+ KEY_BOUNDINGBOXES_BOTTOM + " integer not null, "
							+ KEY_BOUNDINGBOXES_RIGHT + " integer not null)");
			_db.execSQL("create table " + DATABASE_TABLE_NODES + " ("
					+ KEY_NODES_OSMID
					+ " integer constraint NODES_PK primary key, "
					+ KEY_NODES_LATITUDE + " integer not null, "
					+ KEY_NODES_LONGITUDE + " integer not null, "
					+ KEY_NODES_STATE
					+ " integer not null constraint NODES_STATE_CHECK check ("
					+ KEY_NODES_STATE + " between 0 and 3), "
					+ KEY_NODES_VERSION + " integer)");
			_db.execSQL("create table " + DATABASE_TABLE_WAYS + " ("
					+ KEY_WAYS_OSMID
					+ " integer constraint WAYS_PK primary key, "
					+ KEY_WAYS_STATE
					+ " integer not null constraint NODES_STATE_CHECK check ("
					+ KEY_WAYS_STATE + " between 0 and 3), "
					+ KEY_NODES_VERSION + " integer)");
			_db.execSQL("create table " + DATABASE_TABLE_WAYNODES + " ("
					+ KEY_WAYNODES_WAYID + " integer not null, "
					+ KEY_WAYNODES_POSITION + " integer not null, "
					+ KEY_WAYNODES_NODEID
					+ " integer not null, constraint WAYNODES_PK primary key ("
					+ KEY_WAYNODES_WAYID + ", " + KEY_WAYNODES_POSITION + "))");
			_db.execSQL("create table " + DATABASE_TABLE_TAGS + " ("
					+ KEY_TAGS_OSMID + " integer not null, " + KEY_TAGS_TYPE
					+ " integer not null constraint TAGS_TYPE_CHECK check ("
					+ KEY_TAGS_TYPE + " between 0 and 1), " + KEY_TAGS_KEY
					+ " text not null, " + KEY_TAGS_VALUE
					+ " text not null, constraint TAGS_PK primary key ("
					+ KEY_TAGS_OSMID + ", " + KEY_TAGS_TYPE + ", "
					+ KEY_TAGS_KEY + ") on conflict replace)");
		}

		@Override
		public void onUpgrade(final SQLiteDatabase _db, final int _oldVersion,
				final int _newVersion) {
			if (_oldVersion < 2) {
				_db.execSQL("alter table " + DATABASE_TABLE_NODES + " add "
						+ KEY_NODES_VERSION + " integer;");
				_db.execSQL("alter table " + DATABASE_TABLE_WAYS + " add "
						+ KEY_WAYS_VERSION + " integer;");
			}
		}
	}

}
