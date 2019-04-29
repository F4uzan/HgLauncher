package mono.hg.wrappers;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import mono.hg.R;
import mono.hg.helpers.PreferenceHelper;

/**
 * A DialogFragment class that loads a list of apps, in style of ListPreference.
 */
public class AppSelectionPreferenceDialog extends DialogFragment {
    private String preference;
    private String mValue = null;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private int mClickedDialogEntryIndex;

    private DialogInterface.OnClickListener selectItemListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            if (mClickedDialogEntryIndex != which) {
                mClickedDialogEntryIndex = which;
                mValue = mEntryValues[mClickedDialogEntryIndex].toString();
                PreferenceHelper.update(preference, mValue);
            }
            dialog.dismiss();
        }
    };

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            preference = getArguments().getString("key");
            mEntries = getArguments().getCharSequenceArray("entries");
            mEntryValues = getArguments().getCharSequenceArray("entryValues");
        }
    }

    @Override public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mValue == null) {
            Intent i = new Intent().putExtra("key", preference);
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED,
                    i);
            PreferenceHelper.update(preference, "none");
        } else {
            Intent i = new Intent().putExtra("key", preference).putExtra("app", mValue);
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, i);
        }
    }

    @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(requireActivity());
        dialog.setNegativeButton(android.R.string.cancel, null);
        dialog.setTitle(R.string.gesture_action_app_dialog_title);
        dialog.setSingleChoiceItems(mEntries, mClickedDialogEntryIndex, selectItemListener);
        return dialog.create();
    }
}