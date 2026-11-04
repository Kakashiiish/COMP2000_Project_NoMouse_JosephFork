public abstract class MovingEntity extends Entity {
    protected double dx, dy;
    protected double ax, ay;
    protected MovingEntity(double x, double y) {
        super(x, y);
    }
    public double getVelocityX() {
        return dx;
    }

    public double getVelocityY() {
        return dy;
    }

    public double getAccelerationX() {
        return ax;
    }

    public double getAccelerationY() {
        return ay;
    }

    public void set(double x, double y) {
        super.setPosition(x, y);
    }

     protected void limitVelocity(double maximumSpeed) {
        double speed = Math.sqrt(dx * dx + dy * dy);

        if (speed > maximumSpeed) {
            dx = (dx / speed) * maximumSpeed;
            dy = (dy / speed) * maximumSpeed;
        }
    }
}
