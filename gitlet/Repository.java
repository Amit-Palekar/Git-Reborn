package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.LinkedList;
import java.util.Queue;
import java.util.HashSet;
import java.util.Stack;

/**
 * Represents a repository.
 * @author Amit Palekar
 */
public class Repository {

    /**
     * Constructor.
     */
    public Repository() {
        _branches = new ArrayList<>();
    }

    /**
     * Initializes the git repo.
     */
    public void init() {
        File gitlet = new File(_stagingArea);
        if (gitlet.mkdirs() && gitlet.isDirectory()) {
            File blobs = new File(_blobPath);
            blobs.mkdir();

            File rm = new File("./.gitlet/rm/");
            rm.mkdir();

            File commits = new File("./.gitlet/commit/");
            commits.mkdir();

            _head = new Branch("master");
            _branches.add(_head);

            File branches = new File(_branchPath);
            branches.mkdir();
            Utils.writeObject(new File(_branchPath + "TREE"), _branches);

            File head = new File("./.gitlet/HEAD");
            Utils.writeContents(head, "./refs/heads/" + _head.name());
        } else {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
        }
    }

    /**
     * Loads all data necessary for gitlet to run.
     */
    @SuppressWarnings({"unchecked"})
    public void load() {
        try {
            String path = new Scanner(new File("./.gitlet/HEAD"))
                    .nextLine().trim();
            File head = new File(path);
            String branchName = head.getName()
                    .substring(head.getName().lastIndexOf('/') + 1);
            _branches = Utils.readObject(
                    new File(_branchPath + "TREE"), ArrayList.class);
            for (int i = 0; i < _branches.size(); i++) {
                if (branchName.equals(_branches.get(i).name())) {
                    _head = _branches.get(i);
                }
            }
            _head.getHead().updateTracked();
        } catch (IOException e) {
            Utils.error("could not load head branch");
        }
    }

    /**
     * Checks whether the repo is initialized.
     * @return if the repo is initialized.
     */
    public boolean isInitialized() {
        return new File("./.gitlet").exists();
    }

    /**
     * Adds the current file.
     * @param f The file
     * @return success or failure.
     */
    public boolean add(File f) {
        return _head.add(f);
    }

    /**
     * Adds the current file.
     * @param s the name of the file
     * @return whether the file was added.
     */
    public boolean add(String s) {
        return add(new File("./" + s));
    }

    /**
     * Commits with the given message.
     * @param msg the message.
     */
    public void commit(String msg) {
        _head.commit(msg);
        save();
    }

    /**
     * Shows the log.
     */
    public void showLog() {
        _head.log();
    }

    /**
     * Checks out the file.
     * @param name The name of the file
     */
    public void checkout(String name) {
        checkout(_head.getHead().getHash(), name);
    }

    /**
     * Checks out the file at a particular commit.
     * @param hash the id of the commit
     * @param name the name of the commit
     */
    public void checkout(String hash, String name) {
        _head.checkout(hash, name);
    }

    /**
     * Checks out the current branch.
     * @param b the branch to be checked out
     */
    public void checkout(Branch b) {
        Branch get = getBranch(b.name());
        if (get == null) {
            System.out.println("No such branch exists.");
            return;
        }

        if (b.compareTo(_head) == 0) {
            System.out.println("No need to checkout the current branch.");
            return;
        } else {
            if (_head.resetBranch()) {
                _head = get;
                reset(_head.getHead().getHash());
                save();
            }
        }
    }

    /**
     * Removes the current file.
     * @param file The name of the file.
     */
    public void remove(String file) {
        _head.remove(file);

        File f = new File(_stagingArea + file);
        if (f.exists()) {
            f.delete();
        }
    }

    /**
     * The status of the current branch.
     */
    public void status() {
        System.out.println("=== Branches ===");
        Collections.sort(_branches);

        for (Branch b : _branches) {
            if (b.name().equals(_head.name())) {
                System.out.println("*" + b.name());
            } else {
                System.out.println(b.name());
            }
        }
        System.out.println("\n=== Staged Files ===");

        for (String s : Utils.plainFilenamesIn(_stagingArea)) {
            System.out.println(s);
        }

        System.out.println("\n=== Removed Files ===");
        List<String> removed = Utils.plainFilenamesIn("./.gitlet/rm/");
        if (removed != null) {
            for (String file : removed) {
                System.out.println(file);
            }
        }

        System.out.println("\n=== Modifications Not Staged For Commit ===");
        List<String> current = Utils.plainFilenamesIn("./");

        System.out.println("\n=== Untracked Files ===");

        if (current != null) {
            for (String file : current) {
                if (!_head.getHead().isTracking(file)
                    && !staged(file)) {
                    System.out.println(file);
                }
            }
            System.out.println();
        }

    }

