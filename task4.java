package Recommendation;

import java.util.*;
import java.util.stream.*;

public class Recommendation {

    // Sample dataset (userId, itemId, rating)
    private Map<Integer, Map<Integer, Double>> userRatings = new HashMap<>();
    private Map<Integer, Map<Integer, Double>> itemRatings = new HashMap<>();

    public Recommendation() {
        // Hardcoded sample data
        addRating(1, 101, 5.0);
        addRating(1, 102, 3.0);
        addRating(1, 103, 2.5);
        addRating(2, 101, 2.0);
        addRating(2, 102, 2.5);
        addRating(2, 103, 5.0);
        addRating(2, 104, 2.0);
        addRating(3, 101, 2.5);
        addRating(3, 104, 4.0);
        addRating(3, 105, 4.5);
        addRating(3, 107, 5.0);
        addRating(4, 101, 5.0);
        addRating(4, 103, 3.0);
        addRating(4, 104, 4.5);
        addRating(4, 106, 4.0);
        addRating(5, 102, 3.5);
        addRating(5, 103, 4.0);
        addRating(5, 105, 2.0);
        addRating(5, 107, 3.5);
        addRating(6, 101, 3.0);
        addRating(6, 102, 4.0);
        addRating(6, 103, 2.0);
        addRating(6, 104, 3.0);
        addRating(6, 105, 3.0);
    }

    private void addRating(int user, int item, double rating) {
        userRatings.computeIfAbsent(user, k -> new HashMap<>()).put(item, rating);
        itemRatings.computeIfAbsent(item, k -> new HashMap<>()).put(user, rating);
    }

    private double cosineSimilarity(int itemA, int itemB) {
        Map<Integer, Double> a = itemRatings.getOrDefault(itemA, Collections.emptyMap());
        Map<Integer, Double> b = itemRatings.getOrDefault(itemB, Collections.emptyMap());

        Set<Integer> common = new HashSet<>(a.keySet());
        common.retainAll(b.keySet());
        if (common.isEmpty()) return 0.0;

        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (double val : a.values()) normA += val * val;
        for (double val : b.values()) normB += val * val;
        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);

        for (int u : common) dot += a.get(u) * b.get(u);
        return (normA == 0 || normB == 0) ? 0.0 : dot / (normA * normB);
    }

    private Map<Integer, Map<Integer, Double>> computeAllItemSimilarities() {
        Map<Integer, Map<Integer, Double>> sim = new HashMap<>();
        List<Integer> items = new ArrayList<>(itemRatings.keySet());
        for (int i = 0; i < items.size(); i++) {
            int itemI = items.get(i);
            sim.putIfAbsent(itemI, new HashMap<>());
            for (int j = i + 1; j < items.size(); j++) {
                int itemJ = items.get(j);
                double s = cosineSimilarity(itemI, itemJ);
                if (s > 0) {
                    sim.get(itemI).put(itemJ, s);
                    sim.computeIfAbsent(itemJ, k -> new HashMap<>()).put(itemI, s);
                }
            }
        }
        return sim;
    }

    private double predictRatingForUserItem(int user, int item, Map<Integer, Map<Integer, Double>> itemSim) {
        Map<Integer, Double> userRated = userRatings.getOrDefault(user, Collections.emptyMap());
        double num = 0.0, den = 0.0;
        for (Map.Entry<Integer, Double> entry : userRated.entrySet()) {
            int j = entry.getKey();
            double rating = entry.getValue();
            if (j == item) continue;
            double s = itemSim.getOrDefault(item, Collections.emptyMap()).getOrDefault(j, 0.0);
            if (s != 0.0) {
                num += s * rating;
                den += Math.abs(s);
            }
        }
        if (den == 0.0) {
            return userRated.isEmpty()
                    ? globalAverageRating()
                    : userRated.values().stream().mapToDouble(Double::doubleValue).average().orElse(3.0);
        }
        return num / den;
    }

    private double globalAverageRating() {
        return userRatings.values().stream()
                .flatMap(m -> m.values().stream())
                .mapToDouble(Double::doubleValue)
                .average().orElse(3.0);
    }

    private List<Map.Entry<Integer, Double>> recommendForUser(int user, int topN, Map<Integer, Map<Integer, Double>> itemSim) {
        Set<Integer> rated = userRatings.getOrDefault(user, Collections.emptyMap()).keySet();
        List<Integer> candidates = itemRatings.keySet().stream()
                .filter(item -> !rated.contains(item))
                .collect(Collectors.toList());

        Map<Integer, Double> preds = new HashMap<>();
        for (int item : candidates) {
            preds.put(item, predictRatingForUserItem(user, item, itemSim));
        }

        return preds.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        Recommendation rec = new Recommendation();
        Map<Integer, Map<Integer, Double>> itemSim = rec.computeAllItemSimilarities();

        int user = 3;   // Default user
        int topN = 5;   // Top-N recommendations

        System.out.println("Loaded users: " + rec.userRatings.keySet());
        System.out.println("Loaded items: " + rec.itemRatings.keySet());
        System.out.println("\nTop " + topN + " recommendations for user " + user + ":");

        List<Map.Entry<Integer, Double>> recommendations = rec.recommendForUser(user, topN, itemSim);
        int rank = 1;
        for (Map.Entry<Integer, Double> e : recommendations) {
            System.out.printf("%d. Item %d -> predicted rating %.4f%n", rank++, e.getKey(), e.getValue());
        }
    }
}
