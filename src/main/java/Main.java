import org.jfree.data.xy.XYDataset;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Scanner;


class Main {
    private static boolean scannerFlag;
    private static boolean inProcess;
    private static double titrationVolume = 0.0;
    private static double solutionVolume = 0.0;
    private final static ScadaLayers mixerOn;
    private final static ScadaLayers mixerLayer;
    private final static ScadaLayers pumpForTitrationOn;
    private final static ScadaLayers pumpForSolutionOn;
    private final static ScadaLayers pumpForTitration;
    private final static ScadaLayers pumpForSolution;
    private final static ScadaLayers waterTube;
    private final static ScadaLayers valveTitrationClosed;
    private final static ScadaLayers valveTitrationOpened;
    private final static ScadaLayers valveSolutionClosed;
    private final static ScadaLayers valveSolutionOpened;
    private final static ScadaLayers valveWaterClosed;
    private final static ScadaLayers valveWaterOpened;
    private final static ScadaLayers valveOutClosed;
    private final static ScadaLayers valveOutOpened;
    private static JFrame frame = new JFrame("SCADA");
    private static JPanel panel = new JPanel();
    private static JPanel panelRight = new JPanel();
    private static JPanel panelRightTop = new JPanel();
    private static JPanel panelRightBot = new JPanel();
    private static JPanel panelLeft = new JPanel();
    private static JTextArea log = new JTextArea();
    private static JLabel result = new JLabel();
    private static JLabel visualPH = new JLabel();
    private static JLayeredPane scada = new JLayeredPane();
    private static JToggleButton titrationStart = new JToggleButton();
    private static JButton handMode = new JButton();
    private static JPanel flaskLevel = new JPanel();
    private static String line;
    private final static Font fontMarker = new Font("Montserrat", Font.PLAIN, 12);
    private static ListeningThePh listeningThe_pH = new ListeningThePh();
    private static AutoTitration auto = new AutoTitration();


    static {
        mixerOn = new ScadaLayers("src/main/resources/mixerOn.gif", 291, 306, 70, 28);
        mixerLayer = new ScadaLayers("src/main/resources/scadaMixerOff.png", 125, 115, 400, 300);
        pumpForTitrationOn = new ScadaLayers("src/main/resources/Untitled-3.gif", 137, 16, 50, 50);
        pumpForSolutionOn = new ScadaLayers("src/main/resources/Untitled-3.gif", 137, 230, 50, 50);
        pumpForTitration = new ScadaLayers("src/main/resources/pump1Off.png", 0, 0, 400, 300);
        pumpForSolution = new ScadaLayers("src/main/resources/pump2Off.png", -6, 90, 400, 300);
        waterTube = new ScadaLayers("src/main/resources/Water.png", 4, 72, 140, 150);
        valveWaterOpened = new ScadaLayers("src/main/resources/valveOpenedBlue.png", 35, 122, 40, 40);
        valveWaterClosed = new ScadaLayers("src/main/resources/valveClosedBlue.png", 35, 122, 40, 40);
        valveSolutionOpened = new ScadaLayers("src/main/resources/valveOpenedRed.png", 75, 158, 40, 40);
        valveSolutionClosed = new ScadaLayers("src/main/resources/valveClosedRed.png", 75, 158, 40, 40);
        valveTitrationOpened = new ScadaLayers("src/main/resources/valveOpenedRed.png", 75, 88, 40, 40);
        valveTitrationClosed = new ScadaLayers("src/main/resources/valveClosedRed.png", 75, 88, 40, 40);
        valveOutClosed = new ScadaLayers("src/main/resources/valveClosedBlue.png", 412, 302, 40, 40);
        valveOutOpened = new ScadaLayers("src/main/resources/valveOpenedBlue.png", 412, 302, 40, 40);
    }


    private static class ListeningThePh implements Runnable {
        public void run() {
            scannerFlag = true;
            System.out.println("ListeningThePh is loaded");
            log.append("Получаю данные...\n");
            Scanner scanner = new Scanner(InitialClass.arduino.getSerialPort().getInputStream());
            pushToArduino(SendTo.PH_METER_ON);
            while (scannerFlag) {
                try {
                    if (scanner.hasNextLine()) {
                        line = scanner.nextLine();
                        visualPH.setText(line + " pH");
                    }
                } catch (IndexOutOfBoundsException | NumberFormatException e) {
                    e.getStackTrace();
                }
            }
            scanner.close();
            pushToArduino(SendTo.PH_METER_OFF);
            log.append("Данные получены\n");
            System.out.println("thread " + Thread.currentThread() + " finish");
        }

    }