    /**
     * Logs all commits util now.
     */
    public void globalLog() {
        for (Branch b : _branches) {
            b.logAll();
        }
    }

    /**
     * Checks whether the current file is staged.
     * @param f the name of the file.
     * @return whether the file is staged or not.
     */
    public static boolean staged(String f) {
        return new File(_stagingArea + f).exists()
                || new File("./.gitlet/rm/" + f).exists();
    }

    /**
     * Find the commit with the given message.
     * @param msg The commit message.
     */
    public void find(String msg) {
        boolean found = false;
        for (Branch b: _branches) {
            found = found || b.find(msg);
        }

        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    /**
     * Rests to the given hash.
     * @param hash the hash of the corresponding commit.
     */
    public void reset(String hash) {
        _head.reset(hash);
        save();
    }

    /**
     * Adds the branch with that name.
     * @param name The name of the branch
     */
    public void addBranch(String name) {
        Branch b = getBranch(name);
        if (b != null) {
            System.out.println("A branch with that name already exists.");
            return;
        }

        _branches.add(new Branch(name, _head.getHead()));
        save();
    }

    /**
     * Saves the current state of the repo.
     */
    public void save() {
        Utils.writeObject(new File(_branchPath + "TREE"), _branches);
        Utils.writeContents(new File("./.gitlet/HEAD"),
                "./refs/heads/" + _head.name());
    }

    /**
     * Removes the branch with the given name.
     * @param name The name of the branch.
     */
    public void removeBranch(String name) {
        Branch b = getBranch(name);
        if (b != null) {
            if (b.name().equals(_head.name())) {
                System.out.println("Cannot remove the current branch.");
                return;
            }
            _branches.remove(b);
            save();
        } else {
            System.out.println("A branch with that name does not exist.");
        }
    }

    /**
     * Gets the branch with that name.
     * @param name The name of the branch
     * @return The corresponding branch
     */
    private Branch getBranch(String name) {
        Branch get = null;
        for (int i = 0; i < _branches.size(); i++) {
            if (_branches.get(i).name().equals(name)) {
                get = _branches.get(i);
            }
        }
        return get;
    }

    /**
     * Checks that no untracked files exist.
     * @param currentFiles list of files in current directory
     * @return whether this is an untracked file
     */
    public boolean checkUntracked(List<String> currentFiles) {
        for (String s : currentFiles) {
            if (_head.getHead().getBlob(s) == null && !Repository.staged(s)) {
                System.out.println("There is an untracked file in "
                        + "the way; delete it, or add and commit it first.");
                return true;
            }
        }
        return false;
    }

    /**
     * Checks many merge error cases.
     * @param other the name of the given branch.
     * @return whether an error case exists.
     */
    public boolean errorCases(Branch other) {
        if (other == null) {
            System.out.println("A branch with that name does not exist.");
            return true;
        } else if (_head.name().equals(other.name())) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        if (Utils.plainFilenamesIn(_stagingArea).size() > 0
                || Utils.plainFilenamesIn("./.gitlet/rm/").size() > 0) {
            System.out.println("You have uncommitted changes.");
            return true;
        }

        return false;
    }

    /**
     * Merges the given branch with the
     * current branch.
     * @param bName The name of the given branch.
     */
    public void merge(String bName) {
        List<String> currentFiles = Utils.plainFilenamesIn("./");
        if (checkUntracked(currentFiles)) {
            return;
        }
        boolean conflict = false;
        Branch other = getBranch(bName);
        if (errorCases(other)) {
            return;
        }
        Commit split = findSplit(other);
        if (split.getHash().equals(other.getHead().getHash())) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
            return;
        } else if (split.getHash().equals(_head.getHead().getHash())) {
            System.out.println("Current branch fast-forwarded.");
            checkout(other);
            return;
        }
        Set<String> otherFiles = other.getFiles();
        for (String s : currentFiles) {
            File given = null, current = null;
            if (other.getHead().getBlob(s) != null) {
                given = new File("./.gitlet/objects/"
                        + other.getHead().getBlob(s));
            }
            if (_head.getHead().getBlob(s) != null) {
                current = new File("./.gitlet/objects/"
                        + _head.getHead().getBlob(s));
            }
            if (isModified(given, split, s) && !isModified(current, split, s)) {
                if (given != null) {
                    File dest = new File("./" + given.getName());
                    String contents = Utils.readContentsAsString(given);
                    Utils.writeContents(dest, contents);
                    add(dest);
                } else {
                    remove(s);
                }
            } else if (isModified(given, split, s)
                    && isModified(current, split, s)) {
                mergeConflict(given, current, s);
                conflict = true;
            }
        }
        for (String s : otherFiles) {
            if (split.getBlob(s) == null
                    && _head.getHead().getBlob(s) == null) {
                String f = "./.gitlet/objects/" + other.getHead().getBlob(s);
                File read = new File(f);
                String contents = Utils.readContentsAsString(read);
                Utils.writeContents(new File(s), contents);
                checkout(other.getHead().getHash(), s);
                add(new File(s));
            }
        }
        mergeCommit(other, conflict);
    }

