import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

public class Git {

    private File git;
    private File objects;
    private File index;
    private File head;

    public static void main(String[] args) {

        Git repo = new Git();

        try {
            System.out.println(hashFile("Hello.txt"));

            System.out.println("-- Blob creation --");
            repo.createBlob("Hello.txt");
            repo.addFileEntry("Hello.txt");
            repo.createBlob("file1.txt");
            repo.addFileEntry("file1.txt");

        } catch (IOException e) {
            System.out.println("SHA-1 is not available " + e);
        }
        String[] workingList = {
                "blob 0a4d55a8d778e5022fab701977c5d840bbc486d0 myProgram/docs/Hello.txt",
                "blob 483b5e082cf5502b303ba3dd4f3469a49fd3421f myProgram/docs/World.txt",
        };

        System.out.println("-- Tree creation --");
        String first = "";
        try {
            first = repo.createTree(workingList, "myProgram/docs");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String second = "";
        try {
            second = repo.createTree(workingList, "myProgram/docs");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println(first);
        System.out.println(first.equals(second));

    }

    public Git() {
        createGit();
    }

    /**
     * @return the objects
     */
    public File getObjects() {
        return objects;
    }

    /**
     * @param objects the objects to set
     */
    public void setObjects(File objects) {
        this.objects = objects;
    }

    /**
     * @return the index
     */
    public File getIndex() {
        return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(File index) {
        this.index = index;
    }

    /**
     * @return the head
     */
    public File getHead() {
        return head;
    }

    /**
     * @param head the head to set
     */
    public void setHead(File head) {
        this.head = head;
    }

    public void createGit() {
        try {

            int counter = 0;
            git = new File("git/");
            if (!git.mkdir()) {
                counter++;
            }

            objects = new File(git, "objects/");
            if (!objects.mkdir()) {
                counter++;
            }

            index = new File(git, "index");
            if (!index.createNewFile()) {
                counter++;
            }

            head = new File(git, "HEAD");
            if (!head.createNewFile()) {
                counter++;
            }

            if (counter >= 4) {
                System.out.println("Git Repository Already Exists");
            } else {
                System.out.println("Git Repository Created");
            }

        } catch (IOException e) {
            System.out.println("File error: " + e.getMessage());

        }

    }

    public static String hashFile(String filePath) throws IOException {
        // TODO (FH-4): read the whole file, digest it, convert the bytes to hex

        Path path = Path.of(filePath);
        if (!Files.isRegularFile(path)) {
            throw new IOException("No such file: " + filePath);
        }

        byte[] fileBytes = Files.readAllBytes(path);

        MessageDigest digest;

        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is not available", e);
        }

        byte[] hash = digest.digest(fileBytes);

        return HexFormat.of().formatHex(hash);

    }

    public void createBlob(String takenFile) throws IOException {

        String fileHash = hashFile(takenFile);

        File newFile = new File(objects, fileHash);
        newFile.createNewFile();

        BufferedReader fileReader = new BufferedReader(new FileReader(takenFile));
        FileWriter writeFile = new FileWriter(newFile.toPath().toString());
        String fileContents = "";
        while ((fileContents = fileReader.readLine()) != null) {
            writeFile.write(fileContents + "\n");
        }
        fileReader.close();
        writeFile.close();

    }

    public void addFileEntry(String takenFile) throws IOException {

        String fileHash = hashFile(takenFile);

        BufferedReader fileReader = new BufferedReader(new FileReader(index));

        String firstLine = fileReader.readLine();
        String line = firstLine;
        while (line != null) {
            if (line.endsWith(takenFile)) {
                fileReader.close();
            }
            line = fileReader.readLine();
        }
        fileReader.close();

        FileWriter writeFile = new FileWriter(index, true);

        if (firstLine == null) {
            writeFile.write(fileHash + " " + takenFile);
        } else {
            writeFile.write("\n" + fileHash + " " + takenFile);
        }

        writeFile.close();

    }

    public String createTree(String[] workingList, String dirPath) throws IOException {
        String start = dirPath + "/";
        String content = "";
        for (int i = 0; i < workingList.length; i++) {
            if (workingList[i] == null) {
                i++;
            }
            String path = workingList[i].substring(46);
            if (path.startsWith(start)) {
                String hash = workingList[i].substring(0, 46);
                String name = path.substring(start.length());
                content += hash + name + "\n";
            }
        }
        File tempFile = new File("cur");
        tempFile.createNewFile();
        FileWriter curFile = new FileWriter(tempFile);
        curFile.write(content);
        curFile.close();
        createBlob(tempFile.getPath());
        String treeHash = hashFile(tempFile.getPath());
        return treeHash;

    }

    // chekc / and then alphebetical
    public static String[] redoList(String[] workingList) {
        for (int i = 0; i < workingList.length; i++) {
            if (!workingList[i].substring(0, 5).equals("blob ") && !workingList[i].substring(0, 5).equals("tree ")) {
                workingList[i] = "blob " + workingList[i];
            }
        }
        for (int i = 0; i < workingList.length; i++) {
            for (int j = i + 1; j < workingList.length; j++) {
                String first = workingList[i].substring(46);
                String second = workingList[j].substring(46);
                if (countSlashes(first) < countSlashes(second)) {
                    String temp = workingList[i];
                    workingList[i] = workingList[j];
                    workingList[j] = temp;
                } else if (countSlashes(first) == countSlashes(second)) {
                    if (first.compareTo(second) > 0) {
                        String temp = workingList[i];
                        workingList[i] = workingList[j];
                        workingList[j] = temp;
                    }
                }
            }
        }
        return workingList;
    }

    public static int countSlashes(String path) {
        int count = 0;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                count++;
            }
        }
        return count;
    }

    public String[] getLeafRoot(String[] workingList) throws IOException {
        workingList = redoList(workingList);
        String path = workingList[0].substring(46);
        int count = countSlashes(path);
        int other = 0;
        String directory = "";
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                other++;
                if (count == other) {
                    directory = path.substring(0, i);
                }
            }

        }
        String treeHash = createTree(workingList, directory);
        return collapseWorkingList(workingList, directory, treeHash);
    }

    public static String[] collapseWorkingList(String[] workingList, String directory, String treeHash) {
        String prefix = directory + '/';
        int alrExist = 0;
        for (int i = 0; i < workingList.length; i++) {
            String path = workingList[i].substring(46);
            if (path.startsWith(prefix)) {
                alrExist++;
            }
        }
        String[] newList = new String[workingList.length - alrExist + 1];
        int cur = 0;
        for (int i = 0; i < workingList.length; i++) {
            String path = workingList[i].substring(46);
            if (!path.startsWith(prefix)) {
                newList[cur] = workingList[i];
                cur++;
            }
        }
        newList[cur] = "tree " + treeHash + " " + directory;
        return redoList(newList);
    }

    public String createFinalTree(String[] workingList) throws IOException {
        workingList = redoList(workingList);
        while (workingList.length > 1) {
            workingList = getLeafRoot(workingList);
        }
        return workingList[0].substring(5, 45);
    }

}