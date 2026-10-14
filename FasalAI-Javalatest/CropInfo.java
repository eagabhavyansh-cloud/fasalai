/**
 * CropInfo -- display metadata for each crop: a friendly name, a food-group category
 * (used to colour-code results), a one-line agronomic fact, an example set of
 * readings (the median of that crop's own rows in training data, for the "Load an
 * example" dropdown), and the typical 10th-90th percentile range for each feature
 * (for the "Why this crop?" panel). None of this affects the model's predictions --
 * CropModel alone decides those. It only makes the result panel readable.
 *
 * All double[] arrays are in CropModel.FEATURES order:
 * N, P, K, temperature, humidity, ph, rainfall.
 */
import java.util.LinkedHashMap;
import java.util.Map;

public class CropInfo {

    public static final class Crop {
        public final String key, label, group, fact;
        public final int color;
        public final double[] example; // typical reading, model-feature order
        public final double[] rangeLo; // 10th percentile, model-feature order
        public final double[] rangeHi; // 90th percentile, model-feature order

        Crop(String key, String label, String group, int color, String fact,
             double[] example, double[] rangeLo, double[] rangeHi) {
            this.key = key; this.label = label; this.group = group;
            this.color = color; this.fact = fact;
            this.example = example; this.rangeLo = rangeLo; this.rangeHi = rangeHi;
        }
    }

