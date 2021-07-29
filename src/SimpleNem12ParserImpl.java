import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;

public class SimpleNem12ParserImpl implements SimpleNem12Parser {
    private static final int RECORD_TYPE_FIRST_LINE = 100;
    private static final int RECORD_TYPE_LAST_LINE = 900;
    private static final int RECORD_TYPE_METER_READ = 200;
    private static final int RECORD_TYPE_METER_VOLUME = 300;
    private static final int RECORD_TYPE_INVALID = -100;
    private static final String DATE_PATTERN = "yyyyMMdd";

    /**
     * For this implementation of {@link SimpleNem12Parser#parseSimpleNem12(File)}, the system reads the data in the
     * file line by line then map the data to its appropriate object based on the record type. This solution anticipates
     * large amount of data to be processed, hence loading the whole content of the file into the memory is avoided.
     * Instead, each line is processed and perform necessary operations based on the record type value.
     *
     * At any given point, if the current line's does not comply to the expected input format, an
     * {@link InvalidNem12FileException} is thrown. The said exception is thrown in the following scenarios:
     * <ul>
     *     <li>The first line is NOT of type {@link #RECORD_TYPE_FIRST_LINE}.</li>
     *     <li>The last line if NOT of type {@link #RECORD_TYPE_LAST_LINE}</li>
     *     <li>The NMI of the line having {@link #RECORD_TYPE_METER_READ} is not 10 chars long.</li>
     *     <li>There are missing information in each line based on the length of the String array. For
     *     this validation, if the record type is {@link #RECORD_TYPE_METER_READ}, the length of {@link String}[]
     *     should be > 2 (data beyond index 2 will be ignored). On the other hand, if the record type is
     *     {@link #RECORD_TYPE_METER_VOLUME}, the length of
     *     {@link String}[] should be > 3 (data beyond index 3 will be ignored).
     *     </li>
     * </ul>
     * Other {@link RuntimeException} will also be thrown for the following scenarios:
     * <ul>
     *     <li>For lines having {@link #RECORD_TYPE_METER_READ}, the data representing the {@link EnergyUnit} is
     *     NOT "KWH".</li>
     *     <li>For lines having {@link #RECORD_TYPE_METER_VOLUME}, the data representing the date is not in yyyyMMdd
     *     format.</li>
     *     <li>For lines having {@link #RECORD_TYPE_METER_VOLUME}, the data representing the {@link Quality} is NOT
     *     "A" nor "E".</li>
     * </ul>
     *
     * @param simpleNem12File file in Simple NEM12 format
     * @return the Collection of {@link MeterRead} objects
     */
    @Override
    public Collection<MeterRead> parseSimpleNem12(File simpleNem12File) {
        Collection<MeterRead> meterReads = new ArrayList<>();
        try {
            SimpleCSVReader csvReader = new SimpleCSVReader(simpleNem12File);
            Long endOfFileLineNumber = 0L;
            while (true) {
                String[] currentLine = csvReader.readNext();
                Long linesRead = csvReader.getLinesRead();
                if (currentLine == null) {
                    if (!endOfFileLineNumber.equals(linesRead - 1)) {
                        throw new InvalidNem12FileException("Record type of the last line is expected to be " + RECORD_TYPE_LAST_LINE);
                    }
                    break;
                }
                int recordType = parseRecordType(currentLine);
                if (linesRead.equals(1L) && recordType != RECORD_TYPE_FIRST_LINE) {
                    throw new InvalidNem12FileException("Record type of the first line is expected to be " + RECORD_TYPE_FIRST_LINE);
                }
                MeterRead meterRead = null;
                switch (recordType) {
                    case RECORD_TYPE_FIRST_LINE:
                        if (!csvReader.getLinesRead().equals(1L)) {
                            throw new InvalidNem12FileException("Record type " + RECORD_TYPE_FIRST_LINE + " must be the" +
                                    " first line.");
                        }
                        break;
                    case RECORD_TYPE_LAST_LINE:
                        endOfFileLineNumber = linesRead;
                        break;
                    case RECORD_TYPE_METER_READ:
                        meterRead = parseMeterRead(currentLine, linesRead);
                        populateMeterVolumes(csvReader, meterRead);
                        break;
                    case RECORD_TYPE_METER_VOLUME:
                        System.out.println("No parent meter read at line:" + linesRead);
                        break;
                    default:
                        throw new InvalidNem12FileException("Invalid record type (" + recordType + ") at line: " + linesRead);
                }
                if (meterRead != null) {
                    meterReads.add(meterRead);
                }
            }
        } catch (IOException e) {
            throw new InvalidNem12FileException(e);
        }

        return meterReads;
    }

