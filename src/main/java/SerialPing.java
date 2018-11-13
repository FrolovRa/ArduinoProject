import java.util.Scanner;

class SerialPing implements Runnable {

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
