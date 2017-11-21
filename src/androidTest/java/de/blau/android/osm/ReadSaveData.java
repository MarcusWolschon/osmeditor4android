package de.blau.android.osm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.util.FileUtil;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReadSaveData {
	
	private static final String TEST_OSM = "test.osm";
	MockWebServerPlus mockServer = null;
	Context context = null;
	AdvancedPrefDatabase prefDB = null;
	Main main = null;
	
    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    @Before
    public void setup() {
		context = InstrumentationRegistry.getInstrumentation().getTargetContext();
 		main = mActivityRule.getActivity();
		Preferences prefs = new Preferences(context);
		prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
		main.getMap().setPrefs(main, prefs);
		TestUtils.grantPermissons();
		TestUtils.dismissStartUpDialogs(main);
    }
    
    @After
    public void teardown() {
    }
    
    @Test
	public void dataReadSave() {
    	final CountDownLatch signal1 = new CountDownLatch(1);
    	Logic logic = App.getLogic();

    	ClassLoader loader = Thread.currentThread().getContextClassLoader();
    	InputStream is = loader.getResourceAsStream("test2.osm");
    	logic.readOsmFile(main, is, false, new SignalHandler(signal1));
    	try {
    		signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Assert.fail(e.getMessage());
		}
       	try {
    		is.close();
    	} catch (IOException e1) {
    	}
    	
       	final CountDownLatch signal2 = new CountDownLatch(1);
    	logic.writeOsmFile(main, TEST_OSM, new SignalHandler(signal2));
    	try {
    		signal2.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Assert.fail(e.getMessage());
		}
    	
    	try {
			byte[] testContent = readInputStream(new FileInputStream(new File(FileUtil.getPublicDirectory(),TEST_OSM)));
	    	is = loader.getResourceAsStream("test-result.osm");
	    	byte[] correctContent = readInputStream(is);
	    	Assert.assertTrue(dataIsSame(correctContent, testContent));
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
	}
     
    /**
     * Compare skipping build number (roughly)
     * 
     * @param correctContent 
     * @param testContent
     * 
     * @return true if "the same"
     */
    private boolean dataIsSame(byte[] correctContent, byte[] testContent) {
        int oldVersionLength = 12;
        int offset = context.getString(R.string.app_version).length() - oldVersionLength; 
        if (correctContent.length == testContent.length - offset) { // this will fail is more than the build changes
            for (int i=77 + offset;i<correctContent.length;i++) {
                if (correctContent[i-offset]!=testContent[i]) {
                    System.out.println("Files differ at position " + i + " offset " + offset);
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    byte[] readInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int readBytes = -1;
        try {
        	while((readBytes = is.read(buffer)) > -1){
        		baos.write(buffer,0,readBytes);
        	}
        } finally {
        	is.close();
        }
        return baos.toByteArray();
    }
}
