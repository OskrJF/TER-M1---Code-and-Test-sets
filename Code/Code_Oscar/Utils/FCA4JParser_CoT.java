package fca4JToArrays;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FCA4JParser_CoT {
    public static void main(String[] args) {
        String filePath = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            List<Concept> concepts = parseFCA4J(reader);
            // Use or print concepts as needed
            for (Concept c : concepts) {
                System.out.println(c);
            }
        } catch (IOException e) {
            System.err.println("Error handling the file: " + e.getMessage());
        }
    }

    /**
     * Parses a DOT/JSON FCA4J output and returns a list of Concept objects
     * matching the structure used in FormalConceptAnalyzer_CoT.
     */
    public static List<Concept> parseFCA4J(BufferedReader reader) throws IOException {
        String line;
        List<Concept> concepts = new ArrayList<>();
        int numberOfConcepts = 0;

        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) continue;
            if (Character.isDigit(line.charAt(0))) {
                String unformatted_intent = "";
                String unformatted_extent = "";

                int startIndex = line.indexOf('|');
                int endIndex   = line.indexOf('}');
                if (startIndex < 0 || endIndex < 0 || endIndex <= startIndex) continue;
                String substring = line.substring(startIndex, endIndex);

                boolean copyIntent = true;
                for (int i = 1; i < substring.length(); i++) {
                    char c = substring.charAt(i);
                    if (c == '|') {
                        copyIntent = false;
                        continue;
                    }
                    if (copyIntent) unformatted_intent += c;
                    else if (c != '}') unformatted_extent += c;
                }

                // Split on escaped separators (\\n or \\t or similar)
                List<String> intent = splitEscaped(unformatted_intent);
                List<String> extent = splitEscaped(unformatted_extent);

                Concept concept = new Concept(extent, intent);
                concepts.add(concept);
                numberOfConcepts++;
            }
        }
        System.out.println("Parsed " + numberOfConcepts + " concepts.");
        return concepts;
    }

    private static List<String> splitEscaped(String input) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '\\' && i + 1 < input.length()) {
                // End of one item
                parts.add(current.toString());
                current.setLength(0);
                i++; // skip escaped char
            } else {
                current.append(input.charAt(i));
            }
        }
        if (current.length() > 0) parts.add(current.toString());
        return parts;
    }

    public static class Concept {
        private final List<String> extent;
        private final List<String> intent;

        public Concept(List<String> extent, List<String> intent) {
            this.extent = new ArrayList<>(extent);
            this.intent = new ArrayList<>(intent);
        }

        public List<String> getExtent() {
            return new ArrayList<>(extent);
        }

        public List<String> getIntent() {
            return new ArrayList<>(intent);
        }

        @Override
        public String toString() {
            return String.format("Concept %d : extent=%s, intent=%s",
                hashCode(), extent, intent);
        }
    }
}
