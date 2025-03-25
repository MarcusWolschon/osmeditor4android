package io.vespucci.prefs;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import io.vespucci.R;
import io.vespucci.AsyncResult;
import io.vespucci.Authorize;
import io.vespucci.PostAsyncActionHandler;
import io.vespucci.exception.OsmException;
import io.vespucci.net.OAuth1aHelper;
import io.vespucci.net.OAuth2Helper;
import io.vespucci.net.OAuthHelper;
import io.vespucci.prefs.AdvancedPrefDatabase.PresetInfo;
import io.vespucci.util.ScreenMessage;
import io.vespucci.util.Util;

/**
 * Will process vespucci:// URLs.<br>
 *
 * Handles "preset", "oauth" and "oauth2" paths.
 * 
 * @author Jan
 * @author Simon
 *
 */
public class VespucciURLActivity extends AppCompatActivity implements OnClickListener {
    private static final String DEBUG_TAG = VespucciURLActivity.class.getSimpleName().substring(0, Math.min(23, VespucciURLActivity.class.getSimpleName().length()));

    private static final int    REQUEST_PRESETEDIT   = 0;
    private static final String OAUTH1A_PATH         = "oauth";
    static final String         OAUTH2_PATH          = "oauth2";
    public static final String  PRESET_PATH          = "preset";
    public static final String  PRESETNAME_PARAMETER = "presetname";
    public static final String  PRESETURL_PARAMETER  = "preseturl";

    private String preseturl;
    private String presetname;

    private AdvancedPrefDatabase prefdb;
    private boolean              downloadSucessful = false;

    private View mainView;

    private PostAsyncActionHandler oauthResultHandler = new PostAsyncActionHandler() {

        @Override
        public void onSuccess() {
            Intent intent = new Intent(VespucciURLActivity.this, Authorize.class);
            intent.setAction(Authorize.ACTION_FINISH_OAUTH);
            startActivity(intent);
        }

        @Override
        public void onError(AsyncResult result) {
            ScreenMessage.toastTopError(VespucciURLActivity.this, getString(R.string.toast_oauth_handshake_failed, result.getMessage()));
            onSuccess();
        }

    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customHelpViewer_Light);
        } else {
            setTheme(R.style.Theme_customHelpViewer);
        }
        super.onCreate(savedInstanceState);
        mainView = View.inflate(this, R.layout.url_activity, null);
        setContentView(mainView);
        prefdb = new AdvancedPrefDatabase(this);
    }

    @Override
    protected void onResume() {
        Uri data = getIntent().getData();
        if (data == null) {
            Log.i(DEBUG_TAG, "onResume intent without URI");
            super.onResume();
            finish();
            return;
        }
        String path = stripPathSeperators(data.getPath());
        if (Util.isEmpty(path) && data.getQueryParameter(PRESETURL_PARAMETER) != null) {
            // hack as the uri may not be encoded properly
            path = PRESET_PATH;
        }
        Log.i(DEBUG_TAG, "onResume " + path);
        switch (path) {
        case OAUTH1A_PATH:
        case OAUTH2_PATH: // NOSONAR
            mainView.setVisibility(View.GONE);
            final String apiName = data.getQueryParameter(OAuth2Helper.STATE_PARAM);
            try {
                OAuthHelper oa = OAUTH1A_PATH.equals(path) ? new OAuth1aHelper() : new OAuth2Helper(getBaseContext(), apiName);
                oa.getAccessToken(getBaseContext(), data, oauthResultHandler);
            } catch (ExecutionException e) {
                ScreenMessage.toastTopError(this, getString(R.string.toast_oauth_communication));
            } catch (TimeoutException e) {
                ScreenMessage.toastTopError(this, getString(R.string.toast_oauth_timeout));
            } catch (OsmException e) {
                ScreenMessage.toastTopError(this, getString(R.string.toast_no_oauth, apiName));
            } catch (IllegalArgumentException e) {
                ScreenMessage.toastTopError(this, getString(R.string.toast_oauth_handshake_failed, e.getMessage()));
            }
        default: // NOSONAR fall through is intentional
            setResult(RESULT_OK);
            finish();
            break;
        case PRESET_PATH:
            setupPresetUi(data);
            break;
        }
        super.onResume();
    }

    /**
     * Show the preset download UI
     * 
     * @param data the Uri to use
     */
    private void setupPresetUi(@NonNull Uri data) {
        mainView.findViewById(R.id.urldialog_nodata).setVisibility(preseturl == null ? View.VISIBLE : View.GONE);
        preseturl = data.getQueryParameter(PRESETURL_PARAMETER);
        presetname = data.getQueryParameter(PRESETNAME_PARAMETER);
        if (preseturl != null) {
            ActionBar actionbar = getSupportActionBar();
            if (actionbar != null) {
                actionbar.setDisplayShowHomeEnabled(true);
                actionbar.setDisplayHomeAsUpEnabled(true);
                actionbar.setTitle(R.string.preset_download_title);
                actionbar.setDisplayShowTitleEnabled(true);
                actionbar.show();
            }
            mainView.findViewById(R.id.urldialog_layoutPreset).setVisibility(View.VISIBLE);

            ((TextView) mainView.findViewById(R.id.urldialog_textPresetName)).setText(presetname);
            ((TextView) mainView.findViewById(R.id.urldialog_textPresetURL)).setText(preseturl);
            PresetInfo existingPreset = prefdb.getPresetByURL(preseturl);
            if (downloadSucessful) {
                mainView.findViewById(R.id.urldialog_textPresetSuccessful).setVisibility(View.VISIBLE);
                mainView.findViewById(R.id.urldialog_textPresetExists).setVisibility(View.GONE);
            } else {
                mainView.findViewById(R.id.urldialog_textPresetExists).setVisibility(existingPreset != null ? View.VISIBLE : View.GONE);
                mainView.findViewById(R.id.urldialog_textPresetSuccessful).setVisibility(View.GONE);
            }
            mainView.findViewById(R.id.urldialog_checkboxEnable).setVisibility(existingPreset == null ? View.VISIBLE : View.GONE);
            mainView.findViewById(R.id.urldialog_buttonAddPreset).setVisibility(existingPreset == null ? View.VISIBLE : View.GONE);
            mainView.findViewById(R.id.urldialog_buttonAddPreset).setOnClickListener(this);
        }
    }

    /**
     * Remove leading and trailing slashes from a String
     * 
     * @param path the String to remove the slashes from
     * @return the String
     */
    private String stripPathSeperators(@NonNull String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.urldialog_buttonAddPreset) {
            CheckBox enableCheckBox = (CheckBox) mainView.findViewById(R.id.urldialog_checkboxEnable);
            boolean enable = enableCheckBox != null && enableCheckBox.isChecked();
            PresetEditorActivity.startForResult(this, presetname, preseturl, enable, REQUEST_PRESETEDIT);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        Log.d(DEBUG_TAG, "onOptionsItemSelected");
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PRESETEDIT && resultCode == RESULT_OK) {
            downloadSucessful = true;
        }
    }
}
