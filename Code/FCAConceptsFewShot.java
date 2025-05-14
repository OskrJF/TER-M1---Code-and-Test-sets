import java.io.*;
import java.util.*;

public class FCAConcepts {

    public static void main(String[] args) {
        // ← EDIT this to point at your CSV file
        String csvFile = "C:/path/to/context.csv";

        // Read header (attributes) and data (objects × attributes)
        List<String> attributes = new ArrayList<>();
        List<String> objects = new ArrayList<>();
        Map<String, Set<String>> objToAttrs = new LinkedHashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line = br.readLine();
            if (line == null) {
                System.err.println("CSV file is empty.");
                return;
            }
            String[] parts = line.split(";");
            // parts[0] is empty (before first semicolon), then attribute names
            for (int i = 1; i < parts.length; i++) {
                attributes.add(parts[i]);
            }
            // each subsequent line: object name;0/1;0/1;...
            while ((line = br.readLine()) != null) {
                parts = line.split(";");
                String obj = parts[0];
                objects.add(obj);
                Set<String> attrSet = new HashSet<>();
                for (int i = 1; i < parts.length; i++) {
                    if ("1".equals(parts[i])) {
                        attrSet.add(attributes.get(i - 1));
                    }
                }
                objToAttrs.put(obj, attrSet);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Generate all subsets of attributes (by bitmask) and test closure condition
        List<Concept> concepts = new ArrayList<>();
        int m = attributes.size();
        for (int mask = 0; mask < (1 << m); mask++) {
            // candidate intent = { a_j | bit j in mask is 1 }
            Set<String> candidateIntent = new HashSet<>();
            for (int j = 0; j < m; j++) {
                if ((mask & (1 << j)) != 0) {
                    candidateIntent.add(attributes.get(j));
                }
            }
            // extent = all objects having *all* attrs in candidateIntent
            Set<String> extent = new HashSet<>();
            for (String obj : objects) {
                if (objToAttrs.get(obj).containsAll(candidateIntent)) {
                    extent.add(obj);
                }
            }
            // closureIntent = common attrs across that extent
            Set<String> closureIntent = new HashSet<>(attributes);
            if (!extent.isEmpty()) {
                for (String obj : extent) {
                    closureIntent.retainAll(objToAttrs.get(obj));
                }
            }
            // empty extent ⇒ closure remains all attrs
            // if closure equals candidate, it’s a closed set ⇒ a formal concept
            if (closureIntent.equals(candidateIntent)) {
                concepts.add(new Concept(extent, closureIntent));
            }
        }

        // Sort by: 1) descending extent‐size, 2) lexicographic order of sorted extent lists
        concepts.sort((c1, c2) -> {
            int d = c2.extent.size() - c1.extent.size();
            if (d != 0) return d;
            List<String> e1 = new ArrayList<>(c1.extent);
            List<String> e2 = new ArrayList<>(c2.extent);
            Collections.sort(e1);
            Collections.sort(e2);
            for (int i = 0; i < e1.size(); i++) {
                int cmp = e1.get(i).compareTo(e2.get(i));
                if (cmp != 0) return cmp;
            }
            return 0;
        });

        // Print in the requested format
        System.out.println("--- Found Formal Concepts (" + concepts.size() + ") ---");
        int idx = 1;
        for (Concept c : concepts) {
            List<String> extList = new ArrayList<>(c.extent);
            List<String> intList = new ArrayList<>(c.intent);
            Collections.sort(extList);
            Collections.sort(intList);
            System.out.printf("%d. (Extent: %s, Intent: %s)%n",
                    idx++, format(extList), format(intList));
        }
    }

    private static String format(List<String> xs) {
        return "{" + String.join(", ", xs) + "}";
    }

    // simple holder for an FCA‐concept
    private static class Concept {
        final Set<String> extent, intent;
        Concept(Set<String> e, Set<String> i) {
            this.extent = new LinkedHashSet<>(e);
            this.intent = new LinkedHashSet<>(i);
        }
    }
}
