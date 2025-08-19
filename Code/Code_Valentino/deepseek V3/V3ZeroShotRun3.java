
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class V3ZeroShotRun3 {

    private List<String> objects;
    private List<String> attributes;
    private boolean[][] incidenceMatrix;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java V3ZeroShotRun3 <input_csv_file>");
            return;
        }
        String inputPath = args[0];
        String baseName = new java.io.File(inputPath).getName().replaceFirst("\\.csv$", "");
        String outputPath = "llm_" + baseName + ".txt";

        V3ZeroShotRun3 fca = new V3ZeroShotRun3();
        fca.readContextFromCSV(inputPath);
        Set<FormalConcept> concepts = fca.computeFormalConcepts();

        try (FileWriter writer = new FileWriter(outputPath)) {
            for (FormalConcept concept : concepts) {
                List<String> sortedExtent = new ArrayList<>();
                for (int obj : concept.extent) {
                    sortedExtent.add(fca.objects.get(obj));
                }
                List<String> sortedIntent = new ArrayList<>();
                for (int attr : concept.intent) {
                    sortedIntent.add(fca.attributes.get(attr));
                }
                Collections.sort(sortedExtent);
                Collections.sort(sortedIntent);
                writer.write("[[" + String.join(",", sortedIntent) + "],[" + String.join(",", sortedExtent) + "]] ");
            }
        } catch (IOException e) {
            System.err.println("Error writing output: " + e.getMessage());
        }
    }

    public void readContextFromCSV(String csvFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            List<String[]> rows = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                String[] row = line.split(";");
                rows.add(row);
            }
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("Empty CSV file.");
            }
            attributes = new ArrayList<>(Arrays.asList(rows.get(0)));
            attributes.remove(0);
            objects = new ArrayList<>();
            incidenceMatrix = new boolean[rows.size() - 1][attributes.size()];
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                objects.add(row[0]);
                for (int j = 1; j < row.length; j++) {
                    incidenceMatrix[i - 1][j - 1] = row[j].trim().equals("1");
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            System.exit(1);
        }
    }

    public Set<FormalConcept> computeFormalConcepts() {
        Set<FormalConcept> concepts = new LinkedHashSet<>();
        Set<Integer> currentIntent = new HashSet<>();
        while (currentIntent != null) {
            Set<Integer> extent = computeExtent(currentIntent);
            Set<Integer> closedIntent = computeIntent(extent);
            concepts.add(new FormalConcept(extent, closedIntent));
            currentIntent = nextClosure(currentIntent);
        }
        return concepts;
    }

    private Set<Integer> computeExtent(Set<Integer> intent) {
        Set<Integer> extent = new HashSet<>();
        for (int obj = 0; obj < objects.size(); obj++) {
            boolean hasAll = true;
            for (int attr : intent) {
                if (!incidenceMatrix[obj][attr]) {
                    hasAll = false;
                    break;
                }
            }
            if (hasAll) extent.add(obj);
        }
        return extent;
    }

    private Set<Integer> computeIntent(Set<Integer> extent) {
        Set<Integer> intent = new HashSet<>();
        for (int attr = 0; attr < attributes.size(); attr++) {
            boolean allHave = true;
            for (int obj : extent) {
                if (!incidenceMatrix[obj][attr]) {
                    allHave = false;
                    break;
                }
            }
            if (allHave) intent.add(attr);
        }
        return intent;
    }

    private Set<Integer> nextClosure(Set<Integer> currentIntent) {
        for (int i = attributes.size() - 1; i >= 0; i--) {
            if (!currentIntent.contains(i)) {
                Set<Integer> newIntent = new HashSet<>(currentIntent);
                newIntent.add(i);
                Set<Integer> extent = computeExtent(newIntent);
                Set<Integer> closure = computeIntent(extent);
                boolean isMinimal = true;
                for (int j = 0; j < i; j++) {
                    if (!currentIntent.contains(j) && closure.contains(j)) {
                        isMinimal = false;
                        break;
                    }
                }
                if (isMinimal) {
                    return closure;
                }
            }
        }
        return null;
    }

    private static class FormalConcept {
        Set<Integer> extent;
        Set<Integer> intent;
        FormalConcept(Set<Integer> extent, Set<Integer> intent) {
            this.extent = extent;
            this.intent = intent;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FormalConcept that = (FormalConcept) o;
            return extent.equals(that.extent) && intent.equals(that.intent);
        }
        @Override
        public int hashCode() {
            return Objects.hash(extent, intent);
        }
    }
}