    /*
    Converts the first item in the array to int.  The first item in the array is expected to be the record type.
    If the value of the item is not a number it will throw an InvalidNemFileException because of the
    NumberFormatException.
     */
    private int parseRecordType(String[] currentLine) {
        try {
            return Integer.parseInt(currentLine[0]);
        } catch (NumberFormatException e) {
            throw new InvalidNem12FileException(e);
        }
    }

    /*
    Loop through each line in the file, until the next record type is no longer RECORD_TYPE_METER_VOLUME value (200).
    Creates a MeterVolume object and append the volume in the MeterRead's map Meter Volume attribute.
     */
    private void populateMeterVolumes(SimpleCSVReader csvReader, MeterRead meterRead) throws IOException {
        String[] peekedNext = csvReader.peekNext();
        int peekedRecordType = parseRecordType(peekedNext);
        while (peekedRecordType == RECORD_TYPE_METER_VOLUME) {
            String[] currentLine = csvReader.readNext();
            Pair<LocalDate, MeterVolume> meterVolumePair = parseMeterVolume(currentLine, csvReader.getLinesRead());
            if (meterRead != null) {
                meterRead.appendVolume(meterVolumePair.getKey(), meterVolumePair.getValue());
            }
            peekedNext = csvReader.peekNext();
            peekedRecordType = peekedNext != null ? parseRecordType(peekedNext) : RECORD_TYPE_INVALID;
        }
    }

    /*
    Parse the line with record type = RECORD_TYPE_METER_READ (200) to create a MeterRead object.
    It validates if the length of NMI data = 10. If that is not the case, and InvalidNemFileException is thrown. Also
     the said exception is thrown if the number of items in the current line is < 3. This means that there are
     missing data in the current line.
     */
    private MeterRead parseMeterRead(String[] currentLine, Long linesRead) {
        if (currentLine.length > 2) {
            String nmi = currentLine[1];
            if (nmi.length() != 10) {
                throw new InvalidNem12FileException("Invalid NMI line " + linesRead);
            }
            return new MeterRead(nmi, EnergyUnit.valueOf(currentLine[2]));
        }
        throw new InvalidNem12FileException("Invalid Meter Read at line " + linesRead + ". Missing data.");
    }

    /*
    Parse the line with record type = RECORD_TYPE_METER_VOLUME (300) to create MeterVolume object.
    If in case the number of items in the current line < 4, InvalidNemFileException is thrown as this means that
    there are missing data in the current line.
     */
    private Pair<LocalDate, MeterVolume> parseMeterVolume(String[] currentLine, Long linesRead) {
        if (currentLine.length > 3) {
            LocalDate meterDate = LocalDate.parse(currentLine[1], DateTimeFormatter.ofPattern(DATE_PATTERN));
            BigDecimal volume = new BigDecimal(currentLine[2]);
            volume = volume.setScale(2, RoundingMode.HALF_EVEN);
            return new Pair<>(meterDate, new MeterVolume(volume, Quality.valueOf(currentLine[3])));
        }
        throw new InvalidNem12FileException("Invalid Meter Volume at line " + linesRead + ". Missing data.");
    }
}
