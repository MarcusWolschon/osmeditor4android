package io.vespucci;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import io.vespucci.JavaResources;
import io.vespucci.Splash;
import io.vespucci.contract.Paths;
import io.vespucci.util.FileUtil;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
public class MigrationTest {
    private static final String TEST_STYLE = "test-style.xml";
    private static final String TEST_JPG   = "test.jpg";

    Context context;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
    }

    /**
     * Setup files in legacy dirs and then check if they are copied correctly
     */
    @Test
    public void migrationTest() {
        try {
            // setup legacy dirs
            File legacyDir = FileUtil.getLegacyPublicDirectory();
            File pictures = new File(legacyDir, Paths.DIRECTORY_PATH_PICTURES);
            assertTrue(pictures.mkdir());
            File destinationPicture = new File(pictures, TEST_JPG);
            JavaResources.copyFileFromResources(TEST_JPG, null, destinationPicture);
            assertTrue(destinationPicture.exists());
            File privateDir = context.getExternalFilesDirs(null)[0];
            File styleDir = new File(privateDir, Paths.DIRECTORY_PATH_STYLES);
            assertTrue(styleDir.mkdir());
            File destinationStyle = new File(styleDir, TEST_STYLE);
            JavaResources.copyFileFromResources(TEST_STYLE, null, destinationStyle);
            assertTrue(destinationStyle.exists());
            // migrate
            Splash.directoryMigration(context);
            // check if everything was copied
            File newDir = FileUtil.getPublicDirectory();
            assertTrue(newDir.exists());
            pictures = new File(newDir, Paths.DIRECTORY_PATH_PICTURES);
            assertTrue(newDir.exists());
            destinationPicture = new File(pictures, TEST_JPG);
            assertTrue(destinationPicture.exists());
            styleDir = new File(newDir, Paths.DIRECTORY_PATH_STYLES);
            assertTrue(styleDir.exists());
            destinationStyle = new File(styleDir, TEST_STYLE);
            assertTrue(destinationStyle.exists());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}