package ChatGPT;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;

import shared.FormalConcept;
import fca4JToArrays.FCA4JParserSelfConsistency;

public class FormalConceptAnalysis_SelfConsistency2 {
    
    public static Set<FormalConcept> computeConcepts(String csvPath) throws IOException {
        Set<FormalConcept> concepts = new HashSet<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            // 1. Parse header for attribute names
            String headerLine = reader.readLine();
            if (headerLine == null) {
                System.err.println("Context CSV is empty.");
                return concepts;
            }
            String[] headerTokens = headerLine.split(";"); // <----------------------- SPLIT 
            String[] attributeNames = java.util.Arrays.copyOfRange(headerTokens, 1, headerTokens.length);

            // 2. Read object rows into lists
            java.util.List<String> objectList = new java.util.ArrayList<>();
            java.util.List<boolean[]> contextRows = new java.util.ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";"); // <----------------------- SPLIT 
                objectList.add(parts[0].trim());
                boolean[] row = new boolean[attributeNames.length];
                for (int j = 1; j < parts.length; j++) {
                    row[j - 1] = parts[j].trim().equals("1");
                }
                contextRows.add(row);
            }
            String[] objectNames = objectList.toArray(new String[0]);
            boolean[][] context = contextRows.toArray(new boolean[0][0]);

            int nObjects = objectNames.length;
            int nAttributes = attributeNames.length;

            // 3. Enumerate all closed intents via NextClosure
            java.util.List<boolean[]> closedIntents = new java.util.ArrayList<>();
            boolean[] intent = new boolean[nAttributes]; // all false
            intent = closure(intent, context, nObjects, nAttributes);
            while (intent != null) {
                closedIntents.add(intent);
                intent = nextClosure(intent, context, nObjects, nAttributes);
            }

            // 4. Convert to FormalConcept objects
            for (boolean[] ci : closedIntents) {
                boolean[] extent = intentPrime(ci, context, nObjects, nAttributes);
                
                // Convert to sets
                Set<String> intentSet = new TreeSet<>();
                Set<String> extentSet = new TreeSet<>();
                
                for (int i = 0; i < ci.length; i++) {
                    if (ci[i]) intentSet.add(attributeNames[i]);
                }
                
                for (int i = 0; i < extent.length; i++) {
                    if (extent[i]) extentSet.add(objectNames[i]);
                }
                
                concepts.add(new FormalConcept(extentSet, intentSet));
            }
        }
        return concepts;
    }

    // intent → extent (objects having all attributes in intent)
    private static boolean[] intentPrime(
        boolean[] intent, boolean[][] context, int nObjects, int nAttributes
    ) {
        boolean[] extent = new boolean[nObjects];
        java.util.Arrays.fill(extent, true);
        for (int g = 0; g < nObjects; g++) {
            for (int m = 0; m < nAttributes; m++) {
                if (intent[m] && !context[g][m]) {
                    extent[g] = false;
                    break;
                }
            }
        }
        return extent;
    }

    // extent → intent (attributes common to all objects in extent)
    private static boolean[] extentPrime(
        boolean[] extent, boolean[][] context, int nObjects, int nAttributes
    ) {
        boolean[] intent = new boolean[nAttributes];
        java.util.Arrays.fill(intent, true);
        for (int m = 0; m < nAttributes; m++) {
            for (int g = 0; g < nObjects; g++) {
                if (extent[g] && !context[g][m]) {
                    intent[m] = false;
                    break;
                }
            }
        }
        return intent;
    }

    // closure(X) = extentPrime(intentPrime(X))
    private static boolean[] closure(
        boolean[] X, boolean[][] context, int nObjects, int nAttributes
    ) {
        boolean[] ext = intentPrime(X, context, nObjects, nAttributes);
        return extentPrime(ext, context, nObjects, nAttributes);
    }

    // Ganter's NextClosure: returns next closed intent after A, or null if done
    private static boolean[] nextClosure(
        boolean[] A, boolean[][] context, int nObjects, int nAttributes
    ) {
        for (int i = nAttributes - 1; i >= 0; i--) {
            if (!A[i]) {
                // Build B = A[0..i-1], B[i]=true, B[i+1..]=false
                boolean[] B = new boolean[nAttributes];
                System.arraycopy(A, 0, B, 0, i);
                B[i] = true;
                B = closure(B, context, nObjects, nAttributes);
                boolean ok = true;
                for (int j = 0; j < i; j++) {
                    if (B[j] != A[j]) { ok = false; break; }
                }
                if (ok) return B;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        String csvPath = "";
        String dotPath = "";
        try {
            // Compute concepts from CSV
            Set<FormalConcept> computed = computeConcepts(csvPath);

            // Parse concepts from FCA4J
            Set<FormalConcept> parsed = new HashSet<>();
            try (BufferedReader br = new BufferedReader(new FileReader(dotPath))) {
                parsed = FCA4JParserSelfConsistency.parseFCA4J(br);
            }

            // Compare sets
            Set<FormalConcept> onlyInComputed = new HashSet<>(computed);
            onlyInComputed.removeAll(parsed);

            Set<FormalConcept> onlyInParsed = new HashSet<>(parsed);
            onlyInParsed.removeAll(computed);

            Set<FormalConcept> inBoth = new HashSet<>(computed);
            inBoth.retainAll(parsed);

            // Output comparison
            System.out.println("Computed concepts: " + computed.size());
            System.out.println("Parsed concepts:   " + parsed.size());
            System.out.println("In both:           " + inBoth.size());
            System.out.println("Only in computed:  " + onlyInComputed.size());
            for (FormalConcept c : onlyInComputed) System.out.println("  - " + c);
            System.out.println("Only in parsed:    " + onlyInParsed.size());
            for (FormalConcept c : onlyInParsed) System.out.println("  - " + c);

            // 5. Compute precision, recall, and F1 measure
            double tp = inBoth.size();
            double fp = onlyInComputed.size();
            double fn = onlyInParsed.size();
            double precision = tp / (tp + fp);
            double recall = tp / (tp + fn);
            double f1 = (precision + recall > 0) ? 2 * precision * recall / (precision + recall) : 0;

            System.out.printf("Precision: %.4f%n", precision);
            System.out.printf("Recall:    %.4f%n", recall);
            System.out.printf("F1 Measure: %.4f%n", f1);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
