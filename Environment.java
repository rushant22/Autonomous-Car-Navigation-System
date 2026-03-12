import java.util.ArrayList;
import java.util.List;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;
import java.awt.geom.AffineTransform;

public class Environment {
    private Car car;
    private List<Obstacle> obstacles;
    private FrontSensor sensor;
    private FinishPoint finishPoint;

    private final int WORLD_WIDTH = 800;
    private final int WORLD_HEIGHT = 600;
    
    private final double SAFETY_DISTANCE = 80.0;
    private final double MAX_SENSOR_RANGE = 300.0;

    // For PD controller
    private double previousError = 0.0;

    // --- Simulation statistics ---
    /**
     * Data collector for time, speed, collisions and distance. This is
     * initialised in the constructor and populated on every call to
     * {@link #simulationStep()}.
     */
    private final SimulationStatistics stats;
    // Elapsed simulation time in seconds
    private double timeElapsed = 0.0;
    // Cumulative distance travelled
    private double totalDistance = 0.0;
    // Previous position used to calculate incremental distances
    private double prevX;
    private double prevY;
    // Number of collision events that have occurred so far
    private int collisionCount = 0;
    // Tracks if the car was crashed during the previous step to avoid double counting
    private boolean lastCrashed = false;

    /**
     * Index of the currently selected map. Map zero corresponds to the
     * original environment provided with the assignment. Higher indices
     * correspond to the additional example maps defined in {@link #setMap(int)}.
     */
    private int currentMap = 0;

    /**
     * Names of the available maps. These strings are displayed in the UI
     * allowing the user to choose a different road layout. The indices of
     * this array correspond to the values accepted by {@link #setMap(int)}.
     */
    private static final String[] MAP_NAMES = {
        "Original",
        "S-Bend Chicane",
        "Bottleneck",
        "Real Maze",
        "Zig-Zag Corridor",
        "Narrow Bridge",
        "Mini Maze",
        "Diagonal Alley",
        "Stepped Zigzag"// Add this new map name
    };

    public Environment() {
        this.car = new Car(50, 500); // Start at bottom left
        this.sensor = new FrontSensor();
        this.obstacles = new ArrayList<>();
        this.finishPoint = new FinishPoint(750, 100, 20); // End at top right

        loadMap();

        // Initialise statistics after all components are created. We set prevX
        // and prevY to the car's starting location so that the first distance
        // measurement is sensible.
        this.stats = new SimulationStatistics();
        this.prevX = this.car.getX();
        this.prevY = this.car.getY();
    }

 private void loadMap() {
    // --- Add World Boundaries (these are used in all maps) ---
    int wallThickness = 10;
    obstacles.add(new Obstacle(WORLD_WIDTH / 2.0, wallThickness / 2.0, WORLD_WIDTH, wallThickness)); // Top
    obstacles.add(new Obstacle(WORLD_WIDTH / 2.0, WORLD_HEIGHT - wallThickness / 2.0, WORLD_WIDTH, wallThickness)); // Bottom
    obstacles.add(new Obstacle(wallThickness / 2.0, WORLD_HEIGHT / 2.0, wallThickness, WORLD_HEIGHT)); // Left
    obstacles.add(new Obstacle(WORLD_WIDTH - wallThickness / 2.0, WORLD_HEIGHT / 2.0, wallThickness, WORLD_HEIGHT)); // Right

    // --- Original Map (for reference) ---
    obstacles.add(new Obstacle(300, 350, 20, 500));
    obstacles.add(new Obstacle(550, 200, 500, 20));
}
    

