import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.NoSuchElementException;
import java.util.Scanner;
import javax.swing.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


class Main {
    public static void main(String[] args) {
        Main.SetUpChartAndControllers();
    }

    private static XYSeriesCollection data_set = new XYSeriesCollection();
    private static XYSeriesCollection secondData_set = new XYSeriesCollection();
    private static XYSeries series = new XYSeries("pH");
    private static XYSeries secondSeries = new XYSeries("dpH/dV");
    private static XYSeries dot = new XYSeries("Точка эквивалентности");
    private final static Font fontLabel = new Font("Montserrat", Font.PLAIN,18);
    private final static Font fontMarker = new Font("Montserrat", Font.PLAIN,12);
    private final static Double[] Y_LEFT_AXIS = {0.0,14.0};
    private final static Double[] Y_RIGHT_AXIS = {0.0,3.0};
    private final static Double[] X_AXIS = {0.0,100.0};
    private static Main.ListeningThepH listeningThe_pH = new Main.ListeningThepH();
    private static Main.AutoTitration auto = new  Main.AutoTitration();
    private static boolean forScanner;

    // setting SCADA

    private static Layers mixerOn = new Layers("src/main/resources/mixerOn.gif",291,306,70,28);
    private static Layers mixerLayer = new Layers("src/main/resources/scadaMixerOff.png",125,115,400,300);

    private static Layers pumpForTitrationOn = new Layers("src/main/resources/Untitled-3.gif",137,16,50,50);
    private static Layers pumpForSolutionOn = new Layers("src/main/resources/Untitled-3.gif",137,230,50,50);
    private static Layers pumpForTitration = new Layers("src/main/resources/pump1Off.png",0,0,400,300);
    private static Layers pumpForSolution =new Layers("src/main/resources/pump2Off.png", -6,90,400,300);

    private static Layers waterTube = new Layers("resources/Water.png",4,72,140,150);

    private static Layers valveTitrationClosed = new Layers("src/main/resources/valveClosed.png",75,88,40,40);
    private static Layers valveTitrationOpened = new Layers("src/main/resources/valveOpened.png",75,88,40,40);
    private static Layers valveSolutionClosed = new Layers("src/main/resources/valveClosed.png",75,158,40,40);
    private static Layers valveSolutionOpened = new Layers("src/main/resources/valveOpened.png",75,158,40,40);
    private static Layers valveWaterClosed = new Layers("src/main/resources/valveClosed.png",35,122,40,40);
    private static Layers valveWaterOpened = new Layers("src/main/resources/valveOpened.png",35,122,40,40);

    private static Layers valveOutClosed = new Layers("src/main/resources/valveClosed.png",412,302,40,40);
    private static Layers valveOutOpened = new Layers("src/main/resources/valveOpened.png",412,302,40,40);



