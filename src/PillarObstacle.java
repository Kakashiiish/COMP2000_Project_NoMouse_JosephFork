import java.awt.Color;
import java.awt.Graphics2D;
import java.util.stream.DoubleStream;

/** Rectangular pillar; x and y identify its centre, like other entities. */
public class PillarObstacle extends Obstacle {

    private double width;
    private double height;


    @FunctionalInterface
    private interface AvoidanceStrategy {
        double[] calculate(double px, double py, double margin, double maxForce);
    }

    private final AvoidanceStrategy avoidanceStrategy = (px, py, margin, maxForce) -> {
        double[] surface = surface(px, py, 0);

        if ( margin <= 0 || surface[2] >= margin)
        {
            return new double[] {0,0};
        }

        double strength = Math.min(1, (margin - surface[2]) / margin) * maxForce;

        // A small tangential component prevents a head-on approach from stalling.
        return new double[] 
        {
        (surface[0] - surface[1] * 0.4) * strength,
        (surface[1] + surface[0] * 0.4) * strength
        };
    };

    public PillarObstacle(double left, double top, double width, double height) {
        super(left + width / 2, top + height / 2, Math.max(width, height) / 2,
                Long.MAX_VALUE);
        setBounds(left, top, width, height);
    }

    public void setBounds(double left, double top, double width, double height) {
        if (!DoubleStream.of(left, top, width, height)
            .allMatch(Double :: isFinite) || width <= 0 || height <= 0)
        {
            throw new IllegalArgumentException("Pillar bounds must be finite and dimensions positive");
        }
        this.width = width;
        this.height = height;
        setPosition(left + width / 2, top + height / 2);
    }

    @Override
    public double getRadius() { return Math.hypot(width, height) / 2; }

    @Override
    public boolean contains(double px, double py) {
        return Math.abs(px - x) <= width / 2 && Math.abs(py - y) <= height / 2;
    }

    // Outward direction and distance from the nearest face, including inside starts.
    private double[] surface(double px, double py, double padding) {

        double halfWidth = width / 2 + padding;
        double halfHeight = height / 2 + padding;
        double offsetX = px - x;
        double offsetY = py - y;
        double nearestX = Math.max(-halfWidth, Math.min(halfWidth, offsetX));
        double nearestY = Math.max(-halfHeight, Math.min(halfHeight, offsetY));
        double vx = offsetX - nearestX;
        double vy = offsetY - nearestY;
        double distance = Math.hypot(vx, vy);

        if (distance > 0) return new double[] {vx / distance, vy / distance, distance};
        double horizontalDepth = halfWidth - Math.abs(offsetX);
        double verticalDepth = halfHeight - Math.abs(offsetY);

        if (horizontalDepth < verticalDepth) {
            return new double[] {offsetX < 0 ? -1 : 1, 0, -horizontalDepth};
        }

        return new double[] {0, offsetY < 0 ? -1 : 1, -verticalDepth};
    }

    @Override
    public double[] computeAvoidanceForce(double px, double py, double margin, double maxForce) {
        return avoidanceStrategy.calculate(px, py, margin, maxForce);
    }

    /** Keep an entity's body outside the pillar, including after resize or spawn. */
    public void resolveCollision(MovingEntity entity, double padding) {
        double[] surface = surface(entity.getX(), entity.getY(), padding);
        if (surface[2] > 0) return;
        double push = -surface[2] + 0.01;
        entity.setPosition(entity.getX() + surface[0] * push,
                entity.getY() + surface[1] * push);
        double inwardSpeed = entity.dx * surface[0] + entity.dy * surface[1];
        if (inwardSpeed < 0) {
            entity.dx -= inwardSpeed * surface[0];
            entity.dy -= inwardSpeed * surface[1];
        }
    }

    @Override
    public void render(Graphics2D graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            int left = (int) Math.round(x - width / 2);
            int top = (int) Math.round(y - height / 2);
            int w = (int) Math.round(width);
            int h = (int) Math.round(height);

            //obstacle 1
            g.setColor(new Color(120, 70, 30)); //RGB: 78461e (middle)
            g.fillRect(left, top, w, h);

            //obstacle 2
            g.setColor(new Color(190, 140, 85));  //RGB: be8c55 (lightest)
            g.drawLine(left + 2, top + 2, left + w - 2, top + 2);

            //obstacle 3
            g.setColor(new Color(65, 40, 20));  //RGB: 412814 (darkest)
            g.drawRect(left, top, w, h);

        } finally {
            g.dispose();
        }
    }
}
