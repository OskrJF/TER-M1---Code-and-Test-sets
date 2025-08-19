package CSVGenerator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

// Creates a context (a csv file) by populating the matrix with either 
// 1s or 0s with incidence probability 0.5

public class GenerateCSV {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random random = new Random();
        
        System.out.print("Entrez le nombre d'objets (n) : ");
        int n = scanner.nextInt();
        System.out.print("Entrez le nombre d'attributs (m) : ");
        int m = scanner.nextInt();
        
        // Hardcoded directory path
        String directoryPath = ""; 
        
        // Create the directory if it doesn't exist
        java.io.File directory = new java.io.File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        String fileName = "eg" + n + "_" + m + ".csv";
        
        try (FileWriter writer = new FileWriter(fileName)) {
            // Écriture de l'en-tête
            writer.append("");
            for (int j = 1; j <= m; j++) {
                writer.append(";a").append(String.valueOf(j));
            }
            writer.append("\n");
            
            // Écriture des lignes avec valeurs aléatoires
            for (int i = 1; i <= n; i++) {
                writer.append("o").append(String.valueOf(i));
                for (int j = 1; j <= m; j++) {
                    writer.append(";").append(random.nextBoolean() ? "1" : "0");
                }
                writer.append("\n");
            }
            
            System.out.println("Fichier CSV généré avec succès : " + fileName);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier : " + e.getMessage());
        }
        
        scanner.close();
    }
}

