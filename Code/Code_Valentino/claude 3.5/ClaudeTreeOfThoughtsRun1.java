import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class TSTreeOfThoughtsRun1 {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java TSTreeOfThoughtsRun1 <csv_file_path>");
            return;
        }

        try {
            FormalContext context = FormalContext.fromCSV(args[0]);
            NextClosureAlgorithm algorithm = new NextClosureAlgorithm(context);

            // Prepare output file
            String inputFileName = Paths.get(args[0]).getFileName().toString();
            String baseName = inputFileName.replaceFirst("[.][^.]+$", "");
            String outputPath = "llm_" + baseName + ".txt";

            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputPath, false)))) {
                List<FormalConcept> allConcepts = algorithm.computeAllConcepts();
                for (FormalConcept concept : allConcepts) {
                    String intent = bitSetToNameList(concept.getIntent(), context::getAttributeName);
                    String extent = bitSetToNameList(concept.getExtent(), context::getObjectName);
                    writer.print("[[" + intent + "], [" + extent + "]] ");
                    writer.flush(); // Progressive write
                }
            }

            System.out.println("✅ Concepts written to: " + outputPath);

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String bitSetToNameList(BitSet bits, java.util.function.IntFunction<String> nameResolver) {
        List<String> names = new ArrayList<>();
        for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
            names.add(nameResolver.apply(i));
        }
        Collections.sort(names);
        return String.join(", ", names);
    }
}

class FormalContext {
    private final List<String> objects;
    private final List<String> attributes;
    private final List<BitSet> incidenceMatrix;

    private FormalContext(List<String> objects, List<String> attributes, List<BitSet> incidenceMatrix) {
        this.objects = objects;
        this.attributes = attributes;
        this.incidenceMatrix = incidenceMatrix;
    }

    public static FormalContext fromCSV(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        if (lines.isEmpty()) throw new IllegalArgumentException("Empty CSV file");

        String[] header = lines.get(0).split(";");
        List<String> attributes = new ArrayList<>();
        for (int i = 1; i < header.length; i++) {
            attributes.add(header[i].trim());
        }

        List<String> objects = new ArrayList<>();
        List<BitSet> incidenceMatrix = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(";");
            if (parts.length < attributes.size() + 1) continue;

            objects.add(parts[0].trim());

            BitSet bitset = new BitSet(attributes.size());
            for (int j = 1; j < parts.length; j++) {
                String val = parts[j].trim();
                if (val.equals("1") || val.equalsIgnoreCase("x") || val.equalsIgnoreCase("true")) {
                    bitset.set(j - 1);
                }
            }
            incidenceMatrix.add(bitset);
        }

        return new FormalContext(objects, attributes, incidenceMatrix);
    }

    public int getObjectCount() { return objects.size(); }
    public int getAttributeCount() { return attributes.size(); }
    public String getObjectName(int i) { return objects.get(i); }
    public String getAttributeName(int i) { return attributes.get(i); }

    public BitSet deriveIntent(BitSet extent) {
        BitSet intent = new BitSet(attributes.size());
        intent.set(0, attributes.size());
        for (int i = extent.nextSetBit(0); i >= 0; i = extent.nextSetBit(i + 1)) {
            intent.and(incidenceMatrix.get(i));
        }
        return intent;
    }

    public BitSet deriveExtent(BitSet intent) {
        BitSet extent = new BitSet(objects.size());
        extent.set(0, objects.size());
        for (int i = 0; i < objects.size(); i++) {
            BitSet objectAttrs = incidenceMatrix.get(i);
            BitSet temp = (BitSet) intent.clone();
            temp.andNot(objectAttrs);
            if (!temp.isEmpty()) extent.clear(i);
        }
        return extent;
    }

    public BitSet closeIntent(BitSet intent) {
        return deriveIntent(deriveExtent(intent));
    }
}

class FormalConcept {
    private final BitSet extent;
    private final BitSet intent;

    public FormalConcept(BitSet extent, BitSet intent) {
        this.extent = (BitSet) extent.clone();
        this.intent = (BitSet) intent.clone();
    }

    public BitSet getExtent() { return (BitSet) extent.clone(); }
    public BitSet getIntent() { return (BitSet) intent.clone(); }
}

class NextClosureAlgorithm {
    private final FormalContext context;

    public NextClosureAlgorithm(FormalContext context) {
        this.context = context;
    }

    public List<FormalConcept> computeAllConcepts() {
        List<FormalConcept> concepts = new ArrayList<>();
        int m = context.getAttributeCount();

        BitSet currentIntent = new BitSet(m);
        BitSet currentExtent = context.deriveExtent(currentIntent);
        currentIntent = context.deriveIntent(currentExtent);
        concepts.add(new FormalConcept(currentExtent, currentIntent));

        while (true) {
            BitSet nextIntent = computeNextClosure(currentIntent);
            if (nextIntent == null) break;
            BitSet nextExtent = context.deriveExtent(nextIntent);
            concepts.add(new FormalConcept(nextExtent, nextIntent));
            currentIntent = nextIntent;
        }

        return concepts;
    }

    private BitSet computeNextClosure(BitSet currentIntent) {
        int m = context.getAttributeCount();
        for (int i = m - 1; i >= 0; i--) {
            if (currentIntent.get(i)) continue;
            BitSet candidate = new BitSet(m);
            for (int j = 0; j < i; j++) if (currentIntent.get(j)) candidate.set(j);
            candidate.set(i);
            BitSet closure = context.closeIntent(candidate);
            boolean isNextClosure = true;
            for (int j = 0; j < i; j++) {
                if (closure.get(j) && !candidate.get(j)) {
                    isNextClosure = false;
                    break;
                }
            }
            if (isNextClosure) return closure;
        }
        return null;
    }
}
