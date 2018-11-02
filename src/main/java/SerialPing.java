import com.fazecast.jSerialComm.SerialPort;
import java.util.Scanner;

class SerialPing implements Runnable {

    private SerialPort portName;

    SerialPing(SerialPort portName) {
        this.portName = portName;
    }

    @Override
    public void run() {
        try {
        Scanner scanner = new Scanner(InitialClass.arduino.getSerialPort().getInputStream());
        if (scanner.hasNext()) {
            if (scanner.nextLine().equals("Q")) {
                InitialClass.flag = false;
            }
        }
        } catch (NullPointerException e ) {
            e.getStackTrace();
        }
    }

}
