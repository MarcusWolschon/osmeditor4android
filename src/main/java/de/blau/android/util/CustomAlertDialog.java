package de.blau.android.util;

import android.content.Context;
import android.content.DialogInterface;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.widget.AppCompatButton;
import de.blau.android.R;

/*-
 * Minimal custom AlertDialog that avoids Android's buggy AlertDialogLayout button scrolling issue.
 * 
 * This is currently not very refined and only supports a subset of the methods available in AlertDialog, AlertDialog.Builder
 * 
 * AlertDialog
 * 
 * X getButton(int whichButton)
 * - getListView()
 * - onKeyDown(int keyCode, KeyEvent event)     
 * - onKeyUp(int keyCode, KeyEvent event)
 * - setButton(int whichButton, CharSequence text, DialogInterface.OnClickListener listener)   
 * - setButton(int whichButton, CharSequence text, Message msg)
 * - setButton(int whichButton, CharSequence text, Drawable icon, DialogInterface.OnClickListener listener)
 * - setCustomTitle(View customTitleView)
 * - setIcon(Drawable icon)   
 * - setIcon(int resId)
 * - setIconAttribute(int attrId)
 * - setMessage(CharSequence message)
 * - setTitle(CharSequence title)
 * - setView(View view)
 * - setView(View view, int viewSpacingLeft, int viewSpacingTop, int viewSpacingRight, int viewSpacingBottom)
 *
 * AlertFialog.Builder
 * 
 * X create()
 * - getContext()
 * - setAdapter(ListAdapter adapter, DialogInterface.OnClickListener listener)
 * - setCancelable(boolean cancelable)   
 * - setCursor(Cursor cursor, DialogInterface.OnClickListener listener, String labelColumn)
 * - setCustomTitle(View customTitleView)
 * - setIcon(Drawable icon)
 * - setIcon(int iconId)
 * - setIconAttribute(int attrId)   
 * - setInverseBackgroundForced(boolean useInverseBackground)
 * - setItems(CharSequence[] items, DialogInterface.OnClickListener listener)
 * - setItems(int itemsId, DialogInterface.OnClickListener listener)
 * X setMessage(CharSequence message)
 * X setMessage(int messageId)
 * - setMultiChoiceItems(CharSequence[] items, boolean[] checkedItems, DialogInterface.OnMultiChoiceClickListener listener)
 * - setMultiChoiceItems(int itemsId, boolean[] checkedItems, DialogInterface.OnMultiChoiceClickListener listener)  
 * - setMultiChoiceItems(Cursor cursor, String isCheckedColumn, String labelColumn, DialogInterface.OnMultiChoiceClickListener listener)
 * X setNegativeButton(CharSequence text, DialogInterface.OnClickListener listener)
 * X setNegativeButton(int textId, DialogInterface.OnClickListener listener)
 * - setNegativeButtonIcon(Drawable icon)
 * X setNeutralButton(CharSequence text, DialogInterface.OnClickListener listener)  
 * X setNeutralButton(int textId, DialogInterface.OnClickListener listener)   
 * - setNeutralButtonIcon(Drawable icon)  
 * X setOnCancelListener(DialogInterface.OnCancelListener onCancelListener)
 * X setOnDismissListener(DialogInterface.OnDismissListener onDismissListener)
 * - setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener)    
 * - setOnKeyListener(DialogInterface.OnKeyListener onKeyListener)
 * X setPositiveButton(CharSequence text, DialogInterface.OnClickListener listener)
 * X setPositiveButton(int textId, DialogInterface.OnClickListener listener)    
 * - setPositiveButtonIcon(Drawable icon)
 * - setSingleChoiceItems(ListAdapter adapter, int checkedItem, DialogInterface.OnClickListener listener)
 * - setSingleChoiceItems(CharSequence[] items, int checkedItem, DialogInterface.OnClickListener listener) 
 * - setSingleChoiceItems(int itemsId, int checkedItem, DialogInterface.OnClickListener listener)  
 * - setSingleChoiceItems(Cursor cursor, int checkedItem, String labelColumn, DialogInterface.OnClickListener listener)  
 * X setTitle(CharSequence title)   
 * X setTitle(int titleId)
 * - setView(int layoutResId)
 * X setView(View view)
 * X show() 
 */
