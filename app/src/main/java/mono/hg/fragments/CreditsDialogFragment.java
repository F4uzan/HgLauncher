package mono.hg.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import androidx.appcompat.app.AlertDialog;
import androidx.core.text.util.LinkifyCompat;
import mono.hg.R;
import mono.hg.utils.Utils;

public class CreditsDialogFragment extends DialogFragment {

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = View.inflate(getActivity(), R.layout.fragment_credits_dialog, null);
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader br = null;

        builder.setTitle(R.string.about_credits_dialog_title);
        builder.setView(view);
        builder.setPositiveButton(R.string.dialog_action_close, null);
        TextView creditsText = view.findViewById(R.id.credits_placeholder);

        try {
            br = new BufferedReader(
                    new InputStreamReader(getActivity().getAssets().open("credits.txt")));
            String line;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            }
        } catch (IOException e) {
            Utils.sendLog(3, e.toString());
        } finally {
            Utils.closeStream(br);
        }

        creditsText.setText(stringBuilder.toString());
        LinkifyCompat.addLinks(creditsText, Linkify.WEB_URLS);

        return builder.create();
    }
}
