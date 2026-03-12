import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

/**
 * A simple window containing three charts generated from the simulation
 * statistics. It also exposes a button that allows the user to export all
 * graphs to disk in both PNG and PDF formats. The layout employs a
 * {@link GridLayout} to vertically stack the individual graph panels.
 */
public class GraphWindow extends JFrame {
    private final SimulationStatistics stats;

    /**
     * Constructs a new window to visualise the supplied statistics.
     *
     * @param stats simulation data from which the charts will be derived
     */
    public GraphWindow(SimulationStatistics stats) {
        super("Simulation Statistics");
        this.stats = stats;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(640, 900);
        setLayout(new BorderLayout());

        // Create graph panels
        GraphPanel speedPanel = new GraphPanel(
            stats.getTimeSeries(), stats.getSpeedSeries(),
            "Speed vs. Time", "Time (s)", "Speed"
        );
        GraphPanel collisionsPanel = new GraphPanel(
            stats.getTimeSeries(), GraphGenerator.toDoubleList(stats.getCollisionSeries()),
            "Collisions vs. Time", "Time (s)", "Collisions"
        );
        GraphPanel distancePanel = new GraphPanel(
            stats.getTimeSeries(), stats.getDistanceSeries(),
            "Distance vs. Time", "Time (s)", "Distance"
        );

        JPanel graphContainer = new JPanel(new GridLayout(3, 1));
        graphContainer.add(speedPanel);
        graphContainer.add(collisionsPanel);
        graphContainer.add(distancePanel);

        JScrollPane scrollPane = new JScrollPane(graphContainer);
        add(scrollPane, BorderLayout.CENTER);

        // Export button
        JButton exportButton = new JButton("Export Graphs");
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Export to a folder named "graphs" in the working directory
                    GraphGenerator.exportAllGraphs(stats, new File("graphs"));
                    JOptionPane.showMessageDialog(GraphWindow.this,
                            "Graphs exported successfully to the 'graphs' folder.",
                            "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(GraphWindow.this,
                            "Error exporting graphs: " + ex.getMessage(),
                            "Export Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(exportButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }
}