public class CustomAlertDialog extends AppCompatDialog {

    private static final int BUTTON_PADDING_HORIZONTAL = 16; // dp
    private static final int BUTTON_SPACING            = 6;  // dp

    private Button positive;
    private Button neutral;
    private Button negative;

    public CustomAlertDialog(Context context) {
        super(context);
    }

    public static class Builder {
        private static final int MESSAGE_PADDING = 32;
        private static final int TITLE_PADDING   = 24;
        private static final int PADDING         = 16;
        private Context          context;
        private String           title;
        private String           message;
        private View             customView;

        private Button positive;
        private Button neutral;
        private Button negative;

        private DialogInterface.OnClickListener positiveListener;
        private String                          positiveText;

        private DialogInterface.OnClickListener negativeListener;
        private String                          negativeText;

        private DialogInterface.OnClickListener neutralListener;
        private String                          neutralText;

        private DialogInterface.OnDismissListener dismissListener;
        private DialogInterface.OnCancelListener  cancelListener;
        private DialogInterface.OnShowListener    showListener;

        /**
         * Constructor
         * 
         * @param context the Context
         */
        public Builder(@NonNull Context context) {
            this.context = context;
        }

        /**
         * Set the title
         * 
         * @param title the title text
         * @return this builder
         */
        @NonNull
        public Builder setTitle(@Nullable String title) {
            this.title = title;
            return this;
        }

        /**
         * Set the title from a resource
         * 
         * @param titleId the title resource id
         * @return this builder
         */
        @NonNull
        public Builder setTitle(@StringRes int titleId) {
            this.title = context.getString(titleId);
            return this;
        }

        /**
         * Set the message
         * 
         * @param message the message text
         * @return this builder
         */
        @NonNull
        public Builder setMessage(@Nullable String message) {
            this.message = message;
            return this;
        }

        /**
         * Set the message from a resource
         * 
         * @param messageId the message resource id
         * @return this builder
         */
        @NonNull
        public Builder setMessage(@StringRes int messageId) {
            this.message = context.getString(messageId);
            return this;
        }

        /**
         * Set a custom view
         * 
         * @param view the custom view
         * @return this builder
         */
        @NonNull
        public Builder setView(@NonNull View view) {
            this.customView = view;
            return this;
        }

        /**
         * Set the positive button
         * 
         * @param text the button text
         * @param listener the click listener
         * @return this builder
         */
        @NonNull
        public Builder setPositiveButton(@NonNull String text, @Nullable DialogInterface.OnClickListener listener) {
            this.positiveText = text;
            this.positiveListener = listener;
            return this;
        }

        /**
         * Set the positive button from a resource
         * 
         * @param textId the button text resource id
         * @param listener the click listener
         * @return this builder
         */
        @NonNull
        public Builder setPositiveButton(@StringRes int textId, @Nullable DialogInterface.OnClickListener listener) {
            this.positiveText = context.getString(textId);
            this.positiveListener = listener;
            return this;
        }

        /**
         * Set the negative button
         * 
         * @param text the button text
         * @param listener the click listener
         * @return this builder
         */
        @NonNull
        public Builder setNegativeButton(@NonNull String text, @Nullable DialogInterface.OnClickListener listener) {
            this.negativeText = text;
            this.negativeListener = listener;
            return this;
        }

        /**
         * Set the negative button from a resource
         * 
         * @param textId the button text resource id
         * @param listener the click listener
         * @return this builder
         */
        @NonNull
        public Builder setNegativeButton(@StringRes int textId, @Nullable DialogInterface.OnClickListener listener) {
            this.negativeText = context.getString(textId);
            this.negativeListener = listener;
            return this;
        }

        /**
         * Set the neutral button
         * 
         * @param text the button text
         * @param listener the click listener
         * @return this builder
         */
        @NonNull
        public Builder setNeutralButton(@NonNull String text, @Nullable DialogInterface.OnClickListener listener) {
            this.neutralText = text;
            this.neutralListener = listener;
            return this;
        }

