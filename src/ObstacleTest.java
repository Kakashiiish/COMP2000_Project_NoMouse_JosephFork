import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class ObstacleTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        PillarObstacle pillar = new PillarObstacle(100, 100, 120, 40);
        check(pillar.contains(110, 110), "Rectangle must contain its interior");
        check(!pillar.contains(110, 90), "Rectangle must exclude points above its face");
        check(pillar.computeAvoidanceForce(160, 80, 45, 2.5)[1] < 0,
                "Bird above the pillar must be steered upward");
        check(pillar.computeAvoidanceForce(400, 400, 45, 2.5)[0] == 0,
                "Distant entities must not be pushed");
        Bird embedded = new Bird(160, 120);
        pillar.resolveCollision(embedded, 4);
        check(!pillar.contains(embedded.getX(), embedded.getY()), "Inside spawn must be ejected");
        pillar.setBounds(0, 220, 128, 40);
        check(pillar.contains(20, 230) && !pillar.contains(160, 120),
                "Resizing must update collision bounds");

        World world = new World(600, 600);
        PillarObstacle wall = new PillarObstacle(280, 170, 40, 260);
        world.addObstacle(wall);
        Bird bird = new Bird(260, 300);
        world.addBird(bird);
        Predator predator = new ChaserPredator(250, 320);
        world.addPredator(predator);
        world.addBird(new Bird(350, 320));
        for (int i = 0; i < 500; i++) {
            world.update();
            check(!wall.contains(predator.getX(), predator.getY()), "Predator must stay outside pillar");
            for (Bird current : world.getBirds()) {
                check(!wall.contains(current.getX(), current.getY()), "Bird must stay outside pillar");
            }
        }

        BufferedImage preview = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = preview.createGraphics();
        g.setColor(new Color(100, 100, 255));
        g.fillRect(0, 0, 600, 400);
        new PillarObstacle(0, 220, 192, 40).render(g);
        new PillarObstacle(300, 260, 48, 140).render(g);
        check(g.getColor().equals(new Color(100, 100, 255)), "Pillar must preserve graphics state");
        for (int i = 0; i < 25; i++) new Bird(40 + (i * 73) % 520, 30 + (i * 43) % 170).render(g);
        new ChaserPredator(240, 180).render(g);
        new AmbushPredator(430, 270).render(g);
        g.dispose();
        check(preview.getRGB(590, 10) == new Color(100, 100, 255).getRGB(),
                "Background colour must remain unchanged");
        if (args.length > 0) javax.imageio.ImageIO.write(preview, "png", new java.io.File(args[0]));
        System.out.println("Obstacle geometry, avoidance, collision and rendering checks passed.");
    }
}
