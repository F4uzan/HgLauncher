package f4.hg;

public class FileFolder {
    private Boolean isFolder;
    private String name;

    public FileFolder(String name, Boolean isFolder) {
        this.isFolder = isFolder;
        this.name = name;
    }

    public Boolean isFolder() {
        return isFolder;
    }

    public String getName() {
        return name;
    }
}