    private static class AutoTitration implements Runnable {
        private double pH_limit = 2.3;
        @Override
        public void run() {
            try {
                inProcess = true;

                /*START adding the solution*/

                pushToArduino(SendTo.VALVE_SOLUTION_ON);

                pushToArduino(SendTo.PUMP_FOR_SOLUTION_ON);
//                int i = 60 + 55;
//                while (i > 0) {
//                    Thread.sleep(1000);
//                    solutionVolume += 0.869d;
//                    i--;
//                }

                pushToArduino(SendTo.VALVE_SOLUTION_OFF);
                pushToArduino(SendTo.PUMP_FOR_SOLUTION_OFF);
                log.append("Раствор готов\n");

                /*START adding the titration*/

                pushToArduino(SendTo.MIXER_ON);
                pushToArduino(SendTo.VALVE_TITRATION_ON);
                log.append(" Добавление титранта..." + "\n");
                new Chart();
                new Thread(listeningThe_pH).start();
                InitialClass.arduino.serialWrite('3');
                pumpForTitrationOn.setVisible(true);
                while (inProcess) {
                    //pump for titration
                    Thread.sleep(5000);
                    titrationVolume += 1.65;
                        /*
                        Time for sensor to set value
                        */
                    try {
                        Double number = Double.parseDouble(line);
                        Chart.series.add(titrationVolume, (double) number);
                        if(Chart.series.getItemCount() >= 2){
                            double y = (double) Chart.series.getY(Chart.series.getItemCount() - 1) - (double) Chart.series.getY(Chart.series.getItemCount() - 2);
                            if (y < 0) y = -y;
                            if (titrationVolume > 10) inProcess = false;
                            Chart.secondSeries.add(titrationVolume, y);
                        } else {
                            Chart.secondSeries.add(titrationVolume,0);
                        }

                    } catch (IndexOutOfBoundsException | NumberFormatException e) {
                        e.getStackTrace();
                    }
                }

                double[] peak = findPeaks(Chart.secondData_set, 0);
                Chart.dot.add(Chart.series.getDataItem((int) peak[2]));
                result.setText("Концентрация = " + calculateResult(peak[0]) + "%" + "\n" +"  Использовано титранта - " + titrationVolume+ "  Использовано раствора - " + solutionVolume);

                pushToArduino(SendTo.MIXER_OFF);
                pushToArduino(SendTo.PUMP_FOR_TITRATION_OFF);
                pushToArduino(SendTo.VALVE_TITRATION_OFF);

                scannerFlag = false;
                visualPH.setText("_.__pH");

                /*END adding the titration*/
                titrationVolume = 0.0;
                titrationStart.doClick();
                log.append("Автоматическое измерение закончено\n");
            } catch (Exception newOne) {
                newOne.getStackTrace();
            }

        }
    }

    private static float calculateResult(double peak) {
        return (float)(.1d * peak / solutionVolume) * 100;
    }

    private static class ScadaLayers extends JLabel {
        private ScadaLayers(String filename, int x, int y, int width, int height) {
            Image image = Toolkit.getDefaultToolkit().createImage(filename);
            ImageIcon gif = new ImageIcon(image);
            gif.setImageObserver(this);
            setIcon(gif);
            setBounds(x, y, width, height);
            setVisible(true);
        }
    }

    private static class Flushing implements Runnable {

        @Override
        public void run() {
            try {
                setNormalState();
                pushToArduino(SendTo.VALVE_OUT_ON);
                Thread.sleep(147000);
                pushToArduino(SendTo.VALVE_OUT_OFF);


                InitialClass.arduino.serialWrite("D735");
                valveWaterOpened.setVisible(true);
                pumpForSolutionOn.setVisible(true);
                pumpForTitrationOn.setVisible(true);
                mixerOn.setVisible(true);

                Thread.sleep(10000);

                valveWaterOpened.setVisible(false);
                pumpForSolutionOn.setVisible(false);
                pumpForTitrationOn.setVisible(false);
                InitialClass.arduino.serialWrite("C124");
                Thread.sleep(2000);
                pushToArduino(SendTo.VALVE_OUT_ON);

                Thread.sleep(15000);

                setNormalState();
                log.append("Промывка закончена" + "\n");
            } catch (InterruptedException e) {
                e.getStackTrace();
            }
        }
    }

