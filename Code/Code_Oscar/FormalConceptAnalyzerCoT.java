package ChatGPT; // o4-mini-high

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FormalConceptAnalyzer_CoT {
    
    public static void main(String[] args) {
        // 1. Read CSV
        String csvFile = "/Users/oscarj/Documents/M1_S2_ICo/ter/prompts/prompt_engineering/chain-of-thought/testSetSynths/eg20_20.csv";  // <<– adjust path here
        List<String> objects = new ArrayList<>();
        List<String> attributes = new ArrayList<>();
        boolean[][] context = null;
        
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line = br.readLine();
            if (line == null) {
                System.err.println("Empty file!");
                return;
            }
            // Parse header line for attribute names
            String[] header = line.split(";");
            // header[0] is object‐column name, skip it
            for (int j = 1; j < header.length; j++) {
                attributes.add(header[j].trim());
            }
            
            // Read the rest of the lines to build objects + raw context rows
            List<boolean[]> raw = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                objects.add(parts[0].trim());
                boolean[] row = new boolean[attributes.size()];
                for (int j = 1; j < parts.length && j <= attributes.size(); j++) {
                    String cell = parts[j].trim();
                    // treat "1" or "X" as true
                    row[j-1] = cell.equals("1") || cell.equalsIgnoreCase("X");
                }
                raw.add(row);
            }
            
            // Convert raw list to array
            int n = raw.size();
            int m = attributes.size();
            context = new boolean[n][m];
            for (int i = 0; i < n; i++) {
                context[i] = raw.get(i);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
        // 2. Compute all formal concepts via Next‐Closure
        List<Concept> concepts = computeFormalConcepts(context);
        
        // 3. Print them
        int count = 1;
        for (Concept c : concepts) {
            System.out.println("Concept #" + (count++));
            System.out.println("  Extent: " + subsetToNames(c.extent, objects));
            System.out.println("  Intent: " + subsetToNames(c.intent, attributes));
            System.out.println();
        }
    }
    
    /** Holds the extent and intent as boolean masks */
    static class Concept {
        boolean[] extent;
        boolean[] intent;
        Concept(boolean[] extent, boolean[] intent) {
            this.extent = extent;
            this.intent = intent;
        }
    }
    
    /** Compute all concepts with Ganter’s Next‐Closure */
    private static List<Concept> computeFormalConcepts(boolean[][] ctx) {
        int n = ctx.length;          // number of objects
        int m = (n > 0 ? ctx[0].length : 0);  // number of attributes
        List<Concept> result = new ArrayList<>();
        
        // Start with the empty intent
        boolean[] A = new boolean[m];
        
        while (A != null) {
            // 1) Close A to get the proper intent B and its extent
            Closure cl = close(ctx, A);
            result.add(new Concept(cl.extent, cl.intent));
            
            // 2) Compute next intent after B
            A = nextIntent(ctx, cl.intent);
        }
        return result;
    }
    
    /** Represents the closure of an intent: its extent and the closed intent */
    private static class Closure {
        boolean[] extent;
        boolean[] intent;
        Closure(boolean[] extent, boolean[] intent) {
            this.extent = extent;
            this.intent = intent;
        }
    }
    
    /** Given an intent, compute (extent, intent'') */
    private static Closure close(boolean[][] ctx, boolean[] intent) {
        int n = ctx.length, m = intent.length;
        // 1) extent: all objects having all attrs in intent
        boolean[] extent = new boolean[n];
        for (int i = 0; i < n; i++) {
            boolean ok = true;
            for (int j = 0; j < m; j++) {
                if (intent[j] && !ctx[i][j]) {
                    ok = false;
                    break;
                }
            }
            extent[i] = ok;
        }
        // 2) newIntent: all attributes common to those objects
        boolean[] newIntent = new boolean[m];
        for (int j = 0; j < m; j++) {
            boolean common = true;
            for (int i = 0; i < n; i++) {
                if (extent[i] && !ctx[i][j]) {
                    common = false;
                    break;
                }
            }
            newIntent[j] = common;
        }
        return new Closure(extent, newIntent);
    }
    
    /** Given the current (closed) intent B, find the next intent in lex order, or null if done */
    private static boolean[] nextIntent(boolean[][] ctx, boolean[] B) {
        int m = B.length;
        // For i = m-1 down to 0
        for (int i = m - 1; i >= 0; i--) {
            if (!B[i]) {
                // Try adding attribute i
                boolean[] Aplus = B.clone();
                Aplus[i] = true;
                Closure cl = close(ctx, Aplus);
                // Check lex condition: for all j < i, cl.intent[j] == B[j]
                boolean ok = true;
                for (int j = 0; j < i; j++) {
                    if (cl.intent[j] != B[j]) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    return cl.intent;
                }
            }
        }
        return null;  // no next intent
    }
    
    /** Utility: convert a boolean mask to a list of names */
    private static String subsetToNames(boolean[] mask, List<String> names) {
        List<String> sel = new ArrayList<>();
        for (int i = 0; i < mask.length; i++) {
            if (mask[i]) sel.add(names.get(i));
        }
        return sel.toString();
    }
}

