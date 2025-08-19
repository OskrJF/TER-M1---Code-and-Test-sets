package fca4JToArrays;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


// Parses the fca4j-generated dot file and extracts intent/extent pairs from it.
// Intents and Extents are represented as ArrayLists of Strings.
// This parser was later modified and adapted to match whichever structure 
// was used by the LLM to represents extent/intent pairs.

public class FCA4JParserOriginal {
    public static void main(String[] args) {
        String filePath = ""; 
        BufferedReader reader = null;
        ArrayList<ArrayList<ArrayList<String>>> listeConceptsFCAParser = new ArrayList<>();
        try {
            reader = new BufferedReader(new FileReader(filePath));
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        } finally {
        	listeConceptsFCAParser = parseFCA4J(reader);
            // Close the reader in the finally block
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println("Error closing the file: " + e.getMessage());
                }
            }
        }
    } // end main
    
public static ArrayList<ArrayList<ArrayList<String>>> parseFCA4J(BufferedReader reader) {
    	String line;
        ArrayList<ArrayList<ArrayList<String>>> listeConcepts = new ArrayList<>();
        int numberOfConcepts = 0;
        try {
			while ((line = reader.readLine()) != null) {
				String unformatted_intent = "";
			    String unformatted_extent = "";
				if (String.valueOf(line.charAt(0)).matches("\\d")) {
					// System.out.println(line);
					int startIndex = line.indexOf('|');
					int endIndex = line.indexOf('}');
					String substring = line.substring(startIndex,endIndex);
					boolean copy = true;
					for (int i = 1; i < substring.length(); ++i) {
						if (substring.charAt(i) != '|' && copy) {
							unformatted_intent += substring.charAt(i);
						}
						if (substring.charAt(i) == '|') {
							copy = false;
							for (int j = i+1; j < substring.length(); ++j) {
								if (substring.charAt(j) != '}') {
									unformatted_extent += substring.charAt(j);
								}
							}
						}
					}
					// Intent
					int i = 0;
					ArrayList<String> intent = new ArrayList<String>();
					while (i < unformatted_intent.length()) {
						if (unformatted_intent.charAt(i) == '\\') {
							i += 2;
							intent.add(current);
							current = "";
							continue;
						}
						current += unformatted_intent.charAt(i);
						i += 1;
					}
					// Extent
					i = 0;
					current = "";
					ArrayList<String> extent = new ArrayList<String>();
					while (i < unformatted_extent.length()) {
						if (unformatted_extent.charAt(i) == '\\') {
							i += 2;
							extent.add(current);
							current = "";
							continue;
						}
						current += unformatted_extent.charAt(i);
						i += 1;
					}
					concept.add(intent);
					concept.add(extent);
					listeConcepts.add(concept);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
        return listeConcepts;
    }
}