import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.*;
import javax.swing.*;

/*
NOTES:
    fix up sliders
    add lables to sliders
    add uses to sliders
    create piller visule
    make pretty if time
 */

public class App {
    private final World world = new World(600, 600);
    private final FlockPanel flockPanel = new FlockPanel(world);
    private final PillarObstacle leftPillar = new PillarObstacle(0, 220, 128, 40);
    private final PillarObstacle bottomPillar = new PillarObstacle(200, 260, 32, 140);

    private final Frame frame = new Frame("Flock Simulation");
    private final Panel sliderPanel = new Panel();

    private final JSlider separation = new JSlider();
    private final JSlider alignment = new JSlider();
    private final JSlider cohesion = new JSlider();


    //creates a slider with the corect labels
    private Panel labelledSlider(String name, JSlider slider, int min, int max, int start) {
    slider.setMinimum(min);
    slider.setMaximum(max);
    slider.setValue(start);

    JLabel label = new JLabel("     " + name + ": " + start);
    // keeps all the sliders lined up
    label.setPreferredSize(new Dimension(110, 20));   

    // Update the text whenever the slider moves
    slider.addChangeListener(e -> label.setText(name + ": " + slider.getValue()));

    Panel row = new Panel(new BorderLayout());
    row.add(label, BorderLayout.WEST);
    row.add(slider, BorderLayout.CENTER);
    return row;
}


    private final ScheduledExecutorService simLoop = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "sim-loop");
        thread.setDaemon(true);
        return thread;
    });

    //RGB Color of background
    private final Color bgColor = new Color(100,100,255);


        public App() {
            // Create both predator types when the simulation starts.
            try {
                world.addPredator(new ChaserPredator(200, 200));
                world.addPredator(new AmbushPredator(350, 250));
            } catch (IllegalArgumentException e) {
                System.err.println("Failed to add predator: " + e.getMessage());
            }
            world.addObstacle(leftPillar);
            world.addObstacle(bottomPillar);
            // Setting flockPanel paramaters
            flockPanel.setBounds(0, 0, 600, 400);
            flockPanel.setBackground(bgColor);
            // Setting sliderPanel paramaters
            sliderPanel.setBounds(0, 400, 600, 200);
            sliderPanel.setBackground(Color.gray);

            for (int i = 0; i < 600; i++) {
                try {
                    world.addBird(new Bird(Math.random() * (int)world.width, Math.random() * (int)world.height));
                } catch (IllegalArgumentException e) {
                    
                    System.err.println("Failed to add bird " + i + ": " + e.getMessage());
                } catch (CapacityExceededException e) {
                    
                    System.err.println("Stopped adding birds: " + e.getMessage());
                    break;
                }
            }

            
            
            // Add panels to Frame and set Frame layout
            frame.add(flockPanel);
            frame.add(sliderPanel);
            

            // Set frame layout.
            frame.setLayout(null);

            //Set frame size and make it visible.
            frame.setVisible(true);
            frame.setSize((int)world.width, (int)world.height);

            //Add Slider to sliderPanel

            sliderPanel.setLayout(new GridLayout(3, 1, 5, 5));   // one row per slider
            sliderPanel.add(labelledSlider("Separation", separation, 0, 20, 15));
            sliderPanel.add(labelledSlider("Alignment", alignment, 0, 10, 5));
            sliderPanel.add(labelledSlider("Cohesion", cohesion, 0, 10, 1));
            //sliderPanel.add(separation);
            //separation.bounds();
            //sliderPanel.add(alignment);
            //sliderPanel.add(piller);
            //sliderPanel.setLayout(new GridLayout(3, 1, 5, 5));
            
            separation.addChangeListener(e -> BirdBehaviour.separationWeight = separation.getValue() / 10.0);
            alignment.addChangeListener(e  -> BirdBehaviour.alignmentWeight  = alignment.getValue() / 100.0);
            cohesion.addChangeListener(e  -> BirdBehaviour.cohesionWeight  = cohesion.getValue() / 100.0);

            frame.addComponentListener(new ComponentAdapter() {
                @Override 
                public void componentResized(ComponentEvent e) {
                    Dimension d = e.getComponent().getSize();
                    world.changeBounds(d.width, d.height * 2/3);
                    flockPanel.setBounds(0, 0, d.width, d.height * 2/3);
                    sliderPanel.setBounds(0, d.height * 2/3, d.width, d.height / 3);
                    double viewWidth = Math.max(1, flockPanel.getWidth());
                    double viewHeight = Math.max(1, flockPanel.getHeight());
                    synchronized (world) {
                        leftPillar.setBounds(0, viewHeight * 0.55, viewWidth * 0.32, viewHeight * 0.1);
                        bottomPillar.setBounds(viewWidth * 0.5, viewHeight * 0.65,
                                viewWidth * 0.08, viewHeight * 0.35);
                    }
                }
            });

            // If window closes, exit the program.
            frame.addWindowListener(
                new WindowAdapter() {
                    @Override 
                    public void
                    windowClosing(WindowEvent we) {
                        System.exit(0);
                    }
                }
            );
           simLoop.scheduleAtFixedRate(() -> {
            try {
                world.update();
                flockPanel.repaint();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            }, 0, 16, TimeUnit.MILLISECONDS);
    }
    //for testing purposess will be moved later
    public static void main(String[] args) {
        new App();
    }
}
