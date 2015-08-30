package de.blau.android.osb;

import java.util.List;

import com.actionbarsherlock.app.SherlockDialogFragment;

import de.blau.android.Application;
import de.blau.android.R;
import de.blau.android.R.id;
import de.blau.android.R.layout;
import de.blau.android.R.string;
import de.blau.android.listener.UpdateViewListener;
import de.blau.android.osb.Bug.State;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.propertyeditor.TagEditorFragment;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * Very simple dialog fragment to display bug or notes
 * @author simon
 *
 */
public class BugFragment extends SherlockDialogFragment {
	private static final String DEBUG_TAG = BugFragment.class.getSimpleName();
	 
	UpdateViewListener mListener;

    /**
     */
    static public BugFragment newInstance(Bug b) {
    	BugFragment f = new BugFragment();

        Bundle args = new Bundle();
        args.putSerializable("bug", b);

        f.setArguments(args);
        f.setShowsDialog(true);
        
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @SuppressLint("NewApi")
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	final Bug bug = (Bug) getArguments().getSerializable("bug");
    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    	// Get the layout inflater
    	LayoutInflater inflater = getActivity().getLayoutInflater();

    	final Preferences prefs = new Preferences(getActivity());
    	
    	// Inflate and set the layout for the dialog
    	// Pass null as the parent view because its going in the dialog layout
    	final View v = inflater.inflate(R.layout.openstreetbug_edit, null);
    	builder.setView(v)
    		// Add action buttons
    		.setPositiveButton(bug instanceof Note && bug.isNew() ? R.string.openstreetbug_commitbutton : R.string.save, new DialogInterface.OnClickListener() { 
    			public void onClick(DialogInterface dialog, int id) {
    				saveBug(v,bug);
    			}
    		})
    		.setNeutralButton(R.string.transfer_download_current_upload, new DialogInterface.OnClickListener() { 
    			public void onClick(DialogInterface dialog, int id) {
    				saveBug(v,bug);
    				if (bug instanceof Note) {
    					Note n = (Note)bug;
    					NoteComment nc = n.getLastComment();
    					TransferBugs.uploadNote(getActivity(), prefs.getServer(), n, (nc != null && nc.isNew()) ? nc.getText() : null, n.state == State.CLOSED, false);
    				} else if (bug instanceof OsmoseBug) {
    					TransferBugs.uploadOsmoseBug((OsmoseBug)bug);
    				}
    			}
    		})
    		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int id) {
    			}
    		});

    	final Spinner state = (Spinner)v.findViewById(R.id.openstreetbug_state);
    	ArrayAdapter<CharSequence> adapter = null;

    	TextView comments = (TextView)v.findViewById(R.id.openstreetbug_comments);
    	if (bug instanceof Note) {
    		builder.setTitle(getString((bug.isNew() && ((Note)bug).count() == 0) ? R.string.openstreetbug_new_title : R.string.openstreetbug_edit_title));  		
    		comments.setText(Html.fromHtml(((Note)bug).getComment())); // ugly
    		EditText comment = (EditText)v.findViewById(R.id.openstreetbug_comment);
    		NoteComment nc = ((Note) bug).getLastComment();
    		if ((bug.isNew() && ((Note)bug).count() == 0) || (nc != null && !nc.isNew())) { // only show comment field if we don't have an unsaved comment
    			Log.d(DEBUG_TAG,"enabling comment field");
    			comment.setText("");
    			comment.setFocusable( true);
    			comment.setFocusableInTouchMode(true);
    			comment.setEnabled(true);
    		} else {
    			TextView commentLabel = (TextView)v.findViewById(R.id.openstreetbug_comment_label);
        		commentLabel.setVisibility(View.GONE);
        		comment.setVisibility(View.GONE);
    		}
    		adapter = ArrayAdapter.createFromResource(getActivity(),
        	        R.array.note_state, android.R.layout.simple_spinner_item);
    	} else if (bug instanceof OsmoseBug) {
    		builder.setTitle(R.string.openstreetbug_bug_title);
    		comments.setText(Html.fromHtml(((OsmoseBug)bug).getLongDescription(getActivity())));
    		TextView commentLabel = (TextView)v.findViewById(R.id.openstreetbug_comment_label);
    		commentLabel.setVisibility(View.GONE);
    		EditText comment = (EditText)v.findViewById(R.id.openstreetbug_comment);
    		comment.setVisibility(View.GONE);
    		adapter = ArrayAdapter.createFromResource(getActivity(),
        	        R.array.bug_state, android.R.layout.simple_spinner_item);
    	} else {
    		// unknown bug type
    		Log.d(DEBUG_TAG, "Unknown bug type " + bug.getDescription());
    		return null;
    	}

    	// Specify the layout to use when the list of choices appears
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
       	// Apply the adapter to the spinner
    	state.setAdapter(adapter);
    	
    	if (bug.state == State.OPEN) {
    		state.setSelection(Bug.POS_OPEN);
    	} else if (bug.state == State.CLOSED) {
    		state.setSelection(Bug.POS_CLOSED);
    	} else if (bug.state == State.FALSE_POSITIVE) {
    		if (adapter.getCount() == 3) {
    			state.setSelection(Bug.POS_FALSE_POSITIVE);
    		} else {
    			Log.d(DEBUG_TAG, "ArrayAdapter too short");
    		}
    	} 
    	
    	state.setEnabled(!bug.isNew()); // new bugs always open
    	Dialog d = builder.create();
    	
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
    		d.setOnShowListener(new OnShowListener() { // FIXME need replacement for old API
    			@Override
    			public void onShow(DialogInterface dialog) {                    //
    				final Button save = ((AlertDialog) dialog)
    						.getButton(AlertDialog.BUTTON_POSITIVE);
    				if ((bug instanceof Note && bug.isNew() && ((Note)bug).count() == 1) || !bug.hasBeenChanged()) {
    					save.setEnabled(false);
    				}
    				final Button upload = ((AlertDialog) dialog)
    						.getButton(AlertDialog.BUTTON_NEUTRAL);
    				if (!bug.hasBeenChanged()) {
    					upload.setEnabled(false);
    				}
    				state.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
    					@Override
    					public void onItemSelected(AdapterView<?> arg0, View arg1,
    							int arg2, long arg3) {
    						save.setEnabled(true);
    						upload.setEnabled(true);	
    					}

    					@Override
    					public void onNothingSelected(AdapterView<?> arg0) {
    					}
    				});
    				EditText comment = (EditText)v.findViewById(R.id.openstreetbug_comment);
    				comment.addTextChangedListener(new TextWatcher(){
    					@Override
    					public void afterTextChanged(Editable arg0) {
    					}

    					@Override
    					public void beforeTextChanged(CharSequence s, int start,
    							int count, int after) {
    					}

    					@Override
    					public void onTextChanged(CharSequence s, int start,
    							int before, int count) {
    						save.setEnabled(true);
    						upload.setEnabled(true);
    						state.setSelection(Bug.POS_OPEN);
    					}    				
    				});
    			}
    		});
    	}
		d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    	return d;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(DEBUG_TAG, "onAttach");
        try {
            mListener = (UpdateViewListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPresetSelectedListener");
        }
    }
    
    @Override
    public void onDismiss(DialogInterface dialog) {
    	super.onDismiss(dialog);
    	if (mListener != null) {
    		mListener.update();
    	}
    }
    
    public static State pos2state(int pos) {
		if (pos == Bug.POS_CLOSED) {
			return State.CLOSED;
		} else if (pos == Bug.POS_OPEN) {
			return State.OPEN;
		} else if (pos == Bug.POS_FALSE_POSITIVE) {
			return State.FALSE_POSITIVE;
		}
		return State.OPEN;
    }
    
    void saveBug(View v, Bug bug) {
    	if (bug.isNew() && ((Note)bug).count() == 0) {
			Application.getBugStorage().add(bug); // sets dirty
		}
		String c = ((EditText)v.findViewById(R.id.openstreetbug_comment)).getText().toString();
		if (c.length() > 0) {
			((Note)bug).addComment(c);
		}
		final Spinner state = (Spinner)v.findViewById(R.id.openstreetbug_state);
		bug.state = pos2state(state.getSelectedItemPosition());
		bug.changed = true;
		Application.getBugStorage().setDirty();
    }
}
