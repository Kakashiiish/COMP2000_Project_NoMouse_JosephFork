import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Comparator;

public abstract class Predator extends MovingEntity {

    private final double speed;
    private final double size;
    private final BufferedImage scaledSprite;
    private Bird target;
    private double heading;
    private boolean hasHeading;
    private static final double MAX_TURN = 0.12; // radians per simulation update

    private static final BufferedImage SPRITE = loadSprite();

    protected Predator(double x, double y, double speed, double size) {
        super(x, y);
        this.speed = speed;
        this.size = size;
        // Scale once, not from the full-resolution image on every repaint.
        int diameter = (int) (size * 2);
        scaledSprite = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D graphics = scaledSprite.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(SPRITE, 0, 0, diameter, diameter, null);
        } finally {
            graphics.dispose();
        }
    }

    protected abstract double getDetectionRadius();

    public double getBodyRadius() { return size; }

    public void update(List<? extends Bird> birds) {
        update(birds, java.util.Collections.emptyList());
    }

    public void update(List<? extends Bird> birds, List<Obstacle> obstacles) {
        // Keep a valid target instead of switching between nearby birds every frame.
        if (target == null || !birds.contains(target)
                || distanceTo(target) >= getDetectionRadius()) {
            target = findClosestBird(birds);
        }
        if (target != null && distanceTo(target) < size) {
            birds.remove(target);
            target = findClosestBird(birds);
        }
        dx = 0;
        dy = 0;
        if (target != null) moveTowards(target, obstacles);
        x += dx;
        y += dy;
    }

    private Bird findClosestBird(List<? extends Bird> birds) {
        // Filter prey by detection range, then select the nearest bird.
        return birds.stream()
                .filter(bird -> distanceTo(bird) < getDetectionRadius())
                .min(Comparator.comparingDouble(bird -> distanceTo(bird)))
                .orElse(null);
    }

    private void moveTowards(Bird bird, List<Obstacle> obstacles) {
        double distance = Math.max(0.001, distanceTo(bird));
        double[] avoidance = obstacles.stream()
                .map(obstacle -> obstacle.computeAvoidanceForce(x, y, size + 60, 4))
                .reduce(new double[] {0, 0}, (sum, force) ->
                        new double[] {sum[0] + force[0], sum[1] + force[1]});
        double desiredHeading = Math.atan2((bird.getY() - y) / distance + avoidance[1],
                (bird.getX() - x) / distance + avoidance[0]);
        if (!hasHeading) {
            heading = desiredHeading;
            hasHeading = true;
        } else {
            // Wrap to the shortest turn across the -PI/PI boundary.
            double turn = Math.atan2(Math.sin(desiredHeading - heading),
                    Math.cos(desiredHeading - heading));
            heading += Math.max(-MAX_TURN, Math.min(MAX_TURN, turn));
            heading = Math.atan2(Math.sin(heading), Math.cos(heading));
        }
        dx = Math.cos(heading) * speed;
        dy = Math.sin(heading) * speed;
    }

    public double getHeading() {
        // Preserve the last direction while waiting, instead of snapping to zero.
        return heading;
    }

    private static BufferedImage loadSprite() {
        // IDEs can copy assets onto the classpath; plain javac leaves them in src.
        URL resource = Predator.class.getResource("/assets/predator-wolf.png");
        try {
            if (resource == null) {
                Path classes = Path.of(Predator.class.getProtectionDomain()
                        .getCodeSource().getLocation().toURI());
                resource = classes.resolveSibling("src").resolve("assets/predator-wolf.png")
                        .toUri().toURL();
            }
            BufferedImage sprite = ImageIO.read(resource);
            if (sprite == null) throw new IOException("Unsupported predator sprite image");
            return sprite;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load assets/predator-wolf.png", e);
        }
    }

    @Override
    public void render(Graphics2D g2d) {
        // Work on a copy so the predator cannot change the world's drawing state.
        Graphics2D spriteGraphics = (Graphics2D) g2d.create();
        try {
            spriteGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            spriteGraphics.translate(x, y);
            // The source wolf faces left; rotate its snout toward its movement.
            spriteGraphics.rotate(getHeading() - Math.PI);
            int diameter = (int) (size * 2);
            spriteGraphics.drawImage(scaledSprite, -diameter / 2, -diameter / 2, null);
        } finally {
            spriteGraphics.dispose();
        }
    }
}
