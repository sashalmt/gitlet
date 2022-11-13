package gitlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.io.File;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author Sasha L.
 */
public class Main {
    /**
     * The head string.
     */
    private static String headSt;
    /**
     * Current stage.
     */
    private static Staging stage;
    /**
     * Current working directory.
     */
    private static File cwd;
    /**
     * Commit directory.
     */
    private static String commitDir;
    /**
     * Blob Directory.
     */
    private static String blobDir;
    /**
     * Staging Directory.
     */
    private static String stageDir;
    /**
     * Branch Directory.
     */
    private static String branchDir;

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */
    public static void main(String... args) {
        cwd = new File(System.getProperty("user.dir"));
        commitDir = ".gitlet/commits/";
        blobDir = ".gitlet/commits/blobs/";
        stageDir = ".gitlet/staging/";
        branchDir = ".gitlet/branches/";
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }

        switch (args[0]) {
        case "init":
            init();
            break;
        case "commit":
            commitFiles(args);
            break;
        case "add":
            add(args[1]);
            break;
        case "log":
            log();
            break;
        case "checkout":
            checkout(args);
            break;
        case "rm":
            rm(args[1]);
            break;
        case "find":
            find(args[1]);
            break;
        case "status":
            status();
            break;
        case "global-log":
            globalLog();
            break;
        case "branch":
            branch(args[1]);
            break;
        case "rm-branch":
            rmBranch(args[1]);
            break;
        case "reset":
            reset(args[1]);
            break;
        case "merge":
            merge(args[1]);
            break;
        default:
            System.out.println("No command with that name exists.");

        }
    }

    public static void init() {
        File gitSource = new File(".gitlet");
        if (gitSource.exists()) {
            System.out.println("A Gitlet version-control system already "
                    + "exists in the current directory.");
            return;
        }
        gitSource.mkdir();
        new File(commitDir).mkdir();
        new File(blobDir).mkdir();
        new File(branchDir).mkdir();
        new File(stageDir).mkdir();

        Commit initialCommit =
                new Commit(
                        "initial commit", new TreeMap<String, String>(), null);
        File commitPath = new
                File(commitDir + initialCommit.getHash() + ".txt");
        Utils.writeObject(commitPath, initialCommit);

        File master = new File(branchDir + "master.txt");
        Utils.writeContents(master, initialCommit.getHash());

        File head = new File(branchDir + "head.txt");
        Utils.writeContents(head, "master");

        File staging = new File(stageDir + "stage.txt");
        stage = new Staging();
        Utils.writeObject(staging, stage);
    }

    public static Staging getStage() {
        return Utils.readObject
                (new File(stageDir + "stage.txt"), Staging.class);
    }

    public static void add(String fileName) {
        File curFile = new File(fileName);
        stage = getStage();
        if (curFile.exists()) {
            byte[] curContent = Utils.readContents(curFile);
            String curHash = Utils.sha1(curContent);
            if (getCurCommit().getBlobs().get(fileName) != null
                    && getCurCommit()
                    .getBlobs().get(fileName).equals(curHash)) {
                if (stage.getRemove().contains(fileName)) {
                    stage.getRemove().remove(fileName);
                    Utils.writeObject(new File(stageDir + "stage.txt"), stage);

                }
                return;
            }
            Utils.writeContents(
                    new File(blobDir + curHash + ".txt"), curContent);

            if (stage.getRemove().contains(fileName)) {
                stage.getRemove().remove(fileName);
            } else {
                stage.add(fileName, curHash);
            }

            Utils.writeObject(new File(stageDir + "stage.txt"), stage);
        } else {
            System.out.println("File does not exist.");
        }

    }


    public static Commit getCurCommit() {
        headSt = Utils.readContentsAsString(new File(branchDir + "head.txt"));
        String curHash = Utils.readContentsAsString
                (new File(branchDir + headSt + ".txt"));
        return Utils.readObject
                (new File(commitDir + curHash + ".txt"), Commit.class);
    }

    public static void commitFiles(String[] args) {
        stage = getStage();
        if (stage.getAdd().isEmpty() && stage.getRemove().isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        if (args[1].isEmpty()) {
            System.out.println("Please enter a commit message.");
            return;
        }

        Commit curCommit = getCurCommit();
        TreeMap<String, String> toCommit = curCommit.getBlobs();
        for (Map.Entry<String, String> toAdd : stage.getAdd().entrySet()) {
            toCommit.put(toAdd.getKey(), toAdd.getValue());
        }
        for (String toRemove : stage.getRemove()) {
            toCommit.remove(toRemove);
        }
        stage.getAdd().clear();
        stage.getRemove().clear();
        Utils.writeObject(new File(stageDir + "stage.txt"), stage);


        Commit newCommit = new Commit(args[1], toCommit, curCommit.getHash());
        Utils.writeObject(
                new File(commitDir + newCommit.getHash() + ".txt"), newCommit);
        String curBranch = getHeadBr();
        Utils.writeContents(
                new File(branchDir + curBranch + ".txt"), newCommit.getHash());


    }

    public static void rm(String fileName) {
        stage = getStage();
        Commit curCommit = getCurCommit();
        boolean error = true;
        if (stage.getAdd().containsKey(fileName)) {
            stage.getAdd().remove(fileName);
            error = false;
        }
        if (curCommit.getBlobs().containsKey(fileName)) {
            stage.addRemove(fileName);
            Utils.restrictedDelete(new File(fileName));
            error = false;
        }

        Utils.writeObject(new File(stageDir + "stage.txt"),
                stage);
        if (error) {
            System.out.println("No reason to remove the file.");
        }

    }

    public static void log() {
        Commit checkCommit = getCurCommit();
        while (checkCommit != null) {
            System.out.println("===");
            System.out.println("commit " + checkCommit.getHash());
            System.out.println("Date: " + checkCommit.getDate());
            System.out.println(checkCommit.getLog());
            if (checkCommit.getParent() != null) {
                System.out.println();
                File parentFile =
                        new File(commitDir + checkCommit.getParent()
                                + ".txt");
                checkCommit = Utils.readObject(parentFile, Commit.class);
            } else {
                break;
            }
        }


    }


    public static void checkout(String... args) {
        if (args.length == 3) {
            if (!args[1].equals("--")) {
                System.out.println("Incorrect operands.");
            }
            checkoutFile(args[2]);
        } else if (args.length == 4) {
            if (!args[2].equals("--")) {
                System.out.println("Incorrect operands.");
            }
            checkoutCommit(args[1], args[3]);
        } else if (args.length == 2) {
            checkoutBranch(args[1]);
        }
    }

    public static void checkoutBranch(String branchName) {
        if (!(new File(branchDir + branchName + ".txt").exists())) {
            System.out.println("No such branch exists.");
            return;
        }

        Commit curCommit = getCurCommit();
        String brCommitHash = Utils.readContentsAsString
                (new File(branchDir + branchName + ".txt"));

        Commit brCommit = Utils.readObject
                (new File(commitDir + brCommitHash + ".txt"),
                        Commit.class);

        if (getHeadBr().equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        TreeMap<String, String> brBlobs = brCommit.getBlobs();

        TreeMap<String, String> curBlobs = curCommit.getBlobs();

        List<String> cwdFiles = Utils.plainFilenamesIn(cwd);

        for (int i = 0; i < cwdFiles.size(); i++) {
            if (!curCommit.getBlobs().containsKey
                    (cwdFiles.get(i)) && brCommit.getBlobs().
                    containsKey(cwdFiles.get(i))) {
                System.out.println(
                        "There is an untracked file in the way; "
                                + "delete it, or add and commit it first.");
                return;
            }

        }

        for (Map.Entry<String, String> curBlob : curBlobs.entrySet()) {
            if (!brBlobs.containsKey(curBlob.getKey())) {
                Utils.restrictedDelete(
                        new File(cwd.getPath() + "/" + curBlob.getKey()));
            }
        }

        for (Map.Entry<String, String> brBlob : brBlobs.entrySet()) {
            File filePath = new File(
                    cwd.getPath() + "/"
                            + blobDir + (brBlob.getValue()) + ".txt");
            byte[] fileContent = Utils.readContents(filePath);
            File newFile = new File(cwd.getPath() + "/" + brBlob.getKey());
            Utils.writeContents(newFile, fileContent);
        }


        stage = getStage();
        stage.clear();
        Utils.writeObject(new File(stageDir + "stage.txt"), stage);
        Utils.writeContents(new File(branchDir + "head.txt"), branchName);

    }

    public static void checkoutFile(String filename) {
        Commit curCommit = getCurCommit();
        if (!curCommit.getBlobs().containsKey(filename)) {
            System.out.println("File does not exist in that commit.");
            return;
        }

        if (new File(cwd.getPath() + "/" + filename).exists()) {
            Utils.restrictedDelete(filename);
        }
        File filePath = new File(
                cwd.getPath() + "/" + blobDir
                        + curCommit.getBlobs().get(filename) + ".txt");

        byte[] fileContent = Utils.readContents(filePath);
        File newFile = new File(cwd.getPath(), filename);
        Utils.writeContents(newFile, fileContent);


    }

    public static String getShortID(String shortID) {
        for (String commit : Utils.plainFilenamesIn(commitDir)) {
            if (commit.startsWith(shortID)) {
                return commit;

            }
        }
        return null;
    }

    public static void checkoutCommit(String commitHash, String fileName) {
        if (commitHash.length() < Utils.UID_LENGTH) {
            String id = getShortID(commitHash);
            commitHash = id.substring(0, id.length() - 4);
        }
        File commitPath = new File(commitDir + commitHash + ".txt");
        if (!commitPath.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit curCommit = Utils.readObject(commitPath, Commit.class);

        File filePath = new File(
                cwd.getPath() + "/"
                        + blobDir + curCommit.getBlobs().get(fileName)
                        + ".txt");


        if (!curCommit.getBlobs().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        if (new File(cwd.getPath() + fileName).exists()) {
            Utils.restrictedDelete(fileName);
        }

        byte[] fileContent = Utils.readContents(filePath);

        File newFile = new File(cwd.getPath(), fileName);
        Utils.writeContents(newFile, fileContent);

    }

    public static void globalLog() {
        List<String> allCommits = new ArrayList<String>();
        allCommits = Utils.plainFilenamesIn(commitDir);

        for (int i = 0; i < allCommits.size(); i++) {
            File curCommitFile = new File(commitDir + allCommits.get(i));
            Commit curCommit = Utils.readObject(curCommitFile, Commit.class);
            System.out.println("===");
            System.out.println("commit " + curCommit.getHash());
            System.out.println("Date: " + curCommit.getDate());
            System.out.println(curCommit.getLog());
            System.out.println();
        }


    }

    public static void find(String logMessage) {

        List<String> allCommits = Utils.plainFilenamesIn(commitDir);
        ArrayList<String> logCommits = new ArrayList<String>();

        for (int i = 0; i < allCommits.size(); i++) {
            File fileName = new File(commitDir + allCommits.get(i));
            String commitMessage = Utils.readObject
                    (fileName, Commit.class).getLog();
            if (commitMessage.equals(logMessage)) {
                logCommits.add(fileName.getName());
            }
        }
        if (logCommits.isEmpty()) {
            System.out.println("Found no commit with that message.");
            return;
        }
        for (String commits : logCommits) {
            System.out.println(commits.substring(0, commits.length() - 4));
        }


    }

    public static void status() {
        if (!(new File(".gitlet").exists())) {
            System.out.println(
                    "Not in an initialized Gitlet directory.");
            return;
        }
        System.out.println("=== Branches ===");

        List<String> branches = new ArrayList<String>();
        File[] allFiles = new File(".gitlet/branches").listFiles();
        for (File file : allFiles) {
            branches.add(file.getName().substring
                    (0, file.getName().length() - 4));
        }

        String curHead = Utils.readContentsAsString
                (new File(branchDir + "head.txt"));
        System.out.println("*" + curHead);

        branches.remove("head");
        branches.remove(curHead);

        for (int i = 0; i < branches.size(); i++) {
            System.out.println(branches.get(i));
        }
        System.out.println();
        System.out.println("=== Staged Files ===");

        stage = getStage();
        for (Map.Entry<String, String> toAdd : stage.getAdd().entrySet()) {
            System.out.println(toAdd.getKey());
        }
        System.out.println();
        System.out.println("=== Removed Files ===");

        stage = getStage();
        for (String toRemove : stage.getRemove()) {
            System.out.println(toRemove);
        }

        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");

        checkModCommit();

        System.out.println();
        System.out.println("=== Untracked Files ===");
        stage = getStage();
        Commit curCommit = getCurCommit();
        TreeMap<String, String> stageAdd = stage.getAdd();

        List<String> cwdFiles = Utils.plainFilenamesIn(cwd.getPath());
        for (String monkey : cwdFiles) {
            if (!stageAdd.containsKey(monkey)
                    && !curCommit.getBlobs().containsKey(monkey)) {
                System.out.println(monkey);
            }
        }

    }

    public static void checkModCommit() {
        Commit curCommit = getCurCommit();
        TreeMap<String, String> curBlobs = curCommit.getBlobs();
        stage = getStage();

        for (Map.Entry<String, String> blob : curBlobs.entrySet()) {
            File cwdFile = new File(blob.getKey());
            if (cwdFile.exists()) {
                byte[] curContent = Utils.readContents(cwdFile);
                String curHash = Utils.sha1(curContent);
                if (!blob.getValue().equals(curHash)) {
                    System.out.println(blob.getKey() + " (modified)");
                }
            } else if (!stage.getRemove().contains(blob.getKey())) {
                System.out.println(blob.getKey() + " (deleted)");
            }
        }

        TreeMap<String, String> stageAdd = stage.getAdd();
        for (Map.Entry<String, String> toAdd : stageAdd.entrySet()) {
            File cwdFile = new File(toAdd.getKey());
            byte[] curContent = Utils.readContents(cwdFile);
            String curHash = Utils.sha1(curContent);
            if (cwdFile.exists()) {
                if (!toAdd.getValue().equals(curHash)) {
                    System.out.println(toAdd.getKey() + " (modified)");
                }
            } else if (!stage.getRemove().contains(toAdd.getKey())) {
                System.out.println(toAdd.getKey() + " (deleted)");
            }
        }

    }

    public static String getHeadBr() {
        return Utils.readContentsAsString(new File(branchDir + "head.txt"));

    }

    public static String getHeadCommit() {
        return Utils.readContentsAsString
                (new File(branchDir + getHeadBr() + ".txt"));
    }

    public static void branch(String branchName) {
        File newBranch = new File(branchDir + branchName + ".txt");
        if (newBranch.exists()) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        String curCommit = getHeadCommit();

        Utils.writeContents(newBranch, curCommit);


    }

    public static void rmBranch(String branchName) {
        if (branchName.equals(Utils.readContentsAsString
                (new File(branchDir + "head.txt")))) {
            System.out.print("Cannot remove the current branch.");
            return;
        }
        File branchFile = new File(branchDir + branchName + ".txt");
        if (!branchFile.delete()) {
            System.out.print("A branch with that name does not exist.");
        }
    }


    public static void reset(String commitName) {
        if (commitName.length() < Utils.UID_LENGTH) {
            commitName = getShortID(commitName);
        }
        if (!new File(commitDir + commitName + ".txt").exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }

        Commit toCommit = Utils.readObject
                (new File(commitDir + commitName + ".txt"), Commit.class);
        Commit curCommit = getCurCommit();
        List<String> cwdFiles = Utils.plainFilenamesIn(cwd.getPath());

        for (String file : cwdFiles) {
            if (!curCommit.getBlobs().
                    containsKey(file)
                    && toCommit.getBlobs().containsKey(file)) {
                System.out.println(
                        "There is an untracked file in the way; "
                                + "delete it, or add and commit it first.");
                return;
            }
        }

        for (String file : cwdFiles) {
            if (curCommit.getBlobs().containsKey(file)
                    && !toCommit.getBlobs().containsKey(file)) {
                Utils.restrictedDelete(file);
            }
        }

        for (Map.Entry<String, String> blob : toCommit.getBlobs().entrySet()) {

            File filePath = new File(blobDir + blob.getValue() + ".txt");
            byte[] fileContent = Utils.readContents(filePath);
            File newFile = new File(blob.getKey());
            Utils.writeContents(newFile, fileContent);
        }


        stage = getStage();
        stage.clear();
        Utils.writeObject(new File(stageDir + "stage.txt"), stage);
        Utils.writeContents(
                new File(branchDir + getHeadBr() + ".txt"), commitName);

    }


    public static void merge(String other) {
        if (mergeError(other)) {
            return;
        }


    }

    public static boolean mergeError(String other) {
        stage = getStage();
        if (!stage.getAdd().isEmpty() || !stage.getRemove().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return true;
        } else if (!(new File(branchDir + other + ".txt")).exists()) {
            System.out.println("A branch with that name does not exist.");
            return true;
        } else if (other.equals(getHeadBr())) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        return mergeUntracked(other);
    }

    public static boolean mergeUntracked(String other) {
        Commit curCommit = getCurCommit();
        String otherCommitSt = Utils.readContentsAsString
                (new File(branchDir + other + ".txt"));
        Commit otherCommit = Utils.readObject
                (new File(commitDir + otherCommitSt + ".txt"), Commit.class);

        List<String> cwdFiles = Utils.plainFilenamesIn(cwd);
        for (int i = 0; i < cwdFiles.size(); i++) {
            if (!curCommit.getBlobs().containsKey
                    (cwdFiles.get(i)) && otherCommit.getBlobs().
                    containsKey(cwdFiles.get(i))) {
                System.out.println(
                        "There is an untracked file in the way; delete it,"
                                 + " or add and commit it first.");
                return true;
            }
        }
        return false;
    }

    public static void mergeFiles(Commit split, Commit curr, Commit other) {
        ArrayList<String> allFile = findAllFiles(split, curr, other);
        ArrayList<String> toCommit = new ArrayList<>();
        for (String file : allFile) {
            Commit modified = isModified(file, split, curr, other);
            if (modified == null) {
                toCommit.add(file);
            }
            toCommit.add(modified.getBlobs().get(file));

        }

    }

    public static Commit isModified(
            String file, Commit split, Commit curr, Commit other) {
        String splitHash = split.getBlobs().get(file);
        String otherHash = other.getBlobs().get(file);
        String currHash = curr.getBlobs().get(file);

        if (splitHash.equals(otherHash) && splitHash.equals(currHash)) {
            return null;
        }
        if (!splitHash.equals(otherHash) && !splitHash.equals(currHash)) {
            return split;
        }
        if (!splitHash.equals(otherHash) && splitHash.equals(currHash)) {
            return other;
        }
        if (splitHash.equals(otherHash) && !splitHash.equals(currHash)) {
            return curr;
        }
        return null;
    }

    public static ArrayList<String> findAllFiles(
            Commit split, Commit curr, Commit other) {
        ArrayList<String> allFiles = new ArrayList<String>();
        for (Map.Entry<String, String> blob : split.getBlobs().entrySet()) {
            allFiles.add(blob.getKey());
        }

        for (Map.Entry<String, String> blob : curr.getBlobs().entrySet()) {
            if (!allFiles.contains(blob)) {
                allFiles.add(blob.getKey());
            }
        }

        for (Map.Entry<String, String> blob : other.getBlobs().entrySet()) {
            if (!allFiles.contains(blob)) {
                allFiles.add(blob.getKey());
            }

        }

        return allFiles;
    }
}
