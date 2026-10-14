import java.io.*;
import java.util.*;

/** Verifies the Java port reproduces the trained model's accuracy on the held-out test set. */
public class TestAccuracy {
    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader("test_rows.csv"));
        String header = br.readLine();
        String line;
        int total = 0, correct = 0;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");
            double[] x = new double[7];
            for (int i = 0; i < 7; i++) x[i] = Double.parseDouble(parts[i]);
            String label = parts[7];
            List<CropModel.Prediction> ranked = CropModel.predict(x);
            total++;
            if (ranked.get(0).crop.equals(label)) correct++;
        }
        br.close();
        System.out.printf("Java port accuracy on held-out test set: %d/%d = %.4f%n", correct, total, (double) correct / total);
    }
}
