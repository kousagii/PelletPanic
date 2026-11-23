package pelletpanic;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Swing lifecycle and input only; rules, artwork, and saves live separately. */
public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private static final long serialVersionUID = 1L;
    private final int durationSeconds;
    private final String[] map;
    private final Skin skin;
    private final GameRenderer renderer = new GameRenderer();
    final Timer gameLoop = new Timer(GameSession.STEP_MS, this);
    private final PauseScreen pauseScreen;
    private GameSession session;
    private int highScore;
    private CompletableFuture<Void> resultSave;
    private boolean navigationPending;
    private boolean stopped;

    public GamePanel(int durationSeconds, String[] map) {
        this.durationSeconds = durationSeconds;
        this.map = map.clone();
        skin = DatabaseManager.getEquippedSkin(1);
        highScore = DatabaseManager.getHighScore(1);
        session = new GameSession(durationSeconds, map, new Random());
        setPreferredSize(new Dimension(1060, 720));
        setLayout(new BorderLayout());
        setFocusable(true);
        for (int key : new int[]{KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                KeyEvent.VK_SPACE, KeyEvent.VK_R, KeyEvent.VK_ESCAPE}) {
            String action = "release-" + key;
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(key, 0, true), action);
            getActionMap().put(action, new javax.swing.AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent event) {
                    handleKey(key);
                }
            });
        }
        pauseScreen = new PauseScreen(this);
        // Construction is inert: choosing difficulty never creates a live session.
    }

    GameSession session() {
        return session;
    }

    public void startGame() {
        if (!stopped && !session.gameOver) {
            session.start();
            GameAudio.setMusicPlaying(true);
            gameLoop.start();
            requestFocusInWindow();
        }
    }

    public void stopGame() {
        stopped = true;
        session.stop();
        gameLoop.stop();
        GameAudio.pauseGameplay();
    }

    @Override
    public void removeNotify() {
        stopGame();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        renderer.draw(g, getWidth(), getHeight(), session, skin, highScore);
        g.dispose();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (stopped) {
            return;
        }
        session.tick();
        GameAudio.events(session.drainEvents());
        if (session.gameOver) {
            gameLoop.stop();
            highScore = Math.max(highScore, session.score);
            saveResult();
        }
        repaint();
    }

    private CompletableFuture<Void> saveResult() {
        if (resultSave == null) {
            int coins = session.coinsCollected;
            int score = session.score;
            int record = highScore;
            resultSave = GamePersistence.submit(() -> {
                DatabaseManager.saveGameData(coins, score, record, 1);
                return null;
            });
            resultSave.whenComplete((ignored, failure) -> {
                if (failure != null) {
                    SwingUtilities.invokeLater(() -> {
                        if (!navigationPending && !stopped) {
                            showSaveError(failure);
                        }
                    });
                }
            });
        }
        return resultSave;
    }

    private void afterSave(Runnable action) {
        if (navigationPending) {
            return;
        }
        if (!session.gameOver) {
            action.run();
            return;
        }
        navigationPending = true;
        if (resultSave != null && resultSave.isCompletedExceptionally()) {
            resultSave = null; // A failed write did not commit; explicit retry is safe.
        }
        saveResult().whenComplete((ignored, failure) -> SwingUtilities.invokeLater(() -> {
            navigationPending = false;
            if (failure != null) {
                showSaveError(failure);
            } else {
                action.run();
            }
        }));
    }

    private void showSaveError(Throwable failure) {
        Throwable cause = failure.getCause() == null ? failure : failure.getCause();
        GameMessages.show(this, cause.getMessage(), "Could not open local saves");
    }

    public void togglePause() {
        if (stopped || !session.running || session.gameOver) {
            return;
        }
        session.setPaused(!session.paused);
        if (session.paused) {
            gameLoop.stop();
            GameAudio.pauseGameplay();
            add(pauseScreen, BorderLayout.CENTER);
        } else {
            remove(pauseScreen);
            gameLoop.start();
            GameAudio.setMusicPlaying(true);
            requestFocusInWindow();
        }
        revalidate();
        repaint();
    }

    public void retryGame() {
        afterSave(() -> {
            gameLoop.stop();
            remove(pauseScreen);
            session = new GameSession(durationSeconds, map, new Random());
            resultSave = null;
            stopped = false;
            startGame();
            revalidate();
            repaint();
        });
    }

    public void resumeGame() {
        if (session.paused) {
            togglePause();
        }
    }

    public void backToHome() {
        afterSave(() -> {
            stopGame();
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            if (frame != null) {
                frame.dispose();
            }
            new HomeScreen();
        });
    }

    public void quitGame() {
        afterSave(() -> {
            stopGame();
            System.exit(0);
        });
    }

    @Override
    public void keyReleased(KeyEvent event) {
        handleKey(event.getKeyCode());
    }

    private void handleKey(int key) {
        if (stopped || navigationPending) {
            return;
        }
        if (session.gameOver) {
            if (key == KeyEvent.VK_R) {
                retryGame();
            } else if (key == KeyEvent.VK_ESCAPE) {
                backToHome();
            }
            return;
        }
        if (key == KeyEvent.VK_SPACE) {
            togglePause();
            return;
        }
        switch (key) {
            case KeyEvent.VK_UP -> session.turn('U');
            case KeyEvent.VK_DOWN -> session.turn('D');
            case KeyEvent.VK_LEFT -> session.turn('L');
            case KeyEvent.VK_RIGHT -> session.turn('R');
            default -> { }
        }
    }

    @Override
    public void keyTyped(KeyEvent event) { }

    @Override
    public void keyPressed(KeyEvent event) { }
}