    /**
     * Resolves a merge conflict.
     * @param given the given branch file version
     * @param current the current file version
     * @param s the name of the file
     */
    public void mergeConflict(File given, File current, String s) {
        String one = given != null
                ? Utils.readContentsAsString(given) : null;
        String two = current != null
                ? Utils.readContentsAsString(current) : null;
        if (given == null && current != null
                || given != null && given == null
                || !one.equals(two)) {
            String message = "<<<<<<< HEAD" + System.lineSeparator()
                    + (two == null ? "" : two)
                    + "=======" + System.lineSeparator()
                    + (one == null ? "" : one)
                    + ">>>>>>>\n";
            Utils.writeContents(new File(s), message);
            add(new File(s));

        }
    }

    /**
     * Write the merge commit to the branch.
     * @param other the given branch
     * @param conflict whether a conflict exists
     */
    public void mergeCommit(Branch other, boolean conflict) {
        String message = "Merged " + other.name()
                + " into " + _head.name() + ".";
        Commit[] parents = new Commit[]{_head.getHead(), other.getHead()};
        commit(message);
        String original = "./.gitlet/commit/" + _head.getHead().getHash();
        _head.getHead().setParents(parents);
        _head.save();
        save();
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /**
     * Finds the split present in the branch
     * given the corresponding given branch.
     * @param other The given branch.
     * @return The commit corresponding to the split point.
     */
    public Commit findSplit(Branch other) {
        Set<String> seen = new HashSet<>();
        Stack<Commit> stack = new Stack<>();
        stack.add(other.getHead());
        while (!stack.isEmpty()) {
            Commit c = stack.pop();
            if (!seen.contains(c.getHash())) {
                seen.add(c.getHash());
                if (c.getParent(0) != null) {
                    stack.add(c.getParent(0));
                }
                if (c.getParent(1) != null) {
                    stack.add(c.getParent(1));
                }
            }
        }

        Queue<Commit> fringe = new LinkedList<>();
        fringe.add(_head.getHead());
        while (!fringe.isEmpty()) {
            Commit c = fringe.poll();
            if (!seen.contains(c.getHash())) {
                seen.add(c.getHash());
                fringe.add(c.getParent(0));
                if (c.getParent(1) != null) {
                    fringe.add(c.getParent(1));
                }
            } else {
                return c;
            }
        }

        return null;
    }

    /**
     * Checks whether the given file has been modified
     * compared to its version in commit c.
     * @param f The given file.
     * @param c The given commit.
     * @param fName The name of the file.
     * @return Whether the file has been modified.
     */
    private boolean isModified(File f, Commit c, String fName) {
        if (f == null) {
            return c.getBlob(fName) != null;
        }
        return !Utils.sha1(Utils.readContentsAsString(f))
                .equals(c.getBlob(fName));
    }


    /** List of branches. **/
    private ArrayList<Branch> _branches;

    /** Path for file blobs. **/
    private static String _blobPath = "./.gitlet/objects/";

    /** Path for staging. **/
    private static String _stagingArea = "./.gitlet/staging/";

    /** Path for the file that stores branches. **/
    private static String _branchPath = "./.gitlet/branches/";

    /** The active branch. **/
    private Branch _head;
}
