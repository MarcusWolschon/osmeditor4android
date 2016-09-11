package de.blau.android.propertyeditor;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.AppCompatButton;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import ch.poole.conditionalrestrictionparser.Condition;
import ch.poole.conditionalrestrictionparser.Condition.CompOp;
import ch.poole.conditionalrestrictionparser.ConditionalRestrictionParser;
import ch.poole.conditionalrestrictionparser.Conditions;
import ch.poole.conditionalrestrictionparser.ParseException;
import ch.poole.conditionalrestrictionparser.Restriction;
import ch.poole.conditionalrestrictionparser.TokenMgrError;
import ch.poole.conditionalrestrictionparser.Util;
import de.blau.android.R;
import de.blau.android.util.ThemeUtils;

public class ConditionalRestrictionFragment extends DialogFragment {

	private static final int LINEARLAYOUT_ID = 12345;

	private static final String KEY_KEY = "key";
	
	private static final String VALUE_KEY = "value";

	private static final String DEBUG_TAG = ConditionalRestrictionFragment.class.getSimpleName();

	private LayoutInflater inflater = null;

	private TextWatcher watcher;
	
	private String key;
	private String conditionalRestrictionValue;
	
	private ArrayList<Restriction> restrictions = null;
	
	private EditText text;
	
	private OnSaveListener saveListener = null;

