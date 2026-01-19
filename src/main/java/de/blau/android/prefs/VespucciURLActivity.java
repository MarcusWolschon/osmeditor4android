package de.blau.android.prefs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewGroupCompat;
import de.blau.android.AsyncResult;
import de.blau.android.Authorize;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.exception.NoOAuthConfigurationException;
import de.blau.android.net.OAuth1aHelper;
import de.blau.android.net.OAuth2Helper;
import de.blau.android.net.OAuthHelper;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.Util;

/**
 * Will process vespucci:// URLs.<br>
 *
 * Handles "preset", "oauth" and "oauth2" paths.
 * 
 * @author Jan
 * @author Simon
 *
 */
public class VespucciURLActivity extends AppCompatActivity {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, VespucciURLActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = VespucciURLActivity.class.getSimpleName().substring(0, TAG_LEN);

    private static final String OAUTH1A_PATH         = "oauth";
    static final String         OAUTH2_PATH          = "oauth2";
    public static final String  PRESET_PATH          = "preset";
    public static final String  PRESETNAME_PARAMETER = "presetname";
    public static final String  PRESETURL_PARAMETER  = "preseturl";
    public static final String  STYLE_PATH           = "style";
    public static final String  STYLENAME_PARAMETER  = "stylename";
    public static final String  STYLEURL_PARAMETER   = "styleurl";

    private String url;
    private String name;

    private AdvancedPrefDatabase prefdb;
    private boolean              downloadSucessful = false;

    private View                           mainView;
    private ActivityResultLauncher<Intent> startForResult;

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
        ViewGroupCompat.installCompatInsetsDispatch(mainView);
        prefdb = new AdvancedPrefDatabase(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), (ActivityResult result) -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                downloadSucessful = true;
            }
        });
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
        if (Util.isEmpty(path)) {
            path = fixupPath(data);
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
            } catch (NoOAuthConfigurationException e) {
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
        case STYLE_PATH:
            setupStyleUi(data);
            break;
        }
        super.onResume();
    }

    /**
     * Hack as the uri may not be encoded properly
     * 
     * @param data the Uri
     * @return the path value
     */
    private String fixupPath(@NonNull Uri data) {
        if (data.getQueryParameter(PRESETURL_PARAMETER) != null) {
            return PRESET_PATH;
        }
        if (data.getQueryParameter(STYLEURL_PARAMETER) != null) {
            return STYLE_PATH;
        }
        return "";
    }

    /**
     * Show the preset download UI
     * 
     * @param data the Uri to use
     */
    private void setupPresetUi(@NonNull Uri data) {
        setupUi(data, PRESETURL_PARAMETER, PRESETNAME_PARAMETER, R.string.preset, R.string.urldialog_add_preset, u -> prefdb.getPresetByURL(u) != null);
        mainView.findViewById(R.id.urldialog_buttonAdd)
                .setOnClickListener(v -> startForResult.launch(PresetConfigurationEditorActivity.getIntent(this, name, url, enable())));
    }

    /**
     * Show the style download UI
     * 
     * @param data the Uri to use
     */
    private void setupStyleUi(@NonNull Uri data) {
        setupUi(data, STYLEURL_PARAMETER, STYLENAME_PARAMETER, R.string.style, R.string.urldialog_add_style, u -> prefdb.getStyleByURL(u) != null);
        mainView.findViewById(R.id.urldialog_buttonAdd)
                .setOnClickListener(v -> startForResult.launch(StyleConfigurationEditorActivity.getIntent(this, name, url, enable())));
    }

    /**
     * Check if the enable checkbox is checked
     * 
     * @return true if checked
     */
    private boolean enable() {
        CheckBox enableCheckBox = (CheckBox) mainView.findViewById(R.id.urldialog_checkboxEnable);
        return enableCheckBox != null && enableCheckBox.isChecked();
    }

    private interface ResourceExists {
        boolean exists(@NonNull String url);
    }

    /**
     * Setup the UI
     * 
     * @param data the URi
     * @param urlParam parameter used to extract the url
     * @param nameParam parameter used to extract the name
     * @param titleRes resource for the tile
     * @param buttonRes resource for the buttom
     * @param urlExists function to check if the url has already been configured
     */
    private void setupUi(@NonNull Uri data, @NonNull String urlParam, @NonNull String nameParam, int titleRes, int buttonRes,
            @NonNull ResourceExists urlExists) {
        url = data.getQueryParameter(urlParam);
        mainView.findViewById(R.id.urldialog_nodata).setVisibility(url == null ? View.VISIBLE : View.GONE);
        name = data.getQueryParameter(nameParam);
        if (url == null) {
            Log.e(DEBUG_TAG, "Null url " + data);
        }
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayShowHomeEnabled(true);
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setTitle(R.string.resource_download_title);
            actionbar.setDisplayShowTitleEnabled(true);
            actionbar.show();
        }
        mainView.findViewById(R.id.urldialog_layoutPreset).setVisibility(View.VISIBLE);

        ((TextView) mainView.findViewById(R.id.urldialog_textTitle)).setText(titleRes);
        ((TextView) mainView.findViewById(R.id.urldialog_textName)).setText(name);
        ((TextView) mainView.findViewById(R.id.urldialog_textURL)).setText(url);
        boolean exists = urlExists.exists(url);
        if (downloadSucessful) {
            mainView.findViewById(R.id.urldialog_textSuccessful).setVisibility(View.VISIBLE);
            mainView.findViewById(R.id.urldialog_textExists).setVisibility(View.GONE);
        } else {
            mainView.findViewById(R.id.urldialog_textExists).setVisibility(exists ? View.VISIBLE : View.GONE);
            mainView.findViewById(R.id.urldialog_textSuccessful).setVisibility(View.GONE);
        }
        mainView.findViewById(R.id.urldialog_checkboxEnable).setVisibility(!exists ? View.VISIBLE : View.GONE);
        Button add = mainView.findViewById(R.id.urldialog_buttonAdd);
        add.setText(buttonRes);
        add.setVisibility(!exists ? View.VISIBLE : View.GONE);
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
    public boolean onOptionsItemSelected(final MenuItem item) {
        Log.d(DEBUG_TAG, "onOptionsItemSelected");
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
