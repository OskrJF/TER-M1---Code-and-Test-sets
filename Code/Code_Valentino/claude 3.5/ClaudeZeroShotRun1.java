import java.io.*;
import java.util.*;

public class TSZeroShotRun1 {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java TSZeroShotRun1 <csv-file-path>");
            System.exit(1);
        }

        String inputPath = args[0];
        File inputFile = new File(inputPath);
        String baseName = inputFile.getName();
        if (baseName.endsWith(".csv")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }
        String outputName = "llm_" + baseName + ".txt";

        // Read and process CSV
        FormalContext context = parseCSV(inputPath);
        Set<FormalConcept> concepts = computeAllConcepts(context);

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputName, false)))) {
            for (FormalConcept concept : concepts) {
                List<String> intentNames = concept.getIntentNames();
                List<String> extentNames = concept.getExtentNames();
                writer.print("[[" + String.join(", ", intentNames) + "], [" + String.join(", ", extentNames) + "]] ");
                writer.flush(); // Ensure concept is written immediately
            }
            System.out.println("Output written to " + outputName);
        } catch (IOException e) {
            System.err.println("Error writing output file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static FormalContext parseCSV(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String headerLine = reader.readLine();
            String[] headers = headerLine.split(";");
            List<String> attributes = new ArrayList<>();
            for (int i = 1; i < headers.length; i++) {
                attributes.add(headers[i]);
            }

            List<String> objects = new ArrayList<>();
            List<boolean[]> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(";");
                objects.add(values[0]);
                boolean[] row = new boolean[attributes.size()];
                for (int i = 1; i < values.length; i++) {
                    row[i-1] = values[i].equals("1");
                }
                rows.add(row);
            }

            boolean[][] matrix = new boolean[rows.size()][attributes.size()];
            for (int i = 0; i < rows.size(); i++) {
                matrix[i] = rows.get(i);
            }
            return new FormalContext(objects, attributes, matrix);
        } catch (IOException e) {
            throw new RuntimeException("Error reading CSV file", e);
        }
    }

    private static Set<FormalConcept> computeAllConcepts(FormalContext context) {
        Set<FormalConcept> concepts = new HashSet<>();
        Set<Integer> fullExtent = new HashSet<>();
        for (int i = 0; i < context.getObjectCount(); i++) {
            fullExtent.add(i);
        }
        Set<Integer> initialIntent = context.deriveIntent(fullExtent);
        Set<Integer> initialExtent = context.deriveExtent(initialIntent);
        generateConcepts(context, initialExtent, initialIntent, 0, concepts);
        return concepts;
    }

    private static void generateConcepts(FormalContext context,
                                         Set<Integer> extent,
                                         Set<Integer> intent,
                                         int startAttr,
                                         Set<FormalConcept> concepts) {
        FormalConcept concept = new FormalConcept(extent, intent, context);
        if (!concepts.contains(concept)) {
            concepts.add(concept);
        }

        for (int j = startAttr; j < context.getAttributeCount(); j++) {
            if (!intent.contains(j)) {
                Set<Integer> newExtent = new HashSet<>(extent);
                newExtent.retainAll(context.getAttributeExtent(j));
                if (newExtent.isEmpty()) continue;
                Set<Integer> newIntent = context.deriveIntent(newExtent);

                boolean isCanonical = true;
                for (int i = 0; i < j; i++) {
                    if (newIntent.contains(i) && !intent.contains(i)) {
                        isCanonical = false;
                        break;
                    }
                }
                if (isCanonical) {
                    generateConcepts(context, newExtent, newIntent, j + 1, concepts);
                }
            }
        }
    }
}

// --- Supporting Classes ---

class FormalContext {
    private final List<String> objects;
    private final List<String> attributes;
    private final boolean[][] incidenceMatrix;
    private final List<Set<Integer>> attributeExtents;

    public FormalContext(List<String> objects, List<String> attributes, boolean[][] matrix) {
        this.objects = objects;
        this.attributes = attributes;
        this.incidenceMatrix = matrix;
        this.attributeExtents = new ArrayList<>();
        for (int j = 0; j < attributes.size(); j++) {
            Set<Integer> extent = new HashSet<>();
            for (int i = 0; i < objects.size(); i++) {
                if (matrix[i][j]) extent.add(i);
            }
            attributeExtents.add(extent);
        }
    }

    public int getObjectCount() { return objects.size(); }
    public int getAttributeCount() { return attributes.size(); }
    public Set<Integer> getAttributeExtent(int idx) { return attributeExtents.get(idx); }

    public Set<Integer> deriveIntent(Set<Integer> objSet) {
        Set<Integer> intent = new HashSet<>();
        if (objSet.isEmpty()) {
            for (int j = 0; j < attributes.size(); j++) intent.add(j);
            return intent;
        }
        for (int j = 0; j < attributes.size(); j++) {
            boolean ok = true;
            for (int i : objSet) {
                if (!incidenceMatrix[i][j]) { ok = false; break; }
            }
            if (ok) intent.add(j);
        }
        return intent;
    }

    public Set<Integer> deriveExtent(Set<Integer> attrSet) {
        Set<Integer> extent = new HashSet<>();
        if (attrSet.isEmpty()) {
            for (int i = 0; i < objects.size(); i++) extent.add(i);
            return extent;
        }
        for (int i = 0; i < objects.size(); i++) extent.add(i);
        for (int j : attrSet) {
            extent.retainAll(attributeExtents.get(j));
        }
        return extent;
    }

    public String getObjectName(int i) { return objects.get(i); }
    public String getAttributeName(int j) { return attributes.get(j); }
}

class FormalConcept {
    private final Set<Integer> extent;
    private final Set<Integer> intent;
    private final FormalContext context;

    public FormalConcept(Set<Integer> extent, Set<Integer> intent, FormalContext ctx) {
        this.extent = new HashSet<>(extent);
        this.intent = new HashSet<>(intent);
        this.context = ctx;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormalConcept)) return false;
        FormalConcept f = (FormalConcept) o;
        return extent.equals(f.extent) && intent.equals(f.intent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(extent, intent);
    }

    public List<String> getIntentNames() {
        List<String> names = new ArrayList<>();
        for (int j : intent) names.add(context.getAttributeName(j));
        Collections.sort(names);
        return names;
    }

    public List<String> getExtentNames() {
        List<String> names = new ArrayList<>();
        for (int i : extent) names.add(context.getObjectName(i));
        Collections.sort(names);
        return names;
    }
}
