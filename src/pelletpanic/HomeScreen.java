package pelletpanic;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class HomeScreen extends JFrame {
    private static final long serialVersionUID = 1L;

    public HomeScreen() {
        GameAudio.setMusicPlaying(true);
        UiTheme.frame(this, "Start Screen", createContent(
                () -> open(DifficultyFrame::new), () -> open(ShopScreen::new), () -> open(HowtoPlayScreen::new)));
        setVisible(true);
    }

    static JPanel createContent(Runnable start, Runnable shop, Runnable help) {
        UiTheme.SpacePanel root = new UiTheme.SpacePanel();
        JPanel hero = UiTheme.transparent();
        hero.setLayout(new GridBagLayout());
        JPanel copy = UiTheme.transparent();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = UiTheme.label("Pellet Panic", 52, true);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(title);
        copy.add(Box.createVerticalStrut(44));
        String[] words = {"Start", "Shop", "How to Play"};
        Runnable[] actions = {start, shop, help};
        for (int i = 0; i < words.length; i++) {
            JButton button = UiTheme.button(words[i], i == 0);
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(340, 56));
            Runnable action = actions[i];
            button.addActionListener(event -> action.run());
            copy.add(button);
            if (i < words.length - 1) {
                copy.add(Box.createVerticalStrut(14));
            }
        }
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = .52;
        constraints.fill = GridBagConstraints.BOTH;
        hero.add(copy, constraints);
        constraints.gridx = 1;
        constraints.weightx = .48;
        hero.add(new UiTheme.OrbitArt(), constraints);
        root.add(hero, BorderLayout.CENTER);
        return root;
    }

    private void open(Supplier<JFrame> screen) {
        try {
            screen.get();
            dispose();
        } catch (IllegalStateException exception) {
            GameMessages.show(this, exception.getMessage(), "Could not open local saves");
        }
    }
}
