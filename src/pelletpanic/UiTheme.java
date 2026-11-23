package pelletpanic;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.io.IOException;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/** One visual system for every screen; artwork remains the original pixel sprites. */
final class UiTheme {
    static final Color NIGHT = new Color(0x0B0C1C);
    static final Color SURFACE = new Color(0x17182F);
    static final Color LINE = new Color(0x363655);
    static final Color INK = new Color(0xF5F2FF);
    static final Color MUTED = new Color(0xB6B4CE);
    static final Color VIOLET = new Color(0x9868F4);
    static final Color CYAN = new Color(0x7AE0E0);
    private static final Font REGULAR = loadFont("SpaceGrotesk-Regular.otf");
    private static final Font BOLD = loadFont("SpaceGrotesk-Bold.otf");
    private static boolean installed;

    private UiTheme() {
    }

    static Font font(float size, boolean bold) {
        return (bold ? BOLD : REGULAR).deriveFont(size);
    }

    private static Font loadFont(String name) {
        try (var stream = UiTheme.class.getResourceAsStream("/Fonts/" + name)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled font: " + name);
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, stream);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException exception) {
            throw new IllegalStateException("Cannot load bundled font: " + name, exception);
        }
    }

    static void install() {
        if (installed) {
            return;
        }
        installed = true;
        for (Object key : UIManager.getDefaults().keySet().toArray()) {
            if (key.toString().endsWith(".font")) {
                UIManager.put(key, new FontUIResource(font(16, false)));
            }
        }
        UIManager.put("Panel.background", NIGHT);
        UIManager.put("Label.foreground", INK);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", INK);
        UIManager.put("Button.background", SURFACE);
        UIManager.put("Button.foreground", INK);
        UIManager.put("TabbedPane.background", SURFACE);
        UIManager.put("TabbedPane.foreground", MUTED);
        UIManager.put("TabbedPane.selected", new Color(0x373052));
        UIManager.put("TabbedPane.contentAreaColor", SURFACE);
        UIManager.put("TabbedPane.focus", CYAN);
        UIManager.put("ScrollBar.thumb", LINE);
        UIManager.put("ScrollBar.track", SURFACE);
    }

    static JLabel label(String text, float size, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, bold));
        label.setForeground(INK);
        label.putClientProperty(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return label;
    }

    static JPanel transparent() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    static JPanel card() {
        JPanel panel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                quality(g);
                g.setColor(new Color(0, 0, 0, 45));
                g.fillRoundRect(0, 5, getWidth(), getHeight() - 5, 28, 28);
                g.setColor(SURFACE);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 6, 28, 28);
                g.setColor(LINE);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 6, 28, 28);
                g.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 30, 24));
        return panel;
    }

    static JButton button(String text, boolean primary) {
        JButton button = new JButton(text) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                quality(g);
                boolean hover = getModel().isRollover();
                Color top = primary ? new Color(0x9973F5) : new Color(hover ? 0x30304E : 0x202139);
                Color bottom = primary ? new Color(0x7448CE) : new Color(0x191A30);
                if (getModel().isPressed()) {
                    top = top.darker();
                }
                if (!isEnabled()) {
                    top = bottom = new Color(0x272A3E);
                }
                g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
                g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);
                g.setColor(hasFocus() ? CYAN : primary ? new Color(0xB59AF2) : LINE);
                g.setStroke(new BasicStroke(hasFocus() ? 2f : 1f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        button.setFont(font(17, true));
        button.setForeground(INK);
        button.putClientProperty(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.setMargin(new Insets(12, 24, 12, 24));
        button.setPreferredSize(new Dimension(300, 54));
        button.addActionListener(event -> GameAudio.play(Sound.CLICK));
        button.setMinimumSize(new Dimension(120, 48));
        return button;
    }

    static void frame(JFrame frame, String title, JComponent content) {
        install();
        frame.setTitle(title);
        frame.setContentPane(content);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 790);
        frame.setMinimumSize(new Dimension(800, 620));
        Rectangle available = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setSize(Math.min(frame.getWidth(), available.width), Math.min(frame.getHeight(), available.height));
        frame.setLocationRelativeTo(null);
        frame.setIconImage(GameAssets.image("/Images/RedPlanet.png"));
        AudioControls.install(frame);
    }

    static JScrollPane scroll(Component content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                trackColor = SURFACE;
                thumbColor = LINE;
            }

            private JButton emptyButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return emptyButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return emptyButton();
            }
        });
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    static void tabs(javax.swing.JTabbedPane tabs) {
        tabs.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabInsets = new Insets(12, 20, 12, 20);
                contentBorderInsets = new Insets(18, 0, 0, 0);
                tabAreaInsets = new Insets(0, 0, 0, 0);
            }

            @Override
            protected void paintTabBorder(Graphics g, int placement, int index, int x, int y, int w, int h, boolean selected) { }

            @Override
            protected void paintContentBorder(Graphics g, int placement, int selected) { }

            @Override
            protected void paintFocusIndicator(Graphics g, int placement, Rectangle[] rectangles, int index,
                    Rectangle icon, Rectangle text, boolean selected) {
                if (tabs.hasFocus() && selected) {
                    g.setColor(CYAN);
                    g.drawRoundRect(text.x - 6, text.y - 5, text.width + 12, text.height + 10, 8, 8);
                }
            }

            @Override
            protected void paintTabBackground(Graphics graphics, int placement, int index, int x, int y, int w, int h, boolean selected) {
                Graphics2D g = (Graphics2D) graphics.create();
                quality(g);
                g.setColor(selected ? new Color(0x36304F) : SURFACE);
                g.fillRoundRect(x + 2, y + 2, w - 4, h - 4, 14, 14);
                if (selected) {
                    g.setColor(CYAN);
                    g.fillRoundRect(x + 20, y + h - 4, w - 40, 2, 2, 2);
                }
                g.dispose();
            }

            @Override
            protected void paintText(Graphics graphics, int placement, Font font, java.awt.FontMetrics metrics,
                    int index, String title, Rectangle bounds, boolean selected) {
                Graphics2D g = (Graphics2D) graphics.create();
                quality(g);
                g.setFont(font);
                g.setColor(selected ? INK : MUTED);
                g.drawString(title, bounds.x, bounds.y + metrics.getAscent());
                g.dispose();
            }
        });
    }

    static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    }

    static void background(Graphics2D g, int width, int height) {
        quality(g);
        g.setColor(NIGHT);
        g.fillRect(0, 0, width, height);
        float radius = Math.max(1, Math.max(width, height));
        g.setPaint(new RadialGradientPaint(width * .82f, height * .25f, radius * .8f,
                new float[]{0, 1}, new Color[]{new Color(0x292043), NIGHT}));
        g.fillRect(0, 0, width, height);
        Random stars = new Random(2706);
        for (int i = 0; i < 95; i++) {
            int x = (int) (stars.nextDouble() * width);
            int y = (int) (stars.nextDouble() * height);
            int size = i % 9 == 0 ? 3 : 1;
            g.setColor(new Color(184, 196, 244, i % 4 == 0 ? 130 : 45));
            g.fillOval(x, y, size, size);
        }
    }

    static class SpacePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        SpacePanel() {
            setLayout(new BorderLayout(24, 24));
            setBorder(BorderFactory.createEmptyBorder(40, 44, 40, 44));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            background(g, getWidth(), getHeight());
            g.dispose();
        }
    }

    static final class OrbitArt extends JComponent {
        private static final long serialVersionUID = 1L;

        OrbitArt() {
            setPreferredSize(new Dimension(420, 420));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            double scale = Math.min(getWidth(), getHeight()) / 440.0;
            g.translate(getWidth() / 2.0, getHeight() / 2.0);
            g.scale(scale, scale);
            g.setPaint(new RadialGradientPaint(0, 0, 210,
                    new float[]{0, 1}, new Color[]{new Color(129, 83, 225, 70), new Color(0, 0, 0, 0)}));
            g.fillOval(-210, -210, 420, 420);
            g.setColor(new Color(147, 130, 202, 85));
            g.setStroke(new BasicStroke(1));
            for (int diameter : new int[]{200, 310, 410}) {
                g.draw(new Ellipse2D.Double(-diameter / 2.0, -diameter / 2.0, diameter, diameter));
            }
            g.drawImage(GameAssets.image("/Images/RedPlanet.png"), -62, -62, 124, 124, null);
            g.drawImage(GameAssets.image("/Images/Earth.png"), 104, -120, 60, 60, null);
            g.drawImage(GameAssets.image("/Images/PurplePlanet.png"), -177, -24, 52, 52, null);
            g.drawImage(GameAssets.image("/Images/Sun.png"), -90, 158, 56, 56, null);
            g.drawImage(GameAssets.image("/Images/Astronaut.png"), 93, 102, 44, 44, null);
            g.setColor(CYAN);
            g.fillOval(9, -103, 6, 6);
            g.setColor(VIOLET);
            g.fillOval(-117, 99, 8, 8);
            g.dispose();
        }
    }
}
