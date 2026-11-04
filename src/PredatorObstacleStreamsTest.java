import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Regression checks for the predator/obstacle stream refactor. */
public class PredatorObstacleStreamsTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        double[] empty = Predator.collectAvoidanceForces(Stream.empty());
        check(empty[0] == 0 && empty[1] == 0, "No obstacles must mean no avoidance");
        // Detached values are safe for parallel collection. Never parallelise live writes.
        List<double[]> forces = IntStream.range(0, 10000)
                .mapToObj(i -> new double[] {i, -i}).collect(Collectors.toList());
        for (int run = 0; run < 20; run++) {
            double[] sequential = Predator.collectAvoidanceForces(forces.stream());
            double[] parallel = Predator.collectAvoidanceForces(forces.parallelStream());
            check(sequential[0] == 49995000 && sequential[1] == -49995000,
                    "Collector must retain every force");
            check(parallel[0] == sequential[0] && parallel[1] == sequential[1],
                    "Partitioned collection must match sequential collection");
        }
        check(forces.get(500)[0] == 500 && forces.get(500)[1] == -500,
                "Collecting must not modify source vectors");
        double[] cancelled = Predator.collectAvoidanceForces(Stream.of(
                new double[] {2, -3}, new double[] {-2, 3}));
        check(cancelled[0] == 0 && cancelled[1] == 0, "Opposing forces must cancel");

        Obstacle circle = new Obstacle(0, 0, 10, Long.MAX_VALUE);
        check(!circle.overlapsAny(List.of()), "Empty obstacle list cannot overlap");
        check(!circle.overlapsAny(List.of(new Obstacle(20, 0, 10, Long.MAX_VALUE))),
                "Touching circles retain the original non-overlap behaviour");
        check(circle.overlapsAny(List.of(new Obstacle(19, 0, 10, Long.MAX_VALUE))),
                "Intersecting circles must overlap");

        PillarObstacle pillar = new PillarObstacle(100, 100, 120, 40);
        double[] force = pillar.computeAvoidanceForce(160, 80, 40, 2);
        check(Math.abs(force[0] - 0.4) < 1e-12 && force[1] == -1,
                "Typed lambda must preserve the pillar force and tangent");
        double[] outside = pillar.computeAvoidanceForce(160, 60, 40, 2);
        check(outside[0] == 0 && outside[1] == 0, "Avoidance boundary must remain unchanged");
        System.out.println("Predator/obstacle collection, parallel equivalence and lambda checks passed.");
    }
}
