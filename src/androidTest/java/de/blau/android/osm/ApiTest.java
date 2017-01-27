package de.blau.android.osm;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.content.Context;
import android.os.AsyncTask;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.exception.OsmException;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ApiTest {
	
	MockWebServerPlus mockServer = null;
	Context context = null;
	AdvancedPrefDatabase prefDB = null;
	
    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    @Before
    public void setup() {
		context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		Preferences prefs = new Preferences(context);
		prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
    	mockServer = new MockWebServerPlus();
 		HttpUrl mockBaseUrl = mockServer.server().url("/api/0.6/");
		System.out.println("mock api url " + mockBaseUrl.toString());
 		prefDB = new AdvancedPrefDatabase(context);
 		prefDB.deleteAPI("Test");
		prefDB.addAPI("Test", "Test", mockBaseUrl.toString(), null, null, "user", "pass", null, false);
 		prefDB.selectAPI("Test");
    }
    
    @After
    public void teardown() {
		try {
			mockServer.server().shutdown();
		} catch (IOException ioex) {
			System.out.println("Stopping mock webserver exception " + ioex);
		}
    }
    
    @Test
	public void capabilities() {
    	mockServer.enqueue("capabilities1");
    	
 		final Server s = new Server(context, prefDB.getCurrentAPI(),"vesupucci test");
 		
		AsyncTask<Void, Void, Capabilities> task = new AsyncTask<Void, Void, Capabilities>() {
			
			@Override
			protected void onPreExecute() {
				
			}
			
			@Override
			protected Capabilities doInBackground(Void... arg) {			
				return s.getCapabilities();
			}
			
			@Override
			protected void onPostExecute(Capabilities result) {
				Assert.assertNotNull(result);
				Assert.assertEquals(result.minVersion,"0.6");
				Assert.assertEquals(result.gpxStatus, Capabilities.Status.ONLINE);
				Assert.assertEquals(result.apiStatus, Capabilities.Status.ONLINE);
				Assert.assertEquals(result.dbStatus, Capabilities.Status.ONLINE);
				Assert.assertEquals(Way.maxWayNodes, 2001);
			}
		};
		task.execute();
		try {
			task.get(20000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
    @Test
	public void download() {
    	final CountDownLatch signal = new CountDownLatch(1);
    	mockServer.enqueue("capabilities1");
    	mockServer.enqueue("download1");
    	Logic logic = App.getLogic();
    	try {
			logic.downloadBox(new BoundingBox(8.3879800D,47.3892400D,8.3844600D,47.3911300D), false, new PostAsyncActionHandler() {
				@Override
				public void execute() {
					signal.countDown();
				}});
		} catch (OsmException e) {
			Assert.fail(e.getMessage());
		}
    	try {
			signal.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Assert.fail(e.getMessage());
		}
    	Assert.assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 101792984));
	}
    
    @Test
	public void downloadMerge() {
    	download();
    	final CountDownLatch signal = new CountDownLatch(1);
    	mockServer.enqueue("capabilities1");
    	mockServer.enqueue("download2");
    	Logic logic = App.getLogic();
    	try {
			logic.downloadBox(new BoundingBox(8.3865200D,47.3883000D,8.3838500D,47.3898500D), true, new PostAsyncActionHandler() {
				@Override
				public void execute() {				
					signal.countDown();
				}});
		} catch (OsmException e) {
			Assert.fail(e.getMessage());
		}
    	try {
    		signal.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Assert.fail(e.getMessage());
		}
    	Assert.assertNotNull(App.getDelegator().getOsmElement(Node.NAME, 101792984));
	}
    
}
