package pelletpanic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GraphicsDevice.WindowTranslucency;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/** Shared modal messages without native title bars or look-and-feel icons. */
final class GameMessages {
    private GameMessages() {
    }

    static void show(Component parent, String message) {
        show(parent, message, null);
    }

    static void show(Component parent, String message, String heading) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> show(parent, message, heading));
            return;
        }
        Window owner = parent instanceof Window window ? window
                : parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, heading == null ? "Message" : heading, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setBackground(UiTheme.SURFACE);
        if (dialog.getGraphicsConfiguration().getDevice().isWindowTranslucencySupported(WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
            dialog.setBackground(new Color(0, 0, 0, 0));
        }
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int width = Math.min(660, Math.max(240, screen.width - 48));
        JPanel content = content(message, heading, dialog::dispose, width, Math.max(80, screen.height - 220));
        dialog.setContentPane(content);
        dialog.getRootPane().setDefaultButton((JButton) content.getClientProperty("defaultButton"));
        dialog.getRootPane().registerKeyboardAction(event -> dialog.dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setLocationRelativeTo(parent == null ? owner : parent);
        dialog.setVisible(true);
    }

    static JPanel content(String message, String heading, Runnable dismiss, int width, int maxMessageHeight) {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 22));
        JTextArea text = new JTextArea(message);
        text.setFont(UiTheme.font(18, false));
        text.setForeground(UiTheme.INK);
        text.setBackground(UiTheme.SURFACE);
        text.setCaretColor(UiTheme.CYAN);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        int bodyWidth = Math.max(120, width - 48);
        text.setSize(bodyWidth, Integer.MAX_VALUE);
        int bodyHeight = Math.min(maxMessageHeight, text.getPreferredSize().height + 6);
        JScrollPane scroll = UiTheme.scroll(text);
        scroll.setPreferredSize(new Dimension(bodyWidth, bodyHeight));
        card.add(scroll, BorderLayout.CENTER);
        if (heading != null && !heading.isEmpty()) {
            var title = UiTheme.label(heading, 20, true);
            title.setLabelFor(text);
            card.add(title, BorderLayout.NORTH);
        }
        JButton ok = UiTheme.button("OK", true);
        ok.setPreferredSize(new Dimension(120, 46));
        ok.addActionListener(event -> dismiss.run());
        JPanel footer = UiTheme.transparent();
        footer.add(ok);
        card.add(footer, BorderLayout.SOUTH);
        card.putClientProperty("defaultButton", ok);
        // Enter also works while the selectable message text has keyboard focus.
        card.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "dismiss");
        card.getActionMap().put("dismiss", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent event) {
                dismiss.run();
            }
        });
        text.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "dismiss");
        text.getActionMap().put("dismiss", card.getActionMap().get("dismiss"));
        return card;
    }
}