    public void simulationStep() {
        // Duration of each simulation step in seconds. The Swing Timer in
        // Main.java updates roughly every 16 milliseconds; dividing by 1000
        // yields approximately 0.016 seconds per tick. Feel free to adjust
        // this constant to match the desired simulation speed.
        final double dt = 0.016;

        // If the car has crashed or finished we still record time and state
        // but do not attempt to update its position or make decisions.
        if (car.isCrashed() || car.isFinished()) {
            timeElapsed += dt;
            // Record the current (stationary) state
            stats.record(timeElapsed, car.getSpeed(), collisionCount, totalDistance);
            return;
        }

        // --- 1. SENSE ---
        double[] sensorAngles = {-90, -60, -30, -10, 0, 10, 30, 60, 90};
        double[] sensorDistances = new double[sensorAngles.length];

        double angleRad = Math.toRadians(car.getAngle());
        double cosA = Math.cos(angleRad);
        double sinA = Math.sin(angleRad);
        double carFrontX = car.getWidth() / 2.0;
        double carSideY = car.getHeight() / 2.0;
        double flX = car.getX() + carFrontX * cosA - (-carSideY) * sinA;
        double flY = car.getY() + carFrontX * sinA + (-carSideY) * cosA;
        double frX = car.getX() + carFrontX * cosA - carSideY * sinA;
        double frY = car.getY() + carFrontX * sinA + carSideY * cosA;

        for (int i = 0; i < sensorAngles.length; i++) {
            double distLeft = sensor.distanceToObstacle(flX, flY, car.getAngle(), obstacles, sensorAngles[i], MAX_SENSOR_RANGE);
            double distRight = sensor.distanceToObstacle(frX, frY, car.getAngle(), obstacles, sensorAngles[i], MAX_SENSOR_RANGE);
            sensorDistances[i] = Math.min(distLeft, distRight);
        }

        // --- 2. DECIDE (Potential Fields Method) ---
        double goalAngle = Math.toDegrees(Math.atan2(finishPoint.getY() - car.getY(), finishPoint.getX() - car.getX()));
        double carAngle = car.getAngle();

        // These will store the combined "push" and "pull" forces
        double vectorX = 0.0;
        double vectorY = 0.0;

        // --- A. Calculate Repulsive Forces from Obstacles ---
        for (int i = 0; i < sensorAngles.length; i++) {
            double distance = sensorDistances[i];
    
        // If an obstacle is too close, create a "push" away from it
        if (distance < SAFETY_DISTANCE) {
            // The closer the obstacle, the stronger the push
            double repulsionStrength = (SAFETY_DISTANCE - distance) / SAFETY_DISTANCE;
            
            // The vector of the push is opposite to the sensor's direction
            double sensorWorldAngle = carAngle + sensorAngles[i];
            vectorX -= repulsionStrength * Math.cos(Math.toRadians(sensorWorldAngle));
            vectorY -= repulsionStrength * Math.sin(Math.toRadians(sensorWorldAngle));
        }
}

        // --- B. Calculate Attractive Force from the Goal ---
        // This is a constant "pull" towards the finish line
        vectorX += 1.0 * Math.cos(Math.toRadians(goalAngle));
        vectorY += 1.0 * Math.sin(Math.toRadians(goalAngle));

        // --- C. Determine Final Steering Angle ---
        // Find the angle of the combined vector of all pushes and pulls
        double desiredAngle = Math.toDegrees(Math.atan2(vectorY, vectorX));

        // Calculate the difference between where the car is pointing and where it wants to point
        double angleError = desiredAngle - carAngle;

        // Normalize the error to be between -180 and 180
        while (angleError > 180) angleError -= 360;
        while (angleError < -180) angleError += 360;

        // Now, the rest of your ACT logic can use this `angleError`    
        
        // --- 3. ACT (Target-Speed Logic) ---

        // Use our new `angleError` with the PD controller for steering
        double proportional = 0.4 * angleError;
        double derivative = 0.9 * (angleError - previousError);
        car.steer(proportional + derivative);
        previousError = angleError; // Update the error for the next step

        // --- Corrected Speed Control Logic ---

        // Set a target speed based on how clear the path is and how sharp the turn needs to be.
        double targetSpeed;
        // We'll use the forward-facing sensor (index 4) to judge the distance ahead.
        double chosenPathDistance = sensorDistances[4]; 

        // Instead of `bestAngle`, we now use `angleError` to check if it's a sharp turn.
        if (chosenPathDistance < 100 || Math.abs(angleError) > 45) {
            targetSpeed = 1.5; // Creep speed for tight spots and sharp turns
        } else if (chosenPathDistance < 200) {
            targetSpeed = 3.0; // Medium speed
        } else {
            targetSpeed = car.getMaxSpeed(); // Full speed ahead
        }

        // Gently accelerate or brake to match the target speed
        if (car.getSpeed() < targetSpeed) {
            car.accelerate(0.1);
        } else if (car.getSpeed() > targetSpeed) {
            car.brake(0.3);
        }

        // --- 4. UPDATE ---
        // Preserve the car's current position so we can compute the travelled distance
        double oldX = car.getX();
        double oldY = car.getY();
        // Update the car's physical location
        car.updatePosition();
        // Calculate distance moved during this time step
        double dx = car.getX() - oldX;
        double dy = car.getY() - oldY;
        double stepDist = Math.sqrt(dx * dx + dy * dy);
        totalDistance += stepDist;
        prevX = car.getX();
        prevY = car.getY();

        // Collision detection
        checkCollisions();
        // If a new collision occurred since the last update, increment the count
        if (!lastCrashed && car.isCrashed()) {
            collisionCount++;
        }
        lastCrashed = car.isCrashed();
        // Check finish line condition
        checkFinished();

        // Advance simulation time and record statistics
        timeElapsed += dt;
        stats.record(timeElapsed, car.getSpeed(), collisionCount, totalDistance);
    }
    
