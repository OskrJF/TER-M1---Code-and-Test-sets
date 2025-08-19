package fca4JToArrays;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class FCA4JParserOneShot {
    public static void main(String[] args) {
        final String FILE_PATH = "";  // adjust to your actual path
        BufferedReader reader = null;
        List<Concept> concepts = new ArrayList<>();
        try {
            reader = new BufferedReader(new FileReader(FILE_PATH));
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        } finally {
            concepts = parseFCA4J(reader);
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println("Error closing the file: " + e.getMessage());
                }
            }
        }

        // (Optional) print out the concepts
        System.out.printf("--- Found Formal Concepts (%d) ---%n", concepts.size());
        int count = 1;
        for (Concept c : concepts) {
            System.out.printf(
                "%d. (Extent: %s, Intent: %s)%n",
                count++,
                setToString(c.extent),
                setToString(c.intent)
            );
        }
    }

    public static List<Concept> parseFCA4J(BufferedReader reader) {
        List<Concept> listeConcepts = new ArrayList<>();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String unformatted_intent = "";
                String unformatted_extent = "";
                if (String.valueOf(line.charAt(0)).matches("\\d")) {
                    int startIndex = line.indexOf('|');
                    int endIndex   = line.indexOf('}');
                    String substring = line.substring(startIndex, endIndex);

                    boolean copy = true;
                    for (int i = 1; i < substring.length(); ++i) {
                        if (substring.charAt(i) != '|' && copy) {
                            unformatted_intent += substring.charAt(i);
                        }
                        if (substring.charAt(i) == '|') {
                            copy = false;
                            for (int j = i + 1; j < substring.length(); ++j) {
                                if (substring.charAt(j) != '}') {
                                    unformatted_extent += substring.charAt(j);
                                }
                            }
                        }
                    }

                    // parse Intent
                    int i = 0;
                    String current = "";
                    Set<String> intent = new LinkedHashSet<>();
                    while (i < unformatted_intent.length()) {
                        if (unformatted_intent.charAt(i) == '\\') {
                            i += 2;
                            intent.add(current);
                            current = "";
                            continue;
                        }
                        current += unformatted_intent.charAt(i);
                        i += 1;
                    }

                    // parse Extent
                    i = 0;
                    current = "";
                    Set<String> extent = new LinkedHashSet<>();
                    while (i < unformatted_extent.length()) {
                        if (unformatted_extent.charAt(i) == '\\') {
                            i += 2;
                            extent.add(current);
                            current = "";
                            continue;
                        }
                        current += unformatted_extent.charAt(i);
                        i += 1;
                    }

                    listeConcepts.add(new Concept(extent, intent));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return listeConcepts;
    }

    public static String setToString(Set<String> s) {
        if (s.isEmpty()) return "{}";
        return "{" + String.join(", ", s) + "}";
    }

    public static class Concept {
    	public final Set<String> extent;
    	public final Set<String> intent;

    	public Concept(Set<String> ext, Set<String> intt) {
            this.extent = new TreeSet<>(ext);
            this.intent = new TreeSet<>(intt);
        }

    	public List<String> extentList() {
            return new ArrayList<>(extent);
        }
    }
}
