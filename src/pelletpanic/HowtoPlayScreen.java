package pelletpanic;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class HowtoPlayScreen extends JFrame {
    private static final long serialVersionUID = 1L;
    private boolean returning;

    public HowtoPlayScreen() {
        UiTheme.frame(this, "How to Play", createContent(this::backToHome));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                backToHome();
            }
        });
        setVisible(true);
    }

    private void backToHome() {
        if (!returning) {
            returning = true;
            new HomeScreen();
            dispose();
        }
    }

    static JPanel createContent(Runnable goBack) {
        UiTheme.SpacePanel panel = new UiTheme.SpacePanel();
        javax.swing.JLabel title = UiTheme.label("How to Play", 42, true);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(title, BorderLayout.NORTH);
        JEditorPane instructions = new JEditorPane();
        instructions.setContentType("text/html");
        instructions.setEditorKit(new javax.swing.text.html.HTMLEditorKit() {
            private static final long serialVersionUID = 1L;

            @Override
            public javax.swing.text.ViewFactory getViewFactory() {
                return new HTMLFactory() {
                    @Override
                    public javax.swing.text.View create(javax.swing.text.Element element) {
                        Object source = element.getAttributes().getAttribute(javax.swing.text.html.HTML.Attribute.SRC);
                        if (source != null) {
                            for (Difficulty difficulty : Difficulty.values()) {
                                if (source.toString().endsWith("/" + difficulty.label + ".PNG")) {
                                    return new MapPreviewView(element, difficulty);
                                }
                            }
                        }
                        javax.swing.text.View view = super.create(element);
                        if (view instanceof javax.swing.text.html.ImageView image) {
                            image.setLoadsSynchronously(true);
                        }
                        return view;
                    }
                };
            }
        });
        instructions.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        instructions.setFont(UiTheme.font(17, false));
        instructions.setEditable(false);
        instructions.setBackground(UiTheme.SURFACE);
        instructions.setText(htmlContent());
        instructions.setCaretPosition(0);
        panel.add(UiTheme.scroll(instructions), BorderLayout.CENTER);
        JButton back = UiTheme.button("Back to Home", false);
        back.addActionListener(event -> goBack.run());
        JPanel footer = UiTheme.transparent();
        footer.add(back);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    static String htmlContent() {
        return "<html>" +
            "<head>" +
            "<style>" +
            "body { color: #F5F2FF; font-family: Space Grotesk; font-size: 17px; margin: 24px; } h2 { color: #7AE0E0; } p, li { margin-bottom: 14px; }" + 
            "</style>" +
            "</head>" +
                "<h2>Instructions:</h2>" +
                "<p>1. Choose the difficulty you prefer:</p>" +
                "<ul>" +
                "<li>Easy - Time limit: 120 seconds, 4 enemies</li>" +
            "<div style='text-align: center;'>" +
                "<img src='" + HowtoPlayScreen.class.getResource("/Images/Easy.PNG") + "' width='600' height='395'>" +
            "</div>" +
                "<li>Medium - Time limit: 90 seconds, 5 enemies</li>" +
            "<div style='text-align: center;'>" +
                "<img src='" + HowtoPlayScreen.class.getResource("/Images/Medium.PNG") + "' width='600' height='395'>" +
            "</div>" +
                "<li>Hard - Time limit: 90 seconds, 6 enemies</li>" +
            "<div style='text-align: center;'>" +
                "<img src='" + HowtoPlayScreen.class.getResource("/Images/Hard.PNG") + "' width='600' height='395'>" +
            "</div>" +
                "</ul>" +
                "<p>2. Use the arrow keys (↑, ↓, ←, →) to move your character.</p>" +
            "<div style='text-align: center;'>" +
                "<img src='" + HowtoPlayScreen.class.getResource("/Images/Move.PNG") + "' width='250' height='249'>" +
            "</div>" +
                "<p>3. Collect pellets to score points.</p>" +
                "<p>4. Collect power-ups to have an advantage on playing:</p>" +
                "<ul>" +
                "<li>Green orb: Player becomes invulnerable for 10 seconds.</li>" +
            "<div style='text-align: center;'>" +
               "<img src='" + HowtoPlayScreen.class.getResource("/Images/invincibility.png") + "' width='120' height='120'>" +
            "</div>" +
                "<li>Fire: Creates a trail of fire for 10 seconds. If the enemy touches the trail, they will disappear for 10 seconds.</li>" +
            "<div style='text-align: center;'>" +
                "<img src='" + HowtoPlayScreen.class.getResource("/Images/trail.png") + "' width='120' height='120'>" +
            "</div>" +
                "<li>Ice: Freeze the enemy for 10 seconds.</li>" +
                "</ul>" +
            "<div style='text-align: center;'>" +
                "<img src='" + HowtoPlayScreen.class.getResource("/Images/ice.png") + "' width='120' height='120'>" +
            "</div>" +
                "<p>5. Avoid enemies to not lose a life.</p>" +
                "<p>6. Have fun and try to beat your high score!</p>" +
                "<br>" +
                "</html>";
    }
}

