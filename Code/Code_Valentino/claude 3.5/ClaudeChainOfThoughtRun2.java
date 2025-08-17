import java.io.*;
import java.util.*;

public class TSChainOfThoughtRun2 {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java TSChainOfThoughtRun2 <csv-file-path>");
            return;
        }

        try {
            FormalContext context = FormalContext.fromCSV(args[0], ";");
            ConceptComputer computer = new ConceptComputer(context, "fcbo");
            List<FormalConcept> concepts = new ArrayList<>();

            String baseName = args[0].substring(args[0].lastIndexOf('/') + 1).replaceFirst("[.][^.]+$", "");
            String outputFile = "llm_" + baseName + ".txt";

            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, false)))) {
                writer.flush(); // Create file immediately
                for (FormalConcept concept : computer.computeConceptsLive(writer)) {
                    concepts.add(concept); // Keep track for final count
                }
            }

            System.out.println("Concepts written to: " + outputFile);
            System.out.println("Found " + concepts.size() + " formal concepts.");

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error during computation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

class FormalContext {
    private String[] objects;
    private String[] attributes;
    private boolean[][] matrix;

    public FormalContext(String[] objects, String[] attributes, boolean[][] matrix) {
        this.objects = objects;
        this.attributes = attributes;
        this.matrix = matrix;
    }

    public static FormalContext fromCSV(String filePath, String delimiter) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        if (lines.isEmpty()) throw new IOException("Empty file");

        String[] headerParts = lines.get(0).split(delimiter);
        String[] attributes = Arrays.copyOfRange(headerParts, 1, headerParts.length);
        String[] objects = new String[lines.size() - 1];
        boolean[][] matrix = new boolean[objects.length][attributes.length];

        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(delimiter);
            objects[i - 1] = parts[0];
            for (int j = 1; j < parts.length; j++) {
                String value = parts[j].trim().toLowerCase();
                matrix[i - 1][j - 1] = value.equals("1") || value.equals("x") || value.equals("true") || value.equals("yes");
            }
        }

        return new FormalContext(objects, attributes, matrix);
    }

    public int getObjectCount() { return objects.length; }
    public int getAttributeCount() { return attributes.length; }
    public String getObject(int index) { return objects[index]; }
    public String getAttribute(int index) { return attributes[index]; }

    public BitSet intent(BitSet objectSet) {
        BitSet result = new BitSet(attributes.length);
        result.set(0, attributes.length);
        for (int i = objectSet.nextSetBit(0); i >= 0; i = objectSet.nextSetBit(i + 1)) {
            for (int j = 0; j < attributes.length; j++) {
                if (!matrix[i][j]) result.clear(j);
            }
        }
        return result;
    }

    public BitSet extent(BitSet attributeSet) {
        BitSet result = new BitSet(objects.length);
        result.set(0, objects.length);
        for (int j = attributeSet.nextSetBit(0); j >= 0; j = attributeSet.nextSetBit(j + 1)) {
            for (int i = 0; i < objects.length; i++) {
                if (!matrix[i][j]) result.clear(i);
            }
        }
        return result;
    }
}

class FormalConcept {
    private BitSet extent;
    private BitSet intent;
    private FormalContext context;

    public FormalConcept(BitSet extent, BitSet intent, FormalContext context) {
        this.extent = (BitSet) extent.clone();
        this.intent = (BitSet) intent.clone();
        this.context = context;
    }

    public List<String> getExtentObjects() {
        List<String> result = new ArrayList<>();
        for (int i = extent.nextSetBit(0); i >= 0; i = extent.nextSetBit(i + 1)) {
            result.add(context.getObject(i));
        }
        return result;
    }

    public List<String> getIntentAttributes() {
        List<String> result = new ArrayList<>();
        for (int i = intent.nextSetBit(0); i >= 0; i = intent.nextSetBit(i + 1)) {
            result.add(context.getAttribute(i));
        }
        return result;
    }

    public String toInlineFormat() {
        return "[[" + String.join(", ", getIntentAttributes()) + "], [" + String.join(", ", getExtentObjects()) + "]]";
    }
}

class ConceptComputer {
    private FormalContext context;
    private Set<String> seen;
    private List<FormalConcept> concepts;

    public ConceptComputer(FormalContext context, String algorithm) {
        this.context = context;
        this.seen = new HashSet<>();
        this.concepts = new ArrayList<>();
    }

    public List<FormalConcept> computeConceptsLive(PrintWriter writer) {
        BitSet currentIntent = new BitSet(context.getAttributeCount());
        BitSet extent = context.extent(currentIntent);
        BitSet intent = context.intent(extent);
        emitConcept(extent, intent, writer);

        while (true) {
            BitSet nextIntent = nextClosure(intent);
            if (nextIntent == null) break;
            extent = context.extent(nextIntent);
            intent = context.intent(extent);
            emitConcept(extent, intent, writer);
        }

        return concepts;
    }

    private void emitConcept(BitSet extent, BitSet intent, PrintWriter writer) {
        String key = extent.toString() + "/" + intent.toString();
        if (seen.add(key)) {
            FormalConcept concept = new FormalConcept(extent, intent, context);
            writer.print(concept.toInlineFormat() + " ");
            writer.flush();
            concepts.add(concept);
        }
    }

    private BitSet nextClosure(BitSet currentIntent) {
        for (int i = context.getAttributeCount() - 1; i >= 0; i--) {
            if (!currentIntent.get(i)) {
                BitSet candidate = (BitSet) currentIntent.clone();
                for (int j = i + 1; j < context.getAttributeCount(); j++) candidate.clear(j);
                candidate.set(i);
                BitSet closure = context.intent(context.extent(candidate));
                boolean valid = true;
                for (int j = 0; j < i; j++) {
                    if (closure.get(j) && !currentIntent.get(j)) {
                        valid = false;
                        break;
                    }
                }
                if (valid) return closure;
            }
        }
        return null;
    }
}
