import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;

class Chart {

    private final static Font fontLabel = new Font("Montserrat", Font.PLAIN,18);
    private final static Font fontMarker = new Font("Montserrat", Font.PLAIN,12);
    private static XYSeriesCollection data_set = new XYSeriesCollection();
    static XYSeriesCollection secondData_set = new XYSeriesCollection();
    static XYSeries series = new XYSeries("pH");
    static XYSeries secondSeries = new XYSeries("dpH/dV");
    static XYSeries dot = new XYSeries("Точка эквивалентности");
    private final static Double[] Y_LEFT_AXIS = {0.0,14.0};
    private final static Double[] Y_RIGHT_AXIS = {0.0,3.0};
    private final static Double[] X_AXIS = {0.0,100.0};

    Chart () {
        JFrame chartFrame = new JFrame("График титрования");
        data_set.addSeries(series);
        secondData_set.addSeries(secondSeries);
        data_set.addSeries(dot);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "График титрования",
                "Объем добавленного титранта",
                "рН",
                null,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        final XYPlot plot = chart.getXYPlot();

        /*  Creating render objects  */
        XYSplineRenderer renderer1 = new XYSplineRenderer();
        XYSplineRenderer renderer2 = new XYSplineRenderer();

        renderer1.setPrecision(7);
        renderer1.setSeriesShapesVisible(0, false);
        renderer1.setSeriesPaint(0, Color.orange);
        renderer1.setSeriesStroke(0, new BasicStroke(2.5f));
        renderer2.setPrecision(7);
        renderer2.setSeriesShapesVisible(0, false);
        renderer2.setSeriesPaint(0, Color.WHITE);
        renderer2.setSeriesStroke(0, new BasicStroke(2.5f));

        plot.setRenderer(0, renderer1);
        plot.setRenderer(1, renderer2);
        /*  Creating render objects  */

        plot.setDataset(0, data_set);
        plot.setDataset(1, secondData_set);


        final NumberAxis axis2 = new NumberAxis("dpH/dV");
        final ValueAxis axis = plot.getDomainAxis();
        final ValueAxis axis1 = plot.getRangeAxis();

        /* start styling Label and tickLabel for axis */
        chart.getTitle().setPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("Montserrat", Font.PLAIN, 20));

        axis.setAutoRange(true);
        axis.setLabelFont(fontLabel);
        axis.setTickLabelFont(fontMarker);
        axis.setLabelPaint(Color.WHITE);
        axis.setTickLabelPaint(Color.WHITE);

        axis1.setLabelFont(fontLabel);
        axis1.setTickLabelFont(fontMarker);
        axis1.setLabelPaint(Color.WHITE);
        axis1.setTickLabelPaint(Color.WHITE);

        axis2.setLabelPaint(Color.WHITE);
        axis2.setTickLabelPaint(Color.WHITE);
        axis2.setAutoRangeIncludesZero(false);
        axis2.setLabelFont(fontLabel);
        axis2.setTickLabelFont(fontMarker);
        /* finish styling Label and tickLabel for axis */

        /* set range of axis */
        axis.setAutoRange(true);
        axis1.setRange(Y_LEFT_AXIS[0], Y_LEFT_AXIS[1]);
        axis2.setRange(Y_RIGHT_AXIS[0], Y_RIGHT_AXIS[1]);


        plot.setRangeAxis(1, axis2);
        plot.mapDatasetToRangeAxis(1, 1);
        plot.setBackgroundPaint(new Color(19, 28, 48));
        chart.setBackgroundPaint(new Color(19, 28, 48));
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.WHITE);
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.WHITE);


        ChartPanel graph = new ChartPanel(chart);
        graph.setPreferredSize(new Dimension(500, 500));

        chartFrame.add(graph);
        chartFrame.pack();
        chartFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        chartFrame.setLocationRelativeTo(null);
        chartFrame.setVisible(true);
    }
}










