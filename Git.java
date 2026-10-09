import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
        fileReader.close();

        FileWriter writeFile = new FileWriter(index, true);

        if (firstLine == null) {
            writeFile.write(fileHash + " " + takenFile);
        } else {
            writeFile.write("\n" + fileHash + " " + takenFile);
        }

        writeFile.close();

    }

}