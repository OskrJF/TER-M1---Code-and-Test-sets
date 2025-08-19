package ChatGPT; 

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FormalConceptAnalysis_SelfConsistency {
    public static void main(String[] args) {
        // 1. Hard-coded CSV path
        String csvPath = "";  // ← adjust to your file

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            // 2. Parse header for attribute names
            String headerLine = reader.readLine();
            if (headerLine == null) {
                System.err.println("Context CSV is empty.");
                return;
            }
            String[] headerTokens = headerLine.split(";");
            String[] attributeNames = Arrays.copyOfRange(headerTokens, 1, headerTokens.length);

            // 3. Read object rows into lists
            List<String> objectList = new ArrayList<>();
            List<boolean[]> contextRows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
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

            // 4. Enumerate all closed intents via NextClosure
            List<boolean[]> closedIntents = new ArrayList<>();
            boolean[] intent = new boolean[nAttributes];            // all false
            intent = closure(intent, context, nObjects, nAttributes);
            while (intent != null) {
                closedIntents.add(intent);
                intent = nextClosure(intent, context, nObjects, nAttributes);
            }

            // 5. Print each formal concept
            int conceptId = 1;
            for (boolean[] ci : closedIntents) {
                boolean[] extent = intentPrime(ci, context, nObjects, nAttributes);
                System.out.println("Concept " + (conceptId++) + ":");
                System.out.print("  Intent = ");  printSet(ci, attributeNames);
                System.out.print("  Extent = ");  printSet(extent, objectNames);
                System.out.println();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // intent → extent (objects having all attributes in intent)
    private static boolean[] intentPrime(
        boolean[] intent, boolean[][] context, int nObjects, int nAttributes
    ) {
        boolean[] extent = new boolean[nObjects];
        Arrays.fill(extent, true);
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
        Arrays.fill(intent, true);
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

    // Ganter’s NextClosure: returns next closed intent after A, or null if done
    private static boolean[] nextClosure(
        boolean[] A, boolean[][] context, int nObjects, int nAttributes
    ) {
        for (int i = nAttributes - 1; i >= 0; i--) {
            if (!A[i]) {
                // Build B = A[0..i-1], B[i]=true, B[i+1..]=false
                boolean[] B = new boolean[nAttributes];
                System.arraycopy(A, 0, B, 0, i);
                B[i] = true;
                // Close B
                B = closure(B, context, nObjects, nAttributes);
                // Lectic check: B[j]==A[j] for all j<i
                boolean ok = true;
                for (int j = 0; j < i; j++) {
                    if (B[j] != A[j]) { ok = false; break; }
                }
                if (ok) return B;
            }
        }
        return null;
    }

    // Utility: print set elements for which bitset[i]==true
    private static void printSet(boolean[] bitset, String[] names) {
        System.out.print("{ ");
        boolean first = true;
        for (int i = 0; i < bitset.length; i++) {
            if (bitset[i]) {
                if (!first) System.out.print(", ");
                System.out.print(names[i]);
                first = false;
            }
        }
        System.out.println(" }");
    }
}