    private void checkFinished() {
        double dx = car.getX() - finishPoint.getX();
        double dy = car.getY() - finishPoint.getY();
        double distance = Math.sqrt(dx*dx + dy*dy);

        if (distance < finishPoint.getRadius()) {
            car.setFinished(true);
        }
    }

// REPLACEMENT CODE for the checkCollisions() method

private void checkCollisions() {
    // First, create the transformed Area for the car (this part is unchanged)
    Rectangle2D.Double carRect = new Rectangle2D.Double(-car.getWidth() / 2, -car.getHeight() / 2, car.getWidth(), car.getHeight());
    AffineTransform carTransform = new AffineTransform();
    carTransform.translate(car.getX(), car.getY());
    carTransform.rotate(Math.toRadians(car.getAngle()));
    Area carArea = new Area(carRect).createTransformedArea(carTransform);

    // Now, loop through obstacles and create a transformed Area for each one
    for (Obstacle obs : obstacles) {
        // Create a base rectangle for the obstacle, centered at (0,0)
        Rectangle2D.Double obsRect = new Rectangle2D.Double(
            -obs.getWidth() / 2, 
            -obs.getHeight() / 2, 
            obs.getWidth(), 
            obs.getHeight()
        );

        // Create a transform for the obstacle's position and rotation
        AffineTransform obsTransform = new AffineTransform();
        obsTransform.translate(obs.getX(), obs.getY());
        obsTransform.rotate(Math.toRadians(obs.getAngle())); // Use the obstacle's angle

        // Apply the transform to create the final rotated shape
        Area obsArea = new Area(obsRect).createTransformedArea(obsTransform);
        
        // The intersection logic remains the same
        obsArea.intersect(carArea);
        if (!obsArea.isEmpty()) {
            car.setCrashed(true);
            break; // Exit the loop once a collision is detected
        }
    }
}

    // Getters for the panel
    public Car getCar() { return car; }
    public List<Obstacle> getObstacles() { return obstacles; }
    public FinishPoint getFinishPoint() { return finishPoint; }

    /**
     * Exposes the collected simulation statistics. External components such
     * as the graph window can call this method to retrieve the current
     * dataset without modifying it.
     *
     * @return simulation statistics
     */
    public SimulationStatistics getStats() {
        return stats;
    }

    /**
     * Returns the names of the predefined maps available for selection. The
     * returned array is a clone of the internal list to prevent callers
     * modifying the underlying static data. The index of each element
     * corresponds to the map index accepted by {@link #setMap(int)}.
     *
     * @return a copy of the available map names
     */
    public static String[] getMapNames() {
        return MAP_NAMES.clone();
    }

