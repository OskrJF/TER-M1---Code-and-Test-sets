package fca4JToArrays;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses an FCA4J full lattice export (.dot or .json) and returns concepts
 * using the same data structures as NextClosureFCAStepBack:
 * each concept is a pair of index-sets for intent and extent.
 */
public class FCA4JParserStepBack {

    /**
     * Holds a single FCA concept as sets of integer indices.
     */
    public static class Concept {
        public final Set<Integer> intent;
        public final Set<Integer> extent;

        public Concept(Set<Integer> intent, Set<Integer> extent) {
            this.intent = intent;
            this.extent = extent;
        }
    }

    /**
     * Parses the given reader (pointing at a .dot or .json lattice file) and
     * maps attribute/object names to their indices as provided in the
     * attributes and objects lists.
     *
     * @param reader     BufferedReader of the lattice export
     * @param attributes List of all attribute names (order defines indices)
     * @param objects    List of all object names (order defines indices)
     * @return List of Concept, each with intent and extent as Set<Integer>
     */
    public static List<Concept> parseFCA4J(BufferedReader reader,
                                           List<String> attributes,
                                           List<String> objects) {
        List<Concept> concepts = new ArrayList<>();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || !Character.isDigit(line.charAt(0)))
                    continue;
                // Extract the label between '|' and '}'
                int start = line.indexOf('|');
                int end = line.indexOf('}');
                if (start < 0 || end < 0 || end <= start)
                    continue;
                String substring = line.substring(start + 1, end);

                // Split into intent and extent parts
                StringBuilder rawIntent = new StringBuilder();
                StringBuilder rawExtent = new StringBuilder();
                boolean readingIntent = true;
                for (char c : substring.toCharArray()) {
                    if (c == '|') {
                        readingIntent = false;
                        continue;
                    }
                    if (readingIntent)
                        rawIntent.append(c);
                    else if (c != '}')
                        rawExtent.append(c);
                }

                // Decode the label lists (split on '\\')
                List<String> intentNames = splitLabels(rawIntent.toString());
                List<String> extentNames = splitLabels(rawExtent.toString());

                // Map names to indices
                Set<Integer> intentIdx = new HashSet<>();
                for (String name : intentNames) {
                    int idx = attributes.indexOf(name);
                    if (idx >= 0) {
                        intentIdx.add(idx);
                    }
                }
                Set<Integer> extentIdx = new HashSet<>();
                for (String name : extentNames) {
                    int idx = objects.indexOf(name);
                    if (idx >= 0) {
                        extentIdx.add(idx);
                    }
                }

                concepts.add(new Concept(intentIdx, extentIdx));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return concepts;
    }

    /**
     * Splits a raw label string on the '\\' delimiter produced in FCA4J exports.
     */
    public static List<String> splitLabels(String raw) {
        List<String> list = new ArrayList<>();
        if (raw.isEmpty())
            return list;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\') {
                // Skip the next character, then commit
                i++;
                list.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0)
            list.add(current.toString());
        return list;
    }
}
