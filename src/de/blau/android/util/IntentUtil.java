package de.blau.android.util;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import de.blau.android.HelpViewer;

public abstract class IntentUtil {

    public static
    @NonNull
    Intent getHelpViewerIntent(@NonNull Context context, @StringRes int topic) {
        Intent intent = getIntent(context, HelpViewer.class);
        intent.putExtra(HelpViewer.TOPIC, topic);
        return intent;
    }

    private static
    @NonNull
    Intent getIntent(@NonNull Context context, @NonNull Class<?> clazz) {
        return new Intent(context, clazz);
    }

}
