package fca4JToArrays;

import ChatGPT.FCAConceptsFewShot.Concept; // reuse the Concept class
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FCA4JParserFewShot {

    public static List<Concept> parseFCA4J(BufferedReader reader) {
        List<Concept> concepts = new ArrayList<>();
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                // Only process lines starting with a digit
                if (!line.isEmpty() && Character.isDigit(line.charAt(0))) {

                    // Extract substring between first '|' and first '}'
                    int startIndex = line.indexOf('|');
                    int endIndex = line.indexOf('}');
                    if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) continue;
                    String substring = line.substring(startIndex, endIndex);

                    String unformattedIntent = "";
                    String unformattedExtent = "";

                    boolean copyIntent = true;
                    for (int i = 1; i < substring.length(); ++i) {
                        char c = substring.charAt(i);
                        if (c == '|') {
                            copyIntent = false;
                            continue;
                        }
                        if (copyIntent) {
                            unformattedIntent += c;
                        } else {
                            unformattedExtent += c;
                        }
                    }

                    // Parse intent
                    List<String> intentList = new ArrayList<>();
                    StringBuilder current = new StringBuilder();
                    for (int i = 0; i < unformattedIntent.length(); i++) {
                        char c = unformattedIntent.charAt(i);
                        if (c == '\\') {
                            i++; // skip next char
                            intentList.add(current.toString());
                            current.setLength(0);
                        } else {
                            current.append(c);
                        }
                    }
                    if (current.length() > 0) intentList.add(current.toString());

                    // Parse extent
                    List<String> extentList = new ArrayList<>();
                    current.setLength(0);
                    for (int i = 0; i < unformattedExtent.length(); i++) {
                        char c = unformattedExtent.charAt(i);
                        if (c == '\\') {
                            i++; // skip next char
                            extentList.add(current.toString());
                            current.setLength(0);
                        } else {
                            current.append(c);
                        }
                    }
                    if (current.length() > 0) extentList.add(current.toString());

                    // Convert to Set and create Concept
                    Set<String> intentSet = new LinkedHashSet<>(intentList);
                    Set<String> extentSet = new LinkedHashSet<>(extentList);
                    concepts.add(new Concept(extentSet, intentSet));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return concepts;
    }
}
