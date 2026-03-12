import java.util.List;

/**
 * Represents the car's sensor system.
 * This class is responsible for calculating the distance to obstacles from the car.
 */
public class FrontSensor {

    /**
     * Calculates the distance from a sensor to the nearest obstacle.
     * This version is capable of detecting rotated rectangular obstacles.
     *
     * @param startX The sensor's starting X coordinate.
     * @param startY The sensor's starting Y coordinate.
     * @param carAngle The car's current angle in degrees.
     * @param obstacles A list of all obstacles in the environment.
     * @param sensorAngle The angle of the sensor relative to the car (in degrees).
     * @param maxRange The maximum distance the sensor can see.
     * @return The distance to the nearest obstacle, or maxRange if no obstacle is within range.
     */
    public double distanceToObstacle(double startX, double startY, double carAngle, List<Obstacle> obstacles, double sensorAngle, double maxRange) {
        
        // Calculate the absolute world angle of the sensor ray
        double totalAngle = carAngle + sensorAngle;
        double angleRad = Math.toRadians(totalAngle);

        // Calculate the end point of the sensor ray at its maximum range
        double endX = startX + maxRange * Math.cos(angleRad);
        double endY = startY + maxRange * Math.sin(angleRad);

        double minDistance = maxRange;

        // Check for intersection with every obstacle
        for (Obstacle obs : obstacles) {
            // Get the 4 corners of the (potentially rotated) obstacle
            Point[] corners = getObstacleCorners(obs);

            // Check for intersection with each of the 4 edges of the obstacle
            for (int i = 0; i < 4; i++) {
                Point p1 = corners[i];
                Point p2 = corners[(i + 1) % 4]; // Next corner, wrapping around from 3 to 0

                // Find the intersection point of the sensor ray and the obstacle edge
                Point intersection = getLineIntersection(startX, startY, endX, endY, p1.x, p1.y, p2.x, p2.y);

                if (intersection != null) {
                    // If an intersection exists, calculate its distance from the sensor
                    double dx = intersection.x - startX;
                    double dy = intersection.y - startY;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    
                    // Keep track of the closest obstacle found so far
                    if (distance < minDistance) {
                        minDistance = distance;
                    }
                }
            }
        }
        return minDistance;
    }

    /**
     * Calculates the four corner points of an obstacle in world coordinates.
     * @param obs The obstacle.
     * @return An array of 4 Point objects representing the corners.
     */
    private Point[] getObstacleCorners(Obstacle obs) {
        Point[] corners = new Point[4];
        double angleRad = Math.toRadians(obs.getAngle());
        double cosA = Math.cos(angleRad);
        double sinA = Math.sin(angleRad);
        double halfWidth = obs.getWidth() / 2.0;
        double halfHeight = obs.getHeight() / 2.0;

        // Local corner coordinates (before rotation and translation)
        double[] localX = {-halfWidth, halfWidth, halfWidth, -halfWidth};
        double[] localY = {-halfHeight, -halfHeight, halfHeight, halfHeight};

        for (int i = 0; i < 4; i++) {
            // Rotate the corner
            double rotatedX = localX[i] * cosA - localY[i] * sinA;
            double rotatedY = localX[i] * sinA + localY[i] * cosA;
            
            // Translate the corner to the obstacle's world position
            corners[i] = new Point(obs.getX() + rotatedX, obs.getY() + rotatedY);
        }
        return corners;
    }

    /**
     * Finds the intersection point of two line segments.
     * Returns null if they do not intersect or are parallel.
     * @param x1 Start X of line 1
     * @param y1 Start Y of line 1
     * @param x2 End X of line 1
     * @param y2 End Y of line 1
     * @param x3 Start X of line 2
     * @param y3 Start Y of line 2
     * @param x4 End X of line 2
     * @param y4 End Y of line 2
     * @return The intersection Point, or null.
     */
    private Point getLineIntersection(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        double den = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (den == 0) {
            return null; // Lines are parallel
        }

        double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / den;
        double u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / den;

        // If 0 <= t <= 1 and 0 <= u <= 1, the segments intersect
        if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
            return new Point(x1 + t * (x2 - x1), y1 + t * (y2 - y1));
        }

        return null; // Segments do not intersect
    }

    /**
     * A simple helper class to store a 2D point.
     */
    private static class Point {
        final double x, y;
        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}