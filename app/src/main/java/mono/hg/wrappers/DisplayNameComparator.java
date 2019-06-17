package mono.hg.wrappers;

import java.text.Collator;
import java.util.Comparator;

import mono.hg.models.App;

public class DisplayNameComparator implements Comparator<App> {
    private final Collator collator = Collator.getInstance();

    @Override public int compare(App a, App b) {
        return collator.compare(a.getAppName(), b.getAppName());
    }
}
