package gitlet;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Amit Palekar
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        Main m = new Main();
        m.run(args);
    }

    /**
     * Runs the Gitlet program.
     * @param commands the command to be entered.
     */
    public void run(String[] commands) {
        r = new Repository();
        if (commands.length == 0) {
            System.out.println("Please enter a command.");
        } else if (commands[0].equals("init")) {
            r.init();
        } else if (!r.isInitialized()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else {
            r.load();
            if (commands[0].equals("add")) {
                r.add(commands[1]);
            } else if (commands[0].equals("commit")) {
                if (commands[1].equals("")) {
                    System.out.println("Please enter a commit message.");
                }
                r.commit(commands[1]);
            } else if (commands[0].equals("rm")) {
                r.remove(commands[1]);
            } else if (commands[0].equals("checkout")) {
                if (commands.length == 4) {
                    if (!commands[2].equals("--")) {
                        System.out.println("Incorrect operands.");
                    }
                    r.checkout(commands[1], commands[3]);
                } else if (commands.length == 3) {
                    r.checkout(commands[2]);
                } else if (commands.length == 2) {
                    r.checkout(new Branch(commands[1]));
                }
            } else if (commands[0].equals("log")) {
                r.showLog();
            } else if (commands[0].trim().equals("status")) {
                r.status();
            } else if (commands[0].equals("global-log")) {
                r.globalLog();
            } else if (commands[0].equals("find")) {
                r.find(commands[1]);
            } else if (commands[0].equals("rm")) {
                r.remove(commands[1]);
            } else if (commands[0].equals("reset")) {
                r.reset(commands[1]);
            } else if (commands[0].equals("branch")) {
                r.addBranch(commands[1]);
            } else if (commands[0].equals("rm-branch")) {
                r.removeBranch(commands[1]);
            } else if (commands[0].equals("merge")) {
                r.merge(commands[1]);
            } else {
                System.out.println("No command with that name exists.");
            }
        }
        System.exit(0);
    }

    /** The repository in context. */
    private Repository r;
}
