import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class BirdBehaviour {
    
    public static double separationWeight = 1.5;
    public static double alignmentWeight  = 0.05;

    private static Collector<Bird, double[], double[]> repulsionFrom(Bird self) {
    return Collector.of(
            () -> new double[2],
            (force, other) -> {
                double dist = self.distanceTo(other);
                if (dist > 0) {
                    force[0] += (self.getX() - other.getX()) / dist;
                    force[1] += (self.getY() - other.getY()) / dist;
                }
            },
            (a, b) -> { a[0] += b[0]; a[1] += b[1]; return a; },
            Collector.Characteristics.UNORDERED);
    }

    public static class Separation implements Behaviour<Bird> {
        @Override
        public /*Maybe make vector class*/ void  calculate(Bird self, World world) {
            List<Bird> neighbors = world.getEntitiesInRange(world.getBirds(), self, 25.0);
            double[] force = neighbors.stream().collect(repulsionFrom(self));
            self.applyForce(force[0] * separationWeight, force[1] * separationWeight);
        }
    }

    public static class Alignment implements Behaviour<Bird> {
        @Override
        public void calculate(Bird self, World world) {
            List<Bird> neighbors = world.getEntitiesInRange(world.getBirds(), self, 50.0);
            if (neighbors.isEmpty()) return;
 
            double[] avg = neighbors.stream().collect(Collectors.teeing(
                Collectors.averagingDouble(Bird::getVelocityX),
                Collectors.averagingDouble(Bird::getVelocityY),
                (avgDx, avgDy) -> new double[]{avgDx, avgDy}));

            self.applyForce((avg[0] - self.getVelocityX()) * alignmentWeight, (avg[1] - self.getVelocityY()) * alignmentWeight);
        }
    }

    public static class Cohesion implements Behaviour<Bird> {
        @Override
        public void calculate(Bird self, World world) {
            List<Bird> neighbors = world.getEntitiesInRange(world.getBirds(), self, 50);
            if (neighbors.isEmpty()) return;

            double[] center = neighbors.stream().collect(Collectors.teeing(
                Collectors.averagingDouble(Bird::getX),
                Collectors.averagingDouble(Bird::getY),
                (centerX, centerY) -> new double[]{centerX, centerY}));

            self.applyForce((center[0] - self.getX()) * 0.01, (center[1] - self.getY()) * 0.01);
        }
    } 
}

