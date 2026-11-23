package pelletpanic;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PauseScreen extends UiTheme.SpacePanel {
    private static final long serialVersionUID = 1L;

    public PauseScreen(GamePanel gamePanel) {
        setLayout(new GridBagLayout());
        JPanel card = UiTheme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel label = UiTheme.label("Paused", 48, true);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(label);
        card.add(Box.createVerticalStrut(32));
        String[] words = {"Resume", "Retry", "Back to Home"};
        Runnable[] actions = {gamePanel::resumeGame, gamePanel::retryGame, gamePanel::backToHome};
        for (int i = 0; i < words.length; i++) {
            JButton button = UiTheme.button(words[i], i == 0);
            button.setMaximumSize(new Dimension(320, 54));
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            Runnable action = actions[i];
            button.addActionListener(event -> action.run());
            card.add(button);
            if (i < words.length - 1) {
                card.add(Box.createVerticalStrut(14));
            }
        }
        add(card);
    }
}
