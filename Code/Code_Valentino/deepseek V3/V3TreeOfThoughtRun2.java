
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class V3TreeOfThoughtRun2 {

    static class FormalContext {
        List<String> objects;
        List<String> attributes;
        List<BitSet> incidence;

        public FormalContext(String filePath) throws IOException {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            this.attributes = Arrays.stream(lines.get(0).split(";")).skip(1).collect(Collectors.toList());
            this.objects = new ArrayList<>();
            this.incidence = new ArrayList<>();

            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(";");
                this.objects.add(parts[0]);
                BitSet bits = new BitSet(attributes.size());
                for (int j = 1; j < parts.length; j++) {
                    if (parts[j].trim().equals("1") || parts[j].trim().equalsIgnoreCase("X")) {
                        bits.set(j - 1);
                    }
                }
                this.incidence.add(bits);
            }
        }

        public Set<String> computeIntent(BitSet extent) {
            BitSet intent = new BitSet(attributes.size());
            intent.set(0, attributes.size(), true);
            for (int i = 0; i < objects.size(); i++) {
                if (extent.get(i)) {
                    intent.and(incidence.get(i));
                }
            }
            return bitsToAttributes(intent);
        }

        public BitSet computeExtent(Set<String> intent) {
            BitSet extent = new BitSet(objects.size());
            BitSet intentBits = attributesToBits(intent);
            for (int i = 0; i < objects.size(); i++) {
                BitSet row = incidence.get(i);
                if (containsAll(row, intentBits)) {
                    extent.set(i);
                }
            }
            return extent;
        }

        private boolean containsAll(BitSet a, BitSet b) {
            BitSet temp = (BitSet) a.clone();
            temp.and(b);
            return temp.equals(b);
        }

        private Set<String> bitsToAttributes(BitSet bits) {
            Set<String> result = new HashSet<>();
            for (int i = 0; i < attributes.size(); i++) {
                if (bits.get(i)) {
                    result.add(attributes.get(i));
                }
            }
            return result;
        }

        private BitSet attributesToBits(Set<String> intent) {
            BitSet bits = new BitSet(attributes.size());
            for (int i = 0; i < attributes.size(); i++) {
                if (intent.contains(attributes.get(i))) {
                    bits.set(i);
                }
            }
            return bits;
        }
    }

    static class FormalConcept {
        Set<String> extent;
        Set<String> intent;

        public FormalConcept(Set<String> extent, Set<String> intent) {
            this.extent = extent;
            this.intent = intent;
        }

        @Override
        public String toString() {
            return "[[" + String.join(",", intent) + "], [" + String.join(",", extent) + "]]";
        }
    }

    public static List<FormalConcept> computeConcepts(FormalContext context) {
        List<FormalConcept> concepts = new ArrayList<>();
        BitSet initialExtent = new BitSet(context.objects.size());
        initialExtent.set(0, context.objects.size(), true);
        Set<String> initialIntent = context.computeIntent(initialExtent);
        concepts.add(new FormalConcept(new HashSet<>(context.objects), initialIntent));

        for (int i = 0; i < context.objects.size(); i++) {
            BitSet extent = new BitSet();
            extent.set(i);
            Set<String> intent = context.computeIntent(extent);
            BitSet newExtent = context.computeExtent(intent);

            if (!isConceptAlreadyFound(concepts, newExtent, intent, context)) {
                concepts.add(new FormalConcept(bitsToObjects(newExtent, context.objects), intent));
            }
        }
        return concepts;
    }

    private static boolean isConceptAlreadyFound(List<FormalConcept> concepts, BitSet extent, Set<String> intent, FormalContext context) {
        Set<String> extentObjs = bitsToObjects(extent, context.objects);
        for (FormalConcept c : concepts) {
            if (c.extent.equals(extentObjs) && c.intent.equals(intent)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> bitsToObjects(BitSet bits, List<String> objects) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i < objects.size(); i++) {
            if (bits.get(i)) {
                result.add(objects.get(i));
            }
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java V3TreeOfThoughtRun2 <input.csv>");
            return;
        }
        String inputPath = args[0];
        String baseName = new File(inputPath).getName().replaceFirst("\\.csv$", "");
        String outputPath = "llm_" + baseName + ".txt";

        FormalContext context = new FormalContext(inputPath);
        List<FormalConcept> concepts = computeConcepts(context);

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath))) {
            writer.write(concepts.stream().map(Object::toString).collect(Collectors.joining(" ")));
        }
    }
}
