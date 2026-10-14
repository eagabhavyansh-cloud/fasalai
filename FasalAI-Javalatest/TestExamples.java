import java.util.List;

/** Confirms every "Load an example" preset, run through the model, predicts its own crop. */
public class TestExamples {
    public static void main(String[] args) {
        int correct = 0;
        for (CropInfo.Crop c : CropInfo.ALL) {
            double[] e = c.example; // N, P, K, temperature, humidity, ph, rainfall (model order)
            List<CropModel.Prediction> ranked = CropModel.predict(e);
            String predicted = ranked.get(0).crop;
            boolean ok = predicted.equals(c.key);
            if (ok) correct++;
            System.out.printf("%-14s -> %-14s (%.1f%%)  %s%n", c.key, predicted, ranked.get(0).confidence * 100, ok ? "OK" : "MISMATCH");
        }
        System.out.println(correct + " / " + CropInfo.ALL.length + " presets correctly round-trip");
    }
}
