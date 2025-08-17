import java.io.*;
import java.util.*;

public class V3SelfConsistencyRun1 {
    static List<String> attributes;
    static Map<String, Set<String>> context;
    static List<String> objects;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java V3SelfConsistencyRun1 <input.csv>");
            return;
        }

        BufferedReader reader = new BufferedReader(new FileReader(args[0]));
        objects = new ArrayList<>();
        attributes = new ArrayList<>();
        context = new HashMap<>();

        // Parse header (attributes)
        String line = reader.readLine();
        if (line != null) {
            String[] parts = line.split(";");
            attributes.addAll(Arrays.asList(parts).subList(1, parts.length));
        }

        // Parse objects and their attributes
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(";");
            objects.add(parts[0]);
            Set<String> objAttrs = new HashSet<>();
            for (int i = 1; i < parts.length; i++) {
                if (parts[i].equals("1") || parts[i].equalsIgnoreCase("x")) {
                    objAttrs.add(attributes.get(i - 1));
                }
            }
            context.put(parts[0], objAttrs);
        }
        reader.close();

        List<FormalConcept> concepts = new ArrayList<>();
        Set<String> currentIntent = new HashSet<>();
        while (currentIntent != null) {
            Set<String> extent = computeExtent(currentIntent);
            Set<String> closedIntent = computeIntent(extent);
            concepts.add(new FormalConcept(extent, closedIntent));
            currentIntent = nextClosure(closedIntent);
        }

        // Sort and write concepts to file
        concepts.sort((a, b) -> {
            int cmp = Integer.compare(b.extent.size(), a.extent.size());
            return (cmp != 0) ? cmp : Integer.compare(b.intent.size(), a.intent.size());
        });

        String baseName = new File(args[0]).getName().replaceFirst("\\.csv$", "");
        try (PrintWriter writer = new PrintWriter(new FileWriter("llm_" + baseName + ".txt"))) {
            for (FormalConcept concept : concepts) {
                List<String> sortedIntent = new ArrayList<>(concept.intent);
                List<String> sortedExtent = new ArrayList<>(concept.extent);
                Collections.sort(sortedIntent);
                Collections.sort(sortedExtent);
                writer.print("[[" + String.join(",", sortedIntent) + "],[" + String.join(",", sortedExtent) + "]] ");
            }
        }

        System.out.println("Output written to: llm_" + baseName + ".txt");
    }

    private static Set<String> computeIntent(Set<String> extent) {
        if (extent.isEmpty()) return new HashSet<>(attributes);
        Set<String> intent = new HashSet<>(context.get(extent.iterator().next()));
        for (String obj : extent) {
            intent.retainAll(context.get(obj));
        }
        return intent;
    }

    private static Set<String> computeExtent(Set<String> intent) {
        Set<String> extent = new HashSet<>();
        for (String obj : context.keySet()) {
            if (context.get(obj).containsAll(intent)) {
                extent.add(obj);
            }
        }
        return extent;
    }

    private static Set<String> nextClosure(Set<String> intent) {
        for (int i = attributes.size() - 1; i >= 0; i--) {
            String attr = attributes.get(i);
            if (!intent.contains(attr)) {
                Set<String> newIntent = new HashSet<>(intent);
                newIntent.add(attr);
                Set<String> closedIntent = computeIntent(computeExtent(newIntent));
                if (isLexSmallest(closedIntent, attr)) {
                    return closedIntent;
                }
            }
        }
        return null;
    }

    private static boolean isLexSmallest(Set<String> closedIntent, String addedAttr) {
        for (String attr : attributes) {
            if (attr.equals(addedAttr)) break;
            if (!closedIntent.contains(attr)) continue;
            Set<String> test = new HashSet<>(closedIntent);
            test.remove(attr);
            if (computeExtent(test).equals(computeExtent(closedIntent))) {
                return false;
            }
        }
        return true;
    }

    static class FormalConcept {
        Set<String> extent;
        Set<String> intent;

        FormalConcept(Set<String> extent, Set<String> intent) {
            this.extent = extent;
            this.intent = intent;
        }
    }
}
