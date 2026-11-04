import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import javax.swing.JPanel;

public class FlockPanel extends JPanel {

    private final World world;

    public FlockPanel(World world) {
        this.world = world;
        setBackground(Color.GRAY);
    }

    private void renderEntities(Graphics2D g2d, List<? extends Entity> entities) {
        for (Entity entity : entities) {
            if (entity != null) {
                entity.render(g2d);
            }
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        synchronized (world) {
            renderEntities(g2d, world.getBirds());
            renderEntities(g2d, world.getObstacles());
            renderEntities(g2d, world.getPredators());
        }
    }
}