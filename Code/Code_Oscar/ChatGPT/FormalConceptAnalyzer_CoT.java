package ChatGPT;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 *  FormalConceptAnalyzer
 *
 *  Reads a binary context from a CSV file (hard-coded path) and enumerates
 *  every formal concept using Ganter’s **NextClosure** algorithm.
 *
 *  Compile / run in Eclipse:
 *      javac FormalConceptAnalyzer.java
 *      java  FormalConceptAnalyzer
 */
public class FormalConceptAnalyzer_CoT {

    /* ---------- adjust this path to point at your CSV ------------- */
    private static final String CONTEXT_FILE = "";

    /* -------------------- data structures ------------------------- */
    private int objectCount;           // |G|
    private int attributeCount;        // |M|

    /** incidence matrix as BitSets — attributesOf[g] */
    private BitSet[] attributesOf;

    /** inverted index — objectsHaving[a] */
    private BitSet[] objectsHaving;

    /** convenience bit-set with every object bit set */
    private BitSet allObjects;

    /** convenience bit-set with every attribute bit set */
    private BitSet allAttributes;

    /** container for all discovered concepts */
    private final List<Concept> concepts = new ArrayList<>();

    /* --------------------- public API ----------------------------- */
    public static void main(String[] args) throws IOException {
    	FormalConceptAnalyzer_CoT fca = new FormalConceptAnalyzer_CoT();
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

        for (int a = 0; a < attributeCount; a++)
            objectsHaving[a] = new BitSet(objectCount);

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
        BitSet intent = new BitSet(attributeCount);   // start with ∅
        do {
            BitSet extent = extentOf(intent);
            concepts.add(new Concept(extent, intent));

            intent = nextClosure(intent);
        } while (!(intent.isEmpty()));
    }

    /* -------------------- helper routines ------------------------ */
    /** extent = intent' (common objects) */
    private BitSet extentOf(BitSet intent) {
        BitSet objs = (BitSet) allObjects.clone();
        for (int a = intent.nextSetBit(0); a >= 0; a = intent.nextSetBit(a + 1))
            objs.and(objectsHaving[a]);
        return objs;
    }

    /** closure = (intent' )'  */
    private BitSet closure(BitSet intent) {
        // first derivation: objects that have all attributes in intent
        BitSet objs = extentOf(intent);

        // second derivation: attributes common to those objects
        BitSet attrs = (BitSet) allAttributes.clone();
        for (int g = objs.nextSetBit(0); g >= 0; g = objs.nextSetBit(g + 1))
            attrs.and(attributesOf[g]);
        return attrs;
    }

    /** one step of Ganter’s NextClosure */
    private BitSet nextClosure(BitSet current) {
        BitSet A = (BitSet) current.clone();

        for (int i = attributeCount - 1; i >= 0; i--) {
            if (!A.get(i)) {
                BitSet B = (BitSet) A.clone();
                B.set(i);
                B = closure(B);

                // canonicity test: all j<i, (B_j ⇒ A_j)
                boolean canonical = true;
                for (int j = 0; j < i && canonical; j++) {
                    if (B.get(j) && !A.get(j)) canonical = false;
                }
                if (canonical) return B;
            }
        }
        return new BitSet(attributeCount);   // wrapped around → stop
    }

    /* ----------------------- output ------------------------------- */
    private void printConcepts() {
        int id = 0;
        for (Concept c : concepts) {
            System.out.printf("Concept %-3d  extent=%s   intent=%s%n",
                    id++, c.extentIndices(), c.intentIndices());
        }
        System.out.printf("%nTotal concepts: %d%n", concepts.size());
    }

    /* --------------------- inner class --------------------------- */
    private static class Concept {
        private final BitSet extent;   // objects (rows)
        private final BitSet intent;   // attributes (columns)

        Concept(BitSet extent, BitSet intent) {
            this.extent = (BitSet) extent.clone();
            this.intent = (BitSet) intent.clone();
        }

        List<Integer> extentIndices() { return toIndexList(extent); }
        List<Integer> intentIndices() { return toIndexList(intent); }

        private static List<Integer> toIndexList(BitSet bs) {
            List<Integer> list = new ArrayList<>();
            for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
                list.add(i);
            return list;
        }
    }
}
