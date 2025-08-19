
import java.io.*;
import java.util.*;

public class V3RolePromptingRun2 {
    private List<String> objects;
    private List<String> attributes;
    private boolean[][] incidence;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java V3RolePromptingRun2 <input.csv>");
            return;
        }

        V3RolePromptingRun2 fca = new V3RolePromptingRun2();
        fca.readCSV(args[0]);
        List<FormalConcept> concepts = fca.computeAllConcepts();
        fca.writeConceptsToFile(concepts, args[0]);
    }

    public void readCSV(String filename) {
        objects = new ArrayList<>();
        attributes = new ArrayList<>();
        List<boolean[]> rows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String[] header = br.readLine().split(";");
            for (int i = 1; i < header.length; i++) {
                attributes.add(header[i].trim());
            }

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                objects.add(parts[0].trim());
                boolean[] row = new boolean[attributes.size()];
                for (int i = 1; i < parts.length; i++) {
                    row[i - 1] = parts[i].trim().equals("1") || parts[i].trim().equalsIgnoreCase("x");
                }
                rows.add(row);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        incidence = rows.toArray(new boolean[0][]);
    }

    public List<FormalConcept> computeAllConcepts() {
        List<FormalConcept> concepts = new ArrayList<>();
        Set<Set<String>> visitedIntents = new HashSet<>();
        Stack<Set<String>> stack = new Stack<>();
        stack.push(new HashSet<>());

        while (!stack.isEmpty()) {
            Set<String> currentIntent = stack.pop();
            Set<String> closedIntent = computeIntent(computeExtent(currentIntent));

            if (visitedIntents.contains(closedIntent)) continue;
            visitedIntents.add(closedIntent);

            Set<String> extent = computeExtent(closedIntent);
            concepts.add(new FormalConcept(extent, closedIntent));

            for (int i = attributes.size() - 1; i >= 0; i--) {
                String attr = attributes.get(i);
                if (!closedIntent.contains(attr)) {
                    Set<String> newIntent = new HashSet<>(closedIntent);
                    newIntent.add(attr);
                    Set<String> newClosedIntent = computeIntent(computeExtent(newIntent));

                    boolean isMinimal = true;
                    for (int j = 0; j < i; j++) {
                        String smallerAttr = attributes.get(j);
                        if (!closedIntent.contains(smallerAttr) && newClosedIntent.contains(smallerAttr)) {
                            isMinimal = false;
                            break;
                        }
                    }

                    if (isMinimal) {
                        stack.push(newClosedIntent);
                    }
                }
            }
        }

        return concepts;
    }

    private Set<String> computeExtent(Set<String> intent) {
        Set<String> extent = new HashSet<>(objects);
        for (String attr : intent) {
            int index = attributes.indexOf(attr);
            for (int i = 0; i < objects.size(); i++) {
                if (!incidence[i][index]) {
                    extent.remove(objects.get(i));
                }
            }
        }
        return extent;
    }

    private Set<String> computeIntent(Set<String> extent) {
        Set<String> intent = new HashSet<>(attributes);
        for (String obj : extent) {
            int rowIndex = objects.indexOf(obj);
            for (int j = 0; j < attributes.size(); j++) {
                if (!incidence[rowIndex][j]) {
                    intent.remove(attributes.get(j));
                }
            }
        }
        return intent;
    }

    public void writeConceptsToFile(List<FormalConcept> concepts, String inputPath) {
        String baseName = new File(inputPath).getName().replaceFirst("\\.csv$", "");
        String outputPath = "llm_" + baseName + ".txt";

        concepts.sort((c1, c2) -> {
            int cmp = Integer.compare(c2.extent.size(), c1.extent.size());
            if (cmp != 0) return cmp;
            return Integer.compare(c2.intent.size(), c1.intent.size());
        });

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            for (FormalConcept concept : concepts) {
                List<String> sortedIntent = new ArrayList<>(concept.intent);
                List<String> sortedExtent = new ArrayList<>(concept.extent);
                Collections.sort(sortedIntent);
                Collections.sort(sortedExtent);
                writer.print("[[" + String.join(",", sortedIntent) + "],[" + String.join(",", sortedExtent) + "]] ");
            }
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }

        System.out.println("Output written to: " + outputPath);
    }

    static class FormalConcept {
        Set<String> extent;
        Set<String> intent;

        public FormalConcept(Set<String> extent, Set<String> intent) {
            this.extent = extent;
            this.intent = intent;
        }
    }
}
