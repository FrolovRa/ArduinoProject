import org.jfree.data.xy.XYDataset;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Scanner;


class Main {
    private final static Font fontMarker = new Font("Montserrat", Font.PLAIN,12);

    private static Main.ListeningThepH listeningThe_pH = new Main.ListeningThepH();
    private static Main.AutoTitration auto = new  Main.AutoTitration();
    private static boolean forScanner;
    private static boolean process;

    private static Layers mixerOn = new Layers("src/main/resources/mixerOn.gif",291,306,70,28);
    private static Layers mixerLayer = new Layers("src/main/resources/scadaMixerOff.png",125,115,400,300);

    private static Layers pumpForTitrationOn = new Layers("src/main/resources/Untitled-3.gif",137,16,50,50);
    private static Layers pumpForSolutionOn = new Layers("src/main/resources/Untitled-3.gif",137,230,50,50);
    private static Layers pumpForTitration = new Layers("src/main/resources/pump1Off.png",0,0,400,300);
    private static Layers pumpForSolution =new Layers("src/main/resources/pump2Off.png", -6,90,400,300);

    private static Layers waterTube = new Layers("src/main/resources/Water.png",4,72,140,150);

    private static Layers valveTitrationClosed = new Layers("src/main/resources/valveClosed.png",75,88,40,40);
    private static Layers valveTitrationOpened = new Layers("src/main/resources/valveOpened.png",75,88,40,40);
    private static Layers valveSolutionClosed = new Layers("src/main/resources/valveClosed.png",75,158,40,40);
    private static Layers valveSolutionOpened = new Layers("src/main/resources/valveOpened.png",75,158,40,40);
    private static Layers valveWaterClosed = new Layers("src/main/resources/valveClosed.png",35,122,40,40);
    private static Layers valveWaterOpened = new Layers("src/main/resources/valveOpened.png",35,122,40,40);

    private static Layers valveOutClosed = new Layers("src/main/resources/valveClosed.png",412,302,40,40);
    private static Layers valveOutOpened = new Layers("src/main/resources/valveOpened.png",412,302,40,40);

    private static JFrame frame = new JFrame("SCADA");
    private static JPanel panel = new JPanel();
    private static JPanel panelRight = new JPanel();
    private static JPanel panelRightTop = new JPanel();
    private static JPanel panelRightBot = new JPanel();
    private static JTextArea log = new JTextArea();
    private static JLabel result = new JLabel();
    private static JLabel visualPH = new JLabel();
    private static JLayeredPane scada = new JLayeredPane();


    private static class ListeningThepH implements Runnable  {
    public void run() {
        forScanner = true;
        System.out.println("ListeningThepH is loaded");
        log.append("Получаю данные...\n");
        Scanner scanner = new Scanner(InitialClass.arduino.getSerialPort().getInputStream());
        int i = 0;
        new Chart();
        pushToArduino(SendTo.PH_METER_ON);
        while(forScanner) {
           try {
                String line = scanner.nextLine();
                visualPH.setText(line+ " pH");
                Double number = Double.parseDouble(line);
                Chart.series.add((double)i, (double)number);
                double y = (double) Chart.series.getY(Chart.series.getItemCount() - 1) - (double) Chart.series.getY(Chart.series.getItemCount() - 2);
                if(y < 0) y = -y;
                Chart.secondSeries.add((double)i, y);
                } catch (IndexOutOfBoundsException | NumberFormatException e) {
                 e.getStackTrace();
                }
            i++;
        }
        scanner.close();
        pushToArduino(SendTo.PH_METER_OFF);
        log.append("Данные получены\n");
        System.out.println(Chart.secondSeries.getMaxY());
        double[] peak = findPeaks(Chart.secondData_set, 0);
        Chart.dot.add(Chart.series.getDataItem((int) peak[2]));
        result.setText(peak[0] +"ml");
        System.out.println("thread " + Thread.currentThread() +" finish");
    }

    }

