import java.io.*;
import java.util.*;

public class TSFewShotsRun1 {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java TSFewShotsRun1 <csv_file>");
            return;
        }

        String filePath = args[0];

        try {
            StringBuilder input = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    input.append(line).append("\n");
                }
            }

            FormalContext context = parseCsv(input.toString());
            List<FormalConcept> concepts = new ArrayList<>();

            String baseName = filePath.substring(filePath.lastIndexOf('/') + 1).replaceFirst("[.][^.]+$", "");
            String outputFile = "llm_" + baseName + ".txt";

            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, false)))) {
                writer.flush();

                Set<String> intent = new HashSet<>();

                while (true) {
                    Set<String> extent = context.computeExtent(intent);
                    Set<String> closure = context.computeIntent(extent);
                    FormalConcept concept = new FormalConcept(extent, closure);
                    concepts.add(concept);

                    writer.print("[[" + String.join(", ", closure) + "], [" + String.join(", ", extent) + "]] ");
                    writer.flush();

                    intent = nextClosure(context, closure);
                    if (intent == null) break;
                }
            }

            System.out.println("Concepts written to: " + outputFile);
            System.out.println("Found " + concepts.size() + " formal concepts.");

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static FormalContext parseCsv(String input) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(input));
        List<String> objects = new ArrayList<>();
        List<String> attributes = new ArrayList<>();

        String line = reader.readLine();
        String[] parts = line.split(";");
        for (int i = 1; i < parts.length; i++) attributes.add(parts[i]);

        List<boolean[]> rows = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            parts = line.split(";");
            objects.add(parts[0]);
            boolean[] row = new boolean[attributes.size()];
            for (int i = 1; i < parts.length; i++) row[i - 1] = "1".equals(parts[i]);
            rows.add(row);
        }

        boolean[][] incidence = new boolean[objects.size()][attributes.size()];
        for (int i = 0; i < rows.size(); i++) incidence[i] = rows.get(i);

        return new FormalContext(objects, attributes, incidence);
    }

    private static Set<String> nextClosure(FormalContext context, Set<String> currentIntent) {
        List<String> attributes = context.getAttributes();

        for (int i = attributes.size() - 1; i >= 0; i--) {
            String attr = attributes.get(i);
            if (currentIntent.contains(attr)) continue;

            Set<String> candidate = new HashSet<>();
            for (int j = 0; j < i; j++) {
                if (currentIntent.contains(attributes.get(j))) candidate.add(attributes.get(j));
            }
            candidate.add(attr);

            Set<String> extent = context.computeExtent(candidate);
            Set<String> closure = context.computeIntent(extent);

            boolean isNext = true;
            for (int j = 0; j < i; j++) {
                if (closure.contains(attributes.get(j)) && !candidate.contains(attributes.get(j))) {
                    isNext = false;
                    break;
                }
            }

            if (isNext) return closure;
        }

        return null;
    }
}

class FormalContext {
    private final List<String> objects;
    private final List<String> attributes;
    private final boolean[][] incidence;
    private final Map<String, Set<String>> objectAttributes;
    private final Map<String, Set<String>> attributeObjects;

    public FormalContext(List<String> objects, List<String> attributes, boolean[][] incidence) {
        this.objects = objects;
        this.attributes = attributes;
        this.incidence = incidence;
        this.objectAttributes = new HashMap<>();
        this.attributeObjects = new HashMap<>();

        for (int i = 0; i < objects.size(); i++) {
            String obj = objects.get(i);
            Set<String> attrs = new HashSet<>();
            for (int j = 0; j < attributes.size(); j++) {
                String attr = attributes.get(j);
                if (incidence[i][j]) {
                    attrs.add(attr);
                    attributeObjects.computeIfAbsent(attr, k -> new HashSet<>()).add(obj);
                }
            }
            objectAttributes.put(obj, attrs);
        }
    }

    public List<String> getObjects() { return objects; }
    public List<String> getAttributes() { return attributes; }

    public Set<String> computeExtent(Set<String> intent) {
        if (intent.isEmpty()) return new HashSet<>(objects);
        Iterator<String> it = intent.iterator();
        Set<String> extent = new HashSet<>(attributeObjects.getOrDefault(it.next(), new HashSet<>()));
        while (it.hasNext()) extent.retainAll(attributeObjects.getOrDefault(it.next(), new HashSet<>()));
        return extent;
    }

    public Set<String> computeIntent(Set<String> extent) {
        if (extent.isEmpty()) return new HashSet<>(attributes);
        Iterator<String> it = extent.iterator();
        Set<String> intent = new HashSet<>(objectAttributes.getOrDefault(it.next(), new HashSet<>()));
        while (it.hasNext()) intent.retainAll(objectAttributes.getOrDefault(it.next(), new HashSet<>()));
        return intent;
    }
}

class FormalConcept {
    private final Set<String> extent;
    private final Set<String> intent;

    public FormalConcept(Set<String> extent, Set<String> intent) {
        this.extent = extent;
        this.intent = intent;
    }

    public Set<String> getExtent() { return extent; }
    public Set<String> getIntent() { return intent; }
}
