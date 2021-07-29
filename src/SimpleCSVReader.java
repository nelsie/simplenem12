import java.io.*;

/**
 * Reads a CSV file per line and parse each line by splitting the line stored in {@link String} variable using the
 * separator specified in the constructor which is by default ",".
 * <p>
 *     This class has 2 public methods:
 *     <ul>
 *     <li>readNext() method reads the next line and advances the cursor to the next line</li>
 *     <li>peekNext() method reads the content of the next line but the cursor is NOT advanced to the next line</li>
 *     </ul>
 *     Both methods returns an array of {@link String} to store the data split by the input separator.
 * </p>
 *
 */
public class SimpleCSVReader {
    private static final String DEFAULT_SEPARATOR = ",";
    private final BufferedReader br;
    private String[] peekedLine;
    private final String separator;
    private Long linesRead;

    public SimpleCSVReader(File csvFile) throws FileNotFoundException {
        this(csvFile, DEFAULT_SEPARATOR);
    }

    SimpleCSVReader(File csvFile, String separator) throws FileNotFoundException {
        this.br = new BufferedReader(new FileReader(csvFile));
        this.peekedLine = null;
        this.separator = separator;
        this.linesRead = 0L;
    }

    /**
     * Reads the next lne in file then advances the cursor to the next line.
     * @return the data split by the input separator stored in {@link String}[]
     * @throws IOException if a problem has occurred while reading the file.
     */
    public String[] readNext() throws IOException {
        return flexibleRead(true);
    }

    /**
     * Peeks the next line in file without advancing the cursor to the next line.
     * @return the data split by the input separator stored in {@link String}[]
     * @throws IOException if a problem has occurred while reading the file.
     */
    public String[] peekNext() throws IOException {
        return flexibleRead(false);
    }

    /*
   Pass true if the cursor needs to be advanced to the next line, false otherwise.
     */
    private String[] flexibleRead(boolean popLine) throws IOException {
        String nextLine;
        if (this.peekedLine == null) {
            nextLine = getNextLine();
            this.peekedLine = nextLine != null ? nextLine.split(separator) : null;
        }
        String[] result = this.peekedLine;
        if (popLine) {
            ++this.linesRead;
            this.peekedLine = null;
        }
        return result;
    }

    private String getNextLine() throws IOException {
        return this.br.readLine();
    }

    /**
     * Getter method for the number of lines read.
     * @return the number of lines read.
     */
    public Long getLinesRead() {
        return this.linesRead;
    }
}
