import java.io.*;
import java.util.*;

public class TSOneShotRun1 {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java TSOneShotRun1 <csv_file>");
            return;
        }

        String filePath = args[0];

        try {
            FormalContext context = FormalContext.fromCSV(filePath);
            String baseName = filePath.substring(filePath.lastIndexOf('/') + 1).replaceFirst("[.][^.]+$", "");
            String outputFile = "llm_" + baseName + ".txt";

            Set<FormalConcept> concepts = new LinkedHashSet<>();
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, false)))) {
                writer.flush(); // create file immediately

                Set<Integer> fullExtent = new HashSet<>();
                for (int i = 0; i < context.getObjectCount(); i++) fullExtent.add(i);
                Set<Integer> initialIntent = context.deriveIntent(fullExtent);
                Set<Integer> initialExtent = context.deriveExtent(initialIntent);

                generateConcepts(context, initialExtent, initialIntent, 0, concepts, writer);
            }

            System.out.println("Concepts written to: " + outputFile);
            System.out.println("Found " + concepts.size() + " formal concepts.");

        } catch (IOException e) {
            System.err.println("Error reading context file: " + e.getMessage());
        }
    }

    public static void generateConcepts(FormalContext context, Set<Integer> extent,
                                        Set<Integer> intent, int startAttr,
                                        Set<FormalConcept> concepts, PrintWriter writer) {

        FormalConcept concept = new FormalConcept(extent, intent, context);
        if (!concepts.contains(concept)) {
            concepts.add(concept);
            writer.print(concept.toSingleLine() + " ");
            writer.flush();
        }

        for (int attrIndex = startAttr; attrIndex < context.getAttributeCount(); attrIndex++) {
            if (!intent.contains(attrIndex)) {
                Set<Integer> newExtent = new HashSet<>(extent);
                newExtent.retainAll(context.getAttributeExtent(attrIndex));
                if (newExtent.isEmpty()) continue;

                Set<Integer> newIntent = context.deriveIntent(newExtent);

                boolean isCanonical = true;
                for (int i = 0; i < attrIndex; i++) {
                    if (newIntent.contains(i) && !intent.contains(i)) {
                        isCanonical = false;
                        break;
                    }
                }

                if (isCanonical) {
                    generateConcepts(context, newExtent, newIntent, attrIndex + 1, concepts, writer);
                }
            }
        }
    }
}

// SUPPORTING CLASS (not public)
class FormalContext {
    private final List<String> objects = new ArrayList<>();
    private final List<String> attributes = new ArrayList<>();
    private final List<boolean[]> matrix = new ArrayList<>();

    public static FormalContext fromCSV(String filename) throws IOException {
        FormalContext context = new FormalContext();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String[] header = br.readLine().split(";");
            for (int i = 1; i < header.length; i++) context.attributes.add(header[i]);

            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");
                context.objects.add(values[0]);
                boolean[] row = new boolean[context.attributes.size()];
                for (int i = 1; i < values.length; i++) row[i - 1] = values[i].equals("1") || values[i].equalsIgnoreCase("X");
                context.matrix.add(row);
            }
        }
        return context;
    }

    public int getObjectCount() {
        return objects.size();
    }

    public int getAttributeCount() {
        return attributes.size();
    }

    public Set<Integer> getAttributeExtent(int attrIdx) {
        Set<Integer> extent = new HashSet<>();
        for (int i = 0; i < matrix.size(); i++) {
            if (matrix.get(i)[attrIdx]) extent.add(i);
        }
        return extent;
    }

    public Set<Integer> deriveIntent(Set<Integer> objSet) {
        Set<Integer> intent = new HashSet<>();
        if (objSet.isEmpty()) {
            for (int j = 0; j < getAttributeCount(); j++) intent.add(j);
            return intent;
        }
        for (int j = 0; j < getAttributeCount(); j++) {
            boolean allHave = true;
            for (int i : objSet) {
                if (!matrix.get(i)[j]) {
                    allHave = false;
                    break;
                }
            }
            if (allHave) intent.add(j);
        }
        return intent;
    }

    public Set<Integer> deriveExtent(Set<Integer> attrSet) {
        Set<Integer> extent = new HashSet<>();
        for (int i = 0; i < getObjectCount(); i++) extent.add(i);
        for (int j : attrSet) extent.retainAll(getAttributeExtent(j));
        return extent;
    }

    public String getObjectName(int i) {
        return objects.get(i);
    }

    public String getAttributeName(int j) {
        return attributes.get(j);
    }
}

// SUPPORTING CLASS (not public)
class FormalConcept {
    private final Set<Integer> extent;
    private final Set<Integer> intent;
    private final FormalContext context;

    public FormalConcept(Set<Integer> extent, Set<Integer> intent, FormalContext context) {
        this.extent = new HashSet<>(extent);
        this.intent = new HashSet<>(intent);
        this.context = context;
    }

    public String toSingleLine() {
        List<String> intentNames = new ArrayList<>();
        List<String> extentNames = new ArrayList<>();
        for (int i : intent) intentNames.add(context.getAttributeName(i));
        for (int i : extent) extentNames.add(context.getObjectName(i));
        Collections.sort(intentNames);
        Collections.sort(extentNames);
        return "[[" + String.join(",", intentNames) + "],[" + String.join(",", extentNames) + "]]";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FormalConcept)) return false;
        FormalConcept other = (FormalConcept) obj;
        return extent.equals(other.extent) && intent.equals(other.intent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(extent, intent);
    }
}
