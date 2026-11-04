import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

public class Bird extends MovingEntity {
    private double mass = 1.0;
    private double size = 8.0;

   private static final List<Behaviour<Bird>> Behaviours = List.of(
            new BirdBehaviour.Separation(),
            new BirdBehaviour.Alignment(),
            new BirdBehaviour.Cohesion());

    public Bird (double x, double y) {
        super(x, y);
        this.dx = (Math.random() * 4) - 2;
        this.dy = (Math.random() * 4) - 2;
    }
    public double getHeading() {
        return Math.atan2(this.dy, this.dx);
    }
   
    public void applyForce(double forceX, double forceY) {
        this.ax += forceX / this.mass;
        this.ay += forceY / this.mass;
    }

    //@Override
    //needs bird list param
    public void computeForces(World world) {
        Behaviours.forEach(behaviour -> behaviour.calculate(this, world));
    }

    public void integrate(World world) {
        updatePosition();
        avoidWalls(world.width, world.height);
    }
    private void updatePosition() {
        // Acceleration changes velocity
        this.dx += this.ax;
        this.dy += this.ay;
        limitVelocity(3.0);
        // Velocity changes position
        this.x += this.dx;
        this.y += this.dy;

        // Acceleration is reset each frame
        this.ax = 0;
        this.ay = 0;
    }
   private void avoidWalls(double width, double height) {
        double margin = 50.0; 
        double turnFactor = 0.5;
        
        if (this.x < margin) {
            this.ax += turnFactor;
        } else if (this.x > width - margin) {
            this.ax -= turnFactor;
        }
        
        if (this.y < margin) {
            this.ay += turnFactor;
        } else if (this.y > height - margin) {
            this.ay -= turnFactor;
        }
    }

    @Override
    public void render(Graphics2D g2d) {
        double theta = getHeading();
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);

        double[] px = { size * 0.5, -size * 0.4, -size * 0.1, -size * 0.4 };
        double[] py = { 0,          -size * 0.4,  0,           size * 0.4 };

        int[] xPoints = new int[4];
        int[] yPoints = new int[4];

        for (int i = 0; i < 4; i++) {
            xPoints[i] = (int) (this.x + (px[i] * cos - py[i] * sin));
            yPoints[i] = (int) (this.y + (px[i] * sin + py[i] * cos));
        }

        g2d.setColor(Color.WHITE);
        g2d.fillPolygon(xPoints, yPoints, 4);
    }

}
