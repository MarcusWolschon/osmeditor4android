package de.blau.android.presets;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.contract.Files;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.FileUtil;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.collections.MRUList;

/**
 * Container for most recently used tags
 * 
 * @author simon
 *
 */
public class MRUTags {
    private static final String DEBUG_TAG = "MRUTags";

    private static final String MRUTAGS_TAG      = "mrutags";
    private static final String VALUE_TAG        = "value";
    private static final String PATH_TAG         = "path";
    private static final String PRESET_TAG       = "preset";
    private static final String VALUES_TAG       = "values";
    private static final String VALUE_ATTR       = "v";
    private static final String KEY_TAG          = "key";
    private static final String ELEMENTTYPE_ATTR = "elementtype";
    private static final String KEYS_TAG         = "keys";
    private static final String ROLES_TAG        = "roles";
    private static final String ROLE_TAG         = "role";

    private static final int KEY_MRU_SIZE   = 20;
    private static final int VALUE_MRU_SIZE = 10;
    private static final int ROLE_MRU_SIZE  = 10;

    Map<PresetItem, Map<String, MRUList<String>>> valueStore = new HashMap<>();
    Map<ElementType, MRUList<String>>             keyStore   = new HashMap<>();
    Map<PresetItem, MRUList<String>>              roleStore  = new HashMap<>();
    final PresetItem                              dummyItem;
    transient boolean                             dirty      = false;

    /**
     * Construct a new container for most recently used tags
     */
    public MRUTags() {
        dummyItem = new Preset().new PresetItem(null, "dummy", null, null);
        dummyItem.setAppliesToNode();
        dummyItem.setAppliesToWay();
        dummyItem.setAppliesToClosedway();
        dummyItem.setAppliesToRelation();
        dummyItem.setAppliesToArea();
    }

    /**
     * Add a key value tupel to the MRU list
     * 
     * @param item the PresetItem this applies to
     * @param key the key
     * @param value the value
     */
    public synchronized void put(@NonNull PresetItem item, @NonNull String key, @NonNull String value) {
        Log.d(DEBUG_TAG, "item " + item.getName() + " key " + key + " value " + value);
        putValue(item, key, value);
        putKey(item, key);
        dirty = true;
    }

    /**
     * Add a key value tupel to the MRU list
     * 
     * This is for tags for which there is no corresponding PresetItem
     * 
     * @param key the key
     * @param value the value
     */
    public void put(@NonNull String key, @NonNull String value) {
        put(dummyItem, key, value);
    }

    /**
     * Add a key value tupel to the MRU list
     * 
     * @param item the PresetItem this applies to
     * @param key the key
     * @param value the value
     */
    private void putValue(@NonNull PresetItem item, @NonNull String key, @NonNull String value) {
        Map<String, MRUList<String>> keyMap = valueStore.get(item);
        if (keyMap == null) {
            keyMap = new HashMap<>();
            valueStore.put(item, keyMap);
        }
        MRUList<String> mru = keyMap.get(key);
        if (mru == null) {
            mru = new MRUList<>(VALUE_MRU_SIZE);
            keyMap.put(key, mru);
        }
        mru.push(value);
    }

    /**
     * Store a key according to applicable ElementType
     * 
     * @param item the PresetItem
     * @param key the key
     */
    private void putKey(@NonNull PresetItem item, @NonNull String key) {
        for (ElementType elementType : item.appliesTo()) {
            putKey(elementType, key);
        }
    }

    /**
     * Store a key according to applicable ElementType
     * 
     * @param elementType the ElementType the key is applicable for
     * @param key the key
     */
    private void putKey(@NonNull ElementType elementType, @NonNull String key) {
        MRUList<String> mru = keyStore.get(elementType);
        if (mru == null) {
            mru = new MRUList<>(KEY_MRU_SIZE);
            keyStore.put(elementType, mru);
        }
        mru.push(key);
    }

    /**
     * Add a role to the MRU list
     * 
     * @param item the PresetItem
     * @param role the role
     */
    public synchronized void putRole(@NonNull PresetItem item, @NonNull String role) {
        Log.d(DEBUG_TAG, "item " + item.getName() + " role " + role);
        MRUList<String> mru = roleStore.get(item);
        if (mru == null) {
            mru = new MRUList<>(ROLE_MRU_SIZE);
            roleStore.put(item, mru);
        }
        mru.push(role);
        dirty = true;
    }

    /**
     * Add a role to the MRU list
     * 
     * This is for roles for which there is no corresponding PresetItem
     * 
     * @param role the role
     */
    public synchronized void putRole(@NonNull String role) {
        putRole(dummyItem, role);
    }

    /**
     * Get MRU keys for a specific ElementType
     * 
     * @param elementType the ElementType if null all keys will be returned
     * @return a List of keys
     */
    @NonNull
    public synchronized List<String> getKeys(@Nullable ElementType elementType) {
        if (elementType != null) {
            List<String> mru = keyStore.get(elementType);
            return mru != null ? mru : new ArrayList<>();
        } else {
            Set<String> result = new HashSet<>();
            for (ElementType et : ElementType.values()) {
                List<String> mru = keyStore.get(et);
                if (mru != null) {
                    result.addAll(mru);
                }
            }
            return new ArrayList<>(result);
        }
    }

