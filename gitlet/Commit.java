package gitlet;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;
import java.io.Serializable;

/**
 * @author Sasha L.
 */
public class Commit implements Serializable {
    /**
     * Parents Hash.
     */
    private String parentHash;
    /**
     * Own Hash.
     */
    private String hash;
    /**
     * Log Message.
     */
    private String logMessage;
    /**
     * Map of blobs.
     */
    private TreeMap<String, String> blobs;
    /**
     * Date of Commit.
     */
    private String date;

    public Commit(String log,
                  TreeMap<String, String> inputBlobs, String parent) {
        logMessage = log;
        parentHash = parent;
        blobs = inputBlobs;
        if (parent == null) {
            date = new SimpleDateFormat(
                    "EEE MMM dd kk:mm:ss yyyy XX").format(new Date(0));
        } else {
            date = new SimpleDateFormat(
                    "EEE MMM dd kk:mm:ss yyyy XX").format(new Date());
        }
        hash = Utils.sha1(Utils.serialize(this));

    }

    public String getParent() {
        return parentHash;
    }

    public String getLog() {
        return logMessage;
    }

    public String getDate() {
        return date;
    }

    public String getHash() {
        return hash;
    }

    public TreeMap<String, String> getBlobs() {
        return blobs;
    }

}
