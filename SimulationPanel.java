import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;

public class SimulationPanel extends JPanel {
    private Environment environment;
    private boolean editMode = false;
    private double newObstacleWidth = 40.0;
    private double newObstacleHeight = 40.0;

    public SimulationPanel(Environment env) {
        this.environment = env;
        this.setBackground(Color.LIGHT_GRAY);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!editMode) {
                    return;
                }
                if (SwingUtilities.isLeftMouseButton(e)) {
                    double x = e.getX();
                    double y = e.getY();
                    environment.addUserObstacle(x, y, newObstacleWidth, newObstacleHeight);
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    double x = e.getX();
                    double y = e.getY();
                    environment.removeObstacleAtPoint(x, y);
                }
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // --- START of updated drawing logic ---
        // This new loop properly handles rotated obstacles.
        g2d.setColor(Color.DARK_GRAY);
        for (Obstacle obs : environment.getObstacles()) {
            // Save the original transform
            AffineTransform oldTransform = g2d.getTransform();

            // Translate and rotate the canvas to the obstacle's position and angle
            g2d.translate(obs.getX(), obs.getY());
            g2d.rotate(Math.toRadians(obs.getAngle())); // Use the obstacle's angle!

            // Draw the rectangle centered at the new (0,0) origin
            g2d.fillRect(
                (int)(-obs.getWidth() / 2),
                (int)(-obs.getHeight() / 2),
                (int)obs.getWidth(),
                (int)obs.getHeight()
            );

            // Restore the original transform so the next item isn't affected
            g2d.setTransform(oldTransform);
        }
        // --- END of updated drawing logic ---

        // Draw Finish Point
        FinishPoint finish = environment.getFinishPoint();
        g2d.setColor(new Color(0, 200, 0)); // Green
        g2d.fillOval(
            (int)(finish.getX() - finish.getRadius()),
            (int)(finish.getY() - finish.getRadius()),
            (int)(finish.getRadius() * 2),
            (int)(finish.getRadius() * 2)
        );

        // Draw Car
        Car car = environment.getCar();
        if (car.isFinished()) {
            g2d.setColor(Color.GREEN);
        } else if (car.isCrashed()) {
            g2d.setColor(Color.RED);
        } else {
            g2d.setColor(Color.CYAN);
        }
        
        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(car.getX(), car.getY());
        g2d.rotate(Math.toRadians(car.getAngle()));
        
        g2d.fillRect(
            (int)(-car.getWidth() / 2),
            (int)(-car.getHeight() / 2),
            (int)car.getWidth(),
            (int)car.getHeight()
        );
        g2d.setTransform(oldTransform);
    }

    public void setEditMode(boolean edit) {
        this.editMode = edit;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setObstacleSize(double width, double height) {
        if (width > 0) {
            this.newObstacleWidth = width;
        }
        if (height > 0) {
            this.newObstacleHeight = height;
        }
    }

    public double getNewObstacleWidth() {
        return newObstacleWidth;
    }

    public double getNewObstacleHeight() {
        return newObstacleHeight;
    }
}