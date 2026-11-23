package pelletpanic;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class GameFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    public GameFrame(GamePanel panel) {
        UiTheme.frame(this, "Pellet Panic", panel);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                panel.quitGame();
            }
        });
        setVisible(true);
        panel.startGame();
    }

    public static void main(String[] args) {
        Throwable startupFailure = null;
        try {
            DatabaseManager.initialize();
            GameAudio.initialize();
        } catch (IllegalStateException exception) {
            startupFailure = exception;
        }
        Throwable failure = startupFailure;
        SwingUtilities.invokeLater(() -> {
            UiTheme.install();
            if (failure == null) {
                new HomeScreen();
            } else {
                Throwable cause = failure.getCause() == null ? failure : failure.getCause();
                GameMessages.show(null, cause.getMessage(), "Could not open local saves");
            }
        });
    }
}
