import java.io.*;
import java.util.*;

public class TSRolePromptingRun1 {

    public static class FormalContext {
        private final List<String> objects;
        private final List<String> attributes;
        private final boolean[][] incidenceMatrix;

        public FormalContext(List<String> objects, List<String> attributes, boolean[][] incidenceMatrix) {
            this.objects = objects;
            this.attributes = attributes;
            this.incidenceMatrix = incidenceMatrix;
        }

        public List<String> getObjects() {
            return objects;
        }

        public List<String> getAttributes() {
            return attributes;
        }

        public BitSet computeIntent(BitSet objectIndices) {
            BitSet intent = new BitSet(attributes.size());
            intent.set(0, attributes.size());
            if (objectIndices.isEmpty()) return intent;
            for (int objIdx = objectIndices.nextSetBit(0); objIdx >= 0; objIdx = objectIndices.nextSetBit(objIdx + 1)) {
                for (int attrIdx = 0; attrIdx < attributes.size(); attrIdx++) {
                    if (!incidenceMatrix[objIdx][attrIdx]) intent.clear(attrIdx);
                }
            }
            return intent;
        }

        public BitSet computeExtent(BitSet attributeIndices) {
            BitSet extent = new BitSet(objects.size());
            extent.set(0, objects.size());
            if (attributeIndices.isEmpty()) return extent;
            for (int attrIdx = attributeIndices.nextSetBit(0); attrIdx >= 0; attrIdx = attributeIndices.nextSetBit(attrIdx + 1)) {
                for (int objIdx = 0; objIdx < objects.size(); objIdx++) {
                    if (!incidenceMatrix[objIdx][attrIdx]) extent.clear(objIdx);
                }
            }
            return extent;
        }
    }

    public static class FormalConcept {
        private final BitSet extent;
        private final BitSet intent;
        private final FormalContext context;

        public FormalConcept(BitSet extent, BitSet intent, FormalContext context) {
            this.extent = (BitSet) extent.clone();
            this.intent = (BitSet) intent.clone();
            this.context = context;
        }

        public String toSingleLine() {
            List<String> extentLabels = new ArrayList<>();
            for (int i = extent.nextSetBit(0); i >= 0; i = extent.nextSetBit(i + 1)) {
                extentLabels.add(context.getObjects().get(i));
            }

            List<String> intentLabels = new ArrayList<>();
            for (int i = intent.nextSetBit(0); i >= 0; i = intent.nextSetBit(i + 1)) {
                intentLabels.add(context.getAttributes().get(i));
            }

            Collections.sort(intentLabels);
            Collections.sort(extentLabels);
            return "[[" + String.join(", ", intentLabels) + "], [" + String.join(", ", extentLabels) + "]]";
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            FormalConcept that = (FormalConcept) obj;
            return extent.equals(that.extent) && intent.equals(that.intent);
        }

        @Override
        public int hashCode() {
            return Objects.hash(extent, intent);
        }
    }

    public static FormalContext loadContextFromCsv(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String[] parts = reader.readLine().split(";");
        List<String> attributes = new ArrayList<>(Arrays.asList(parts).subList(1, parts.length));
        List<String> objects = new ArrayList<>();
        List<boolean[]> incidenceData = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            parts = line.split(";");
            objects.add(parts[0]);
            boolean[] row = new boolean[attributes.size()];
            for (int i = 0; i < attributes.size() && i + 1 < parts.length; i++) {
                String value = parts[i + 1].trim();
                row[i] = value.equals("X") || value.equals("1") || value.equalsIgnoreCase("true");
            }
            incidenceData.add(row);
        }
        reader.close();
        boolean[][] incidenceMatrix = new boolean[objects.size()][attributes.size()];
        for (int i = 0; i < incidenceData.size(); i++) {
            incidenceMatrix[i] = incidenceData.get(i);
        }
        return new FormalContext(objects, attributes, incidenceMatrix);
    }

    public static void closeByOneRecursive(FormalContext context, BitSet extent, BitSet intent, int attrStart,
                                           Set<FormalConcept> seen, PrintWriter writer) {
        FormalConcept concept = new FormalConcept(extent, intent, context);
        if (!seen.contains(concept)) {
            seen.add(concept);
            writer.print(concept.toSingleLine() + " ");
            writer.flush(); // Progressive write
        }

        for (int attrIdx = attrStart; attrIdx < context.getAttributes().size(); attrIdx++) {
            if (intent.get(attrIdx)) continue;
            BitSet newIntent = (BitSet) intent.clone();
            newIntent.set(attrIdx);
            BitSet newExtent = context.computeExtent(newIntent);
            BitSet closure = context.computeIntent(newExtent);
            boolean isCanonical = true;
            for (int i = 0; i < attrIdx; i++) {
                if (!intent.get(i) && closure.get(i)) {
                    isCanonical = false;
                    break;
                }
            }
            if (isCanonical) {
                closeByOneRecursive(context, newExtent, closure, attrIdx + 1, seen, writer);
            }
        }
    }

    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                System.out.println("Usage: java TSRolePromptingRun1 <csv_file>");
                System.exit(1);
            }

            String filename = args[0];
            FormalContext context = loadContextFromCsv(filename);

            String baseName = filename.substring(filename.lastIndexOf('/') + 1).replaceFirst("[.][^.]+$", "");
            String outFile = "llm_" + baseName + ".txt";

            // Create the output file immediately
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outFile, false)))) {
                BitSet initialIntent = new BitSet(context.getAttributes().size());
                BitSet initialExtent = context.computeExtent(initialIntent);
                Set<FormalConcept> seen = new LinkedHashSet<>();
                closeByOneRecursive(context, initialExtent, context.computeIntent(initialExtent), 0, seen, writer);
            }

            System.out.println("✅ Output written to " + outFile);

        } catch (IOException e) {
            System.err.println("❌ Error reading file: " + e.getMessage());
        }
    }
}