    private static class AutoTitration implements Runnable {
        @Override
        public void run() {
            try {
                boolean threadAlive = true;
                process = true;
                /*START adding the solution*/
                pushToArduino(SendTo.VALVE_SOLUTION_ON);

                pushToArduino(SendTo.PUMP_FOR_SOLUTION_ON);

                Thread.sleep(4000); //Как будет известен расход поправить время закрытия клапана для того чтоб остаточую жидкость высосало.

                pushToArduino(SendTo.VALVE_SOLUTION_OFF);

                Thread.sleep(2000);

                pushToArduino(SendTo.PUMP_FOR_SOLUTION_OFF);
                log.append("Раствор готов\n");

                /*END adding the solution*/

                    /*START adding the titration*/
                    //motor for mixer
                    pushToArduino(SendTo.MIXER_ON);

                    pushToArduino(SendTo.VALVE_TITRATION_ON);

                    for (int i = 1; i<=10; i++) {
                        //pump for titration
                        log.append(i + " Добавление титранта" + "\n");
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
                    pushToArduino(SendTo.VALVE_TITRATION_OFF);

                    pushToArduino(SendTo.MIXER_OFF);

                    forScanner = false;

                    /*END adding the titration*/

                log.append("Автоматическое измерение закончено\n");
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

    private static class Flushing implements Runnable {

        @Override
        public void run() {
            try {
                setNormalState();

                pushToArduino(SendTo.VALVE_WATER_ON);
                pushToArduino(SendTo.PUMP_FOR_SOLUTION_ON);
                pushToArduino(SendTo.PUMP_FOR_TITRATION_ON);
                pushToArduino(SendTo.MIXER_ON);
                Thread.sleep(8000);
                pushToArduino(SendTo.VALVE_OUT_ON);
                Thread.sleep(8000);
                setNormalState();
                log.append("Промывка закончена" + "\n");
            } catch(InterruptedException e) {e.getStackTrace();}
        }
    }

    private static JToggleButton addToggle(String name, char whenOn, char whenOff, Layers on) {
        JToggleButton a = new JToggleButton(name);
        a.addPropertyChangeListener(e -> {
            if (process) {
                a.setEnabled(false);
            }
            else a.setEnabled(true);
        });
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

    static void SetUpScadaAndControllers() {
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


        JToggleButton titrationStart = new JToggleButton();
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
                forScanner = false;
                log.append("Завершение" + "\n");

            }
        });

        JButton washing = new JButton();
        washing.setPreferredSize(new Dimension(200, 30));
        washing.setText("Промывка");

        washing.addActionListener(e -> {
            washing.setText("В процессе");
            new Thread(new Flushing()).start();
            washing.setText("Промывка");
        });

        JButton handMode = new JButton();
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
                forScanner = true;
                new Thread(listeningThe_pH).start();
            } else if (handMode.getText().equals("Включен ручной режим !")) {
                forScanner = false;
                titrationStart.setEnabled(true);
                washing.setEnabled(true);
                log.append("Ручной режим отключен\n");
                handMode.setText("Начать ручной режим");
            }
        });


        JPanel flaskLevel = new JPanel();
        flaskLevel.setBounds(225, 200, 200, 200);
        flaskLevel.setBackground(Color.orange);

        /*
        *Setting SCADA layered window
         */
        scada.setPreferredSize(new Dimension(620, 400));

        scada.add(flaskLevel, 1);
        scada.add(mixerLayer, 2);
        scada.add(waterTube, 3);
        scada.add(pumpForTitration, 4);
        scada.add(pumpForSolution, 5);
        scada.add(pumpForTitrationOn, 6);
        scada.add(pumpForSolutionOn, 7);
        scada.add(mixerOn, 8);
        scada.add(valveTitrationClosed, 9);
        scada.add(valveTitrationOpened, 10);
        scada.add(valveSolutionClosed, 11);
        scada.add(valveSolutionOpened, 12);
        scada.add(valveOutClosed, 13);
        scada.add(valveOutOpened, 14);
        scada.add(valveWaterClosed, 15);
        scada.add(valveWaterOpened, 16);
        scada.add(visualPH, 17);

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
        panelRightBot.add(result);

        panelRight.setLayout(new BorderLayout());
        panelRight.setPreferredSize(new Dimension(200, 400));
        panelRight.add(panelRightBot, BorderLayout.CENTER);
        panelRight.add(panelRightTop, BorderLayout.NORTH);
        panelRight.setBackground(new Color(19, 28, 48));

        panel.setLayout(new BorderLayout());
        panel.setBackground(new Color(19, 28, 48));
        panel.add(scada, BorderLayout.WEST);
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

    /*
     close all valve and make motors off
      */
    private static void setNormalState() {
        InitialClass.arduino.serialWrite('0');
        InitialClass.arduino.serialWrite('6');
        InitialClass.arduino.serialWrite('2');
        InitialClass.arduino.serialWrite('8');
        InitialClass.arduino.serialWrite('C');
        InitialClass.arduino.serialWrite('A');
        InitialClass.arduino.serialWrite('4');
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