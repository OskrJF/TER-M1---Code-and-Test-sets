import java.io.*;
import java.util.*;

public class V3ChainOfThoughtRun2 {

    static class FormalConcept {
        private final Set<String> extent;
        private final Set<String> intent;

        public FormalConcept(Set<String> extent, Set<String> intent) {
            this.extent = new HashSet<>(extent);
            this.intent = new HashSet<>(intent);
        }

        public Set<String> getExtent() { return extent; }
        public Set<String> getIntent() { return intent; }

        @Override
        public String toString() {
            return "[[" + String.join(",", intent) + "],[" + String.join(",", extent) + "]]";
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FormalConcept)) return false;
            FormalConcept other = (FormalConcept) obj;
            return this.extent.equals(other.extent) && this.intent.equals(other.intent);
        }

        @Override
        public int hashCode() {
            return extent.hashCode() + intent.hashCode();
        }
    }

    static class FormalContext {
        private final List<String> objects;
        private final List<String> attributes;
        private final Map<String, Set<String>> incidence;

        public FormalContext(List<String> objects, List<String> attributes, Map<String, Set<String>> incidence) {
            this.objects = new ArrayList<>(objects);
            this.attributes = new ArrayList<>(attributes);
            this.incidence = new HashMap<>(incidence);
        }

        public Set<String> deriveIntent(Set<String> extent) {
            if (extent.isEmpty()) return new HashSet<>(attributes);
            Set<String> intent = new HashSet<>(incidence.get(extent.iterator().next()));
            for (String obj : extent) {
                intent.retainAll(incidence.get(obj));
            }
            return intent;
        }

        public Set<String> deriveExtent(Set<String> intent) {
            if (intent.isEmpty()) return new HashSet<>(objects);
            Set<String> extent = new HashSet<>();
            for (String obj : objects) {
                if (incidence.get(obj).containsAll(intent)) {
                    extent.add(obj);
                }
            }
            return extent;
        }

        public Set<String> closure(Set<String> intent) {
            return deriveIntent(deriveExtent(intent));
        }

        public List<String> getObjects() { return objects; }
        public List<String> getAttributes() { return attributes; }
    }

    public static FormalContext parseCSV(String filePath) throws IOException {
        List<String> objects = new ArrayList<>();
        List<String> attributes = new ArrayList<>();
        Map<String, Set<String>> incidence = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String headerLine = br.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new IOException("CSV file is empty or missing header: " + filePath);
            }

            String delimiter = headerLine.contains(";") ? ";" : ",";
            String[] headers = headerLine.split(delimiter);
            if (headers.length < 2) {
                throw new IOException("CSV header is malformed in file: " + filePath);
            }

            attributes = Arrays.asList(Arrays.copyOfRange(headers, 1, headers.length));

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(delimiter);
                if (parts.length < 2) continue;  // skip malformed lines
                String obj = parts[0].trim();
                objects.add(obj);
                Set<String> objAttrs = new HashSet<>();
                for (int i = 1; i < parts.length; i++) {
                    String val = parts[i].trim();
                    if (val.equals("1") || val.equalsIgnoreCase("X")) {
                        objAttrs.add(attributes.get(i - 1));
                    }
                }
                incidence.put(obj, objAttrs);
            }
        }

        return new FormalContext(objects, attributes, incidence);
    }

    public static Set<FormalConcept> computeConcepts(FormalContext context) {
        Set<FormalConcept> concepts = new HashSet<>();
        List<String> attributes = context.getAttributes();
        Set<String> emptyIntent = new HashSet<>();
        Queue<Set<String>> queue = new LinkedList<>();
        queue.add(emptyIntent);

        while (!queue.isEmpty()) {
            Set<String> intent = queue.poll();
            Set<String> closedIntent = context.closure(intent);
            Set<String> extent = context.deriveExtent(closedIntent);

            FormalConcept concept = new FormalConcept(extent, closedIntent);
            if (!concepts.contains(concept)) {
                concepts.add(concept);
                for (String attr : attributes) {
                    if (!closedIntent.contains(attr)) {
                        Set<String> newIntent = new HashSet<>(closedIntent);
                        newIntent.add(attr);
                        newIntent = context.closure(newIntent);
                        queue.add(newIntent);
                    }
                }
            }
        }
        return concepts;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java V3ChainOfThoughtRun2 <csv_file>");
            System.exit(1);
        }

        try {
            String inputPath = args[0];
            String baseName = new File(inputPath).getName().replaceFirst("\\.csv$", "");
            String outputPath = "llm_" + baseName + ".txt";

            FormalContext context = parseCSV(inputPath);
            Set<FormalConcept> concepts = computeConcepts(context);

            List<FormalConcept> sortedConcepts = new ArrayList<>(concepts);
            sortedConcepts.sort((c1, c2) -> {
                int cmp = Integer.compare(c2.getExtent().size(), c1.getExtent().size());
                if (cmp != 0) return cmp;
                return Integer.compare(c2.getIntent().size(), c1.getIntent().size());
            });

            try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
                for (FormalConcept concept : sortedConcepts) {
                    writer.print(concept.toString() + " ");
                }
            }

            System.out.println("Output written to: " + outputPath);

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
