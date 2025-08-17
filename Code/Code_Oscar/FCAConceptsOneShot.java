package ChatGPT;

import java.io.*;
import java.util.*;

public class FCAConcepts {
    public static void main(String[] args) throws IOException {
        // ← Modified: read from a hardcoded CSV file instead of stdin
        final String FILE_PATH = "/Users/oscarj/Documents/M1_S2_ICo/ter/examples/synthetic_cgpt/CGPT_FormalConceptsFinder_o3/eg9_9.csv";  // adjust to your actual path
        BufferedReader br = new BufferedReader(new FileReader(FILE_PATH));

        List<String> lines = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
        br.close();

        if (lines.isEmpty()) {
            System.out.println("No input provided.");
            return;
        }

        // Parse header: ;a1;a2;...;a9
        String[] header = lines.get(0).split(";");
        List<String> attributes = new ArrayList<>();
        for (int i = 1; i < header.length; i++) {
            attributes.add(header[i]);
        }
        int m = attributes.size();

        // Parse each object line: o1;0;1;1;...
        List<String> objects = new ArrayList<>();
        boolean[][] context = new boolean[lines.size()-1][m];
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(";");
            objects.add(parts[0]);
            for (int j = 1; j < parts.length; j++) {
                context[i-1][j-1] = parts[j].equals("1");
            }
        }

        // Compute all concepts by checking closure of each attribute‐subset
        List<Concept> concepts = new ArrayList<>();
        int total = 1 << m;
        for (int mask = 0; mask < total; mask++) {
            Set<String> intentA = new LinkedHashSet<>();
            for (int j = 0; j < m; j++) {
                if ((mask & (1 << j)) != 0) {
                    intentA.add(attributes.get(j));
                }
            }
            Set<String> extent = new LinkedHashSet<>();
            for (int i = 0; i < objects.size(); i++) {
                boolean all = true;
                for (int j = 0; j < m; j++) {
                    if ((mask & (1 << j)) != 0 && !context[i][j]) {
                        all = false;
                        break;
                    }
                }
                if (all) extent.add(objects.get(i));
            }
            Set<String> intentClos = new LinkedHashSet<>(attributes);
            for (String obj : extent) {
                int idx = objects.indexOf(obj);
                for (int j = 0; j < m; j++) {
                    if (!context[idx][j]) {
                        intentClos.remove(attributes.get(j));
                    }
                }
            }
            if (intentClos.equals(intentA)) {
                concepts.add(new Concept(extent, intentClos));
            }
        }

        // sort according to: |extent| ↓, then |intent| ↑, then lex extent ↑
        concepts.sort(Comparator
            .comparingInt((Concept c) -> c.extent.size()).reversed()
            .thenComparingInt(c -> c.intent.size())
            .thenComparing(c -> c.extentList(), 
                (l1, l2) -> {
                    int n = Math.min(l1.size(), l2.size());
                    for (int i = 0; i < n; i++) {
                        int cmp = l1.get(i).compareTo(l2.get(i));
                        if (cmp != 0) return cmp;
                    }
                    return Integer.compare(l1.size(), l2.size());
                }
            )
        );

        // print
        System.out.printf("--- Found Formal Concepts (%d) ---%n", concepts.size());
        int count = 1;
        for (Concept c : concepts) {
            System.out.printf("%d. (Extent: %s, Intent: %s)%n",
                count++,
                setToString(c.extent),
                setToString(c.intent)
            );
        }
    }

    private static String setToString(Set<String> s) {
        if (s.isEmpty()) return "{}";
        return "{"
            + String.join(", ", s)
            + "}";
    }

    private static class Concept {
        final Set<String> extent;
        final Set<String> intent;
        Concept(Set<String> ext, Set<String> intt) {
            this.extent  = new TreeSet<>(ext);
            this.intent  = new TreeSet<>(intt);
        }
        List<String> extentList() {
            return new ArrayList<>(extent);
        }
    }
}