package mono.hg.models;

import androidx.annotation.Nullable;

public class WebSearchProvider {
    private String url;
    private String name;
    private String id;

    public WebSearchProvider(String name, String url) {
        this.url = url;
        this.name = name;
        this.id = name;
    }

    public WebSearchProvider(String name, String url, String id) {
        this.url = url;
        this.name = name;
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    @Override public boolean equals(@Nullable Object obj) {
        WebSearchProvider object = (WebSearchProvider) obj;

        // URL can be shared, but names should stay unique.
        if (object != null) {
            return this.getName().equals(object.getName()) || this.getClass() == obj.getClass();
        } else {
            return false;
        }
    }
}
