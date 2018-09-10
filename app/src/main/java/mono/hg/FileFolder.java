package mono.hg;

public class FileFolder {
    private Boolean isFolder, shouldHighlight;
    private String name;

    public FileFolder(String name, Boolean isFolder, Boolean shouldHighlight) {
        this.isFolder = isFolder;
        this.name = name;
        this.shouldHighlight = shouldHighlight;
    }

    public Boolean shouldHighlight() {
        return shouldHighlight;
    }

    public Boolean isFolder() {
        return isFolder;
    }

    public String getName() {
        return name;
    }
}
