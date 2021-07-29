## Coding Skills Challenge - SimpleNem12Parser
### How to run the application
* Requirements: Java 8
* Inside the .../out/production/simplenem12 folder 
  * run java TestHarness <CSV_File_Full_Path>

## Project Scope
As per the developer's understanding of the challenge, this simple application will read the input NEM12 file in CSV 
format and parse the file to interpret the data in the file. The file contains details about the meter 
reading represented by `MeterRead.java` entity, and the meter volumes represented by `MeterVolume.java` entity. The 
lines in the file could be `MeterRead` entity or `MeterVolume` entity, and it is determined by the number in the 
first entry in the line, called record type.
This application is not a complete system, hence it is assumed that if in case a line in a file does not comply with 
the expected format, the system will no longer read the rest of the file, and instead will throw an Exception. The 
different scenarios as to when an exception will be thrown are written in the comments of `SimpleNem12ParserImpl.java`.

### Solution Details
#### <u>SimpleNem12ParserImpl.java</u>
Reading and parsing of the file are implemented in the `parseSimpleNem12()` method of `SimpleNem12ParserImpl.java` 
class. From high level perspective, the system will read each line in the file and will try to determine how to 
process the line based on the record type value - the first number in each line. As written in the specification:
* Record type 100 should be the first line
* Record type 900 should be the last line
* Record type 200 is the start of the meter read block
* Record type 300 is the meter volume

To be able to read the and parse the whole meter read block, when the application read 200 record type and since it is 
expected that next lines after are 1 or more lines having record type 300. Hence, the application will continously 
read the next line until the peeked (see SimpleCSVReader.java for the definition of peek) record type 200 is found. 
Each record type 200 will be 
mapped to `MeterVolume` 
entity and it will be appended to the `volumes` attribute of the `MeterRead` entity.

#### <u>SimpleCSVReader.java</u>
This class implements the reading of each line of the CSV file and storing the details of each line into the memory 
using the String[]. The details in each line is derived by splitting the line by the input 
separator, which is by default ",".

This class has the following methods:
* `readNext()` method, will read the details of the current line, and advance the cursor to the next line.
* `peekNext()` method will read the details of the current line, however the cursor will not advance to the next line. 

#### <u>InvalidNem12FileException</u>
This is the generic RuntimeException class that is thrown if the data in the file is in invalid format.

### Solution Rationale
#### <u>Why read and process the data per line?</u>
Considering the possibility that the file could be large, hence one time storing the whole file into the memory is 
not the preferred solution. This is to avoid the out of memory exception.
Also, so that the application will perform the loop once into the whole file, each line is processed everytime it is 
read.

### Implementation
The application is written in Java 8 as it is the developer's language of preference. No other libraries outside 
Java 8 are used.

