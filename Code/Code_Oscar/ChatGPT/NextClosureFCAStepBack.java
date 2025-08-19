package ChatGPT;

//NextClosureFCAStepBack.java
//A simple implementation of the NextClosure algorithm for enumerating formal concepts.
//Reads a formal context from a CSV file at a hardcoded path and prints all concepts and their total count.
//
//CSV format:
//First row: header with "Object" in the first column, then attribute names.
//Subsequent rows: each row starts with an object name, then "1" (has attribute) or "0" (does not) for each attribute.
//
//To use:
//1. Adjust the `csvFile` path below to point to your CSV file.
//2. Create a new Java project in Eclipse and add this file under the default package.
//3. Run this class as a Java application.

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NextClosureFCAStepBack {
 public static void main(String[] args) {
     // Hardcoded path to the CSV file
     String csvFile = ""; // <-- change this to your file location

     List<String> attributes = new ArrayList<>();
     List<String> objects = new ArrayList<>();
     List<boolean[]> relationList = new ArrayList<>();

     // Read the CSV file
     try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
         String line = br.readLine();
         if (line != null) {
             String[] header = line.split(";");
             // Header: skip first column (object label)
             for (int i = 1; i < header.length; i++) {
                 attributes.add(header[i].trim());
             }
         }
         while ((line = br.readLine()) != null) {
             String[] parts = line.split(";");
             objects.add(parts[0].trim());
             boolean[] row = new boolean[attributes.size()];
             for (int i = 1; i < parts.length; i++) {
                 row[i - 1] = parts[i].trim().equals("1");
             }
             relationList.add(row);
         }
     } catch (IOException e) {
         System.err.println("Error reading CSV file: " + e.getMessage());
         return;
     }

     int numObjects = objects.size();
     int numAttributes = attributes.size();
     boolean[][] relation = new boolean[numObjects][numAttributes];
     for (int i = 0; i < numObjects; i++) {
         relation[i] = relationList.get(i);
     }

     // NextClosure enumeration
     Set<Integer> intent = closure(new HashSet<>(), relation, numObjects, numAttributes);
     int count = 0; // counter for concepts
     while (intent != null) {
         // Compute extent for this intent
         Set<Integer> extent = computeExtent(intent, relation, numObjects);
         // Print the concept
         System.out.println("Concept → Extent: " + names(extent, objects)
                          + ", Intent: " + names(intent, attributes));
         count++;
         // Move to next intent
         intent = nextClosure(intent, relation, numObjects, numAttributes);
     }
     // Print total number of concepts
     System.out.println("Total concepts: " + count);
 }

 /**
  * Computes the closure (double prime) of a given set of attribute indices.
  */
 private static Set<Integer> closure(Set<Integer> current,
                                     boolean[][] rel,
                                     int nObj,
                                     int nAttr) {
     // Compute extent: objects having all attributes in 'current'
     Set<Integer> extent = new HashSet<>();
     for (int i = 0; i < nObj; i++) {
         boolean hasAll = true;
         for (int a : current) {
             if (!rel[i][a]) { hasAll = false; break; }
         }
         if (hasAll) extent.add(i);
     }
     // Compute intent: attributes common to all objects in extent
     Set<Integer> closure = new HashSet<>();
     for (int j = 0; j < nAttr; j++) {
         boolean inAll = true;
         for (int i : extent) {
             if (!rel[i][j]) { inAll = false; break; }
         }
         if (inAll) closure.add(j);
     }
     return closure;
 }

 /**
  * Finds the next closed set (intent) in lectic order, or null if done.
  */
 private static Set<Integer> nextClosure(Set<Integer> current,
                                         boolean[][] rel,
                                         int nObj,
                                         int nAttr) {
     for (int j = nAttr - 1; j >= 0; j--) {
         if (!current.contains(j)) {
             Set<Integer> candidate = new HashSet<>(current);
             candidate.add(j);
             Set<Integer> closed = closure(candidate, rel, nObj, nAttr);
             boolean ok = true;
             for (int k = 0; k < j; k++) {
                 boolean inCurr = current.contains(k);
                 boolean inClosed = closed.contains(k);
                 if (inCurr != inClosed) { ok = false; break; }
             }
             if (ok) {
                 return closed;
             }
         }
     }
     return null;
 }

 /**
  * Computes the extent (set of object indices) for a given intent.
  */
 private static Set<Integer> computeExtent(Set<Integer> intent,
                                           boolean[][] rel,
                                           int nObj) {
     Set<Integer> extent = new HashSet<>();
     for (int i = 0; i < nObj; i++) {
         boolean hasAll = true;
         for (int a : intent) {
             if (!rel[i][a]) { hasAll = false; break; }
         }
         if (hasAll) extent.add(i);
     }
     return extent;
 }

 /**
  * Utility to map a set of indices to their names.
  */
 private static Set<String> names(Set<Integer> indices, List<String> labels) {
     return indices.stream()
                   .map(labels::get)
                   .collect(Collectors.toSet());
 }
}