    private static JToggleButton addToggle(String name, char whenOn, char whenOff, ScadaLayers on) {
        JToggleButton a = new JToggleButton(name);

//        a.addContainerListener(e -> {
//            if (handMode.getText().equals("Включен ручной режим !")) {
//                a.setEnabled(true);
//            } else a.setEnabled(false);
//        });

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

    private static JToggleButton addToggle(String name, char whenOn, char whenOff) {
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

    public static void main(String[] args) {
        SetUpScadaAndControllers();
    }

    private static int y_FlaskLevel (double solutionVolume){
        if (solutionVolume == 0) return 366;
        return (int) map(solutionVolume,0,150,366,199);

    }
    private static double map (double value, double fromSource, double toSource, double fromTarget, double toTarget)
    {
        return (value - fromSource) / (toSource - fromSource) * (toTarget - fromTarget) + fromTarget;
    }

    static void SetUpScadaAndControllers() {
        new Thread(() -> {
            while(true) {
                int y = y_FlaskLevel(solutionVolume);
                flaskLevel.setBounds(225, y, 200, 366 - y);
                flaskLevel.revalidate();
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        /*
         hide animated images of scada
        */

        pumpForSolutionOn.setVisible(false);
        pumpForTitrationOn.setVisible(false);
        mixerOn.setVisible(false);
        valveTitrationOpened.setVisible(false);
        valveSolutionOpened.setVisible(false);
        valveWaterOpened.setVisible(false);
        valveOutOpened.setVisible(false);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("connection closed");
                setNormalState();
                InitialClass.arduino.closeConnection();
            }
        });

        log.setFont(fontMarker);
        log.setRows(10);
        log.setPreferredSize(new Dimension(200, 400));
        log.setBackground(new Color(19, 28, 48));
        log.setForeground(Color.WHITE);


        visualPH.setBounds(460, 90, 150, 30);
        visualPH.setText("_.__pH");
        visualPH.setForeground(Color.WHITE);
        visualPH.setFont(new Font("Montserrat", Font.PLAIN, 32));


        titrationStart.setPreferredSize(new Dimension(200, 30));
        titrationStart.setText("Автоматическое измерение");

        titrationStart.addItemListener(ev -> {
            if (ev.getStateChange() == ItemEvent.SELECTED) {
                Chart.series.clear();
                Chart.secondSeries.clear();
                Chart.dot.clear();
                titrationStart.setText("В процессе");
                new Thread(auto).start();

            } else if (ev.getStateChange() == ItemEvent.DESELECTED) {
                log.append("Завершение" + "\n");
                titrationStart.setText("Автоматическое измерение");
                inProcess = false;
            }
        });

        JButton washing = new JButton();
        washing.setPreferredSize(new Dimension(200, 30));
        washing.setText("Промывка");

        washing.addActionListener(e -> new Thread(new Flushing()).start());


        handMode.setPreferredSize(new Dimension(200, 30));
        handMode.setText("Начать ручной режим");

        handMode.addActionListener(e -> {
            if (handMode.getText().equals("Начать ручной режим")) {
                handMode.setText("Включен ручной режим !");
                titrationStart.setEnabled(false);
                washing.setEnabled(false);
                Chart.series.clear();
                Chart.secondSeries.clear();
                Chart.dot.clear();
                scannerFlag = true;
                new Chart();
                new Thread(listeningThe_pH).start();
            } else if (handMode.getText().equals("Включен ручной режим !")) {
                scannerFlag = false;
                titrationStart.setEnabled(true);
                washing.setEnabled(true);
                log.append("Ручной режим отключен\n");
                handMode.setText("Начать ручной режим");
                inProcess = false;
            }
        });

        flaskLevel.setBackground(Color.orange);

        /*
         *Setting SCADA layered window
         */
        scada.setPreferredSize(new Dimension(620, 400));

        scada.add(flaskLevel, Integer.valueOf(1));
        scada.add(mixerLayer, Integer.valueOf(2));
        scada.add(waterTube, Integer.valueOf(3));
        scada.add(pumpForTitration, Integer.valueOf(4));
        scada.add(pumpForSolution, Integer.valueOf(5));
        scada.add(pumpForTitrationOn, Integer.valueOf(6));
        scada.add(pumpForSolutionOn, Integer.valueOf(7));
        scada.add(mixerOn, Integer.valueOf(8));
        scada.add(valveTitrationClosed, Integer.valueOf(9));
        scada.add(valveTitrationOpened, Integer.valueOf(10));
        scada.add(valveSolutionClosed, Integer.valueOf(11));
        scada.add(valveSolutionOpened, Integer.valueOf(12));
        scada.add(valveOutClosed, Integer.valueOf(13));
        scada.add(valveOutOpened, Integer.valueOf(14));
        scada.add(valveWaterClosed, Integer.valueOf(15));
        scada.add(valveWaterOpened, Integer.valueOf(16));
        scada.add(visualPH, Integer.valueOf(17));

        result.setPreferredSize(new Dimension(200, 50));
//        result.setText("Концентрация");
        result.setText(" Концентрация = " + "___%" + "\n" +"  Использовано титранта - " + titrationVolume+ "  Использовано раствора - " + solutionVolume);

        result.setFont(new Font("Montserrat", Font.PLAIN, 22));
        result.setForeground(Color.WHITE);

        panelRightTop.setPreferredSize(new Dimension(200, 75));
        panelRightTop.add(titrationStart);
        panelRightTop.add(washing);
        panelRightTop.setBackground(new Color(10, 70, 90));

        JLabel mode = new JLabel("Ручной режим");
        mode.setForeground(Color.WHITE);
        mode.setFont(fontMarker);

        panelRightBot.setLayout(new FlowLayout());
        panelRightBot.setPreferredSize(new Dimension(200, 325));
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
//        panelRightBot.add(result);

        panelRight.setLayout(new BorderLayout());
        panelRight.setPreferredSize(new Dimension(200, 400));
        panelRight.add(panelRightBot, BorderLayout.CENTER);
        panelRight.add(panelRightTop, BorderLayout.NORTH);
        panelRight.setBackground(new Color(19, 28, 48));

        panel.setLayout(new BorderLayout());
        panel.setBackground(new Color(19, 28, 48));
        panelLeft.setLayout(new BorderLayout());
        panelLeft.setBackground(new Color(19, 28, 48));
        panelLeft.add(scada, BorderLayout.EAST);
        JLabel a = new JLabel("<html> <br><br><br>" +
                " &nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp  H2O <br>" +
                " &nbsp&nbsp&nbsp&nbsp&nbsp NaOH<br>" +
                " &nbsp C2H2O4<br></html>");
        a.setFont(new Font("Montserrat", Font.PLAIN, 26));
        a.setForeground(Color.WHITE);
        a.setVerticalAlignment(SwingConstants.NORTH);

        panelLeft.add( a,BorderLayout.WEST);
        panel.add(panelLeft, BorderLayout.WEST);
//        panel.add(scada, BorderLayout.WEST);
        panel.add(panelRight, BorderLayout.CENTER);
        panel.add(log, BorderLayout.EAST);
        panel.add(result, BorderLayout.SOUTH);

        panelRightTop.setLayout(new FlowLayout());

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(850, 500));
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
    }

    /*
     close all valve and make motors off
      */
    private static void setNormalState() {
        InitialClass.arduino.serialWrite("0628CA4");
        /// hide animated images of scada
        pumpForSolutionOn.setVisible(false);
        pumpForTitrationOn.setVisible(false);
        mixerOn.setVisible(false);
        valveTitrationOpened.setVisible(false);
        valveSolutionOpened.setVisible(false);
        valveWaterOpened.setVisible(false);
        valveOutOpened.setVisible(false);

        log.append("Клапана закрыты\nдвигатели выключены\n");

    }

    private static double[] findPeaks(XYDataset dataset, int seriesIndex) {
        double[] result = new double[3];
        double maxY = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < dataset.getItemCount(seriesIndex); i++) {
            double x = dataset.getXValue(seriesIndex, i);
            double y = dataset.getYValue(seriesIndex, i);
            if (!Double.isNaN(x) && !Double.isNaN(y)) {
                if (y > maxY) {
                    maxY = y;
                    result[0] = x;
                    result[1] = y;
                    result[2] = i;
                }
            }
        }
        return result;
    }

