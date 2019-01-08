import javax.swing.*;
import java.awt.*;

class Loading {
    private static JFrame loadingFrame = new JFrame();
    private static JLayeredPane inFrame = new JLayeredPane();
    private static JLabel jLabel = new JLabel();
    private static JLabel inLabel = new JLabel("Подключите Arduino..");

    static void setUp(){

        inFrame.setLayout(new OverlayLayout(inFrame));

        Image image = Toolkit.getDefaultToolkit().createImage("src/main/resources/giphy.gif");
        ImageIcon imageIcon = new ImageIcon(image);
        imageIcon.setImageObserver(jLabel);
        jLabel.setIcon(imageIcon);

        inFrame.add(jLabel, Integer.valueOf(1));
        inFrame.add(inLabel,Integer.valueOf(2));
        inLabel.setForeground(new Color(219, 242, 255));
        inLabel.setOpaque(true);
        inLabel.setBackground(new Color(19, 28, 48));
        inLabel.setFont(new Font("montserrat", Font.BOLD, 12));

        jLabel.setAlignmentX(0.35f);
        jLabel.setAlignmentY(0.71f);

        loadingFrame.add(inFrame);
        loadingFrame.setSize(480, 260);
        loadingFrame.setUndecorated(true);
        loadingFrame.setVisible(true);
        loadingFrame.setLocationRelativeTo(null);
    }

    static void closeUp() {
        loadingFrame.dispose();
    }

    static void setText(String text) {
        inLabel.setText(text);
    }
}
