import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Obstacle extends Entity {

    private static Random RNG = new Random();
    private double radius;
    private long spawnTimeMillis;
    private long lifespanMillis;

    public Obstacle(double x, double y, double radius, long lifespanMillis) { // Constructor for obstacle
        super(x, y); 
        if( radius <= 0) {
            throw new IllegalArgumentException("Radius must be positive.");
        }
        else {
            this.radius = radius;
        }
        this.spawnTimeMillis = System.currentTimeMillis();
        this.lifespanMillis = lifespanMillis;
    }

    public static class SpawnException extends Exception {
        public SpawnException(String message){
            super(message);
        }
    }
    
    public static Obstacle spawnRandom(int frameWidth, int frameHeight, long lifespanMillis, List<? extends Obstacle> existing) throws SpawnException {
        double minDim = Math.min(frameWidth, frameHeight);
        double minDiameter = minDim / 12.0;
        double maxDiameter = minDim / 8.0;

        int maxAttempts = 10;
        Obstacle candidate = null; //holds the new potential obstacle 

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            double diameter = minDiameter + RNG.nextDouble() * (maxDiameter - minDiameter);
            double radius = diameter / 2.0;

            // Keep the whole circle inside the frame bounds.
            double x = radius + RNG.nextDouble() * Math.max(0, frameWidth - diameter);
            double y = radius + RNG.nextDouble() * Math.max(0, frameHeight - diameter);

            candidate = new Obstacle(x, y, radius, lifespanMillis);
            if (existing == null || !candidate.overlapsAny(existing)) {
                return candidate;
            }
        }

        throw new SpawnException("No valid spot found after " + maxAttempts + " attempts");
    }

    public boolean isExpired() { //checks if the obstacle has exceeded its lifespan
        return System.currentTimeMillis() - spawnTimeMillis >= lifespanMillis;
    }

    public long getAgeMillis() {
        return System.currentTimeMillis() - spawnTimeMillis;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return radius; }

    public double distanceTo(double px, double py) { //calculates the distance from the center of the obstacle to a point (px, py)
        double dx = px - x;
        double dy = py - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** True if the point (px, py) is inside this pillar. */
    public boolean contains(double px, double py) {
        return distanceTo(px, py) <= radius;
    }

    public boolean overlaps(Obstacle other) {
        return distanceTo(other.x, other.y) < (this.radius + other.radius);
    }

    public boolean overlapsAny(List<? extends Obstacle> others) {
        for (Obstacle o : others) {
            if (this.overlaps(o)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------
    // Boid avoidance
    // ---------------------------------------------------------------
    /**
     * Calculates the avoidance force that a boid should apply to avoid this obstacle. The force is stronger the closer the boid is to the obstacle, and it is zero if the boid is outside the avoidance zone.
     * 
     *
     * @param boidX      boid's current x position
     * @param boidY      boid's current y position
     * @param avoidMargin buffer distance (beyond the radius) at which
     *                    the boid starts reacting
     * @param maxForce    strength of the push when the boid is right at the
     *                    pillar's edge
     * @return a {dx, dy} force vector; {0, 0} if the boid is outside the
     *         avoidance zone
     */

    public double[] computeAvoidanceForce(double boidX, double boidY, double avoidMargin, double maxForce) {
        double dx = boidX - x;
        double dy = boidY - y;
        double dist = Math.sqrt(dx * dx + dy * dy); // distance from boid to center of obstacle
        double safeDist = radius + avoidMargin; // distance at which the boid starts to react

        if (dist >= safeDist) {
            return new double[]{0, 0}; // no force if the boid is outside the avoidance zone
        }
        if (dist == 0) {
            return new double[]{maxForce, 0}; // Boid is exactly on the center; push it in an arbitrary direction.
        }

        double overlap = safeDist - dist;          
        double strength = (overlap / safeDist) * maxForce; 
        double nx = dx / dist; 
        double ny = dy / dist; 

        return new double[]{nx * strength, ny * strength};
    }

    // might need to remove this render method if it causes issues with the graphics context
    public void render(Graphics2D g2d) {
        int drawX = (int) (x - radius);
        int drawY = (int) (y - radius);
        int diameter = (int) (radius * 2);

        g2d.setColor(new Color(120, 70, 30));
        g2d.fillOval(drawX, drawY, diameter, diameter);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(drawX, drawY, diameter, diameter);
    }

   
    // Manager: handles spawn interval, lifespan and the max cap
    public static class Manager {
        private final List<Obstacle> obstacles = new ArrayList<>();
        private final int frameWidth;
        private final int frameHeight;
        private final long spawnIntervalMillis;
        private final long lifespanMillis;
        private final int maxObstacles;
        private long lastSpawnTime;

        // Constructor 1 with default maxObstacles set
        public Manager(int frameWidth, int frameHeight, long spawnIntervalMillis, long lifespanMillis) {
            this(frameWidth, frameHeight, spawnIntervalMillis, lifespanMillis, 3);
        }

        // Constructor 2 which allows the user to set the maxObstacles
        public Manager(int frameWidth, int frameHeight, long spawnIntervalMillis, long lifespanMillis, int maxObstacles) {
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
            this.spawnIntervalMillis = spawnIntervalMillis;
            this.lifespanMillis = lifespanMillis;
            this.maxObstacles = maxObstacles;
            this.lastSpawnTime = System.currentTimeMillis();
        }


        // Manages spawning and removing of obstacles
        public void update() {
            obstacles.removeIf(Obstacle::isExpired);
            long now = System.currentTimeMillis();
            if (now - lastSpawnTime >= spawnIntervalMillis) {
                lastSpawnTime = now;
                if (obstacles.size() < maxObstacles) {
                    try{
                    obstacles.add(Obstacle.spawnRandom(frameWidth, frameHeight, lifespanMillis, obstacles));
                } catch(SpawnException e){
                    System.out.println(e.getMessage());
                }
             }
            }
        }


        public List<Obstacle> getObstacles() {
            return obstacles;
        }

        // Renders all obstacles in the list
        public void render(Graphics2D g2d) {
            for (Obstacle o : obstacles) {
                o.render(g2d);
            }
        }
    }
}
