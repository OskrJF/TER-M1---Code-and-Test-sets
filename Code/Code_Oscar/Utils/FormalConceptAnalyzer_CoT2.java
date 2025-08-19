package ChatGPT;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fca4JToArrays.FCA4JParser_CoT;

/**
 *  FormalConceptAnalyzer_CoT2
 *
 *  Reads a binary context from a CSV file (hard-coded path) and enumerates
 *  every formal concept using Ganter’s NextClosure algorithm.
 *  Then compares with parsed concepts from FCA4JParser_CoT and computes
 *  precision, recall, and F1 measures.
 */
public class FormalConceptAnalyzer_CoT2 {

    /* ---------- adjust this path to point at your CSV ------------- */
    private static final String CONTEXT_FILE = "";
    private static final String FCA4J_PATH   = "";

    /* -------------------- data structures ------------------------- */
    private int objectCount;    // |G|
    private int attributeCount; // |M|

    // incidence matrix as BitSets — attributesOf[g]
    private BitSet[] attributesOf;

    // inverted index — objectsHaving[a]
    private BitSet[] objectsHaving;

    // convenience bit-set with every object bit set
    private BitSet allObjects;

    // convenience bit-set with every attribute bit set
    private BitSet allAttributes;

    // container for all discovered concepts
    private final List<Concept> concepts = new ArrayList<>();

    /* --------------------- public API ----------------------------- */
    public static void main(String[] args) throws IOException {
        FormalConceptAnalyzer_CoT2 fca = new FormalConceptAnalyzer_CoT2();
        fca.loadContext(CONTEXT_FILE);
        fca.enumerateConcepts();
        fca.printConcepts();
    }

    /* -------------------- context loading ------------------------- */
    private void loadContext(String csvPath) throws IOException {
        List<boolean[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] tokens = line.split("[,;\\s]+");
                if (attributeCount == 0) attributeCount = tokens.length;
                boolean[] row = new boolean[attributeCount];
                for (int j = 0; j < attributeCount; j++) {
                    row[j] = tokens[j].trim().equals("1");
                }
                rows.add(row);
            }
        }
        objectCount = rows.size();
        attributesOf = new BitSet[objectCount];
        objectsHaving = new BitSet[attributeCount];
        for (int a = 0; a < attributeCount; a++) {
            objectsHaving[a] = new BitSet(objectCount);
        }
        for (int g = 0; g < objectCount; g++) {
            BitSet attrSet = new BitSet(attributeCount);
            boolean[] row = rows.get(g);
            for (int a = 0; a < attributeCount; a++) {
                if (row[a]) {
                    attrSet.set(a);
                    objectsHaving[a].set(g);
                }
            }
            attributesOf[g] = attrSet;
        }
        allObjects = new BitSet(objectCount);
        allObjects.set(0, objectCount);
        allAttributes = new BitSet(attributeCount);
        allAttributes.set(0, attributeCount);
    }

    /* --------------------- enumeration ---------------------------- */
    private void enumerateConcepts() {
        BitSet intent = new BitSet(attributeCount);
        do {
            BitSet extent = extentOf(intent);
            concepts.add(new Concept(extent, intent));
            intent = nextClosure(intent);
        } while (!intent.isEmpty());
    }

    /* -------------------- helper routines ------------------------ */
    private BitSet extentOf(BitSet intent) {
        BitSet objs = (BitSet) allObjects.clone();
        for (int a = intent.nextSetBit(0); a >= 0; a = intent.nextSetBit(a + 1)) {
            objs.and(objectsHaving[a]);
        }
        return objs;
    }

    private BitSet closure(BitSet intent) {
        BitSet objs = extentOf(intent);
        BitSet attrs = (BitSet) allAttributes.clone();
        for (int g = objs.nextSetBit(0); g >= 0; g = objs.nextSetBit(g + 1)) {
            attrs.and(attributesOf[g]);
        }
        return attrs;
    }

    private BitSet nextClosure(BitSet current) {
        BitSet A = (BitSet) current.clone();
        for (int i = attributeCount - 1; i >= 0; i--) {
            if (!A.get(i)) {
                BitSet B = (BitSet) A.clone();
                B.set(i);
                B = closure(B);
                boolean canonical = true;
                for (int j = 0; j < i && canonical; j++) {
                    if (B.get(j) && !A.get(j)) canonical = false;
                }
                if (canonical) return B;
            }
        }
        return new BitSet(attributeCount);
    }

    /* ----------------------- output ------------------------------- */
    private void printConcepts() throws IOException {
        Set<String> computed = new HashSet<>();
        int id = 0;
        for (Concept c : concepts) {
            String repr = c.extentIndices().toString() + "|" + c.intentIndices().toString();
            computed.add(repr);
            System.out.printf("Concept %-3d  extent=%s   intent=%s%n",
                              id++, c.extentIndices(), c.intentIndices());
        }
        System.out.printf("\nTotal concepts: %d%n", concepts.size());

        // Parse concepts from FCA4JParser_CoT
        List<FCA4JParser_CoT.Concept> parsed = FCA4JParser_CoT.parseFCA4J(
                new BufferedReader(new FileReader(FCA4J_PATH)));
        Set<String> gold = new HashSet<>();
        for (FCA4JParser_CoT.Concept pc : parsed) {
            gold.add(pc.getExtent().toString() + "|" + pc.getIntent().toString());
        }

        // Compute metrics
        int truePos  = 0;
        for (String s : computed) if (gold.contains(s)) truePos++;
        int falsePos = computed.size() - truePos;
        int falseNeg = gold.size() - truePos;

        double precision = truePos / (double) (truePos + falsePos);
        double recall    = truePos / (double) (truePos + falseNeg);
        double f1        = (precision + recall > 0)
                           ? 2 * (precision * recall) / (precision + recall)
                           : 0.0;

        // Print results
        System.out.printf("\nTrue Positives:  %d%nFalse Positives: %d%nFalse Negatives: %d%n",
                          truePos, falsePos, falseNeg);
        System.out.printf("Precision: %.4f (TP=%d, FP=%d)%n", precision, truePos, falsePos);
        System.out.printf("Recall:    %.4f (TP=%d, FN=%d)%n", recall, truePos, falseNeg);
        System.out.printf("F1 score:  %.4f%n", f1);
    }

    /* --------------------- inner class --------------------------- */
    private static class Concept {
        private final BitSet extent;
        private final BitSet intent;

        Concept(BitSet extent, BitSet intent) {
            this.extent = (BitSet) extent.clone();
            this.intent = (BitSet) intent.clone();
        }

        List<Integer> extentIndices() { return toIndexList(extent); }
        List<Integer> intentIndices() { return toIndexList(intent); }

        private static List<Integer> toIndexList(BitSet bs) {
            List<Integer> list = new ArrayList<>();
            for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
                list.add(i);
            }
            return list;
        }
    }
}
