package de.blau.android.osm;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Assert;

import android.os.AsyncTask;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.test.suitebuilder.annotation.LargeTest;
import de.blau.android.Main;
import de.blau.android.osm.Capabilities;
import de.blau.android.osm.Server;
import de.blau.android.prefs.Preferences;
import android.support.test.runner.AndroidJUnit4;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class ApiTest {

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    @Test
	public void capabilities() {
		final Preferences prefs = new Preferences(InstrumentationRegistry.getInstrumentation().getTargetContext());
		final Server s = prefs.getServer();
		
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
}
