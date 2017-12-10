package de.blau.android.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.OsmoseBug;
import de.blau.android.tasks.Task;
import de.blau.android.tasks.TransferTasks;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OsmoseTest {
	
	MockWebServerPlus mockServer = null;
	Context context = null;
	AdvancedPrefDatabase prefDB = null;
	Main main = null;
	Task t = null;
	
    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    @Before
    public void setup() {
		context = InstrumentationRegistry.getInstrumentation().getTargetContext();
 		main = mActivityRule.getActivity();
		Preferences prefs = new Preferences(context);
		prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
		main.getMap().setPrefs(main, prefs);
    	mockServer = new MockWebServerPlus();
 		HttpUrl mockBaseUrl = mockServer.server().url("/en/api/0.2/");
		System.out.println("mock api url " + mockBaseUrl.toString());
		prefs.putString(R.string.config_osmoseServer_key, mockBaseUrl.scheme()+"://"+mockBaseUrl.host()+":"+mockBaseUrl.port()+"/");
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
	public void osmoseDownload() {
    	final CountDownLatch signal = new CountDownLatch(1);
    	mockServer.enqueue("osmoseDownload");
    	App.getTaskStorage().reset();
    	try {
    		final Server s = new Server(context, prefDB.getCurrentAPI(),"vesupucci test");
    		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
    		Resources r = context.getResources();
    		String osmoseErrorSelector = r.getString(R.string.bugfilter_osmose_error);
    		String osmoseWarningSelector = r.getString(R.string.bugfilter_osmose_warning);
    		String osmoseMinorIssueSelector = r.getString(R.string.bugfilter_osmose_minor_issue);
    		Set<String> set = new HashSet<String>(Arrays.asList(osmoseErrorSelector,osmoseWarningSelector,osmoseMinorIssueSelector)) ;
			p.edit().putStringSet(r.getString(R.string.config_bugFilter_key), set).commit();
    		Assert.assertTrue(new Preferences(context).taskFilter().contains(osmoseErrorSelector));
    		Assert.assertTrue(new Preferences(context).taskFilter().contains(osmoseWarningSelector));
    		Assert.assertTrue(new Preferences(context).taskFilter().contains(osmoseMinorIssueSelector));
			TransferTasks.downloadBox(context,s,new BoundingBox(8.3879800D,47.3892400D,8.3844600D,47.3911300D), false, new SignalHandler(signal));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
    	try {
			signal.await(40, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Assert.fail(e.getMessage());
		}
    	ArrayList<Task> tasks = App.getTaskStorage().getTasks();
    	// 
    	Assert.assertEquals(92, tasks.size());
    	try {
    		tasks = App.getTaskStorage().getTasks(new BoundingBox(8.3702816, 47.3050382, 8.3702818, 47.3050384));
    	} catch (Exception e){
    		Assert.fail(e.getMessage());
    	}
    	t = tasks.get(0);
    	Assert.assertTrue(t instanceof OsmoseBug);
    	Assert.assertEquals(11187837418L,t.getId());
	}
    
    
    @Test
	public void osmoseUpload() {
    	osmoseDownload();
    	OsmoseBug b = (OsmoseBug) t; // ugly but removes code duplication
    	b.setFalse();
    	b.setChanged();
     	Assert.assertTrue(b.hasBeenChanged());
    	final CountDownLatch signal = new CountDownLatch(1);
    	mockServer.enqueue("200");
    	try {
    		Assert.assertTrue(TransferTasks.uploadOsmoseBug(context, b, false, new SignalHandler(signal)));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
       	try {
    		signal.await(40, TimeUnit.SECONDS);
    	} catch (InterruptedException e) {
    		Assert.fail(e.getMessage());
    	}
       	Assert.assertFalse(b.hasBeenChanged());
	}
    
    @Test
	public void osmoseUploadFail() {
    	osmoseDownload();
    	OsmoseBug b = (OsmoseBug) t; // ugly but removes code duplication
    	b.setFalse();
    	b.setChanged();
     	Assert.assertTrue(b.hasBeenChanged());
    	final CountDownLatch signal = new CountDownLatch(1);
    	mockServer.enqueue("500");
    	try {
    		Assert.assertFalse(TransferTasks.uploadOsmoseBug(context, b, false, new SignalHandler(signal)));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
       	try {
    		signal.await(40, TimeUnit.SECONDS);
    	} catch (InterruptedException e) {
    		Assert.fail(e.getMessage());
    	}
       	Assert.assertTrue(b.hasBeenChanged());
	}
    
    @Test
 	public void notesAmdOsmoseUpload() {
     	osmoseDownload();
     	OsmoseBug b = (OsmoseBug) t; // ugly but removes code duplication
     	b.setFalse();
     	b.setChanged();
     	Assert.assertTrue(b.hasBeenChanged());
     	Note n = new Note((int)(51.0*1E7D),(int)(0.1*1E7D));
		Assert.assertTrue(n.isNew());
		App.getTaskStorage().add(n);
     	final CountDownLatch signal = new CountDownLatch(1);
     	mockServer.enqueue("200");
   		final Server s = new Server(context, prefDB.getCurrentAPI(),"vesupucci test");
     	try {
     		TransferTasks.upload( main, s, new SignalHandler(signal));
 		} catch (Exception e) {
 			Assert.fail(e.getMessage());
 		}
        try {
     		signal.await(40, TimeUnit.SECONDS);
     	} catch (InterruptedException e) {
     		Assert.fail(e.getMessage());
     	}
        Assert.assertFalse(b.hasBeenChanged());
        Assert.assertFalse(n.hasBeenChanged());
 	}
    
    
}
