
import java.io.*;
import java.util.*;

public class V3FewShotsRun1 {
    private List<String> objects;
    private List<String> attributes;
    private boolean[][] incidence;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java V3FewShotsRun1 <input_file.csv>");
            return;
        }

        try {
            V3FewShotsRun1 fca = new V3FewShotsRun1();
            fca.readContextFromCSV(args[0]);
            List<Concept> concepts = fca.computeAllConcepts();
            fca.writeConceptsToFile(concepts, args[0]);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    public void readContextFromCSV(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line = reader.readLine();
            String[] parts = line.split(";");
            attributes = new ArrayList<>(Arrays.asList(parts).subList(1, parts.length));

            objects = new ArrayList<>();
            List<boolean[]> incidenceList = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                parts = line.split(";");
                objects.add(parts[0]);
                boolean[] row = new boolean[attributes.size()];
                for (int i = 1; i < parts.length; i++) {
                    row[i - 1] = parts[i].equals("1") || parts[i].equalsIgnoreCase("x");
                }
                incidenceList.add(row);
            }

            incidence = new boolean[objects.size()][attributes.size()];
            for (int i = 0; i < objects.size(); i++) {
                incidence[i] = incidenceList.get(i);
            }
        }
    }

    public List<Concept> computeAllConcepts() {
        List<Concept> concepts = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        int n = (int) Math.pow(2, objects.size());
        for (int i = 0; i < n; i++) {
            BitSet subset = BitSet.valueOf(new long[]{i});
            BitSet intent = computeIntent(subset);
            BitSet extent = computeExtent(intent);
            Concept concept = new Concept(extent, intent);
            String key = concept.getKey(objects, attributes);
            if (!seen.contains(key)) {
                seen.add(key);
                concepts.add(concept);
            }
        }
        return concepts;
    }

    private BitSet computeExtent(BitSet intent) {
        BitSet extent = new BitSet(objects.size());
        extent.set(0, objects.size());

        for (int a = 0; a < attributes.size(); a++) {
            if (intent.get(a)) {
                for (int o = 0; o < objects.size(); o++) {
                    if (!incidence[o][a]) {
                        extent.clear(o);
                    }
                }
            }
        }
        return extent;
    }

    private BitSet computeIntent(BitSet extent) {
        BitSet intent = new BitSet(attributes.size());
        intent.set(0, attributes.size());

        for (int o = 0; o < objects.size(); o++) {
            if (extent.get(o)) {
                for (int a = 0; a < attributes.size(); a++) {
                    if (!incidence[o][a]) {
                        intent.clear(a);
                    }
                }
            }
        }
        return intent;
    }

    public void writeConceptsToFile(List<Concept> concepts, String inputPath) {
        String baseName = new File(inputPath).getName().replaceFirst("\\.csv$", "");
        String outputPath = "llm_" + baseName + ".txt";

        concepts.sort((c1, c2) -> {
            int cmp = Integer.compare(c2.extent.cardinality(), c1.extent.cardinality());
            if (cmp != 0) return cmp;
            return Integer.compare(c2.intent.cardinality(), c1.intent.cardinality());
        });

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            for (Concept c : concepts) {
                writer.print("[[" + c.getIntentString(attributes) + "],[" + c.getExtentString(objects) + "]] ");
            }
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }

        System.out.println("Output written to: " + outputPath);
    }

    private static class Concept {
        private final BitSet extent;
        private final BitSet intent;

        public Concept(BitSet extent, BitSet intent) {
            this.extent = (BitSet) extent.clone();
            this.intent = (BitSet) intent.clone();
        }

        public String getExtentString(List<String> objects) {
            List<String> result = new ArrayList<>();
            for (int i = extent.nextSetBit(0); i >= 0; i = extent.nextSetBit(i + 1)) {
                result.add(objects.get(i));
            }
            Collections.sort(result);
            return String.join(",", result);
        }

        public String getIntentString(List<String> attributes) {
            List<String> result = new ArrayList<>();
            for (int i = intent.nextSetBit(0); i >= 0; i = intent.nextSetBit(i + 1)) {
                result.add(attributes.get(i));
            }
            Collections.sort(result);
            return String.join(",", result);
        }

        public String getKey(List<String> objects, List<String> attributes) {
            return getIntentString(attributes) + "|" + getExtentString(objects);
        }
    }
}
