import javax.swing.*;
import com.fazecast.jSerialComm.*;

public class InitialClass {

	static Arduino arduino = new Arduino();
	static Boolean flag = true;

	public static void main(String[] args) throws InterruptedException {
		try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());} catch (Exception e) {e.getStackTrace();}
			Loading.setUp();
		do {
			SerialPort[] portNames = SerialPort.getCommPorts();
			try {
				for (SerialPort portName : portNames) {
					arduino.setPortDescription(String.valueOf(portName.getSystemPortName()));
					System.out.println(portName.getSystemPortName());
					arduino.setBaudRate(9600);
					if (arduino.openConnection()){
						arduino.getSerialPort().setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
						Thread echoArduino = new Thread(new SerialPing());
						echoArduino.start();
						arduino.serialWrite('Q');
						arduino.serialWrite('Q'); // 2x for sure disable ping statement in sketch
						echoArduino.join(100);
					}
					if (flag) arduino.closeConnection();
				}
			} catch (NullPointerException e) {
				e.getStackTrace();
				arduino.closeConnection();
			}
		} while (flag) ;
		System.out.println("Connection is available");
		Loading.setText("Arduino подключено!");
		Thread.sleep(700);
//		flag = false;
		Loading.closeUp();

		SwingUtilities.invokeLater(Main::SetUpScadaAndControllers);

	}
}


