/** A larger, slower hunter that waits until a bird comes within 100 pixels. */
public class AmbushPredator extends Predator {
    public AmbushPredator(double x, double y) {
        super(x, y, 1.5, 28.0);
    }

    @Override
    protected double getDetectionRadius() {
        return 100.0;
    }
}