    /**
     * Resets the environment to the selected map. This method clears any
     * existing obstacles (including user-added obstacles), repositions
     * the car and finish point to their starting locations, resets
     * controller state and simulation statistics, and then populates the
     * environment with the static obstacles for the given map. Index zero
     * loads the original map defined by {@link #loadMap()}, while other
     * indices load alternative layouts such as the chicane or bottleneck.
     *
     * @param index the index of the map to load; must be within the range
     *              returned by {@link #getMapNames()}
     */
    public void setMap(int index) {
        if (index < 0 || index >= MAP_NAMES.length) {
            throw new IllegalArgumentException("Invalid map index: " + index);
        }
        this.currentMap = index;
        // Reset car and sensors
        this.car = new Car(50, 500);
        this.sensor = new FrontSensor();
        this.finishPoint = new FinishPoint(750, 100, 20);
        this.previousError = 0.0;
        // Reset simulation state
        this.timeElapsed = 0.0;
        this.totalDistance = 0.0;
        this.collisionCount = 0;
        this.lastCrashed = false;
        // Clear statistics and update previous position
        this.stats.clear();
        this.prevX = this.car.getX();
        this.prevY = this.car.getY();
        // Remove all obstacles (including user added) and add walls and map-specific elements
        this.obstacles.clear();
        int wallThickness = 10;
        // World boundaries
        obstacles.add(new Obstacle(WORLD_WIDTH / 2.0, wallThickness / 2.0, WORLD_WIDTH, wallThickness)); // Top
        obstacles.add(new Obstacle(WORLD_WIDTH / 2.0, WORLD_HEIGHT - wallThickness / 2.0, WORLD_WIDTH, wallThickness)); // Bottom
        obstacles.add(new Obstacle(wallThickness / 2.0, WORLD_HEIGHT / 2.0, wallThickness, WORLD_HEIGHT)); // Left
        obstacles.add(new Obstacle(WORLD_WIDTH - wallThickness / 2.0, WORLD_HEIGHT / 2.0, wallThickness, WORLD_HEIGHT)); // Right
        // Add map-specific obstacles
        switch (index) {
            case 1:
                // S-Bend Chicane
                obstacles.add(new Obstacle(250, 450, 20, 300));
                obstacles.add(new Obstacle(550, 150, 20, 300));
                break;
            case 2:
                // Bottleneck
                obstacles.add(new Obstacle(400, 200, 700, 20)); // Top wall of gap
                obstacles.add(new Obstacle(400, 400, 300, 20)); // Bottom wall of gap
                break;
            case 3:
                // Real Maze
                obstacles.add(new Obstacle(300, 400, 20, 400));
                obstacles.add(new Obstacle(550, 400, 500, 20));
                break;
            case 4:
                // Zig-Zag Corridor
                // Start at the bottom, path is on the left side
                obstacles.add(new Obstacle(600, 600, 20, 200));

                // This wall is far to the left, forcing a sharp left turn
                obstacles.add(new Obstacle(200, 400, 20, 300));

                // This wall is back on the right, forcing a sharp right turn
                obstacles.add(new Obstacle(600, 200, 20, 300));

                // This wall is back on the left, forcing a final left turn to the exit
                obstacles.add(new Obstacle(200, 0, 20, 300));
                break;
            case 5:
                // Narrow Bridge
                obstacles.add(new Obstacle(200, 200, 500, 20));
                obstacles.add(new Obstacle(200, 400, 500, 20));
                obstacles.add(new Obstacle(200, 200, 20, 200));
                obstacles.add(new Obstacle(680, 200, 20, 200));
                break;
            case 6:
                // Mini Maze / Box Maze
                // --- Set the correct Start and Finish points for this maze ---
                this.car = new Car(50, 300); // Start at the middle-left entrance
                this.finishPoint = new FinishPoint(650, 550, 20); // Finish at the bottom-right exit

                // --- Obstacles manually placed to match your drawing ---
                int wall = 20; // A consistent wall thickness

                // --- Outer Frame with Gaps ---
                obstacles.add(new Obstacle(400, 100, 650, wall, 0, false)); // Top wall (Full)
                obstacles.add(new Obstacle(700, 300, wall, 400, 0, false)); // Right wall (Full)
                
                // Left wall (in two pieces to create an entrance gap)
                obstacles.add(new Obstacle(100, 187.5, wall, 175, 0, false)); // Top-half of left wall
                obstacles.add(new Obstacle(100, 412.5, wall, 175, 0, false)); // Bottom-half of left wall

                // Bottom wall (in two pieces to create an exit gap)
                obstacles.add(new Obstacle(350, 500, 550, wall, 0, false)); // Left-part of bottom wall
                obstacles.add(new Obstacle(700, 500, 50, wall, 0, false)); // Right-part of bottom wall


                // --- Inner Walls (Unchanged) ---
                obstacles.add(new Obstacle(200, 250, wall, 300, 0, false));
                obstacles.add(new Obstacle(250, 200, 100, wall, 0, false));
                obstacles.add(new Obstacle(300, 250, wall, 100, 0, false));
                obstacles.add(new Obstacle(500, 175, 200, wall, 0, false));
                obstacles.add(new Obstacle(600, 250, wall, 150, 0, false));
                obstacles.add(new Obstacle(450, 300, 300, wall, 0, false));
                obstacles.add(new Obstacle(250, 400, 300, wall, 0, false));
                obstacles.add(new Obstacle(400, 425, wall, 50, 0, false));
                obstacles.add(new Obstacle(500, 425, wall, 150, 0, false));
                obstacles.add(new Obstacle(550, 350, 100, wall, 0, false));
                obstacles.add(new Obstacle(600, 450, wall, 100, 0, false));
                break;

            case 7:
                // "Diagonal Alley"
                // A wall rotated by -45 degrees
                obstacles.add(new Obstacle(250, 450, 300, 20, -45, false));
                
                // Another wall rotated by 45 degrees
                obstacles.add(new Obstacle(550, 200, 300, 20, 45, false));
                break;    
            case 8: // Custom Maze (Based on your hand-drawn image)
                // --- Set the correct Start and Finish points from the drawing ---
                this.car = new Car(50, 50); // Start at top-left
                this.finishPoint = new FinishPoint(750, 550, 20); // Finish at bottom-right

                // --- These obstacles were manually placed to match your drawing ---
                wall = 20; // Wall thickness

                // Top-left section
                obstacles.add(new Obstacle(200, 100, 350, wall, 0, false));
                obstacles.add(new Obstacle(380, 200, wall, 200, 0, false));

                // Mid-left section
                obstacles.add(new Obstacle(250, 300, 250, wall, 0, false));
                obstacles.add(new Obstacle(120, 400, wall, 200, 0, false));

                // Bottom-left section
                obstacles.add(new Obstacle(320, 500, 400, wall, 0, false));

                // Top-right section
                obstacles.add(new Obstacle(580, 200, wall, 200, 0, false));
                obstacles.add(new Obstacle(650, 300, 300, wall, 0, false));

                // Mid-right section
                obstacles.add(new Obstacle(500, 400, wall, 200, 0, false));
                obstacles.add(new Obstacle(650, 400, 300, wall, 0, false));

                // Bottom-right section
                obstacles.add(new Obstacle(700, 500, wall, 200, 0, false));
                break;
            default:
                // Original map: simple plus shape
                obstacles.add(new Obstacle(300, 350, 20, 500));
                obstacles.add(new Obstacle(550, 200, 500, 20));
                break;
        }
    }

