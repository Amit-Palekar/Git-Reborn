package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Scanner;
import java.util.List;
import java.util.Set;

/**
 * Represents a Branch.
 * @author Amit Palekar
 */
public class Branch implements Comparable<Branch>, Serializable {
    /**
     * Constructor with branch name.
     * @param name the name of the branch
     */
    public Branch(String name) {
        this(name, new Commit("initial commit", new Date(0), new Commit[2]));
    }

    /**
     * Constructor with branch name and starting commit.
     * @param name the name of the branch
     * @param start the starting commit
     */
    public Branch(String name, Commit start) {
        _name = name;
        _branchPath = "./.gitlet/" + name + "/";
        _commitPath = "./.gitlet/commit/";
        File branch = new File(_branchPath);

        if (!branch.exists()) {
            branch.mkdir();
            _head = start;
            save();
        } else {
            File head = new File(_branchPath + "HEAD");
            try {
                String hash = new Scanner(head).next();
                _head = Utils.readObject(
                        new File(_commitPath + hash), Commit.class);
            } catch (IOException e) {
                Utils.error("cannot find head commit");
            }
        }
    }

    /**
     * Saves the branch.
     */
    public void save() {
        String name = _head.getHash();
        Utils.writeObject(new File(_commitPath + name), _head);
        Utils.writeContents(new File(_branchPath + "HEAD"), name);
    }

    /**
     * Adds the file to the branch.
     * @param f the name of the file
     * @return whether the file was added or not.
     */
    public boolean add(File f) {
        if (f.exists()) {
            File removed = new File("./.gitlet/rm/" + f);
            if (removed.exists()) {
                removed.delete();
            }

            String content = Utils.readContentsAsString(f);
            if (!Utils.sha1(content).equals(_head.getBlob(f.getName()))) {
                Utils.writeContents(
                        new File("./.gitlet/staging/" + f), content);
                return true;
            }
            return false;
        } else {
            System.out.println("File does not exist.");
        }
        return false;
    }

    /**
     * Commits with the given message on the current branch.
     * @param msg The name of the message
     */
    public void commit(String msg) {
        _head = new Commit(msg, new Date(), new Commit[]{_head, null});
        save();
    }

    /**
     * Returns the name of the current branch.
     * @return the name of the branch.
     */
    public String name() {
        return _name;
    }

    /**
     * Logs the list of current commits.
     */
    public void log() {
        Commit curr = new Commit(_head);
        while (curr != null) {
            System.out.println(curr);
            curr = curr.getParent(0);
        }
    }

    /**
     * Logs all possible commits.
     */
    public void logAll() {
        List<String> files = Utils.plainFilenamesIn(_commitPath);
        for (String file : files) {
            Commit c = Utils.readObject(
                    new File(_commitPath + file), Commit.class);
            System.out.println(c);
        }
    }

    /**
     * Checkout the commit on the current branch.
     * @param id The name of the branch.
     * @param name The name of the branch
     */
    public void checkout(String id, String name) {
        boolean found = false;
        List<String> commits = Utils.plainFilenamesIn(_commitPath);
        for (String s : commits) {
            if (s.contains(id)) {
                id = s;
                found = true;
                break;
            }
        }

        if (!found) {
            System.out.println("No commit with that id exists.");
            return;
        }

        Commit check = Utils.readObject(
                new File(_commitPath + id), Commit.class);
        if (check.getBlob(name) == null) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        File file = new File("./.gitlet/objects/" + check.getBlob(name));
        String real = Utils.readContentsAsString(file);
        Utils.writeContents(new File("./" + name), real);
    }

    /**
     * Get the head of the current branch.
     * @return The head commit.
     */
    public Commit getHead() {
        return _head;
    }

    /**
     * Set the head of the current branch.
     * @param head the name of the branch.
     */
    public void setHead(Commit head) {
        _head = head;
        save();
    }

    /**
     * Removes the current file from the branch.
     * @param file The name of the file.
     */
    public void remove(String file) {
        if (_head.isTracking(file)) {
            new File(file).renameTo(new File("./.gitlet/rm/" + file));
        } else {
            if (!new File("./.gitlet/staging/" + file).exists()
                    && !new File("./.gitlet/rm/" + file).exists()) {
                System.out.println("No reason to remove the file.");
            }
        }
    }

    /**
     * Finds the commit with the given message.
     * @param msg The message
     * @return whether the message was found.
     */
    public boolean find(String msg) {
        boolean found = false;
        List<String> commits = Utils.plainFilenamesIn(_commitPath);
        for (String file : commits) {
            Commit c = Utils.readObject(
                    new File(_commitPath + file), Commit.class);
            if (c.getMessage().equals(msg)) {
                System.out.println(file);
                found = true;
            }
        }

        return found;
    }

    /**
     * Resets all files to the given commit.
     * @param hash the id of the given commit.
     */
    public void reset(String hash) {
        List<String> current = Utils.plainFilenamesIn("./");
        for (String file : current) {
            if (!_head.isTracking(file)
                    && !Repository.staged(file)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            } else {
                Utils.restrictedDelete(file);
            }
        }

        List<String> staged = Utils.plainFilenamesIn("./.gitlet/staging/");
        List<String> removed = Utils.plainFilenamesIn("./.gitlet/rm/");
        for (String file : staged) {
            new File("./.gitlet/staging/" + file).delete();
        }

        for (String file : removed) {
            new File("./.gitlet/rm/" + file).delete();
        }

        File f = new File(_commitPath + hash);
        if (f.exists()) {
            Commit c = Utils.readObject(f, Commit.class);
            for (String s : c.trackedFiles()) {
                checkout(hash, s);
            }
            _head = c;
        } else {
            System.out.println("No commit with that id exists.");
        }
    }

    @Override
    public int compareTo(Branch o) {
        return this.name().compareTo(o.name());
    }

    /**
     * Resets the branch to only include tracked files.
     * @return if the current state is the most updated.
     */
    public boolean resetBranch() {
        List<String> curr = Utils.plainFilenamesIn("./");
        for (String s :  curr) {
            if (!_head.isTracking(s)
                    && !Repository.staged(s)) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
                return false;
            }
            Utils.restrictedDelete(s);
        }
        return true;
    }

    /**
     * Gets a list of all tracked files.
     * @return The set of tracked files.
     */
    public Set<String> getFiles() {
        return _head.trackedFiles();
    }

    /** The name of the branch. */
    private String _name;

    /** The head commit. */
    private Commit _head;

    /** The path to the current branch. */
    private String _branchPath;

    /** The path to the commits. */
    private String _commitPath;
}
