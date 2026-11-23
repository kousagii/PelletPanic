package pelletpanic;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class DifficultyPanel extends UiTheme.SpacePanel {
    private static final long serialVersionUID = 1L;

    DifficultyPanel(Consumer<Difficulty> selected) {
        JLabel title = UiTheme.label("Select Difficulty", 42, true);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);
        JPanel center = UiTheme.transparent();
        center.setLayout(new GridBagLayout());
        JPanel card = UiTheme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        for (Difficulty difficulty : Difficulty.values()) {
            JButton button = UiTheme.button(difficulty.label, difficulty == Difficulty.EASY);
            button.setMaximumSize(new Dimension(360, 60));
            button.setPreferredSize(new Dimension(360, 60));
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.addActionListener(event -> selected.accept(difficulty));
            card.add(button);
            card.add(Box.createVerticalStrut(16));
        }
        center.add(card);
        add(center, BorderLayout.CENTER);
        JButton back = UiTheme.button("Back", false);
        back.addActionListener(event -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            new HomeScreen();
            if (frame != null) {
                frame.dispose();
            }
        });
        JPanel footer = UiTheme.transparent();
        footer.add(back);
        add(footer, BorderLayout.SOUTH);
    }
}
