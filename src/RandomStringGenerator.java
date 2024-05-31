import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class RandomStringGenerator {
    private final NavigableMap<Double, String> cumulativeMap = new TreeMap<>();
    private final Random random = new Random();
    private double total = 0;

    public RandomStringGenerator(Map<String, Integer> probabilities) {
        for (Map.Entry<String, Integer> entry : probabilities.entrySet()) {
            total += entry.getValue();
            cumulativeMap.put(total, entry.getKey());
        }
    }

    public String getRandomString() {
        double value = random.nextDouble() * total;
        return cumulativeMap.higherEntry(value).getValue();
    }
}