        /**
         * Set the neutral button from a resource
         * 
         * @param textId the button text resource id
         * @param listener the click listener
         * @return this builder
         */
        @NonNull
        public Builder setNeutralButton(@StringRes int textId, @Nullable DialogInterface.OnClickListener listener) {
            this.neutralText = context.getString(textId);
            this.neutralListener = listener;
            return this;
        }

        /**
         * Set the dismiss listener
         * 
         * @param listener the listener
         * @return this builder
         */
        @NonNull
        public Builder setOnDismissListener(@NonNull DialogInterface.OnDismissListener listener) {
            this.dismissListener = listener;
            return this;
        }

        /**
         * Set the cancel listener
         * 
         * @param listener the listener
         * @return this builder
         */
        @NonNull
        public Builder setOnCancelListener(@NonNull DialogInterface.OnCancelListener listener) {
            this.cancelListener = listener;
            return this;
        }

        /**
         * Set the show listener
         * 
         * @param listener the listener
         * @return this builder
         */
        @NonNull
        public Builder setOnShowListener(@NonNull DialogInterface.OnShowListener listener) {
            this.showListener = listener;
            return this;
        }

        /**
         * Create the dialog
         * 
         * @return the created AppCompatDialog
         */
        @NonNull
        public CustomAlertDialog create() {
            // Create main container
            LinearLayout mainContainer = new LinearLayout(context);
            mainContainer.setOrientation(LinearLayout.VERTICAL);
            mainContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // Area for content
            LinearLayout contentContainer = new LinearLayout(context);
            contentContainer.setOrientation(LinearLayout.VERTICAL);
            contentContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 2));

            final int padding = Density.dpToPx(context, PADDING);
            final int titlePadding = Density.dpToPx(context, TITLE_PADDING);
            final int messagePadding = Density.dpToPx(context, MESSAGE_PADDING);

            // Add title if present
            if (title != null) {
                TextView titleView = new TextView(context);
                titleView.setText(title);
                titleView.setTextAppearance(context, R.style.TextAppearance_AppCompat_Title);
                titleView.setPadding(titlePadding, 0, titlePadding, padding);
                LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                contentContainer.addView(titleView, titleParams);
            }

            // Add custom view or message
            if (customView != null) {
                ViewGroup.LayoutParams params = customView.getLayoutParams();
                if (params == null) {
                    params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                }
                contentContainer.addView(customView, params);
            } else if (message != null) {
                TextView messageView = new TextView(context);
                messageView.setText(message);
                messageView.setTextAppearance(context, R.style.TextAppearance_AppCompat_Medium);
                messageView.setPadding(messagePadding, padding, messagePadding, padding);
                LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                ScrollView sv = new ScrollView(context);
                sv.addView(messageView, messageParams);
                sv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                contentContainer.addView(sv);
            }

            // Create button panel with dynamic orientation
            LinearLayout buttonPanel = createButtonPanel();

            mainContainer.addView(contentContainer);
            mainContainer.addView(buttonPanel);

            // Create the dialog
            CustomAlertDialog dialog = new CustomAlertDialog(context);
            dialog.setContentView(mainContainer);

