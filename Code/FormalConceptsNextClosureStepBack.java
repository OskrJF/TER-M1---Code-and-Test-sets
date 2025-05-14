import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class NextClosureFCA {

    // === CONFIGURATION: update this to point at your CSV file ===
    private static final String CSV_PATH = "C:\\path\\to\\context.csv";

    public static void main(String[] args) {
        // Read the context
        List<String> attributeNames = new ArrayList<>();
        List<String> objectNames = new ArrayList<>();
        List<boolean[]> data = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(CSV_PATH))) {
            // First line: header row: [ignored],[attr1],[attr2],...
            String headerLine = br.readLine();
            if (headerLine == null) {
                System.err.println("Empty CSV file.");
                return;
            }
            String[] headers = headerLine.split(",");
            for (int i = 1; i < headers.length; i++) {
                attributeNames.add(headers[i].trim());
            }
            // Remaining lines: [objName],[0/1],[0/1],...
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                objectNames.add(parts[0].trim());
                boolean[] row = new boolean[attributeNames.size()];
                for (int i = 1; i < parts.length && i <= attributeNames.size(); i++) {
                    String cell = parts[i].trim();
                    row[i - 1] = cell.equals("1") || cell.equalsIgnoreCase("true") || cell.equalsIgnoreCase("X");
                }
                data.add(row);
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV: " + e.getMessage());
            return;
        }

        int G = data.size();
        int M = attributeNames.size();
        boolean[][] context = new boolean[G][M];
        for (int g = 0; g < G; g++) {
            context[g] = data.get(g);
        }

        // Start enumeration
        BitSet current = closure(new BitSet(M), context);
        int conceptCount = 0;

        System.out.println("Formal Concepts (extent → intent):");
        while (current != null) {
            // Compute extent = current'
            BitSet extent = deriveObjects(current, context);

            // Print extent
            List<String> extentNames = new ArrayList<>();
            for (int g = extent.nextSetBit(0); g >= 0; g = extent.nextSetBit(g + 1)) {
                extentNames.add(objectNames.get(g));
            }

            // Print intent
            List<String> intentNames = new ArrayList<>();
            for (int m = current.nextSetBit(0); m >= 0; m = current.nextSetBit(m + 1)) {
                intentNames.add(attributeNames.get(m));
            }

            System.out.printf("  %s → %s%n", extentNames, intentNames);
            conceptCount++;

            // Get next closed set
            current = nextClosed(current, context);
        }

        System.out.println("Total concepts: " + conceptCount);
    }

    /** Computes the closure A'' of the given attribute set A. */
    private static BitSet closure(BitSet A, boolean[][] context) {
        int G = context.length;
        int M = context[0].length;

        // A' = set of all objects having every attribute in A
        BitSet objects = new BitSet(G);
        objects.set(0, G);               // start with all objects
        for (int m = A.nextSetBit(0); m >= 0; m = A.nextSetBit(m + 1)) {
            BitSet hasM = new BitSet(G);
            for (int g = 0; g < G; g++) {
                if (context[g][m]) {
                    hasM.set(g);
                }
            }
            objects.and(hasM);
        }

        // A'' = set of all attributes common to every object in A'
        BitSet closure = new BitSet(M);
        closure.set(0, M);
        for (int g = objects.nextSetBit(0); g >= 0; g = objects.nextSetBit(g + 1)) {
            BitSet attrs = new BitSet(M);
            for (int m = 0; m < M; m++) {
                if (context[g][m]) {
                    attrs.set(m);
                }
            }
            closure.and(attrs);
        }
        return closure;
    }

    /** Derivation: given an attribute set A, return A' (the extent). */
    private static BitSet deriveObjects(BitSet A, boolean[][] context) {
        int G = context.length;
        BitSet objects = new BitSet(G);
        objects.set(0, G);
        for (int m = A.nextSetBit(0); m >= 0; m = A.nextSetBit(m + 1)) {
            BitSet hasM = new BitSet(G);
            for (int g = 0; g < G; g++) {
                if (context[g][m]) {
                    hasM.set(g);
                }
            }
            objects.and(hasM);
        }
        return objects;
    }

    /**
     * Implements the NextClosure step: finds the next closed set
     * after the given one, or returns null if none.
     */
    private static BitSet nextClosed(BitSet A, boolean[][] context) {
        int M = context[0].length;

        for (int i = M - 1; i >= 0; i--) {
            if (!A.get(i)) {
                // Candidate: add attribute i, then close
                BitSet temp = (BitSet) A.clone();
                temp.set(i);
                BitSet B = closure(temp, context);

                // Canonicity check: for all j < i, B[j] == A[j]
                boolean ok = true;
                for (int j = 0; j < i; j++) {
                    if (B.get(j) != A.get(j)) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    return B;
                }
            }
        }
        return null;
    }
}
