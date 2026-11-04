import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Read-only Week 12 analysis: workers never access live simulation entities. */
public final class PredatorAnalytics {
    public enum Zone { DANGER, WARNING, SAFE }

    // Analysis thresholds only: these do not change detection or capture distances.
    public static final double DANGER_DISTANCE = 60;
    public static final double WARNING_DISTANCE = 120;

    public static final class Position {
        public final double x;
        public final double y;

        public Position(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public static final class Snapshot {
        public final List<Position> birds;
        public final List<Position> predators;

        public Snapshot(List<Position> birds, List<Position> predators) {
            this.birds = List.copyOf(birds);
            this.predators = List.copyOf(predators);
        }
    }

    private PredatorAnalytics() { }

    private static Zone classify(Position bird, List<Position> predators) {
        double nearestSquared = predators.stream().mapToDouble(predator -> {
            double dx = bird.x - predator.x;
            double dy = bird.y - predator.y;
            return dx * dx + dy * dy;
        }).min().orElse(Double.POSITIVE_INFINITY);
        if (nearestSquared < DANGER_DISTANCE * DANGER_DISTANCE) return Zone.DANGER;
        if (nearestSquared < WARNING_DISTANCE * WARNING_DISTANCE) return Zone.WARNING;
        return Zone.SAFE;
    }

    /** Parallel stream tasks normally use the shared ForkJoinPool.commonPool(). */
    public static Map<Zone, Long> analyseParallel(Snapshot snapshot) {
        return complete(snapshot.birds.parallelStream().collect(Collectors.groupingByConcurrent(
                bird -> classify(bird, snapshot.predators), Collectors.counting())));
    }

    /** Same calculation for correctness checks and performance comparisons. */
    public static Map<Zone, Long> analyseSequential(Snapshot snapshot) {
        return complete(snapshot.birds.stream().collect(Collectors.groupingBy(
                bird -> classify(bird, snapshot.predators), Collectors.counting())));
    }

    private static Map<Zone, Long> complete(Map<Zone, Long> counts) {
        EnumMap<Zone, Long> result = new EnumMap<>(Zone.class);
        for (Zone zone : Zone.values()) result.put(zone, counts.getOrDefault(zone, 0L));
        return Collections.unmodifiableMap(result);
    }
}
