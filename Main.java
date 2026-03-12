import javax.swing.JFrame;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Use the event dispatch thread for all UI creation to avoid subtle race conditions
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Autonomous Car Simulation");
            Environment environment = new Environment();
            SimulationPanel panel = new SimulationPanel(environment);

            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);

            // Control panel with buttons and selection drop-down
            JPanel controls = new JPanel();
            // Graph controls
            JButton showGraphs = new JButton("Show Graphs");
            JButton exportGraphs = new JButton("Export Graphs");
            controls.add(showGraphs);
            controls.add(exportGraphs);

            // Map selection
            JLabel mapLabel = new JLabel("Map:");
            String[] mapOptions = Environment.getMapNames();
            javax.swing.JComboBox<String> mapSelector = new javax.swing.JComboBox<>(mapOptions);
            mapSelector.setSelectedIndex(0);
            controls.add(mapLabel);
            controls.add(mapSelector);

            // Map editing controls
            JButton toggleEdit = new JButton("Enable Edit Mode");
            JButton clearObstacles = new JButton("Clear Obstacles");
            JLabel widthLabel = new JLabel("W:");
            JTextField widthField = new JTextField("40", 4);
            JLabel heightLabel = new JLabel("H:");
            JTextField heightField = new JTextField("40", 4);
            controls.add(toggleEdit);
            controls.add(clearObstacles);
            controls.add(widthLabel);
            controls.add(widthField);
            controls.add(heightLabel);
            controls.add(heightField);
            frame.add(controls, BorderLayout.SOUTH);

            // Document listener to update obstacle size as the user types
            DocumentListener sizeListener = new DocumentListener() {
                private void updateSize() {
                    try {
                        double w = Double.parseDouble(widthField.getText());
                        double h = Double.parseDouble(heightField.getText());
                        panel.setObstacleSize(w, h);
                    } catch (NumberFormatException ex) {
                        // ignore invalid input
                    }
                }
                @Override
                public void insertUpdate(DocumentEvent e) { updateSize(); }
                @Override
                public void removeUpdate(DocumentEvent e) { updateSize(); }
                @Override
                public void changedUpdate(DocumentEvent e) { updateSize(); }
            };
            widthField.getDocument().addDocumentListener(sizeListener);
            heightField.getDocument().addDocumentListener(sizeListener);

            // Toggle edit mode
            toggleEdit.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean newState = !panel.isEditMode();
                    panel.setEditMode(newState);
                    toggleEdit.setText(newState ? "Disable Edit Mode" : "Enable Edit Mode");
                }
            });

            // Clear user-added obstacles
            clearObstacles.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    environment.clearUserObstacles();
                    panel.repaint();
                }
            });

            // Change map when the user selects a different option. Resetting the
            // environment will reposition the car and reload obstacles for
            // the selected layout. After switching maps we repaint the panel
            // to reflect the new configuration.
            mapSelector.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int idx = mapSelector.getSelectedIndex();
                    environment.setMap(idx);
                    // Reset edit mode and update button text
                    panel.setEditMode(false);
                    toggleEdit.setText("Enable Edit Mode");
                    panel.repaint();
                }
            });

            // Define button actions
            showGraphs.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Open a separate window containing the graphs. Each call will
                    // instantiate a fresh GraphWindow using the latest statistics.
                    GraphWindow window = new GraphWindow(environment.getStats());
                    window.setVisible(true);
                }
            });
            exportGraphs.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        // Write files into the "graphs" directory. A message box
                        // will inform the user of success or failure.
                        GraphGenerator.exportAllGraphs(environment.getStats(), new java.io.File("graphs"));
                        javax.swing.JOptionPane.showMessageDialog(frame,
                                "Graphs exported successfully to the 'graphs' folder.",
                                "Export Complete", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        javax.swing.JOptionPane.showMessageDialog(frame,
                                "Error exporting graphs: " + ex.getMessage(),
                                "Export Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            frame.setSize(800, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Main simulation loop
            Timer timer = new Timer(16, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    environment.simulationStep();
                    panel.repaint();
                }
            });
            timer.start();
        });
    }
}