            if (dismissListener != null) {
                dialog.setOnDismissListener(dismissListener);
            }
            if (cancelListener != null) {
                dialog.setOnCancelListener(cancelListener);
            }
            if (showListener != null) {
                dialog.setOnShowListener(showListener);
            }
            dialog.positive = positive;
            if (positive != null) {
                positive.setOnClickListener(v -> dialog.handleButtonClick(positiveListener, DialogInterface.BUTTON_POSITIVE));
            }
            dialog.neutral = neutral;
            if (neutral != null) {
                neutral.setOnClickListener(v -> dialog.handleButtonClick(neutralListener, DialogInterface.BUTTON_NEUTRAL));
            }
            dialog.negative = negative;
            if (negative != null) {
                negative.setOnClickListener(v -> dialog.handleButtonClick(negativeListener, DialogInterface.BUTTON_NEGATIVE));
            }
            return dialog;
        }

        /**
         * Create the button panel with appropriate orientation, this is a bit hackish
         * 
         * @return the configured button panel
         */
        @NonNull
        private LinearLayout createButtonPanel() {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            // Add neutral button first (left/top in both layouts)
            float buttonsHorizontal = 0;
            int buttonCount = 0;
            if (neutralText != null) {
                neutral = createButton(inflater, neutralText);
                neutral.setId(android.R.id.button3);
                buttonsHorizontal += neutral.getPaint().measureText(neutralText);
                buttonCount++;
            }

            // Add negative button (middle)
            if (negativeText != null) {
                negative = createButton(inflater, negativeText);
                negative.setId(android.R.id.button2);
                buttonsHorizontal += negative.getPaint().measureText(negativeText);
                buttonCount++;
            }

            // Add positive button (right/bottom in both layouts)
            if (positiveText != null) {
                positive = createButton(inflater, positiveText);
                positive.setId(android.R.id.button1);
                buttonsHorizontal += positive.getPaint().measureText(positiveText);
                buttonCount++;
            }

            int panelPadding = Density.dpToPx(context, BUTTON_PADDING_HORIZONTAL);

            boolean horizontal = buttonsHorizontal + (buttonCount - 1) * panelPadding < maxWidth();

            LinearLayout buttonPanel = new LinearLayout(context);
            buttonPanel.setOrientation(horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
            buttonPanel.setPadding(panelPadding, panelPadding, panelPadding, panelPadding);

            LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            buttonPanel.setLayoutParams(panelParams);

            // Button layout parameters
            int spacing = Density.dpToPx(context, BUTTON_SPACING);
            LinearLayout.LayoutParams buttonParams;
            if (horizontal && buttonCount > 1) {
                // Horizontal: equal weight distribution
                buttonParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                buttonParams.setMargins(spacing / 2, 0, spacing / 2, 0);
            } else {
                // Vertical: full width, stacked
                buttonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                buttonParams.setMargins(0, spacing / 2, 0, spacing / 2);
                buttonParams.gravity = android.view.Gravity.END;
            }

            if (neutralText != null) {
                buttonPanel.addView(neutral, buttonParams);
            }

            // Add negative button (middle)
            if (negativeText != null) {
                buttonPanel.addView(negative, buttonParams);
            }

            // Add positive button (right/bottom in both layouts)
            if (positiveText != null) {
                buttonPanel.addView(positive, buttonParams);
            }

            buttonPanel.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);

            return buttonPanel;
        }

        /**
         * Determine max wiDth of dialog
         * 
         * @return width in dp
         */
        private int maxWidth() {
            // Get screen width
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int screenWidthDp = (int) (displayMetrics.widthPixels / displayMetrics.density);

            // Calculate available width for buttons (accounting for dialog padding and margins)
            // Dialog typically has ~24dp padding on each side
            return screenWidthDp - 48;
        }

        /**
         * Create a styled button
         * 
         * @param inflater a LayoutInflater
         * @param text the button text
         * @return the created button
         */
        @NonNull
        private Button createButton(@NonNull LayoutInflater inflater, @NonNull String text) {
            AppCompatButton button = (AppCompatButton) inflater.inflate(R.layout.styled_button, null);
            button.setText(text);
            return button;
        }

        /**
         * Create and show the dialog
         * 
         * @return the created and shown AppCompatDialog
         */
        @NonNull
        public AppCompatDialog show() {
            AppCompatDialog dialog = create();
            dialog.show();
            return dialog;
        }

    }

    /**
     * Handle button click
     * 
     * @param listener the click listener
     * @param which which button was clicked
     */
    private void handleButtonClick(@Nullable DialogInterface.OnClickListener listener, int which) {
        if (listener != null) {
            listener.onClick(this, which);
        }
        this.dismiss();
    }

    @Nullable
    public Button getButton(int button) {
        switch (button) {
        case DialogInterface.BUTTON_POSITIVE:
            return positive;
        case DialogInterface.BUTTON_NEUTRAL:
            return neutral;
        case DialogInterface.BUTTON_NEGATIVE:
            return negative;
        default:
            throw new IllegalArgumentException("Unknoen button index");
        }
    }
}