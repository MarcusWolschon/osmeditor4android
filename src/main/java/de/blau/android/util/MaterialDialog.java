/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package de.blau.android.util;

import static com.google.android.material.theme.overlay.MaterialThemeOverlay.wrap;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.InsetDialogOnTouchListener;
import com.google.android.material.dialog.MaterialDialogs;
import com.google.android.material.resources.MaterialAttributes;
import com.google.android.material.shape.MaterialShapeDrawable;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.view.ContextThemeWrapper;

/**
 * A Material styled general dialog based on code extracted from googles MaterialAlertDialogBuilder
 */
public class MaterialDialog extends AppCompatDialog {

    private static final int DEF_STYLE_ATTR = androidx.appcompat.R.attr.alertDialogStyle;

    private static final int DEF_STYLE_RES = com.google.android.material.R.style.MaterialAlertDialog_MaterialComponents;

    private static final int MATERIAL_ALERT_DIALOG_THEME_OVERLAY = com.google.android.material.R.attr.materialAlertDialogTheme;

    @Nullable
    private Drawable   background;
    @NonNull
    private final Rect backgroundInsets;

    private static int getMaterialAlertDialogThemeOverlay(@NonNull Context context) {
        TypedValue materialAlertDialogThemeOverlay = MaterialAttributes.resolve(context, MATERIAL_ALERT_DIALOG_THEME_OVERLAY);
        if (materialAlertDialogThemeOverlay == null) {
            return 0;
        }
        return materialAlertDialogThemeOverlay.data;
    }

    private static Context createMaterialAlertDialogThemedContext(@NonNull Context context) {
        int themeOverlayId = getMaterialAlertDialogThemeOverlay(context);
        Context themedContext = wrap(context, null, DEF_STYLE_ATTR, DEF_STYLE_RES);
        if (themeOverlayId == 0) {
            return themedContext;
        }
        return new ContextThemeWrapper(themedContext, themeOverlayId);
    }

    private static int getOverridingThemeResId(@NonNull Context context, int overrideThemeResId) {
        return overrideThemeResId == 0 ? getMaterialAlertDialogThemeOverlay(context) : overrideThemeResId;
    }

    public MaterialDialog(Context context) {

        super(createMaterialAlertDialogThemedContext(context),
                getOverridingThemeResId(context, -1 /* overrideThemeResId */));
        // Ensure we are using the correctly themed context rather than the context that was passed in.
        context = getContext();
        Theme theme = context.getTheme();

        backgroundInsets = MaterialDialogs.getDialogBackgroundInsets(context, DEF_STYLE_ATTR, DEF_STYLE_RES);

        int backgroundColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, getClass().getCanonicalName());

        TypedArray a = context.obtainStyledAttributes(/* set= */ null, com.google.android.material.R.styleable.MaterialAlertDialog, DEF_STYLE_ATTR,
                DEF_STYLE_RES);

        a.recycle();

        MaterialShapeDrawable materialShapeDrawable = new MaterialShapeDrawable(context, null, DEF_STYLE_ATTR, DEF_STYLE_RES);
        materialShapeDrawable.initializeElevationOverlay(context);
        materialShapeDrawable.setFillColor(ColorStateList.valueOf(backgroundColor));

        // dialogCornerRadius first appeared in Android Pie
        if (Build.VERSION.SDK_INT >= VERSION_CODES.P) {
            TypedValue dialogCornerRadiusValue = new TypedValue();
            theme.resolveAttribute(android.R.attr.dialogCornerRadius, dialogCornerRadiusValue, true);
            float dialogCornerRadius = dialogCornerRadiusValue.getDimension(getContext().getResources().getDisplayMetrics());
            if (dialogCornerRadiusValue.type == TypedValue.TYPE_DIMENSION && dialogCornerRadius >= 0) {
                materialShapeDrawable.setCornerSize(dialogCornerRadius);
            }
        }
        background = materialShapeDrawable;

        Window window = getWindow();
        View decorView = window.getDecorView();
        if (background instanceof MaterialShapeDrawable) {
            ((MaterialShapeDrawable) background).setElevation(decorView.getElevation());
        }
        // reducing the size of the drawable by the size of the insets that the original code does here doesn't actually
        // work for large dialogs
        window.setBackgroundDrawable(background);
        decorView.setOnTouchListener(new InsetDialogOnTouchListener(this, backgroundInsets));
    }
}