    static void SetUpChartAndControllers() {
        SetWindow.setUpWindow();
        SetWindow.frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("connection closed");
                InitialClass.arduino.closeConnection();
            }
        });
    }


    static class ListeningThepH implements Runnable  {
    public synchronized void run() {
        forScanner = true;
        System.out.println("ListeningThepH is loaded");
        SetWindow.log.append("Получаю данные...\n");
        Scanner scanner = new Scanner(InitialClass.arduino.getSerialPort().getInputStream());
        int i = 0;
        InitialClass.arduino.serialWrite('Y');
        String line2 = scanner.nextLine();
        System.out.println(line2);

        while(forScanner) {
           try {
                String line = scanner.nextLine();
                System.out.println(line);
                Double number = Double.parseDouble(line);
                series.add((double)i, (double)number);
                double y = (double) series.getY(series.getItemCount() - 1) - (double) series.getY(series.getItemCount() - 2);
                if(y < 0) y = -y;
                secondSeries.add((double)i, y);
            } catch (IndexOutOfBoundsException | NumberFormatException | NoSuchElementException e) {
                 e.getStackTrace();

             }
            i++;
        }
        scanner.close();
        InitialClass.arduino.serialWrite('X');
        SetWindow.log.append("Данные получены\n");
        System.out.println(secondSeries.getMaxY());
        double[] peak = findPeaks(secondData_set, 0);

        dot.add(series.getDataItem((int) peak[2]));
        SetWindow.result.setText(peak[0] +"ml");
    }

    }

    static class AutoTitration implements Runnable {
        @Override
        public void run() {
            try {
                boolean threadAlive = true;
                /*START adding the solution*/

                InitialClass.arduino.serialWrite('9');
                SetWindow.log.append("Клапан раствора открыт\n");
                valveSolutionOpened.setVisible(true);


                InitialClass.arduino.serialWrite('5');
                SetWindow.log.append("Добавление раствора...\n");
                pumpForSolutionOn.setVisible(true);

                Thread.sleep(4000); //Как будет известен расход поправить время закрытия клапана для того чтоб остаточую жидкость высосало.
                InitialClass.arduino.serialWrite('8');
                valveSolutionOpened.setVisible(false);
                SetWindow.log.append("Клапан раствора закрыт\n");

                Thread.sleep(2000);
                InitialClass.arduino.serialWrite('4');
                pumpForSolutionOn.setVisible(false);
                SetWindow.log.append("Раствор готов\n");

                /*END adding the solution*/

                    /*START adding the titration*/
                    //motor for mixer
                    SetWindow.log.append("Мешалка включена\n");
                    InitialClass.arduino.serialWrite('7');
                    mixerOn.setVisible(true);

                    SetWindow.log.append("Клапан титранта открыт\n");
                    InitialClass.arduino.serialWrite('B');
                    valveTitrationOpened.setVisible(true);

                    for (int i = 0; i<10; i++) {
                        //pump for titration
                        SetWindow.log.append(i + " Добавление титранта" + "\n");
                        InitialClass.arduino.serialWrite('3');
                        pumpForTitrationOn.setVisible(true);
                        if (threadAlive) {
                            new Thread(listeningThe_pH).start();
                            threadAlive = false;
                        }
                        Thread.sleep(1000);
                        InitialClass.arduino.serialWrite('2');
                        pumpForTitrationOn.setVisible(false);
                    }
                    InitialClass.arduino.serialWrite('A');
                    SetWindow.log.append("Клапан тиранта закрыт\n");
                    valveTitrationOpened.setVisible(false);

                    InitialClass.arduino.serialWrite('6');
                    SetWindow.log.append("Мешалка выключена\n");
                    mixerOn.setVisible(false);

                    forScanner = false;

                    /*END adding the titration*/

                SetWindow.log.append("Автоматическое измерение закончено\n");
            } catch (Exception newOne) {
                newOne.getStackTrace();
            }

        }
    }

    private static class Layers extends JLabel {
        private Layers(String filename, int x, int y, int width, int height) {
            Image image = Toolkit.getDefaultToolkit().createImage(filename);
            ImageIcon gif = new ImageIcon(image);
            gif.setImageObserver(this);
            setIcon(gif);
            setBounds(x,y,width,height);
            setVisible(true);
        }
    }

    private static class SetWindow {
        static JFrame frame = new JFrame("Titration");
        static JPanel panel = new JPanel();
        static JPanel panelRight = new JPanel();
        static JPanel panelRightTop = new JPanel();
        static JPanel panelRightBot = new JPanel();
        static JTextArea log = new JTextArea();
        static JLabel result = new JLabel();
        static JLayeredPane scada = new JLayeredPane();


        static private JToggleButton addToggle(String name, char whenOn, char whenOff, Layers on) {
            JToggleButton a = new JToggleButton(name);
            a.addItemListener(ev -> {
                if (ev.getStateChange() == ItemEvent.SELECTED) {
                    a.setText(name + " вкл");
                    System.out.println(name + " on");
                    InitialClass.arduino.serialWrite(whenOn);
                    log.append(name + " вкл\n");
                    on.setVisible(true);


                } else if (ev.getStateChange() == ItemEvent.DESELECTED) {
                    InitialClass.arduino.serialWrite(whenOff);
                    a.setText(name + " выкл");
                    System.out.println(name + " off");
                    log.append(name + " выкл\n");
                    on.setVisible(false);

                }
            });
            a.setPreferredSize(new Dimension(200, 30));
            return a;
        }

        static private JToggleButton addToggle(String name, char whenOn, char whenOff) {
            JToggleButton a = new JToggleButton(name);
            a.addItemListener(ev -> {
                if (ev.getStateChange() == ItemEvent.SELECTED) {
                    a.setText(name + " вкл");
                    System.out.println(name + " on");
                    InitialClass.arduino.serialWrite(whenOn);
                    log.append(name + " вкл\n");

                } else if (ev.getStateChange() == ItemEvent.DESELECTED) {
                    InitialClass.arduino.serialWrite(whenOff);
                    a.setText(name + " выкл");
                    System.out.println(name + " off");
                    log.append(name + " выкл\n");
                }
            });
            a.setPreferredSize(new Dimension(200, 30));
            return a;
        }

        private static void setUpWindow() {

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

            log.setFont(fontMarker);
            log.setPreferredSize(new Dimension(200, 750));
            log.setBackground(new Color(19, 28, 48));
            log.setForeground(Color.WHITE);

            JToggleButton titrationStart = new JToggleButton();
            titrationStart.setPreferredSize(new Dimension(200, 30));
            titrationStart.setText("Автоматическое измерение");
            titrationStart.addItemListener(ev -> {
                if (ev.getStateChange() == ItemEvent.SELECTED) {
                    series.clear();
                    secondSeries.clear();
                    dot.clear();
                    titrationStart.setText("В процессе");
                    new Thread(auto).start();
                    // changing place serialwrite

                } else if (ev.getStateChange() == ItemEvent.DESELECTED) {
                    forScanner = false;
                    InitialClass.arduino.serialWrite('2');
                    System.out.println(" off");
                    log.append("Завершение" + "\n");
                }
            });

            JButton washing = new JButton();
            washing.setPreferredSize(new Dimension(200, 30));
            washing.setText("Промывка");

            washing.addActionListener(e -> {
                washing.setText("В процессе");
                washing.setEnabled(false);
                try {
                    doWash();
                } catch (InterruptedException ea) {
                    ea.printStackTrace();
                }
                washing.setEnabled(true);
                washing.setText("Промывка");
            });

            JButton handMode = new JButton();
            handMode.setPreferredSize(new Dimension(200, 30));
            handMode.setText("Начать ручной режим");

            handMode.addActionListener(e -> {
                handMode.setText("Включен ручной режим !");
                series.clear();
                secondSeries.clear();
                dot.clear();
                titrationStart.setText("В процессе");
                forScanner = true;
                new Thread(listeningThe_pH).start();
            });


            ChartPanel graph = new ChartPanel(chart);
            graph.setPreferredSize(new Dimension(500, 500));


            pumpForSolutionOn.setVisible(false);
            pumpForTitrationOn.setVisible(false);
            mixerOn.setVisible(false);
            valveTitrationOpened.setVisible(false);
            valveSolutionOpened.setVisible(false);
            valveWaterOpened.setVisible(false);
            valveOutOpened.setVisible(false);

            JPanel flaskLevel = new JPanel();
            flaskLevel.setBounds(225, 200, 200, 200);
            flaskLevel.setBackground(Color.orange);

            scada.setPreferredSize(new Dimension(600, 400));

            scada.add(flaskLevel, Integer.valueOf(1));
            scada.add(mixerLayer, Integer.valueOf(2));
            scada.add(waterTube, Integer.valueOf(3));
            scada.add(valveTitrationClosed, Integer.valueOf(9));
            scada.add(valveTitrationOpened, Integer.valueOf(10));
            scada.add(valveSolutionClosed, Integer.valueOf(11));
            scada.add(valveSolutionOpened, Integer.valueOf(12));
            scada.add(valveOutClosed, Integer.valueOf(13));
            scada.add(valveOutOpened, Integer.valueOf(14));
            scada.add(valveWaterClosed, Integer.valueOf(15));
            scada.add(valveWaterOpened, Integer.valueOf(16));
            scada.add(mixerOn, Integer.valueOf(8));
            scada.add(pumpForTitrationOn, Integer.valueOf(6));
            scada.add(pumpForSolutionOn, Integer.valueOf(7));
            scada.add(pumpForTitration, Integer.valueOf(4));
            scada.add(pumpForSolution, Integer.valueOf(5));


            // setting SCADA

            result.setPreferredSize(new Dimension(200, 50));
            result.setText("0.00 ml");
            result.setFont(new Font("Montserrat", Font.PLAIN, 32));
            result.setForeground(Color.WHITE);

            panelRightTop.setPreferredSize(new Dimension(200, 75));
            panelRightTop.add(titrationStart);
            panelRightTop.add(washing);
            panelRightTop.setBackground(new Color(10, 70, 90));
            JLabel mode = new JLabel("Ручной режим");
            mode.setForeground(Color.WHITE);
            mode.setFont(fontMarker);

            panelRightBot.setLayout(new FlowLayout());
            panelRightBot.setPreferredSize(new Dimension(200, 270));
            panelRightBot.setBackground(new Color(19, 28, 48));
            panelRightBot.add(mode);
            panelRightBot.add(handMode);
            panelRightBot.add(addToggle("Помпа раствора", '5', '4', pumpForSolutionOn));
            panelRightBot.add(addToggle("Помпа титранта", '3', '2', pumpForTitrationOn));
            panelRightBot.add(addToggle("Клапан для воды", 'D', 'C', valveWaterOpened));
            panelRightBot.add(addToggle("Клапан для раствора", '9', '8', valveSolutionOpened));
            panelRightBot.add(addToggle("Клапан для титранта", 'B', 'A', valveTitrationOpened));
            panelRightBot.add(addToggle("Клапан для слива", '1', '0', valveOutOpened));
            panelRightBot.add(addToggle("Мешалка", '7', '6', mixerOn));
            panelRightBot.add(addToggle("Датчик", 'Y', 'X'));

            panelRightBot.add(result);

            panelRight.setLayout(new BorderLayout());
//            panelRight.setPreferredSize(new Dimension(200,750));
            panelRight.add(panelRightBot, BorderLayout.CENTER);
            panelRight.add(scada, BorderLayout.NORTH);
            panelRight.add(panelRightTop, BorderLayout.SOUTH);
            panelRight.setBackground(new Color(19, 28, 48));

            panel.setLayout(new BorderLayout());
            panel.add(graph, BorderLayout.WEST);
            panel.add(panelRight, BorderLayout.CENTER);
            panel.add(log, BorderLayout.EAST);

            panelRightTop.setLayout(new FlowLayout());

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setMinimumSize(new Dimension(850, 500));
            frame.add(panel);
            frame.pack();
            frame.setVisible(true);
            frame.setLocationRelativeTo(null);
        }
    }


    private static double[] findPeaks(XYDataset dataset, int seriesIndex){
        double[] result = new double[3];
        double maxY = Double.NEGATIVE_INFINITY;

        for(int i = 0; i < dataset.getItemCount(seriesIndex); i++){
            double x = dataset.getXValue(seriesIndex, i);
            double y = dataset.getYValue(seriesIndex, i);
            if(!Double.isNaN(x) && !Double.isNaN(y)){
                if(y > maxY) {
                    maxY = y;
                    result[0] = x;
                    result[1] = y;
                    result[2] = i;
                }
            }
        }
        return result;
    }

    private static void doWash() throws InterruptedException {
        InitialClass.arduino.serialWrite('D');
        InitialClass.arduino.serialWrite('3');
        InitialClass.arduino.serialWrite('5');
        InitialClass.arduino.serialWrite('7');
        Thread.sleep(10000);
        InitialClass.arduino.serialWrite('C');
        InitialClass.arduino.serialWrite('2');
        InitialClass.arduino.serialWrite('4');
        InitialClass.arduino.serialWrite('6');
        SetWindow.log.append("Промывка закончена" + "\n");

    }
}