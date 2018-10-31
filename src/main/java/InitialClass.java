
import javax.swing.*;
import com.fazecast.jSerialComm.*;

public class InitialClass {

	static Arduino arduino = new Arduino();
	private static Boolean flag = true;
	private static final String PORT_NAME = "COM3";
	private static int introTime = 60;

	public static void main(String[] args) {
		try {
			// Set System L&F
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			// handle exception
			e.getStackTrace();
		}
			Loading.setUp();
		do {
			SerialPort[] portNames = SerialPort.getCommPorts();
				for (SerialPort portName : portNames) {
					if(portName.getSystemPortName().equals(PORT_NAME)){
						arduino.setPortDescription(String.valueOf(portName.getSystemPortName()));
						arduino.setBaudRate(9600);
						arduino.openConnection();
						arduino.getSerialPort().setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
					}
				}
			try {
				if (arduino.openConnection()) {
					Loading.setText("Arduino подключено!");
					Thread.sleep(700);
					flag = false;
					Loading.closeUp();
				}
			} catch (NullPointerException e) {
				e.getStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
				introTime--;
				if (introTime == 0) {
					System.exit(0);
				}
		} while (flag);
		SwingUtilities.invokeLater(Main::SetUpChartAndControllers);
		}
}


