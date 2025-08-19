import java.io.*;
import java.util.*;

public class TSSelfConsistencyRun1 {
    private List<String> objects;
    private List<String> attributes;
    private BitSet[] incidenceMatrix;
    private PrintWriter liveWriter;

    public TSSelfConsistencyRun1(String filename, String outputFile) throws IOException {
        objects = new ArrayList<>();
        attributes = new ArrayList<>();
        parseCSV(filename);
        liveWriter = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, false))); // Create early, overwrite
    }

    private void parseCSV(String filename) throws IOException {
        List<String[]> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");
                lines.add(values);
            }
        }
        String[] headerRow = lines.get(0);
        for (int i = 1; i < headerRow.length; i++) {
            attributes.add(headerRow[i]);
        }
        incidenceMatrix = new BitSet[lines.size() - 1];
        for (int i = 1; i < lines.size(); i++) {
            String[] row = lines.get(i);
            objects.add(row[0]);
            BitSet objectAttributes = new BitSet(attributes.size());
            for (int j = 1; j < row.length; j++) {
                if (row[j].equals("1")) {
                    objectAttributes.set(j - 1);
                }
            }
            incidenceMatrix[i - 1] = objectAttributes;
        }
    }

    public BitSet deriveIntent(BitSet extent) {
        BitSet intent = new BitSet(attributes.size());
        intent.set(0, attributes.size());
        for (int i = extent.nextSetBit(0); i >= 0; i = extent.nextSetBit(i + 1)) {
            intent.and(incidenceMatrix[i]);
        }
        return intent;
    }

    public BitSet deriveExtent(BitSet intent) {
        BitSet extent = new BitSet(objects.size());
        extent.set(0, objects.size());
        for (int j = intent.nextSetBit(0); j >= 0; j = intent.nextSetBit(j + 1)) {
            for (int i = 0; i < objects.size(); i++) {
                if (!incidenceMatrix[i].get(j)) {
                    extent.clear(i);
                }
            }
        }
        return extent;
    }

    public void computeAllConcepts() {
        BitSet topExtent = new BitSet(objects.size());
        topExtent.set(0, objects.size());
        BitSet topIntent = deriveIntent(topExtent);
        closeByOne(topExtent, topIntent, 0);
    }

    private void closeByOne(BitSet extent, BitSet intent, int attributeStart) {
        writeConcept(extent, intent);
        for (int j = attributeStart; j < attributes.size(); j++) {
            if (!intent.get(j)) {
                BitSet newExtent = copyBitSet(extent);
                for (int i = 0; i < objects.size(); i++) {
                    if (extent.get(i) && !incidenceMatrix[i].get(j)) {
                        newExtent.clear(i);
                    }
                }
                BitSet newIntent = deriveIntent(newExtent);
                boolean isCanonical = true;
                for (int k = attributeStart; k < j; k++) {
                    if (newIntent.get(k) && !intent.get(k)) {
                        isCanonical = false;
                        break;
                    }
                }
                if (isCanonical) {
                    closeByOne(newExtent, newIntent, j + 1);
                }
            }
        }
    }

    private void writeConcept(BitSet extent, BitSet intent) {
        List<String> intentNames = new ArrayList<>();
        for (int i = intent.nextSetBit(0); i >= 0; i = intent.nextSetBit(i + 1)) {
            intentNames.add(attributes.get(i));
        }
        List<String> extentNames = new ArrayList<>();
        for (int i = extent.nextSetBit(0); i >= 0; i = extent.nextSetBit(i + 1)) {
            extentNames.add(objects.get(i));
        }
        String formatted = "[[" + String.join(", ", intentNames) + "], [" + String.join(", ", extentNames) + "]]";
        liveWriter.print(formatted + " ");
        liveWriter.flush();
    }

    private BitSet copyBitSet(BitSet original) {
        BitSet copy = new BitSet(original.size());
        copy.or(original);
        return copy;
    }

    public void closeWriter() {
        if (liveWriter != null) {
            liveWriter.close();
        }
    }

    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                System.out.println("Usage: java TSSelfConsistencyRun1 <csv_file>");
                return;
            }
            String inputFile = args[0];
            String baseName = inputFile.substring(inputFile.lastIndexOf('/') + 1).replaceFirst("[.][^.]+$", "");
            String outputFile = "llm_" + baseName + ".txt";

            TSSelfConsistencyRun1 fca = new TSSelfConsistencyRun1(inputFile, outputFile);
            fca.computeAllConcepts();
            fca.closeWriter();
            System.out.println("✅ Concepts written to: " + outputFile);

        } catch (IOException e) {
            System.err.println("❌ Error reading file: " + e.getMessage());
        }
    }
}
