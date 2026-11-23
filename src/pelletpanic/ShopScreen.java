package pelletpanic;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;

public class ShopScreen extends JFrame {
    private static final long serialVersionUID = 1L;

    public ShopScreen() {
        ShopPanel panel = new ShopPanel(() -> {
            new HomeScreen();
            dispose();
        });
        UiTheme.frame(this, "Shop Screen", panel);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                panel.whenIdle(() -> System.exit(0));
            }
        });
        setVisible(true);
    }
}
