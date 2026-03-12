public class Obstacle {
    private double x, y, width, height, angle;
    private boolean removable;

    // Constructor for rotated obstacles
    public Obstacle(double x, double y, double width, double height, double angle, boolean removable) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.angle = angle; // Angle in degrees
        this.removable = removable;
    }

    // Overloaded constructor for non-rotated obstacles (angle defaults to 0)
    public Obstacle(double x, double y, double width, double height) {
        this(x, y, width, height, 0.0, false);
    }
    
    // Overloaded constructor for user-added obstacles
    public Obstacle(double x, double y, double width, double height, boolean removable) {
        this(x, y, width, height, 0.0, removable);
    }

    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getAngle() { return angle; } // New getter
    public boolean isRemovable() { return removable; }
}