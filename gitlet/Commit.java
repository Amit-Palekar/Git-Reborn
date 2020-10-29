package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a commit.
 * @author Amit Palekar
 */
public class Commit implements Serializable {

    /**
     * Constructor.
     * @param msg The message
     * @param timestamp The timestamp
     * @param parents The parents of the commit
     */
    public Commit(String msg, Date timestamp, Commit[] parents) {
        _message = msg;
        SimpleDateFormat sdf = new SimpleDateFormat("E LLL F HH:mm:ss yyyy Z");
        _timestamp = sdf.format(timestamp);

        _fileToBlob = new HashMap<>();
        if (parents[0] != null) {
            Map<String, String> firstParent = parents[0]._fileToBlob;
            for (String key: firstParent.keySet()) {
                _fileToBlob.put(key, firstParent.get(key));
            }
        }
        _parents = parents;
        commit();
    }

    /**
     * Constructor to copy a commit.
     * @param c the commit to copy
     */
    public Commit(Commit c) {
        _message = c._message;
        _timestamp = c._timestamp;
        _fileToBlob = c._fileToBlob;
        _parents = c._parents;
    }

    /**
     * Commits the current commit into the working tree.
     */
    public void commit() {
        List<String> stagedFiles = Utils.plainFilenamesIn("./.gitlet/staging");
        List<String> removedFiles = Utils.plainFilenamesIn("./.gitlet/rm");
        if (_parents[0] != null && stagedFiles.size() == 0
                && removedFiles.size() == 0) {
            System.out.println("No changes added to the commit.");
            return;
        }

        if (stagedFiles != null) {
            for (String f : stagedFiles) {
                File file = new File("./.gitlet/staging/" + f);
                String contents = Utils.readContentsAsString(file);
                String hash = Utils.sha1(contents);
                _fileToBlob.put(f, hash);
                Utils.writeContents(new File("./.gitlet/objects/"
                        + hash), contents);
                file.delete();
            }
        }

        if (removedFiles != null) {
            for (String f : removedFiles) {
                new File("./.gitlet/rm/" + f).delete();
                _fileToBlob.remove(f);
            }
        }
    }

    /**
     * Gets the time of the current commit.
     * @return The timestamp of the commit
     */
    public String getTime() {
        return _timestamp;
    }

    @Override
    public String toString() {
        if (_parents[1] == null) {
            return "===\n"
                    + "commit " + getHash() + "\n"
                    + "Date: " + _timestamp + "\n"
                    + _message + "\n";
        }

        return "===\n"
                + "commit " + getHash() + "\n"
                + "Merge: " +  _parents[0].getHash().substring(0, 7)
                + " " + _parents[1].getHash().substring(0, 7) + "\n"
                + "Date: " + _timestamp + "\n"
                + _message + "\n";
    }

    /**
     * Gets the hash of the current commit.
     * @return the hash of the current commit
     */
    public String getHash() {
        return Utils.sha1(Utils.serialize(this));
    }

    /**
     * Gets the blob associated with the given file.
     * @param fileName The file name
     * @return The associated blob name.
     */
    public String getBlob(String fileName) {
        return _fileToBlob.get(fileName);
    }

    /**
     * Gets the given parent of the commit.
     * @param i the parent number (0 or 1)
     * @return the given parent
     */
    public Commit getParent(int i) {
        assert i == 0 || i == 1;
        return _parents[i];
    }

    /**
     * Checks whether the commit is tracking the file.
     * @param file the name of the file
     * @return whether the file is being tracked.
     */
    public boolean isTracking(String file) {
        return _fileToBlob.containsKey(file);
    }

    /**
     * Returns a set of tracked files.
     * @return The set of all tracked files.
     */
    public Set<String> trackedFiles() {
        return _fileToBlob.keySet();
    }

    /**
     * Updates the tracked files in the commit.
     */
    public void updateTracked() {
        for (String file : trackedFiles()) {
            if (!new File(file).exists()) {
                Utils.writeContents(new File("./.gitlet/rm/" + file));
            }
        }
    }

    /**
     * Gets the message of the current commit.
     * @return The commit message.
     */
    public String getMessage() {
        return _message;
    }

    /**
     * Sets the parents of the current commit.
     * @param parents the array of 2 commits
     */
    public void setParents(Commit[] parents) {
        _parents = parents;
    }

    /**
     * Untracks the given file in the commit.
     * @param file The name of the file
     */
    public void untrack(String file) {
        _fileToBlob.remove(file);
    }

    /** The commit message. */
    private String _message;

    /** The timestamp of the commit. */
    private String _timestamp;

    /** A map of file names to blobs. */
    private Map<String, String> _fileToBlob;

    /** The array of the parents of the commit. */
    private Commit[] _parents;
}
