package fca4JToArrays;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;

import shared.FormalConcept;

public class FCA4JParserSelfConsistency {
 public static void main(String[] args) {
     String filePath = "";
     BufferedReader reader = null;
     Set<FormalConcept> listeConceptsFCAParser = new HashSet<>();
     try {
         reader = new BufferedReader(new FileReader(filePath));
     } catch (IOException e) {
         System.err.println("Error reading the file: " + e.getMessage());
     } finally {
         listeConceptsFCAParser = parseFCA4J(reader);
         if (reader != null) {
             try {
                 reader.close();
             } catch (IOException e) {
                 System.err.println("Error closing the file: " + e.getMessage());
             }
         }
     }

     // (Optionally) print out the parsed concepts:
     System.out.println("Parsed " + listeConceptsFCAParser.size() + " formal concepts:");
     for (FormalConcept concept : listeConceptsFCAParser) {
         System.out.println(concept);
     }
 } // end main

 public static Set<FormalConcept> parseFCA4J(BufferedReader reader) {
     String line;
     Set<FormalConcept> concepts = new HashSet<>();
     try {
         while ((line = reader.readLine()) != null) {
             if (line.isEmpty() || !Character.isDigit(line.charAt(0))) {
                 continue;
             }
             String unformatted_intent = "";
             String unformatted_extent = "";
             int startIndex = line.indexOf('|');
             int endIndex = line.indexOf('}');
             String substring = line.substring(startIndex, endIndex);

             boolean copyingIntent = true;
             for (int i = 1; i < substring.length(); ++i) {
                 char c = substring.charAt(i);
                 if (copyingIntent && c != '|') {
                     unformatted_intent += c;
                 } else if (c == '|') {
                     copyingIntent = false;
                 } else if (!copyingIntent && c != '}') {
                     unformatted_extent += c;
                 }
             }

             // split on the \" escapes
             ArrayList<String> intentList = new ArrayList<>();
             ArrayList<String> extentList = new ArrayList<>();
             StringBuilder current = new StringBuilder();
             for (int i = 0; i < unformatted_intent.length(); i++) {
                 char c = unformatted_intent.charAt(i);
                 if (c == '\\') {
                     intentList.add(current.toString());
                     current.setLength(0);
                     i += 1; // skip escape
                 } else {
                     current.append(c);
                 }
             }
             if (current.length() > 0) {
                 intentList.add(current.toString());
             }

             current.setLength(0);
             for (int i = 0; i < unformatted_extent.length(); i++) {
                 char c = unformatted_extent.charAt(i);
                 if (c == '\\') {
                     extentList.add(current.toString());
                     current.setLength(0);
                     i += 1; // skip escape
                 } else {
                     current.append(c);
                 }
             }
             if (current.length() > 0) {
                 extentList.add(current.toString());
             }

             // convert to sets and wrap in FormalConcept
             Set<String> intentSet = new TreeSet<>(intentList);
             Set<String> extentSet = new TreeSet<>(extentList);
             concepts.add(new FormalConcept(extentSet, intentSet));
         }
     } catch (IOException e) {
         e.printStackTrace();
     }
     return concepts;
 }
}

