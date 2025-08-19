package ChatGPT;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import shared.ConceptComparator;
import shared.FormalConcept;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class FCAConceptsRolePrompt2 { /* Comparator included */

    // TODO: change this to the full path of your context CSV file
    private static final String CSV_FILE_PATH = "";

    public static void main(String[] args) throws IOException {
        // 1) Read context
        BufferedReader br = new BufferedReader(new FileReader(CSV_FILE_PATH));
        String line = br.readLine();
        if (line == null) {
            System.err.println("Empty CSV file.");
            br.close();
            return;
        }

        // First row: attribute names (first cell ignored)
        String[] header = line.split(";"); // <----------------- SPLIT
        int numAttributes = header.length - 1;
        List<String> attributes = new ArrayList<>();
        for (int i = 1; i < header.length; i++) {
            attributes.add(header[i].trim());
        }

        // Remaining rows: object name + 0/1 (or X) entries
        List<String> objects = new ArrayList<>();
        List<boolean[]> context = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(";"); // <----------------- SPLIT
            objects.add(parts[0].trim());
            boolean[] row = new boolean[numAttributes];
            for (int i = 1; i < parts.length; i++) {
                String cell = parts[i].trim();
                row[i - 1] = cell.equals("1") || cell.equalsIgnoreCase("x")
                             || cell.equalsIgnoreCase("true");
            }
            context.add(row);
        }
        br.close();

        // 2) Generate all concepts via NextClosure
        // Start with closure of the empty intent
        boolean[] intent = closure(new boolean[numAttributes], context);

        while (intent != null) {
            // Compute extent of this intent
            Set<Integer> extent = computeExtent(intent, context);

            // Print the concept
            System.out.printf("Concept #%2d  Extent=%s  Intent=%s%n",
                conceptCounter++,
                formatNames(extent, objects),
                formatNames(intent, attributes)
            );

            // Move to next closure
            intent = nextClosure(intent, context);
        }
        
     // 1. Collect all generated concepts
     List<FormalConcept> myConcepts = new ArrayList<>();
     boolean[] currentIntent = closure(new boolean[attributes.size()], context);
     while (currentIntent != null) {
         Set<Integer> extentIndices = computeExtent(currentIntent, context);
         
         // Convert extent indices to names
         Set<String> extentNames = new TreeSet<>();
         for (Integer index : extentIndices) {
             extentNames.add(objects.get(index));
         }
         
         // Convert intent bits to attribute names
         Set<String> intentNames = new TreeSet<>();
         for (int i = 0; i < currentIntent.length; i++) {
             if (currentIntent[i]) {
                 intentNames.add(attributes.get(i));
             }
         }
         
         // Print the concept (optional)
         //System.out.printf("Concept #%2d  Extent=%s  Intent=%s%n",
           //  conceptCounter++,
             //extentNames.toString(),
             //intentNames.toString()
         //);
         
         myConcepts.add(new FormalConcept(extentNames, intentNames));
         currentIntent = nextClosure(currentIntent, context);
     }

     // 2. Compare with FCA4J
     try {
         ConceptComparator.compareConcepts(
             myConcepts,
             objects,         // List of object names
             attributes,      // List of attribute names
             "" // Update path
         );
     } catch (IOException e) {
         System.err.println("Error comparing concepts: " + e.getMessage());
     }
    }

    private static int conceptCounter = 1;

    /** Given an intent (attribute set), compute its closure (intent'') via object derivation. */
    private static boolean[] closure(boolean[] intent, List<boolean[]> ctx) {
        int m = intent.length;
        // 1) Find extent = all objects having every attribute in intent
        List<Integer> extent = new ArrayList<>();
        for (int g = 0; g < ctx.size(); g++) {
            boolean ok = true;
            for (int i = 0; i < m; i++) {
                if (intent[i] && !ctx.get(g)[i]) {
                    ok = false;
                    break;
                }
            }
            if (ok) extent.add(g);
        }
        // 2) Build closure: attributes common to all objects in extent
        boolean[] cls = new boolean[m];
        if (extent.isEmpty()) {
            // By FCA definition, if no object has the given intent, its closure is the full attribute set
            Arrays.fill(cls, true);
        } else {
            for (int i = 0; i < m; i++) {
                boolean common = true;
                for (int g : extent) {
                    if (!ctx.get(g)[i]) {
                        common = false;
                        break;
                    }
                }
                cls[i] = common;
            }
        }
        return cls;
    }

    /** Compute the extent (set of objects) of a given intent. */
    private static Set<Integer> computeExtent(boolean[] intent, List<boolean[]> ctx) {
        int m = intent.length;
        Set<Integer> extent = new LinkedHashSet<>();
        for (int g = 0; g < ctx.size(); g++) {
            boolean ok = true;
            for (int i = 0; i < m; i++) {
                if (intent[i] && !ctx.get(g)[i]) {
                    ok = false;
                    break;
                }
            }
            if (ok) extent.add(g);
        }
        return extent;
    }

    /** Implements the NextClosure step: returns the next intent in lectic order, or null when done. */
    private static boolean[] nextClosure(boolean[] curr, List<boolean[]> ctx) {
        int m = curr.length;
        for (int i = m - 1; i >= 0; i--) {
            if (!curr[i]) {
                // candidate = (curr ∪ {i}) minus all attributes > i
                boolean[] cand = curr.clone();
                cand[i] = true;
                for (int j = i + 1; j < m; j++) {
                    cand[j] = false;
                }
                // compute its closure
                boolean[] cls = closure(cand, ctx);
                // check the lectic condition: cls[j] == curr[j] for all j < i
                boolean ok = true;
                for (int j = 0; j < i; j++) {
                    if (cls[j] != curr[j]) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    return cls;
                }
            }
        }
        return null;
    }

    /** Utility to format a set of indices into their names. */
    private static String formatNames(boolean[] bitset, List<String> names) {
        List<String> picked = new ArrayList<>();
        for (int i = 0; i < bitset.length; i++) {
            if (bitset[i]) picked.add(names.get(i));
        }
        return picked.toString();
    }

    /** Overload for formatting an index‐set of objects. */
    private static String formatNames(Set<Integer> idxs, List<String> names) {
        List<String> picked = new ArrayList<>();
        for (int i : idxs) {
            picked.add(names.get(i));
        }
        return picked.toString();
    }
}

