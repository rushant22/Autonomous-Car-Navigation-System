import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * A reusable Swing component that renders a simple line chart using the
 * {@link GraphGenerator}. The panel holds references to the data it should
 * display as well as descriptive labels. Whenever the underlying data is
 * updated a call to {@link #repaint()} will cause a fresh chart to be drawn.
 */
public class GraphPanel extends JPanel {
    private List<? extends Number> x;
    private List<? extends Number> y;
    private final String title;
    private final String xLabel;
    private final String yLabel;

    /**
     * Constructs a new panel configured to render a line chart.
     *
     * @param xData   x-axis values (e.g. time)
     * @param yData   y-axis values (e.g. speed)
     * @param title   chart title
     * @param xLabel  label for the horizontal axis
     * @param yLabel  label for the vertical axis
     */
    public GraphPanel(List<? extends Number> xData, List<? extends Number> yData,
                      String title, String xLabel, String yLabel) {
        this.x = xData;
        this.y = yData;
        this.title = title;
        this.xLabel = xLabel;
        this.yLabel = yLabel;
        // Ensures the panel will repaint at an appropriate time
        setDoubleBuffered(true);
    }

    /**
     * Updates the data displayed by this panel. After calling this method,
     * invoke {@link #repaint()} to refresh the graph.
     *
     * @param xData new x values
     * @param yData new y values
     */
    public synchronized void updateData(List<? extends Number> xData, List<? extends Number> yData) {
        this.x = xData;
        this.y = yData;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        BufferedImage img = GraphGenerator.createLineChart(
            x,
            y,
            title,
            xLabel,
            yLabel,
            width,
            height
        );
        g.drawImage(img, 0, 0, this);
    }
}