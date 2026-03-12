import java.util.ArrayList;
import java.util.List;

/**
 * A simple data container that collects statistics from the running simulation.
 * Each call to {@link #record(double, double, int, double)} appends a new entry
 * representing the state of the system at a given point in time. Recorded
 * quantities include the simulation time, the current speed of the car,
 * the number of collisions encountered so far and the total distance
 * travelled.
 */
public class SimulationStatistics {
    /**
     * Time values (in seconds) corresponding to each recorded entry.
     */
    private final List<Double> timeSeries = new ArrayList<>();
    /**
     * Car speed (units per second) sampled at each time point.
     */
    private final List<Double> speedSeries = new ArrayList<>();
    /**
     * Number of collisions encountered up to each time point.
     */
    private final List<Integer> collisionSeries = new ArrayList<>();
    /**
     * Cumulative distance travelled (same units as car position) at each time point.
     */
    private final List<Double> distanceSeries = new ArrayList<>();

    /**
     * Clears all recorded statistics. This method removes all entries from
     * the internal series so that a fresh simulation run starts without
     * carrying over data from a previous run. It is synchronised to ensure
     * thread safety when called from the UI while the simulation might
     * concurrently append to the statistics.
     */
    public synchronized void clear() {
        timeSeries.clear();
        speedSeries.clear();
        collisionSeries.clear();
        distanceSeries.clear();
    }

    /**
     * Appends a new entry into the statistics log. All series grow in lock-step,
     * so the nth element of each list corresponds to the same moment in time.
     *
     * @param time      simulation time in seconds
     * @param speed     current speed of the car
     * @param collisions total number of collisions encountered so far
     * @param distance  cumulative distance travelled since the start
     */
    public synchronized void record(double time, double speed, int collisions, double distance) {
        timeSeries.add(time);
        speedSeries.add(speed);
        collisionSeries.add(collisions);
        distanceSeries.add(distance);
    }

    /**
     * @return an immutable view of the recorded time stamps
     */
    public List<Double> getTimeSeries() {
        return List.copyOf(timeSeries);
    }

    /**
     * @return an immutable view of the recorded speed values
     */
    public List<Double> getSpeedSeries() {
        return List.copyOf(speedSeries);
    }

    /**
     * @return an immutable view of the recorded collision counts
     */
    public List<Integer> getCollisionSeries() {
        return List.copyOf(collisionSeries);
    }

    /**
     * @return an immutable view of the recorded cumulative distances
     */
    public List<Double> getDistanceSeries() {
        return List.copyOf(distanceSeries);
    }
}