	/**
	 */
	static public ConditionalRestrictionFragment newInstance(String key,String value) {
		ConditionalRestrictionFragment f = new ConditionalRestrictionFragment();

		Bundle args = new Bundle();
		args.putSerializable(KEY_KEY, key);
		args.putSerializable(VALUE_KEY, value);

		f.setArguments(args);
		// f.setShowsDialog(true);

		return f;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d(DEBUG_TAG, "onAttach");
        try {
            saveListener = (OnSaveListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnSaveListener");
        }
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(DEBUG_TAG, "onCreate");
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
	  Dialog dialog = super.onCreateDialog(savedInstanceState);

	  // request a window without the title
	  dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
	  return dialog;
	}
	
	@SuppressLint("InflateParams")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		this.inflater = ThemeUtils.getLayoutInflater(getActivity());
		
		LinearLayout conditionalRestrictionLayout = (LinearLayout) inflater.inflate(R.layout.conditionalrestriction, null);

		if (savedInstanceState == null) {
			key = getArguments().getString(KEY_KEY);
			conditionalRestrictionValue = getArguments().getString(VALUE_KEY);
		} else {
			key = savedInstanceState.getString(KEY_KEY);
			conditionalRestrictionValue = savedInstanceState.getString(VALUE_KEY);
		}
		
		buildLayout(conditionalRestrictionLayout, conditionalRestrictionValue);

		// add callbacks for the buttons
		AppCompatButton cancel = (AppCompatButton) conditionalRestrictionLayout.findViewById(R.id.cancel);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();				
			}});
		
		AppCompatButton save = (AppCompatButton) conditionalRestrictionLayout.findViewById(R.id.save);
		save.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveListener.save(key,text.getText().toString());	
				dismiss();
			}});
		
		return conditionalRestrictionLayout;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		Log.d(DEBUG_TAG, "onOptionsItemSelected");
		return true;
	}
	
	/**
	 * 
	 * @param conditionalRestrictionLayout
	 * @param openingHoursValue2
	 */
	private void buildLayout(final LinearLayout conditionalRestrictionLayout, String conditionalRestrictionValue) {
		text = (EditText) conditionalRestrictionLayout.findViewById(R.id.conditional_restriction_string_edit);
		final ScrollView sv = (ScrollView) conditionalRestrictionLayout.findViewById(R.id.conditional_restriction_view);
		if (text != null && sv != null) {
			text.setText(conditionalRestrictionValue);
			sv.removeAllViews();
			
			watcher = new TextWatcher() {
				@Override
				public void afterTextChanged(Editable s) {
					Runnable rebuild = new Runnable() {
						@Override
						public void run() {				
							ConditionalRestrictionParser parser = new ConditionalRestrictionParser(new ByteArrayInputStream(text.getText().toString().getBytes()));
							try {
								restrictions = parser.restrictions();
								removeHighlight(text);
							} catch (ParseException pex) {
								Log.d(DEBUG_TAG, pex.getMessage());
								highlightParseError(text, pex);
							} catch (TokenMgrError err) {
								// we currently can't do anything reasonable here except ignore
								Log.e(DEBUG_TAG, err.getMessage());
							}
							if (restrictions == null) { // couldn't parse anything
								restrictions = new ArrayList<Restriction>();
							}
							buildForm(sv,restrictions);
						}
					};
					text.removeCallbacks(rebuild);
					text.postDelayed(rebuild, 500);
				}
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}
			};
			text.addTextChangedListener(watcher);
			
			FloatingActionButton add = (FloatingActionButton) conditionalRestrictionLayout.findViewById(R.id.add);
			add.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					LinearLayout ll = (LinearLayout) conditionalRestrictionLayout.findViewById(12345);
					Restriction r = new Restriction("", new Conditions(Arrays.asList(new Condition("",false)),false));
					restrictions.add(r);
					addRestriction(ll, r);
				}});
			
			ConditionalRestrictionParser parser = new ConditionalRestrictionParser(new ByteArrayInputStream(conditionalRestrictionValue.getBytes()));
			try {
				restrictions = parser.restrictions();
				removeHighlight(text);
			} catch (ParseException pex) {
				Log.d(DEBUG_TAG, pex.getMessage());
				highlightParseError(text, pex);
			} catch (TokenMgrError err) {
				// we currently can't do anything reasonable here except ignore 
				Log.e(DEBUG_TAG, err.getMessage());
			}
			if (restrictions == null) { // couldn't parse anything
				restrictions = new ArrayList<Restriction>();
			}
			buildForm(sv,restrictions);
		}
	}
	
	/**
	 * Highlight the position of a parse error
	 * @param text
	 * @param pex
	 */
	private void highlightParseError(EditText text, ParseException pex) {
		if (text.length() > 0) {
			int c = pex.currentToken.next.beginColumn-1; // starts at 1
			int pos = text.getSelectionStart();
			Spannable spannable = new SpannableString(text.getText());
			spannable.setSpan(new ForegroundColorSpan(Color.RED), c, Math.max(c,Math.min(c+1,spannable.length())), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			text.removeTextChangedListener(watcher); // avoid infinite loop
			text.setText(spannable, TextView.BufferType.SPANNABLE);
			text.setSelection(Math.min(pos,spannable.length()));
			Toast.makeText(text.getContext(), pex.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}
		text.addTextChangedListener(watcher);
	}
	
	/**
	 * Remove all highlighting
	 * @param text
	 */
	private void removeHighlight(EditText text) {
		int pos = text.getSelectionStart();
		int prevLen = text.length();
		text.removeTextChangedListener(watcher); // avoid infinite loop
		String t = Util.restrictionsToString(restrictions);
		text.setText(t);
		// text.setText(text.getText().toString());
		text.setSelection(prevLen < text.length() ? text.length() : Math.min(pos,text.length()));
		text.addTextChangedListener(watcher);
	}

	private synchronized void buildForm(ScrollView sv, ArrayList<Restriction> restrictions) {

		sv.removeAllViews();
		LinearLayout ll = new LinearLayout(getActivity());
		ll.setId(LINEARLAYOUT_ID);
		ll.setPadding(0, 0, 0, dpToPixels(64));
		ll.setOrientation(LinearLayout.VERTICAL);
		sv.addView(ll);

		int n = 1;
		for (Restriction r : restrictions) {
			addRestriction(ll, r);
		}
	}
	
	private void addRestriction(LinearLayout ll, final Restriction r) {
		LinearLayout groupHeader = (LinearLayout) inflater.inflate(R.layout.restriction_header, null);
		TextView header = (TextView) groupHeader.findViewById(R.id.header);
		header.setText(R.string.tag_restriction_header);
		final AutoCompleteTextView value = (AutoCompleteTextView) groupHeader.findViewById(R.id.editValue);
		value.setText(r.getValue().trim());
		TextWatcher valueWatcher = new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				r.setValue(value.getText().toString().trim());
				Runnable rebuild = new Runnable() {
					@Override
					public void run() {
						updateRestrictionStringFromView(value, r);
					}
				};
				value.removeCallbacks(rebuild);
				value.postDelayed(rebuild, 500);
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		};
		value.addTextChangedListener(valueWatcher);
		addMenuItems(groupHeader, r, null);
		ll.addView(groupHeader);
		boolean first = true;
		for (int i=0;i<r.getConditions().size();i++) {
			Condition c = r.getConditions().get(i);
			if (c != null) {
				final int index = i;
				if (c.isExpression()) {
					LinearLayout expression = (LinearLayout) inflater.inflate(R.layout.expression, null);
					final TextView expressionText = (TextView) expression.findViewById(R.id.text);
					if (first) {
						expressionText.setText(R.string.tag_restriction_when);
						first = false;
					} else {
						expressionText.setText(R.string.tag_restriction_and);
					}
					final AutoCompleteTextView term1 = (AutoCompleteTextView) expression.findViewById(R.id.editTerm1);
					term1.setText(c.term1());
					final AutoCompleteTextView term2 = (AutoCompleteTextView) expression.findViewById(R.id.editTerm2);
					term2.setText(c.term2());
					addMenuItems(expression, r, c);
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(
						    getActivity(), R.layout.support_simple_spinner_dropdown_item, Condition.compOpStrings);
					// adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
					final Spinner operator = (Spinner) expression.findViewById(R.id.operator);
					operator.setAdapter(adapter);
					operator.setSelection(Condition.compOpStrings.indexOf(Condition.opToString(c.operator())));
					final Runnable rebuild = new Runnable() {
						@Override
						public void run() {				
							updateRestrictionStringFromView(null, r);
						}
					};
					operator.setOnItemSelectedListener(new OnItemSelectedListener() {
						@Override
						public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
							List<Condition> list = r.getConditions(); 
							final String c = term1.getText().toString().trim()+operator.getSelectedItem()+term2.getText().toString().trim();
							list.set(index, new Condition(c, false));
							term1.removeCallbacks(rebuild);
							term1.post(rebuild);
						}
						@Override
						public void onNothingSelected(AdapterView<?> parent) {
						}});
					TextWatcher expressionWatcher = new TextWatcher() {
						@Override
						public void afterTextChanged(Editable s) {
							List<Condition> list = r.getConditions(); 
							final String c = term1.getText().toString().trim()+operator.getSelectedItem()+term2.getText().toString().trim();
							list.set(index, new Condition(c, false));
							term1.removeCallbacks(rebuild);
							term1.postDelayed(rebuild, 500);
						}
						@Override
						public void beforeTextChanged(CharSequence s, int start, int count, int after) {
						}
						@Override
						public void onTextChanged(CharSequence s, int start, int before, int count) {
						}
					};
					term1.addTextChangedListener(expressionWatcher);
					term2.addTextChangedListener(expressionWatcher);
					ll.addView(expression);
				} else {
					// for now simply fill the ATV differently for OH
					LinearLayout condition = (LinearLayout) inflater.inflate(R.layout.condition, null);
					final TextView conditionText = (TextView) condition.findViewById(R.id.text);
					if (first) {
						conditionText.setText("when");
						first = false;
					} else {
						conditionText.setText("and");
					}
					final AutoCompleteTextView atv = (AutoCompleteTextView) condition.findViewById(R.id.editCondition);					
					if (c.isOpeningHours()) {
					} else {
					}					
					atv.setText(c.toString());
					TextWatcher conditionWatcher = new TextWatcher() {
						@Override
						public void afterTextChanged(Editable s) {
							List<Condition> list = r.getConditions(); 
							list.set(index, new Condition(atv.getText().toString().trim(), false));
							Runnable rebuild = new Runnable() {
								@Override
								public void run() {				
									updateRestrictionStringFromView(atv, r);
								}
							};
							atv.removeCallbacks(rebuild);
							atv.postDelayed(rebuild, 500);
						}
						@Override
						public void beforeTextChanged(CharSequence s, int start, int count, int after) {
						}
						@Override
						public void onTextChanged(CharSequence s, int start, int before, int count) {
						}
					};
					atv.addTextChangedListener(conditionWatcher);
					addMenuItems(condition, r, c);
					ll.addView(condition);
				}
			}
		}
	}
	
	private void updateRestrictionStringFromView(EditText view, Restriction r) {
		text.removeTextChangedListener(watcher);
		int pos = 0;
		if (view != null) {
			pos = view.getSelectionStart();
		}
		conditionalRestrictionValue = Util.restrictionsToString(restrictions);
		text.setText(conditionalRestrictionValue);
		String myText = r.toString();
		int textPos = conditionalRestrictionValue.lastIndexOf(myText);
		 // make what we are currently editing visible, this is a bit of a hack
		if (textPos < conditionalRestrictionValue.length()/2) {
			text.setSelection(textPos);
		} else {
			text.setSelection(textPos + myText.length());
		}
		if (view != null) {
			view.setSelection(Math.min(pos,text.length()));
		}
		text.addTextChangedListener(watcher);
	}
	
	/**
	 * Add menu items to the restriction menu to add various types of conditions
	 * @param menu
	 * @param item
	 * @param r
	 * @param c
	 */
	private void addConditionItem(Menu menu,int item,final Restriction r, final Condition c) {
		MenuItem mi = menu.add(item);
		mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem arg0) {
				Log.d(DEBUG_TAG, "onMenuItemClick");
				r.getConditions().add(c);
				conditionalRestrictionValue = Util.restrictionsToString(restrictions);
				text.setText(conditionalRestrictionValue);
				return true;
			}});
		MenuItemCompat.setShowAsAction(mi,MenuItemCompat.SHOW_AS_ACTION_NEVER);
	}
	
	private Menu addMenuItems(LinearLayout row, final Restriction r, final Condition c) {
		ActionMenuView amv = (ActionMenuView) row.findViewById(R.id.menu);
		Menu menu = amv.getMenu();
		if (c == null) {
			addConditionItem(menu,R.string.tag_restriction_add_simple_condition,r,new Condition("",false));
			addConditionItem(menu,R.string.tag_restriction_add_expression,r,new Condition("",CompOp.EQ,""));
			addConditionItem(menu,R.string.tag_restriction_add_opening_hours,r,new Condition("",true));
		}
		MenuItem mi = menu.add(R.string.delete);
		mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem arg0) {
				Log.d(DEBUG_TAG, "onMenuItemClick");
				if (c == null) {
					restrictions.remove(r);
				} else {
					r.getConditions().remove(c);
				}
				conditionalRestrictionValue = Util.restrictionsToString(restrictions);
				text.setText(conditionalRestrictionValue);
				return true;
			}});
		MenuItemCompat.setShowAsAction(mi,MenuItemCompat.SHOW_AS_ACTION_NEVER);
		return menu;
	}
	
	Runnable updateStringRunnable = new Runnable() {
		@Override
		public void run() {
			int pos = text.getSelectionStart();
			int prevLen = text.length();
			text.removeTextChangedListener(watcher);
			String oh = Util.restrictionsToString(restrictions);
			text.setText(oh);
			text.setSelection(prevLen < text.length() ? text.length() : Math.min(pos,text.length()));
			text.addTextChangedListener(watcher);
		}
	};
	
	/**
	 * Update the actual CR string
	 */
	private void updateString() {
		text.removeCallbacks(updateStringRunnable);
		text.postDelayed(updateStringRunnable,100);
	}

    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	Log.d(DEBUG_TAG, "onSaveInstanceState");
    	outState.putSerializable(VALUE_KEY, text.getText().toString());
    }  

	@Override
	public void onPause() {
		super.onPause();
		Log.d(DEBUG_TAG, "onPause");
		// savedParents = getParentRelationMap();
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d(DEBUG_TAG, "onStop");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(DEBUG_TAG, "onDestroy");
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		// disable address tagging for stuff that won't have an address
		// menu.findItem(R.id.tag_menu_address).setVisible(!type.equals(Way.NAME)
		// || element.hasTagKey(Tags.KEY_BUILDING));
	}

	/**
	 * Return the view we have our rows in and work around some android
	 * craziness
	 * 
	 * @return
	 */
	public View getOurView() {
		// android.support.v4.app.NoSaveStateFrameLayout
		View v = getView();
		if (v != null) {
			if (v.getId() == R.id.conditional_restriction_layout) {
				Log.d(DEBUG_TAG, "got correct view in getView");
				return v;
			} else {
				v = v.findViewById(R.id.conditional_restriction_layout);
				if (v == null) {
					Log.d(DEBUG_TAG, "didn't find R.id.openinghours_layout");
				} else {
					Log.d(DEBUG_TAG, "Found R.id.openinghours_layoutt");
				}
				return v;
			}
		} else {
			Log.d(DEBUG_TAG, "got null view in getView");
		}
		return null;
	}
	
	private int dpToPixels(int dp) {
		Resources r = getResources();
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
	}
}
