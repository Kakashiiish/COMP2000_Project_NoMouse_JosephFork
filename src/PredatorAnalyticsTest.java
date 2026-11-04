import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PredatorAnalyticsTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static PredatorAnalytics.Position position(double x, double y) {
        return new PredatorAnalytics.Position(x, y);
    }

    public static void main(String[] args) throws Exception {
        PredatorAnalytics.Snapshot boundaries = new PredatorAnalytics.Snapshot(
                List.of(position(0, 0), position(59, 0), position(60, 0),
                        position(119, 0), position(120, 0), position(1000, 0)),
                List.of(position(0, 0), position(1000, 0)));
        Map<PredatorAnalytics.Zone, Long> counts = PredatorAnalytics.analyseParallel(boundaries);
        check(counts.get(PredatorAnalytics.Zone.DANGER) == 3, "Both predators must contribute");
        check(counts.get(PredatorAnalytics.Zone.WARNING) == 2, "60 belongs to WARNING");
        check(counts.get(PredatorAnalytics.Zone.SAFE) == 1, "120 belongs to SAFE");
        check(counts.equals(PredatorAnalytics.analyseSequential(boundaries)), "Modes must agree");
        check(PredatorAnalytics.analyseParallel(new PredatorAnalytics.Snapshot(
                boundaries.birds, List.of())).get(PredatorAnalytics.Zone.SAFE) == 6,
                "Without predators all birds are safe");
        check(PredatorAnalytics.analyseParallel(new PredatorAnalytics.Snapshot(List.of(), List.of()))
                .values().stream().mapToLong(Long::longValue).sum() == 0, "Empty flock must count zero");
        try {
            counts.put(PredatorAnalytics.Zone.SAFE, 99L);
            throw new AssertionError("Results must be immutable");
        } catch (UnsupportedOperationException expected) { }

        List<PredatorAnalytics.Position> mutable = new ArrayList<>(boundaries.birds);
        PredatorAnalytics.Snapshot copy = new PredatorAnalytics.Snapshot(mutable, boundaries.predators);
        mutable.clear();
        check(copy.birds.size() == 6, "Snapshots must copy caller lists");
        try {
            copy.birds.clear();
            throw new AssertionError("Snapshot lists must be immutable");
        } catch (UnsupportedOperationException expected) { }

        World world = new World(600, 600);
        Bird bird = new Bird(10, 10);
        world.addBird(bird);
        Predator predator = new ChaserPredator(20, 20);
        world.addPredator(predator);
        PredatorAnalytics.Snapshot frozen = world.snapshotForAnalytics();
        bird.setPosition(400, 400);
        predator.setPosition(500, 500);
        check(frozen.birds.get(0).x == 10 && frozen.predators.get(0).x == 20,
                "Snapshots must retain copied coordinates, not live entity references");
        PredatorAnalytics.analyseParallel(frozen);
        check(bird.getX() == 400 && predator.getX() == 500, "Analysis must not move entities");

        // Real updates run concurrently with capture and analysis. Each analysis uses one frame.
        for (int i = 0; i < 50; i++) world.addBird(new Bird(50 + i * 4, 50 + i * 3));
        ExecutorService updater = Executors.newSingleThreadExecutor();
        try {
            Future<?> updates = updater.submit(() -> {
                for (int i = 0; i < 100; i++) world.update();
            });
            for (int i = 0; i < 100; i++) {
                PredatorAnalytics.Snapshot snapshot = world.snapshotForAnalytics();
                Map<PredatorAnalytics.Zone, Long> parallel = PredatorAnalytics.analyseParallel(snapshot);
                check(parallel.equals(PredatorAnalytics.analyseSequential(snapshot)),
                        "Concurrent world updates must not change snapshot results");
                check(parallel.values().stream().mapToLong(Long::longValue).sum() == snapshot.birds.size(),
                        "Every snapshot bird must be counted exactly once");
            }
            updates.get(30, TimeUnit.SECONDS);
        } finally {
            updater.shutdownNow();
        }
        System.out.println("Analytics boundary, immutability, equivalence and concurrency checks passed.");
        if (args.length > 0 && args[0].equals("--benchmark")) benchmark();
    }

    // Illustrative timings, not a rigorous benchmark: JVM/hardware/load affect the result.
    private static void benchmark() {
        for (int size : new int[] {50, 500, 5000}) {
            List<PredatorAnalytics.Position> birds = new ArrayList<>();
            for (int i = 0; i < size; i++) birds.add(position(i % 600, (i * 37) % 600));
            PredatorAnalytics.Snapshot snapshot = new PredatorAnalytics.Snapshot(birds,
                    List.of(position(200, 200), position(350, 250)));
            for (int i = 0; i < 10; i++) {
                PredatorAnalytics.analyseSequential(snapshot);
                PredatorAnalytics.analyseParallel(snapshot);
            }
            long sequential = 0;
            long parallel = 0;
            for (int i = 0; i < 20; i++) {
                long start = System.nanoTime();
                Map<PredatorAnalytics.Zone, Long> first = PredatorAnalytics.analyseSequential(snapshot);
                sequential += System.nanoTime() - start;
                start = System.nanoTime();
                Map<PredatorAnalytics.Zone, Long> second = PredatorAnalytics.analyseParallel(snapshot);
                parallel += System.nanoTime() - start;
                check(first.equals(second), "Benchmark modes must agree");
            }
            System.out.printf("%d birds: sequential %.3f ms, parallel %.3f ms (mean of 20)%n",
                    size, sequential / 20e6, parallel / 20e6);
        }
    }
}
