import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
//NAME: Henry Dada
//ASSIGNMENT: Program 3 - Sentiment Analysis
//LAB SECTION:
//LECTURE SECTION:
//Utilized Claude Code for debugging.

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        ArrayList<SentList> sentList = new ArrayList<SentList>();
        ArrayList<SentList> posList = new ArrayList<SentList>();
        ArrayList<SentList> negList = new ArrayList<SentList>();

        // load all sentiment words, plus the positive and negative lists
        readSentimentList(sentList, posList, negList);

        PrintWriter outFile = new PrintWriter("reviews.txt");

        // loop over the 8 review files (file names built, not hardcoded)
        for (int i = 1; i <= 8; i++) {
            String inFileName = "review" + i + ".txt";
            if (i==5) { 
                inFileName = "review5a.txt";
            }
            // fresh list for each review so reviews don't mix
            ArrayList<Words> wordList = new ArrayList<Words>();
            if (readReview(sentList, posList, negList, wordList, inFileName)) {
                printReview(wordList, inFileName, outFile);
            } else {
                System.out.println("Error: could not open " + inFileName);
            }
        }
        outFile.close();
    }

    //PRE:  accept the empty ArrayLists created in main
    //POST: the arrays are loaded with the proper words and information
    public static void readSentimentList(ArrayList<SentList> sentList,
                                         ArrayList<SentList> posList,
                                         ArrayList<SentList> negList) {
        String csvFilePath = "sentiment.txt";
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            while ((line = br.readLine()) != null) {
                while ((line = br.readLine()) != null) {
    String[] parts = line.split(",");
    if (parts.length < 2) continue;       // skip bad lines
    String word = parts[0].trim().toLowerCase();

    double value;
    try {
        value = Double.parseDouble(parts[1].trim());
    } catch (NumberFormatException e) {
        System.out.println("Skipping bad line: [" + line + "]");
        continue;
    }

    SentList item = new SentList(word, value);
    sentList.add(item);
    if (value > 1.25) posList.add(item);
    else if (value < -1.25) negList.add(item);
}
            }
        } catch (IOException e) {
            System.out.println("Error reading the file.");
            e.printStackTrace();
        }
    }

    //PRE:  accept the word lists, an empty wordList, and the file name to open
    //POST: wordList holds one Words object per word in the review, with the
    //      original value and a possible positive/negative replacement.
    //      returns true if the file was read, false if it could not be opened
    public static boolean readReview(ArrayList<SentList> sentList,
                                     ArrayList<SentList> posList,
                                     ArrayList<SentList> negList,
                                     ArrayList<Words> wordList,
                                     String fileName) {
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            while ((line = br.readLine()) != null) {
                String[] values = line.split(" ");
                for (String w : values) {
                    if (w.length() == 0) continue;     // skip empty pieces

                    String edit = cleanWord(w);        // lowercase, no punctuation
                    double value = getSentiment(sentList, edit);
                    Words item = new Words(w, edit, value);

                    // negative enough -> swap in a random positive word
                    if (value < -1 && posList.size() > 0) {
                        SentList pick = posList.get((int) (Math.random() * posList.size()));
                        item.posWord = pick.getWord();
                        item.posValue = pick.getValue();
                        item.posReplaced = true;
                    }
                    // positive enough -> swap in a random negative word
                    if (value > 1 && negList.size() > 0) {
                        SentList pick = negList.get((int) (Math.random() * negList.size()));
                        item.negWord = pick.getWord();
                        item.negValue = pick.getValue();
                        item.negReplaced = true;
                    }
                    wordList.add(item);
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    //PRE:  accept the updated wordlist, the input file name, and the output file
    //POST: prints the original, more positive, and more negative reviews
    //      (80 chars per line max) with sentiment totals and the replaced words
    public static void printReview(ArrayList<Words> wordList, String inFile,
                                   PrintWriter outFile) {
        String origText = "", posText = "", negText = "";
        double origTotal = 0, posTotal = 0, negTotal = 0;

        for (Words w : wordList) {
            origText += w.origWord + " ";
            posText += (w.posReplaced ? w.posWord : w.origWord) + " ";
            negText += (w.negReplaced ? w.negWord : w.origWord) + " ";
            origTotal += w.origValue;
            posTotal += (w.posReplaced ? w.posValue : w.origValue);
            negTotal += (w.negReplaced ? w.negValue : w.origValue);
        }

        outFile.println("PROCESSING FILE: " + inFile);
        outFile.println("FORMATTED REVIEW:");
        printWrapped(origText.trim(), outFile);
        outFile.println("ORIGINAL SENTIMENT: " + String.format("%.2f", origTotal));
        outFile.println();

        // ----- more positive -----
        outFile.println("WORDS UPDATED TO BE MORE POSITIVE:");
        boolean any = false;
        for (Words w : wordList) {
            if (w.posReplaced) {
                any = true;
                outFile.println(String.format("%-15s %6.2f   %-15s %6.2f",
                        w.editWord, w.origValue, w.posWord, w.posValue));
            }
        }
        if (!any) outFile.println("No words were replaced.");
        outFile.println("MORE POSITIVE REVIEW:");
        printWrapped(posText.trim(), outFile);
        outFile.println("NEW SENTIMENT: " + String.format("%.2f", posTotal));
        outFile.println();

        // ----- more negative -----
        outFile.println("WORDS UPDATED TO BE MORE NEGATIVE:");
        any = false;
        for (Words w : wordList) {
            if (w.negReplaced) {
                any = true;
                outFile.println(String.format("%-15s %6.2f   %-15s %6.2f",
                        w.editWord, w.origValue, w.negWord, w.negValue));
            }
        }
        if (!any) outFile.println("No words were replaced.");
        outFile.println("MORE NEGATIVE REVIEW:");
        printWrapped(negText.trim(), outFile);
        outFile.println("NEW SENTIMENT: " + String.format("%.2f", negTotal));

        outFile.println();
        outFile.println();
        outFile.println();
    }

    //PRE:  accept text and an output file
    //POST: writes the text with no line longer than 80 characters,
    //      breaking between words only
    static void printWrapped(String text, PrintWriter out) {
        int lineLen = 0;
        for (String w : text.split(" ")) {
            if (lineLen > 0 && lineLen + 1 + w.length() > 80) {
                out.println();
                lineLen = 0;
            }
            if (lineLen > 0) {
                out.print(" ");
                lineLen++;
            }
            out.print(w);
            lineLen += w.length();
        }
        out.println();
    }

    //PRE:  accept a word
    //POST: returns the word in lower case with punctuation removed
    static String cleanWord(String w) {
        String result = "";
        for (int i = 0; i < w.length(); i++) {
            if (!isPunctuation(w.charAt(i))) result += w.charAt(i);
        }
        return result.toLowerCase();
    }

    //PRE:  accept a character
    //POST: return true if this character is punctuation; false otherwise
    static boolean isPunctuation(char ch) {
        if (ch == '!' || ch == '\"' || ch == '#' || ch == '$' || ch == '%' || ch == '&' || ch == '\'' || ch == '(' || ch == ')' || ch == '*' || ch == '+' || ch == ',' || ch == '-' || ch == '.' || ch == '/' || ch == ':' || ch == ';' || ch == '<' || ch == '=' || ch == '>' || ch == '?' || ch == '@' || ch == '[' || ch == '\\' || ch == ']' || ch == '^' || ch == '`' || ch == '{' || ch == '|' || ch == '}')
            return true;
        return false;
    }

    //PRE:  accept the sentiment words list and a word to find
    //POST: return the value of the sentiment if found, 0 otherwise
    static double getSentiment(ArrayList<SentList> sentList, String eWord) {
        for (SentList s : sentList) {
            if (s.getWord().equals(eWord)) return s.getValue();
        }
        return 0.0;
    }
}

// one word from sentiment.txt and its value
class SentList {
    private String word;
    private double value;

    public SentList(String word, double value) {
        this.word = word;
        this.value = value;
    }
    public String getWord() { return word; }
    public double getValue() { return value; }
}

// one word from a review: original, cleaned, its value, and the replacements
class Words {
    String origWord;      // as written in the review (punctuation, capitals)
    String editWord;      // lowercase, no punctuation
    double origValue;
    String posWord;       // replacement for the "more positive" version
    double posValue;
    boolean posReplaced = false;
    String negWord;       // replacement for the "more negative" version
    double negValue;
    boolean negReplaced = false;

    public Words(String origWord, String editWord, double origValue) {
        this.origWord = origWord;
        this.editWord = editWord;
        this.origValue = origValue;
        this.posWord = origWord;
        this.posValue = origValue;
        this.negWord = origWord;
        this.negValue = origValue;
    }
}