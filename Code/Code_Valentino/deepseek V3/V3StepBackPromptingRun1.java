
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class V3StepBackPromptingRun1 {

    public static class FormalConcept {
        Set<String> extent;
        Set<String> intent;

        public FormalConcept(Set<String> extent, Set<String> intent) {
            this.extent = new HashSet<>(extent);
            this.intent = new HashSet<>(intent);
        }
    }

    private List<String> objects;
    private List<String> attributes;
    private boolean[][] context;

    public V3StepBackPromptingRun1(String csvFilePath) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                rows.add(line.split(";"));
            }
        }

        attributes = Arrays.stream(rows.get(0)).skip(1).map(String::trim).collect(Collectors.toList());
        objects = new ArrayList<>();
        context = new boolean[rows.size() - 1][attributes.size()];
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            objects.add(row[0].trim());
            for (int j = 1; j < row.length; j++) {
                context[i - 1][j - 1] = row[j].trim().equals("1") || row[j].trim().equalsIgnoreCase("x");
            }
        }
    }

    private Set<String> computeIntent(Set<String> extent) {
        if (extent.isEmpty()) return new HashSet<>(attributes);
        Set<String> intent = new HashSet<>(attributes);
        for (String obj : extent) {
            int idx = objects.indexOf(obj);
            for (int j = 0; j < attributes.size(); j++) {
                if (!context[idx][j]) intent.remove(attributes.get(j));
            }
        }
        return intent;
    }

    private Set<String> computeExtent(Set<String> intent) {
        if (intent.isEmpty()) return new HashSet<>(objects);
        Set<String> extent = new HashSet<>();
        for (int i = 0; i < objects.size(); i++) {
            boolean match = true;
            for (String attr : intent) {
                if (!context[i][attributes.indexOf(attr)]) {
                    match = false;
                    break;
                }
            }
            if (match) extent.add(objects.get(i));
        }
        return extent;
    }

    public List<FormalConcept> findAllConcepts() {
        Set<FormalConcept> concepts = new HashSet<>();
        int n = attributes.size();
        for (int mask = 0; mask < (1 << n); mask++) {
            Set<String> intent = new HashSet<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    intent.add(attributes.get(i));
                }
            }
            Set<String> extent = computeExtent(intent);
            Set<String> closedIntent = computeIntent(extent);
            if (intent.equals(closedIntent)) {
                concepts.add(new FormalConcept(extent, intent));
            }
        }
        return new ArrayList<>(concepts);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java V3StepBackPromptingRun1 <input.csv>");
            return;
        }

        try {
            V3StepBackPromptingRun1 analyzer = new V3StepBackPromptingRun1(args[0]);
            List<FormalConcept> concepts = analyzer.findAllConcepts();

            concepts.sort((c1, c2) -> {
                int cmp = Integer.compare(c2.extent.size(), c1.extent.size());
                if (cmp != 0) return cmp;
                return Integer.compare(c2.intent.size(), c1.intent.size());
            });

            String baseName = new File(args[0]).getName().replaceFirst("\\.csv$", "");
            String outputPath = "llm_" + baseName + ".txt";
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
                for (FormalConcept concept : concepts) {
                    List<String> sortedIntent = new ArrayList<>(concept.intent);
                    List<String> sortedExtent = new ArrayList<>(concept.extent);
                    Collections.sort(sortedIntent);
                    Collections.sort(sortedExtent);
                    writer.print("[[" + String.join(",", sortedIntent) + "],[" + String.join(",", sortedExtent) + "]] ");
                }
            }
            System.out.println("Output written to: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}
