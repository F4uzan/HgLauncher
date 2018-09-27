package mono.hg.wrappers;

import android.app.Fragment;
import android.os.Bundle;

/*
 * A fragment class meant to handle back button press.
 * Taken from http://vinsol.com/blog/2014/10/01/handling-back-button-press-inside-fragments/
 */

public abstract class BackHandledFragment extends Fragment {
    protected BackHandlerInterface backHandlerInterface;

    public abstract boolean onBackPressed();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!(getActivity() instanceof BackHandlerInterface)) {
            throw new ClassCastException("Hosting activity must implement BackHandlerInterface");
        } else {
            backHandlerInterface = (BackHandlerInterface) getActivity();
        }
    }

    @Override public void onStart() {
        super.onStart();

        // Mark this fragment as the selected Fragment.
        backHandlerInterface.setSelectedFragment(this);
    }

    public interface BackHandlerInterface {
        void setSelectedFragment(BackHandledFragment backHandledFragment);
    }
}