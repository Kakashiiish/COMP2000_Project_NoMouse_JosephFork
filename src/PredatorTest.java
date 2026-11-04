import java.util.ArrayList;
import java.util.List;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class PredatorTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static List<Bird> prey(double x, double y) {
        List<Bird> birds = new ArrayList<>();
        birds.add(new Bird(x, y));
        return birds;
    }

    public static void main(String[] args) throws Exception {
        Predator chaser = new ChaserPredator(0, 0);
        chaser.update(prey(300, 0));
        check(chaser.getX() == 2.5, "Chaser must pursue distant prey at its own speed");
        chaser.update(new ArrayList<Bird>());
        check(chaser.getX() == 2.5 && chaser.getVelocityX() == 0,
                "Chaser must stop without prey");

        Predator ambush = new AmbushPredator(0, 0);
        ambush.update(prey(100, 0));
        check(ambush.getX() == 0, "Ambush must wait outside detection radius");
        ambush.update(prey(80, 0));
        check(ambush.getX() == 1.5, "Ambush must pursue nearby prey at its own speed");
        ambush.update(prey(300, 0));
        check(ambush.getX() == 1.5 && ambush.getVelocityX() == 0,
                "Ambush must stop when prey leaves its range");

        for (Predator predator : new Predator[] {
                new ChaserPredator(50, 50), new AmbushPredator(50, 50)}) {
            List<Bird> birds = prey(50, 50);
            predator.update(birds);
            check(birds.isEmpty(), "Both predators must capture overlapping prey");
            check(predator.getX() == 50 && predator.getY() == 50,
                    "Capture must not cause drift or invalid coordinates");
        }
        List<Bird> targets = prey(200, 0);
        targets.add(new Bird(0, 60));
        chaser = new ChaserPredator(0, 0);
        chaser.update(targets);
        check(Math.abs(chaser.getX()) < 1e-9 && chaser.getY() == 2.5,
                "Chaser must select the nearest prey, not the first entry");

        // A newly closer bird must not make the hunter reverse direction.
        chaser = new ChaserPredator(0, 0);
        List<Bird> movingTargets = prey(200, 0);
        chaser.update(movingTargets);
        movingTargets.add(new Bird(-40, 0));
        chaser.update(movingTargets);
        check(chaser.getHeading() == 0, "Hunter must retain its live target");
        movingTargets.get(0).setPosition(-200, 0);
        chaser.update(movingTargets);
        check(Math.abs(chaser.getHeading()) <= 0.120000001,
                "Sharp prey reversals must produce bounded turns");
        double lastHeading = chaser.getHeading();
        chaser.update(new ArrayList<Bird>());
        check(chaser.getHeading() == lastHeading, "Idle predator must retain its heading");

        // Exercise target removal by another predator and continuous pursuit.
        movingTargets.remove(0);
        for (int i = 0; i < 1000; i++) {
            double before = chaser.getHeading();
            chaser.update(movingTargets);
            double turn = Math.atan2(Math.sin(chaser.getHeading() - before),
                    Math.cos(chaser.getHeading() - before));
            check(Math.abs(turn) <= 0.120000001, "Turning must remain smooth across angle wrap");
            check(Double.isFinite(chaser.getX()) && Double.isFinite(chaser.getY()),
                    "Long-running pursuit must stay finite");
        }
        check(movingTargets.isEmpty(), "Smooth steering must still catch stationary prey");

        World world = new World(600, 600);
        world.addPredator(new ChaserPredator(50, 50));
        world.addPredator(new AmbushPredator(50, 50));
        world.addBird(new Bird(50, 50));
        world.update();
        check(world.getBirds().isEmpty(), "Shared prey must be safely removed during world updates");
        world.update();

        for (Predator predator : new Predator[] {
                new ChaserPredator(50, 50), new AmbushPredator(50, 50)}) {
            BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 0, 100, 100);
            java.awt.geom.AffineTransform transform = graphics.getTransform();
            predator.render(graphics);
            check(graphics.getTransform().equals(transform), "Rendering must preserve transform");
            check(graphics.getColor().equals(Color.BLUE), "Rendering must preserve drawing colour");
            graphics.dispose();
            check(image.getRGB(0, 0) == Color.BLUE.getRGB(), "Background must stay unchanged");
            int drawnPixels = 0;
            for (int y = 0; y < 100; y++) {
                for (int x = 0; x < 100; x++) {
                    if (image.getRGB(x, y) != Color.BLUE.getRGB()) drawnPixels++;
                }
            }
            check(drawnPixels > 100, "Both predators must draw their wolf sprite");
            check(image.getRGB(31, 31) == Color.BLUE.getRGB(),
                    "Transparent sprite corners must preserve the background");
        }
        System.out.println("Predator behaviour and sprite checks passed.");
    }
}