    public static final Crop[] ALL = {
        new Crop("apple", "Apple", "Fruit", 0xC8692F, "A temperate fruit tree that needs cool temperatures and chilling hours.",
            new double[]{24, 136.5, 200, 22.6, 92.4, 5.9, 113},
            new double[]{2, 122, 196, 21.3, 90.4, 5.6, 103},
            new double[]{35, 144, 205, 23.8, 94.5, 6.3, 122.2}),
        new Crop("banana", "Banana", "Fruit", 0xC8692F, "A heavy feeder -- needs high potassium and steady humidity year-round.",
            new double[]{100.5, 81, 50, 27.4, 80.2, 6, 105},
            new double[]{85, 72, 45, 25.4, 76.5, 5.6, 91.5},
            new double[]{117, 92.1, 55, 29.2, 84.2, 6.3, 117.6}),
        new Crop("blackgram", "Black Gram", "Pulse", 0xB8822A, "Needs warm weather and suits a rotation slot after cereals.",
            new double[]{41, 67, 19, 29.7, 65, 7.2, 68.3},
            new double[]{21.9, 58, 15, 26.4, 61.3, 6.6, 62.3},
            new double[]{57, 78, 24, 33.9, 68.8, 7.6, 73.6}),
        new Crop("chickpea", "Chickpea", "Pulse", 0xB8822A, "A cool-season pulse, usually sown right after the monsoon retreats.",
            new double[]{39, 68, 79, 18.9, 16.7, 7.4, 79.7},
            new double[]{24, 57, 76, 17.3, 14.7, 6.3, 69.2},
            new double[]{57.1, 78, 85, 20.6, 19.3, 8.5, 90.9}),
        new Crop("coconut", "Coconut", "Fruit", 0xC8692F, "A coastal palm that wants high rainfall, humidity and sandy soil.",
            new double[]{24, 15.5, 31, 27.4, 95, 6, 172},
            new double[]{3, 6, 26, 25.5, 91.4, 5.6, 140.7},
            new double[]{37, 29, 35, 29.2, 98.6, 6.4, 218}),
        new Crop("coffee", "Coffee", "Plantation", 0x5C3A21, "A plantation crop that prefers shaded, cool-to-moderate hill climates.",
            new double[]{103, 29, 30, 25.7, 57.6, 6.8, 157.8},
            new double[]{83.9, 18, 26, 23.4, 52.1, 6.1, 122.8},
            new double[]{117, 39, 35, 27.5, 67.4, 7.3, 192.7}),
        new Crop("cotton", "Cotton", "Fibre", 0x6B5B3A, "A cash fibre crop that needs warm weather and a long frost-free season.",
            new double[]{117, 46, 19, 24, 80, 6.8, 80.2},
            new double[]{102, 37, 15, 22.4, 75.9, 6.1, 65},
            new double[]{133, 57.1, 24, 25.5, 84.1, 7.8, 93.7}),
        new Crop("grapes", "Grapes", "Fruit", 0xC8692F, "Prefers well-drained soil with high potassium and a moderate climate.",
            new double[]{24, 133, 201, 23, 81.7, 6, 69.5},
            new double[]{6, 121.9, 195, 11.2, 80.3, 5.6, 65.9},
            new double[]{39, 142, 204, 39, 83.5, 6.4, 74}),
        new Crop("jute", "Jute", "Fibre", 0x6B5B3A, "Grown in hot, humid conditions with high rainfall -- the classic monsoon fibre crop.",
            new double[]{78, 46, 40, 25, 79.5, 6.7, 175.6},
            new double[]{63, 38, 35, 23.3, 72.2, 6.1, 153.7},
            new double[]{91, 57, 44, 26.6, 87.9, 7.3, 194.1}),
        new Crop("kidneybeans", "Kidney Beans", "Pulse", 0xB8822A, "Prefers mild temperatures and well-drained, moderately fertile soil.",
            new double[]{22, 67, 20, 19.9, 21.3, 5.7, 107.4},
            new double[]{5.9, 57.9, 15.9, 16.5, 18.7, 5.6, 67.6},
            new double[]{35.1, 79, 24, 23.8, 24.7, 6, 139.8}),
        new Crop("lentil", "Lentil", "Pulse", 0xB8822A, "A winter pulse that prefers cooler temperatures and modest rainfall.",
            new double[]{16.5, 68, 19, 24.9, 64.1, 7, 46.6},
            new double[]{3, 58.9, 15, 19.6, 60.8, 6.2, 37.1},
            new double[]{36.1, 78.1, 23.1, 28.7, 69, 7.7, 53.1}),
        new Crop("maize", "Maize", "Cereal", 0x2E8F5B, "A warm-season cereal that tolerates a wide range of soils once nitrogen is adequate.",
            new double[]{76, 48.5, 20, 22.8, 65.3, 6.3, 83.5},
            new double[]{61.9, 37, 16, 18.6, 57.8, 5.7, 64.7},
            new double[]{95.1, 59, 24, 25.7, 72.8, 6.8, 107.2}),
        new Crop("mango", "Mango", "Fruit", 0xC8692F, "Sets fruit best with a distinct dry spell ahead of flowering.",
            new double[]{21, 27.5, 30, 31.3, 50.3, 5.7, 94.9},
            new double[]{2.9, 17, 26, 27.7, 46.2, 4.8, 90.3},
            new double[]{37, 37, 34, 35.4, 53.8, 6.8, 98.9}),
        new Crop("mothbeans", "Moth Beans", "Pulse", 0xB8822A, "Bred for arid zones -- handles very low rainfall and high heat.",
            new double[]{22, 48.5, 20, 28.4, 53.7, 7.2, 51.2},
            new double[]{4.9, 37, 16, 25.3, 43.6, 4.2, 33.8},
            new double[]{36, 58, 24, 31, 61.6, 9.1, 70.5}),
        new Crop("mungbean", "Mung Bean", "Pulse", 0xB8822A, "A fast-maturing pulse, often grown as a short summer catch crop.",
            new double[]{22, 47, 20, 28.4, 86, 6.7, 49},
            new double[]{4, 37, 15, 27.4, 81.1, 6.4, 38.1},
            new double[]{35.1, 59, 24, 29.7, 89.1, 7.1, 58}),
        new Crop("muskmelon", "Muskmelon", "Fruit", 0xC8692F, "Similar to watermelon -- warm days, low humidity, sandy loam.",
            new double[]{100, 18, 50, 28.9, 92.1, 6.4, 24.7},
            new double[]{83, 7, 46, 27.4, 90.5, 6.1, 21},
            new double[]{117, 26, 54.1, 29.8, 94.6, 6.7, 28.7}),
        new Crop("orange", "Orange", "Fruit", 0xC8692F, "A citrus crop that favours humid, moderate climates with good drainage.",
            new double[]{19, 16, 10, 22.9, 92, 7, 110.7},
            new double[]{4.9, 7, 6, 11.9, 90.3, 6.2, 102.1},
            new double[]{37, 28.1, 14, 32.3, 94.4, 7.8, 117.7}),
        new Crop("papaya", "Papaya", "Fruit", 0xC8692F, "Fast-growing and heat-tolerant, but sensitive to waterlogging.",
            new double[]{49, 60, 50, 33.3, 92.7, 6.7, 139},
            new double[]{34, 48, 46, 24.9, 90.6, 6.6, 62.2},
            new double[]{68, 68, 54, 42.4, 94.4, 7, 234.5}),
        new Crop("pigeonpeas", "Pigeon Peas", "Pulse", 0xB8822A, "A hardy legume that fixes its own nitrogen and tolerates dry spells.",
            new double[]{20, 69.5, 20, 28.9, 47.2, 5.7, 154.3},
            new double[]{5, 57, 16.9, 19.5, 34.7, 4.7, 100.5},
            new double[]{37, 77, 24, 35.1, 63.5, 7.1, 191.1}),
        new Crop("pomegranate", "Pomegranate", "Fruit", 0xC8692F, "A hardy orchard fruit that handles semi-arid conditions well.",
            new double[]{18, 20, 40, 22.4, 89.9, 6.4, 107.6},
            new double[]{4, 7.9, 36, 18.9, 86.1, 5.8, 103.6},
            new double[]{38.1, 27.1, 44, 24.6, 94.3, 7.1, 111.3}),
        new Crop("rice", "Rice", "Cereal", 0x2E8F5B, "Thrives in standing water and high humidity -- the classic Kharif paddy crop.",
            new double[]{80, 47, 40, 23.7, 82.2, 6.4, 233.1},
            new double[]{62.9, 36, 36, 20.9, 80.3, 5.4, 191.6},
            new double[]{95, 58, 44, 26.5, 84.1, 7.5, 284}),
        new Crop("watermelon", "Watermelon", "Fruit", 0xC8692F, "A warm-season vine crop that needs sandy, well-drained soil.",
            new double[]{99, 17.5, 50.5, 25.6, 85, 6.5, 50.7},
            new double[]{83, 7, 45, 24.4, 81, 6.1, 42.8},
            new double[]{118, 27, 55, 26.8, 89.1, 6.9, 58})
    };

    private static final Map<String, Crop> BY_KEY = new LinkedHashMap<>();
    static {
        for (Crop c : ALL) BY_KEY.put(c.key, c);
    }

    public static Crop get(String key) {
        return BY_KEY.get(key);
    }
}
