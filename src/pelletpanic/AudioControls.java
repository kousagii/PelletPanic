package pelletpanic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/** A compact speaker control; original menu wording and button labels are unchanged. */
final class AudioControls {
    private AudioControls() {
    }

    static void install(JFrame frame) {
        JButton button = new JButton(new SpeakerIcon());
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFocusPainted(true);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.addActionListener(event -> GameAudio.toggleMuted());
        Runnable refresh = () -> SwingUtilities.invokeLater(() -> {
            String action = GameAudio.settings().muted() ? "Unmute audio" : "Mute audio";
            button.setToolTipText(action + " (M); right-click for volume");
            button.getAccessibleContext().setAccessibleName(action);
            button.repaint();
        });
        refresh.run();
        GameAudio.addListener(refresh);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                GameAudio.removeListener(refresh);
            }
        });
        button.addMouseListener(new MouseAdapter() {
            private void popup(MouseEvent event) {
                if (event.isPopupTrigger()) {
                    showVolumes(button);
                }
            }

            @Override
            public void mousePressed(MouseEvent event) {
                popup(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                popup(event);
            }
        });
        JLayeredPane layers = frame.getLayeredPane();
        layers.add(button, JLayeredPane.PALETTE_LAYER);
        Runnable position = () -> button.setBounds(Math.max(0, layers.getWidth() - 38), Math.max(0, layers.getHeight() - 38), 28, 28);
        layers.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                position.run();
            }
        });
        position.run();
        for (int modifiers : new int[]{0, KeyEvent.SHIFT_DOWN_MASK}) {
            frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_M, modifiers), "toggle-audio");
        }
        frame.getRootPane().getActionMap().put("toggle-audio", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                GameAudio.toggleMuted();
            }
        });
    }

    private static void showVolumes(JButton button) {
        AudioSettings settings = GameAudio.settings();
        JPopupMenu popup = new JPopupMenu();
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBackground(UiTheme.SURFACE);
        controls.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JSlider music = new JSlider(0, 100, Math.round(settings.music() * 100));
        JSlider effects = new JSlider(0, 100, Math.round(settings.effects() * 100));
        for (int i = 0; i < 2; i++) {
            String name = i == 0 ? "BGM" : "SFX";
            JLabel label = UiTheme.label(name, 15, true);
            JSlider slider = i == 0 ? music : effects;
            label.setLabelFor(slider);
            slider.getAccessibleContext().setAccessibleName(name);
            slider.setOpaque(false);
            slider.addChangeListener(event -> {
                if (!slider.getValueIsAdjusting()) {
                    GameAudio.setVolumes(music.getValue() / 100f, effects.getValue() / 100f);
                }
            });
            controls.add(label);
            controls.add(slider);
        }
        popup.add(controls);
        popup.show(button, -230, -175);
    }

    private static final class SpeakerIcon implements Icon {
        @Override
        public int getIconWidth() { return 24; }

        @Override
        public int getIconHeight() { return 24; }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            UiTheme.quality(g);
            g.translate(x, y);
            g.setColor(component.hasFocus() ? UiTheme.CYAN : UiTheme.MUTED);
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawPolygon(new int[]{3, 7, 12, 12, 7, 3}, new int[]{9, 9, 5, 19, 15, 15}, 6);
            if (GameAudio.settings().muted()) {
                g.setColor(UiTheme.VIOLET);
                g.drawLine(16, 9, 21, 15);
                g.drawLine(21, 9, 16, 15);
            } else {
                g.drawArc(10, 6, 10, 12, -55, 110);
                g.drawArc(10, 3, 15, 18, -55, 110);
            }
            g.dispose();
        }
    }
}
