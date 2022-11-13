package gitlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;

public class Staging implements Serializable {

    /**
     * Files to add.
     */
    private TreeMap<String, String> addFiles;
    /**
     * Files to remove.
     */
    private ArrayList<String> removeFiles;

    public Staging() {
        addFiles = new TreeMap<>();
        removeFiles = new ArrayList<>();
    }

    public void clear() {
        addFiles.clear();
        removeFiles.clear();
    }

    public TreeMap<String, String> getAdd() {
        return addFiles;
    }

    public ArrayList<String> getRemove() {
        return removeFiles;
    }

    public void add(String fileName, String fileHash) {
        addFiles.put(fileName, fileHash);
    }

    public void addRemove(String fileName) {
        removeFiles.add(fileName);
    }


}
