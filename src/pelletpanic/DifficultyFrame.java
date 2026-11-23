package pelletpanic;

import javax.swing.JFrame;

public class DifficultyFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    public DifficultyFrame() {
        UiTheme.frame(this, "Select Difficulty", new DifficultyPanel(difficulty -> {
            try {
                GamePanel panel = new GamePanel(difficulty.seconds, difficulty.map());
                new GameFrame(panel);
                dispose();
            } catch (IllegalStateException exception) {
                GameMessages.show(this, exception.getMessage(), "Could not open local saves");
            }
        }));
        setVisible(true);
    }
}
