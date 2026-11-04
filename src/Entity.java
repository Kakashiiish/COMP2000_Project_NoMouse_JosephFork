import java.awt.Graphics2D;

public abstract class Entity {
    protected double x, y;
    
    protected Entity(){
        x = 0;
        y = 0;
    }

    protected Entity(double x, double y) {
        this.x = x;
        this.y = y;
    }
    //public abstract void update();
    public abstract void render(Graphics2D g2d);

    public double distanceTo(Entity other) throws IllegalArgumentException {
        if (other == null) {
            throw new IllegalArgumentException("Cannot calculate distance to a null entity.");
        }
        double diffX = this.x - other.x;
        double diffY = this.y - other.y;
        return Math.sqrt(diffX * diffX + diffY * diffY);
    }

    public double getX() {
        return x;
    }

     public double getY() {
        return y;
    }
    
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

}
