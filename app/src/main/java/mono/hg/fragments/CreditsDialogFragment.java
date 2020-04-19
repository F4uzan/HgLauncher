package mono.hg.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.util.Linkify;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.text.util.LinkifyCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import mono.hg.R;
import mono.hg.databinding.FragmentCreditsDialogBinding;
import mono.hg.utils.Utils;

public class CreditsDialogFragment extends DialogFragment {
    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        FragmentCreditsDialogBinding binding = FragmentCreditsDialogBinding.inflate(getActivity().getLayoutInflater());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader br = null;

        builder.setTitle(R.string.about_credits_dialog_title);
        builder.setView(binding.getRoot());
        builder.setPositiveButton(R.string.dialog_action_close, null);
        TextView creditsText = binding.creditsPlaceholder;

        try {
            br = new BufferedReader(
                    new InputStreamReader(getActivity().getAssets().open("credits.txt")));
            String line;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            }
        } catch (IOException e) {
            Utils.sendLog(Utils.LogLevel.ERROR,
                    "Exception in reading credits file: " + e.toString());
        } finally {
            Utils.closeStream(br);
        }

        creditsText.setText(stringBuilder.toString());
        LinkifyCompat.addLinks(creditsText, Linkify.WEB_URLS);

        return builder.create();
    }
}
