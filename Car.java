public class Car {
    private double x, y;
    private double speed;
    private double angle; // In degrees
    private final double width = 20;
    private final double height = 10;
    private final double maxSpeed = 4.0;
    private final double friction = 0.05;

    private boolean crashed = false;
    private boolean finished = false;

    public Car(double x, double y) {
        this.x = x;
        this.y = y;
        this.speed = 0;
        this.angle = 0; // Pointing right
    }

    public void steer(double steeringInput) {
        if (crashed || finished) return;
        double maxSteer = 5.0; 
        steeringInput = Math.max(-maxSteer, Math.min(maxSteer, steeringInput));
        this.angle += steeringInput;
    }

    public void accelerate(double amount) {
        if (crashed || finished) return;
        this.speed += amount;
        if (this.speed > maxSpeed) {
            this.speed = maxSpeed;
        }
    }

    public void brake(double amount) {
        if (crashed || finished) return;
        this.speed -= amount;
        if (this.speed < 0) {
            this.speed = 0;
        }
    }

    public void updatePosition() {
        if (crashed || finished) return;

        if (speed > 0) {
            speed -= friction;
        } else {
            speed = 0;
        }

        double angleRad = Math.toRadians(this.angle);
        this.x += this.speed * Math.cos(angleRad);
        this.y += this.speed * Math.sin(angleRad);
    }
    
    public void setCrashed(boolean crashed) {
        this.crashed = crashed;
        if (crashed) {
            this.speed = 0;
        }
    }
    
    public void setFinished(boolean finished) {
        this.finished = finished;
        if (finished) {
            this.speed = 0;
        }
    }

    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getAngle() { return angle; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public boolean isCrashed() { return crashed; }
    public boolean isFinished() { return finished; }
    public double getSpeed() { return speed; }
    public double getMaxSpeed() { return maxSpeed; }
}