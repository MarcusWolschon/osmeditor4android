/**
 * TagKeyAutocompletionAdapter.java
 * created: 12.06.2010 10:43:37
 * (c) 2010 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
 * This file is part of OSMEditor by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  OSMEditor is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  OSMEditor is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OSMEditor.  If not, see <http://www.gnu.org/licenses/>.
 *
 ***********************************
 * Editing this file:
 *  -For consistent code-quality this file should be checked with the
 *   checkstyle-ruleset enclosed in this project.
 *  -After the design of this file has settled it should get it's own
 *   JUnit-Test that shall be executed regularly. It is best to write
 *   the test-case BEFORE writing this class and to run it on every build
 *   as a regression-test.
 */
package de.blau.android.presets;

//other imports
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.AttributeList;
import org.xml.sax.HandlerBase;
import org.xml.sax.SAXException;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import de.blau.android.R;
import de.blau.android.TagEditor;


/**
 * Project: OSMEditor<br/>
 * TagKeyAutocompletionAdapter.java<br/>
 * created: 12.06.2010 10:43:37 <br/>
 *<br/><br/>
 * <b>Adapter for the {@link AutoCompleteTextView} in the {@link TagEditor} that is for the VALUE
 * of key-value -tags.</a>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class TagValueAutocompletionAdapter extends ArrayAdapter<String> {

    private static final Map<String, List<String>> myTagValuesPerKey = new HashMap<String, List<String>>();
    /**
     * The tag we use for Android-logging.
     */
    private static final String DEBUG_TAG = TagValueAutocompletionAdapter.class.getName();

    /**
     * Reduce constructor-time by optionally calling
     * this method during program-start.
     * @param aContext
     */
    public static void fillCache(final Context aContext) {
        try {
            getArray("", aContext);
        } catch (Exception e) {
            Log.w(DEBUG_TAG, "Cannot parse presets.xml", e);
        }
    }

    /**
     * Get the values to suggest in auto-completion for a given key
     * @param aTagKey the key we shall suggest values for
     * @param aContext used to load resources
     * @return the suggested values
     * @throws ParserConfigurationException if we cannot parse presets.xml
     * @throws SAXException if we cannot parse presets.xml
     * @throws FactoryConfigurationError if we cannot parse presets.xml
     * @throws IOException if we cannot parse presets.xml
     */
    private static String[] getArray(final String aTagKey, final Context aContext) throws ParserConfigurationException, SAXException, FactoryConfigurationError, IOException {

        // parse only the first time
        if (myTagValuesPerKey.size() > 0) {
            List<String> suggestedValues = myTagValuesPerKey.get(aTagKey);
            if (suggestedValues == null) {
                suggestedValues = new LinkedList<String>();
            }
            return (String[]) suggestedValues.toArray(new String[suggestedValues.size()]);
        }
 
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        InputStream input = aContext.getResources().openRawResource(R.raw.presets);
        saxParser.parse(input, new HandlerBase() {

            /** 
             * ${@inheritDoc}.
             */
            @Override
            public void startElement(String aName, AttributeList aAttributes)
                                                                             throws SAXException {
                //TODO: check "<item type="node,closedway..."
                if (aName.equals("key")) {
                    // <key key="{aTagKey}" value={...}/>
                    String key = null;
                    for (int i = 0; i < aAttributes.getLength(); i++) {
                        String attrName = aAttributes.getName(i);
                        if (attrName.equals("key")) {
                            String value = aAttributes.getValue(i);
                            key = value;
                        }
                    }

                    if (key == null) {
                        return;
                    }
                    List<String> myValidKeyValues = myTagValuesPerKey.get(key);
                    if (myValidKeyValues == null) {
                        myValidKeyValues = new LinkedList<String>();
                        myTagValuesPerKey.put(key, myValidKeyValues);
                    }

                    for (int i = 0; i < aAttributes.getLength(); i++) {
                        String attrName = aAttributes.getName(i);
                        if (attrName.equals("value")) {
                            String value = aAttributes.getValue(i);
                            
                            if (!myValidKeyValues.contains(value)) {
                                myValidKeyValues.add(value);
                            }
                            break;
                        }
                    }
                }

                // ------------------------------------
                if (aName.equals("text")) {
                    // <text key="{aTagKey}" default={...}/>
                    String key = null;
                    for (int i = 0; i < aAttributes.getLength(); i++) {
                        String attrName = aAttributes.getName(i);
                        if (attrName.equals("key")) {
                            String value = aAttributes.getValue(i);
                            key = value;
                            break;
                        }
                    }

                    if (key == null) {
                        return;
                    }
                    List<String> myValidKeyValues = myTagValuesPerKey.get(key);
                    if (myValidKeyValues == null) {
                        myValidKeyValues = new LinkedList<String>();
                        myTagValuesPerKey.put(key, myValidKeyValues);
                    }

                    for (int i = 0; i < aAttributes.getLength(); i++) {
                        String attrName = aAttributes.getName(i);
                        if (attrName.equals("default")) {
                            String value = aAttributes.getValue(i);
                            if (!aTagKey.equals(value)) {
                                if (!myValidKeyValues.contains(value)) {
                                    myValidKeyValues.add(value);
                                }
                                break;
                            }
                        }
                    }
                }

                // ------------------------------------
                if (aName.equals("text")) {
                    // <combo key="{aTagKey}" values={...}/>
                    String key = null;
                    for (int i = 0; i < aAttributes.getLength(); i++) {
                        String attrName = aAttributes.getName(i);
                        if (attrName.equals("key")) {
                            String value = aAttributes.getValue(i);
                            key = value;
                            break;
                        }
                    }

                    if (key == null) {
                        return;
                    }
                    List<String> myValidKeyValues = myTagValuesPerKey.get(key);
                    if (myValidKeyValues == null) {
                        myValidKeyValues = new LinkedList<String>();
                        myTagValuesPerKey.put(key, myValidKeyValues);
                    }

                    for (int i = 0; i < aAttributes.getLength(); i++) {
                        String attrName = aAttributes.getName(i);
                        if (attrName.equals("values")) {
                            String values = aAttributes.getValue(i);
                            String[] splits = values.split(",");
                            for (int j = 0; j < splits.length; j++) {
                                if (!myValidKeyValues.contains(splits[j])) {
                                    myValidKeyValues.add(splits[j]);
                                }
                            }
                            break;
                        }
                    }
                }
            }
            
        });
 
        List<String> suggestedValues = myTagValuesPerKey.get(aTagKey);
        if (suggestedValues == null) {
            suggestedValues = new LinkedList<String>();
        }
        return (String[]) suggestedValues.toArray(new String[suggestedValues.size()]);
    }

    /**
     * 
     * @param aContext used to load resources
     * @param aTextViewResourceId given to {@link ArrayAdapter}
     * @throws ParserConfigurationException if we cannot parse presets.xml
     * @throws SAXException if we cannot parse presets.xml
     * @throws FactoryConfigurationError if we cannot parse presets.xml
     * @throws IOException if we cannot parse presets.xml
     */
    public TagValueAutocompletionAdapter(final Context aContext,
                                       final int aTextViewResourceId,
                                       final String aTagKey) throws ParserConfigurationException, SAXException, FactoryConfigurationError, IOException {
        super(aContext, aTextViewResourceId, getArray(aTagKey, aContext));

    }

}