    /**
     * Inserts a new user-defined obstacle into the environment. The new
     * obstacle is created with its removable flag set to {@code true} so
     * that it can later be cleared or modified by the user. The centre
     * coordinates (x, y) correspond to the simulation panel's coordinate
     * system.
     *
     * @param x      x coordinate of the obstacle centre
     * @param y      y coordinate of the obstacle centre
     * @param width  width of the obstacle
     * @param height height of the obstacle
     */
    public void addUserObstacle(double x, double y, double width, double height) {
        Obstacle obs = new Obstacle(x, y, width, height, true);
        obstacles.add(obs);
    }

    /**
     * Removes a user-defined obstacle located at the provided coordinates. If
     * multiple removable obstacles overlap the point, only the first match
     * (nearest in the list) will be removed. Non-removable obstacles, such
     * as boundaries, are never removed.
     *
     * @param px x coordinate of the point to test
     * @param py y coordinate of the point to test
     */
    public void removeObstacleAtPoint(double px, double py) {
        Obstacle target = null;
        for (Obstacle obs : obstacles) {
            if (!obs.isRemovable()) continue;
            double left = obs.getX() - obs.getWidth() / 2.0;
            double right = obs.getX() + obs.getWidth() / 2.0;
            double top = obs.getY() - obs.getHeight() / 2.0;
            double bottom = obs.getY() + obs.getHeight() / 2.0;
            if (px >= left && px <= right && py >= top && py <= bottom) {
                target = obs;
                break;
            }
        }
        if (target != null) {
            obstacles.remove(target);
        }
    }

    /**
     * Removes all user-defined obstacles from the environment. Boundaries and
     * built-in obstacles remain untouched. Use this to reset the map to its
     * original state without clearing the simulation statistics.
     */
    public void clearUserObstacles() {
        obstacles.removeIf(Obstacle::isRemovable);
    }
}