
import java.io.*;
import java.util.*;

public class V3OneShotRun3 {

    static class Concept {
        Set<String> extent;
        Set<String> intent;

        Concept(Set<String> extent, Set<String> intent) {
            this.extent = extent;
            this.intent = intent;
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java V3OneShotRun3 <input.csv>");
            return;
        }

        String inputPath = args[0];
        String baseName = new File(inputPath).getName().replaceFirst("\\.csv$", "");
        String outputPath = "llm_" + baseName + ".txt";

        List<String> objects = new ArrayList<>();
        List<String> attributes = new ArrayList<>();
        Map<String, Set<String>> objectAttributes = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(inputPath))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (firstLine) {
                    for (int i = 1; i < parts.length; i++) {
                        attributes.add(parts[i]);
                    }
                    firstLine = false;
                } else {
                    String object = parts[0];
                    objects.add(object);
                    Set<String> attrs = new HashSet<>();
                    for (int i = 1; i < parts.length; i++) {
                        if (parts[i].equals("1") || parts[i].equalsIgnoreCase("x")) {
                            attrs.add(attributes.get(i - 1));
                        }
                    }
                    objectAttributes.put(object, attrs);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return;
        }

        List<Concept> concepts = computeFormalConcepts(objects, attributes, objectAttributes);

        concepts.sort((c1, c2) -> {
            int cmp = Integer.compare(c2.extent.size(), c1.extent.size());
            if (cmp != 0) return cmp;
            return Integer.compare(c2.intent.size(), c1.intent.size());
        });

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            for (Concept concept : concepts) {
                List<String> sortedExtent = new ArrayList<>(concept.extent);
                List<String> sortedIntent = new ArrayList<>(concept.intent);
                Collections.sort(sortedExtent);
                Collections.sort(sortedIntent);
                writer.print("[[" + String.join(",", sortedIntent) + "],[" + String.join(",", sortedExtent) + "]] ");
            }
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }

        System.out.println("Output written to: " + outputPath);
    }

    static List<Concept> computeFormalConcepts(List<String> objects, List<String> attributes, Map<String, Set<String>> objectAttributes) {
        List<Concept> concepts = new ArrayList<>();
        Set<Set<String>> visitedIntents = new HashSet<>();

        Set<String> initialIntent = new HashSet<>();
        Set<String> initialExtent = computeExtent(initialIntent, objects, objectAttributes);
        concepts.add(new Concept(initialExtent, computeIntent(initialExtent, objectAttributes)));
        visitedIntents.add(new HashSet<>());

        Queue<Set<String>> queue = new LinkedList<>();
        queue.add(initialIntent);

        while (!queue.isEmpty()) {
            Set<String> currentIntent = queue.poll();
            for (String attr : attributes) {
                if (!currentIntent.contains(attr)) {
                    Set<String> newIntent = new HashSet<>(currentIntent);
                    newIntent.add(attr);

                    Set<String> extent = computeExtent(newIntent, objects, objectAttributes);
                    Set<String> closedIntent = computeIntent(extent, objectAttributes);

                    if (!visitedIntents.contains(closedIntent)) {
                        visitedIntents.add(closedIntent);
                        concepts.add(new Concept(extent, closedIntent));
                        queue.add(closedIntent);
                    }
                }
            }
        }
        return concepts;
    }

    static Set<String> computeExtent(Set<String> intent, List<String> objects, Map<String, Set<String>> objectAttributes) {
        Set<String> extent = new HashSet<>();
        for (String obj : objects) {
            if (objectAttributes.get(obj).containsAll(intent)) {
                extent.add(obj);
            }
        }
        return extent;
    }

    static Set<String> computeIntent(Set<String> extent, Map<String, Set<String>> objectAttributes) {
        if (extent.isEmpty()) return new HashSet<>();
        Set<String> intent = new HashSet<>(objectAttributes.get(extent.iterator().next()));
        for (String obj : extent) {
            intent.retainAll(objectAttributes.get(obj));
        }
        return intent;
    }
}