    /**
     * Get MRU values for a specific PresetItem and key
     * 
     * @param item the PresetItem
     * @param key the key
     * @return a List of values or null if none found
     */
    @Nullable
    public synchronized List<String> getValues(@NonNull PresetItem item, @NonNull String key) {
        Map<String, MRUList<String>> keyMap = valueStore.get(item);
        if (keyMap != null) {
            return keyMap.get(key);
        }
        return null;
    }

    /**
     * Get MRU values for a specific PresetItem and key
     * 
     * This is for tags for which there is no corresponding PresetItem
     * 
     * @param key the key
     * @return a List of values or null if none found
     */
    @Nullable
    public List<String> getValues(@NonNull String key) {
        return getValues(dummyItem, key);
    }

    /**
     * Get MRU roles for a specific PresetItem
     * 
     * @param item the PresetItem
     * @return a List of roles or null if none found
     */
    @Nullable
    public synchronized List<String> getRoles(@NonNull PresetItem item) {
        return roleStore.get(item);
    }

    /**
     * Get MRU roles
     * 
     * This is for tags for which there is no corresponding PresetItem
     * 
     * @return a List of roles or null if none found
     */
    @Nullable
    public List<String> getRoles() {
        return getRoles(dummyItem);
    }

    /**
     * Save the contents to the save file
     * 
     * @param ctx Android Context
     */
    public void save(@NonNull Context ctx) {
        AsyncTask<Void, Void, Void> save = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                FileOutputStream fout = null;
                OutputStream out = null;
                try {
                    File outfile = FileUtil.openFileForWriting(FileUtil.getPublicDirectory() + "/" + Files.FILE_NAME_MRUTAGS);
                    Log.d(DEBUG_TAG, "Saving to " + outfile.getPath());

                    fout = new FileOutputStream(outfile);
                    out = new BufferedOutputStream(fout);
                    writeXml(ctx, out);
                    dirty = false;
                } catch (IllegalArgumentException | IllegalStateException | IOException | XmlPullParserException e) {
                    Log.e(DEBUG_TAG, "Saving failed with " + e.getMessage());
                } finally {
                    SavingHelper.close(out);
                    SavingHelper.close(fout);
                }

                return null;
            }
        };
        if (dirty) {
            save.execute();
        } else {
            Log.d(DEBUG_TAG, "Not dirty, not saving");
        }
    }

    /**
     * Write the contents to an OutputStream in XML format
     * 
     * @param ctx an Android Context
     * @param outputStream the OutputStream to write to
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws IOException
     * @throws XmlPullParserException
     */
    private synchronized void writeXml(@NonNull Context ctx, @NonNull OutputStream outputStream)
            throws IllegalArgumentException, IllegalStateException, IOException, XmlPullParserException {
        Log.d(DEBUG_TAG, "writing MRUTags to xml");
        Map<PresetItem, PresetElementPath> pathCache = new HashMap<>();

        XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
        serializer.setOutput(outputStream, "UTF-8");
        serializer.startDocument("UTF-8", null);
        serializer.startTag("", MRUTAGS_TAG);
        for (Entry<ElementType, MRUList<String>> entry : keyStore.entrySet()) {
            serializer.startTag("", KEYS_TAG);
            serializer.attribute("", ELEMENTTYPE_ATTR, entry.getKey().name());
            for (String key : entry.getValue()) {
                serializer.startTag("", KEY_TAG);
                serializer.attribute("", VALUE_ATTR, key);
                serializer.endTag("", KEY_TAG);
            }
            serializer.endTag("", KEYS_TAG);
        }
        for (Entry<PresetItem, Map<String, MRUList<String>>> entry : valueStore.entrySet()) {
            serializer.startTag("", VALUES_TAG);
            writePathXML(ctx, pathCache, serializer, entry.getKey());
            for (Entry<String, MRUList<String>> entry2 : entry.getValue().entrySet()) {
                if (entry2 != null) {
                    serializer.startTag("", KEY_TAG);
                    serializer.attribute("", VALUE_ATTR, entry2.getKey());
                    for (String v : entry2.getValue()) {
                        serializer.startTag("", VALUE_TAG);
                        serializer.attribute("", VALUE_ATTR, v);
                        serializer.endTag("", VALUE_TAG);
                    }
                    serializer.endTag("", KEY_TAG);
                }
            }
            serializer.endTag("", VALUES_TAG);
        }

        for (Entry<PresetItem, MRUList<String>> entry : roleStore.entrySet()) {
            serializer.startTag("", ROLES_TAG);
            writePathXML(ctx, pathCache, serializer, entry.getKey());
            for (String v : entry.getValue()) {
                serializer.startTag("", ROLE_TAG);
                serializer.attribute("", VALUE_ATTR, v);
                serializer.endTag("", ROLE_TAG);
            }
            serializer.endTag("", ROLES_TAG);
        }

        serializer.endTag("", MRUTAGS_TAG);
        serializer.endDocument();
        serializer.flush();
    }

    /**
     * Write the PresetElementPath of a PresetITem as XML
     * 
     * @param ctx an Android Context
     * @param pathCache the path cache
     * @param serializer the serializer for writing
     * @param item the PresetItem
     * @throws IOException it something went wrong while writing
     */
    private void writePathXML(Context ctx, Map<PresetItem, PresetElementPath> pathCache, XmlSerializer serializer, PresetItem item) throws IOException {
        serializer.startTag("", PRESET_TAG);
        PresetElementPath path = pathCache.get(item);
        if (path == null) {
            path = item.getPath(App.getCurrentRootPreset(ctx).getRootGroup());
            pathCache.put(item, path);
        }
        if (path != null) {
            for (String p : path.getPath()) {
                serializer.startTag("", PATH_TAG);
                serializer.attribute("", VALUE_ATTR, p);
                serializer.endTag("", PATH_TAG);
            }
        }
        serializer.endTag("", PRESET_TAG);
    }

    /**
     * Load the contents from the save file
     * 
     * @param ctx Android Context
     */
    public synchronized void load(@NonNull Context ctx) {
        AsyncTask<Void, Void, Void> load = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                FileInputStream fin = null;
                InputStream in = null;
                try {
                    File infile = FileUtil.openFileForWriting(FileUtil.getPublicDirectory() + "/" + Files.FILE_NAME_MRUTAGS);
                    Log.d(DEBUG_TAG, "Loading from " + infile.getPath());

                    fin = new FileInputStream(infile);
                    in = new BufferedInputStream(fin);
                    readXml(ctx, in);
                } catch (ParserConfigurationException | IOException | SAXException e) {
                    Log.e(DEBUG_TAG, "Saving failed with " + e.getMessage());
                } finally {
                    SavingHelper.close(in);
                    SavingHelper.close(fin);
                }
                return null;
            }
        };
        if (!dirty) {
            load.execute();
        } else {
            Log.e(DEBUG_TAG, "Attempt to load saved filr on dirty contents");
        }
    }

    /**
     * Read the contents from an InputStream in XML format
     * 
     * @param ctx an Android Context
     * @param input the InputStream to read from
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private void readXml(@NonNull Context ctx, @NonNull InputStream input) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser;

        saxParser = SAXParserFactory.newInstance().newSAXParser();
        PresetGroup rootGroup = App.getCurrentRootPreset(ctx).getRootGroup();

        saxParser.parse(input, new DefaultHandler() {

            boolean      inKeys      = false;
            boolean      inValues    = false;
            boolean      inPreset    = false;
            boolean      inRoles     = false;
            ElementType  elementType = null;
            String       valueKey    = null;
            PresetItem   presetItem  = dummyItem;
            List<String> path        = new ArrayList<>();

            /**
             * ${@inheritDoc}.
             */
            @Override
            public void startElement(String uri, String localName, String name, Attributes attr) throws SAXException {
                switch (name) {
                case KEYS_TAG:
                    inKeys = true;
                    elementType = ElementType.valueOf(attr.getValue(ELEMENTTYPE_ATTR));
                    break;
                case KEY_TAG:
                    if (inKeys) {
                        putKey(elementType, attr.getValue(VALUE_ATTR));
                    } else if (inValues) {
                        valueKey = attr.getValue(VALUE_ATTR);
                    }
                    break;
                case VALUES_TAG:
                    inValues = true;
                    break;
                case PRESET_TAG:
                    inPreset = true;
                    path.clear();
                    break;
                case PATH_TAG:
                    if (inPreset) {
                        path.add(attr.getValue(VALUE_ATTR));
                    }
                    break;
                case VALUE_TAG:
                    if (inValues) {
                        putValue(presetItem, valueKey, attr.getValue(VALUE_ATTR));
                    }
                    break;
                case ROLES_TAG:
                    inRoles = true;
                    break;
                case ROLE_TAG:
                    if (inRoles) {
                        putRole(presetItem, attr.getValue(VALUE_ATTR));
                    }
                    break;
                default:
                }
            }

            @Override
            public void endElement(String uri, String localName, String name) throws SAXException {
                switch (name) {
                case KEYS_TAG:
                    inKeys = false;
                    break;
                case PRESET_TAG:
                    if (path.isEmpty()) {
                        presetItem = dummyItem;
                    } else {
                        presetItem = (PresetItem) Preset.getElementByPath(rootGroup, new PresetElementPath(path));
                        if (presetItem == null) {// preset changed
                            presetItem = dummyItem;
                        }
                    }
                    inPreset = false;
                    break;
                case VALUES_TAG:
                    inValues = false;
                    break;
                case ROLES_TAG:
                    inRoles = false;
                    break;
                default:

                }
            }
        });
    }
}