    private static void pushToArduino(SendTo whatToDo) {
        switch (whatToDo) {
            case VALVE_OUT_OFF:
                InitialClass.arduino.serialWrite(SendTo.VALVE_OUT_OFF.getChar());
                log.append("Клапан для слива закрыт\n");
                valveOutOpened.setVisible(false);
                break;
            case PH_METER_OFF:
                InitialClass.arduino.serialWrite(SendTo.PH_METER_OFF.getChar());
                log.append("Датчик отключен\n");
                break;
            case PH_METER_ON:
                InitialClass.arduino.serialWrite(SendTo.PH_METER_ON.getChar());
                log.append("Получение данных от датчика..\n");
                break;
            case VALVE_OUT_ON:
                InitialClass.arduino.serialWrite(SendTo.VALVE_OUT_ON.getChar());
                log.append("Клапан для слива открыт\n");
                valveOutOpened.setVisible(true);
                break;
            case VALVE_WATER_OFF:
                InitialClass.arduino.serialWrite(SendTo.VALVE_WATER_OFF.getChar());
                log.append("Клапан для воды закрыт\n");
                valveWaterOpened.setVisible(false);
                break;
            case VALVE_WATER_ON:
                InitialClass.arduino.serialWrite(SendTo.VALVE_WATER_ON.getChar());
                log.append("Клапан для воды открыт\n");
                valveWaterOpened.setVisible(true);
                break;
            case VALVE_SOLUTION_OFF:
                InitialClass.arduino.serialWrite(SendTo.VALVE_SOLUTION_OFF.getChar());
                log.append("Клапан для раствора закрыт\n");
                valveSolutionOpened.setVisible(false);
                break;
            case VALVE_SOLUTION_ON:
                InitialClass.arduino.serialWrite(SendTo.VALVE_SOLUTION_ON.getChar());
                log.append("Клапан для раствора открыт\n");
                valveSolutionOpened.setVisible(true);
                break;
            case VALVE_TITRATION_OFF:
                InitialClass.arduino.serialWrite(SendTo.VALVE_TITRATION_OFF.getChar());
                log.append("Клапан для титранта закрыт\n");
                valveTitrationOpened.setVisible(false);
                break;
            case VALVE_TITRATION_ON:
                InitialClass.arduino.serialWrite(SendTo.VALVE_TITRATION_ON.getChar());
                log.append("Клапан для титранта открыт\n");
                valveTitrationOpened.setVisible(true);
                break;
            case PUMP_FOR_SOLUTION_OFF:
                InitialClass.arduino.serialWrite(SendTo.PUMP_FOR_SOLUTION_OFF.getChar());
                log.append("Насос раствора выключен\n");
                pumpForSolutionOn.setVisible(false);
                break;
            case PUMP_FOR_SOLUTION_ON:
                InitialClass.arduino.serialWrite(SendTo.PUMP_FOR_SOLUTION_ON.getChar());
                log.append("Насос раствора включен\n");
                pumpForSolutionOn.setVisible(true);
                break;
            case PUMP_FOR_TITRATION_OFF:
                InitialClass.arduino.serialWrite(SendTo.PUMP_FOR_TITRATION_OFF.getChar());
                log.append("Насос титранта выключен\n");
                pumpForTitrationOn.setVisible(false);
                break;
            case PUMP_FOR_TITRATION_ON:
                InitialClass.arduino.serialWrite(SendTo.PUMP_FOR_TITRATION_ON.getChar());
                log.append("Насос титранта включен\n");
                pumpForTitrationOn.setVisible(true);
                break;
            case MIXER_OFF:
                InitialClass.arduino.serialWrite(SendTo.MIXER_OFF.getChar());
                log.append("Мешалка выключена\n");
                mixerOn.setVisible(false);
                break;
            case MIXER_ON:
                InitialClass.arduino.serialWrite(SendTo.MIXER_ON.getChar());
                log.append("Мешкалка включена\n");
                mixerOn.setVisible(true);
                break;
            default:
                System.out.println("error in arguments");
                break;
        }
    }
}