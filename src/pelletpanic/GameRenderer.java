package pelletpanic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/** Rendering only: no persistence, component changes, or game-state mutations. */
final class GameRenderer {
    private static final java.util.Map<Difficulty, java.awt.image.BufferedImage> PREVIEWS = new java.util.EnumMap<>(Difficulty.class);

    static synchronized java.awt.image.BufferedImage mapPreview(Difficulty difficulty) {
        return PREVIEWS.computeIfAbsent(difficulty, selected -> {
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(1100, 724, java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            try {
                new GameRenderer().draw(graphics, 1100, 724,
                        new GameSession(selected.seconds, selected.map(), new java.util.Random(1)), LocalGameStore.starterSkin(), 0);
            } finally {
                graphics.dispose();
            }
            return image;
        });
    }

    void draw(Graphics2D g, int width, int height, GameSession session, Skin skin, int highScore) {
        UiTheme.background(g, width, height);
        UiTheme.quality(g);
        double scale = Math.max(.1, Math.min((width - 72.0) / (session.map[0].length() * 32),
                (height - 180.0) / (session.map.length * 32)));
        double boardWidth = session.map[0].length() * 32 * scale;
        double boardHeight = session.map.length * 32 * scale;
        double x = (width - boardWidth) / 2;
        double y = 95 + (height - 180 - boardHeight) / 2;
        g.setColor(new Color(126, 99, 186, 90));
        g.drawRoundRect((int) x - 9, (int) y - 9, (int) boardWidth + 18, (int) boardHeight + 18, 20, 20);
        Graphics2D board = (Graphics2D) g.create();
        board.translate(x, y);
        board.scale(scale, scale);
        board.setColor(new Color(0x101426));
        board.fillRect(0, 0, session.map[0].length() * 32, session.map.length * 32);
        for (GameSession.Actor wall : session.walls) {
            board.setColor(new Color(0x2E284F));
            board.fillRoundRect(wall.x + 2, wall.y + 2, 28, 28, 7, 7);
            board.setColor(new Color(0x51446E));
            board.drawRoundRect(wall.x + 2, wall.y + 2, 28, 28, 7, 7);
        }
        board.setColor(new Color(0xF2DFA1));
        for (GameSession.Actor food : session.foods) {
            board.fillOval(food.x, food.y, 4, 4);
        }
        for (GameSession.Trail trail : session.fireTrail) {
            sprite(board, "/Images/firetrail.png", trail);
        }
        for (GameSession.Pickup coin : session.coins) {
            sprite(board, "/Images/coin.png", coin);
        }
        for (GameSession.Pickup power : session.powerUps) {
            sprite(board, effectImage(power.effect), power);
        }
        for (GameSession.Actor astronaut : session.astronauts) {
            if (astronaut.inactiveUntil <= session.elapsed) {
                sprite(board, "/Images/Astronaut.png", astronaut);
            }
        }
        if (!session.active(GameSession.Effect.INVINCIBILITY) || (session.elapsed / 200) % 2 == 0) {
            sprite(board, playerImage(session, skin), session.player);
        }
        board.dispose();
        hud(g, width, height, session, highScore);
        if (session.gameOver) {
            gameOver(g, width, height, session, highScore);
        }
    }

    private void hud(Graphics2D g, int width, int height, GameSession session, int highScore) {
        g.setColor(UiTheme.SURFACE);
        g.fillRoundRect(28, 22, width - 56, 50, 16, 16);
        g.setColor(UiTheme.INK);
        String hudText = "Lives: " + session.lives + " | Score: " + session.score + " | Coins: " + session.coinsCollected;
        fit(g, hudText, (int) (width * .59) - 40, 18);
        g.drawString(hudText, 48, 54);
        String record = "High Score: " + highScore;
        fit(g, record, (int) (width * .36), 18);
        g.setColor(UiTheme.CYAN);
        g.drawString(record, width - 48 - g.getFontMetrics().stringWidth(record), 54);
        g.setFont(UiTheme.font(18, true));
        g.setColor(UiTheme.INK);
        g.drawString("Time Left: " + session.timeRemaining() + "s", 40, height - 32);
        int iconX = width - 90;
        for (GameSession.Effect effect : GameSession.Effect.values()) {
            if (session.active(effect)) {
                g.drawImage(GameAssets.image(effectImage(effect)), iconX, height - 60, 30, 30, null);
                long expires = switch (effect) {
                    case FREEZE -> session.freezeUntil;
                    case INVINCIBILITY -> session.invincibleUntil;
                    case TRAIL -> session.trailUntil;
                };
                g.setStroke(new BasicStroke(2));
                g.setColor(UiTheme.CYAN);
                g.drawArc(iconX - 5, height - 65, 40, 40, 90, -(int) (360 * (expires - session.elapsed) / GameSession.EFFECT_MS));
                iconX -= 62;
            }
        }
    }

    private void gameOver(Graphics2D g, int width, int height, GameSession session, int highScore) {
        g.setColor(new Color(5, 5, 18, 205));
        g.fillRect(0, 0, width, height);
        int cardWidth = Math.min(800, width - 70);
        int x = (width - cardWidth) / 2;
        int y = (height - 300) / 2;
        g.setColor(UiTheme.SURFACE);
        g.fillRoundRect(x, y, cardWidth, 300, 28, 28);
        g.setColor(UiTheme.LINE);
        g.drawRoundRect(x, y, cardWidth, 300, 28, 28);
        g.setColor(UiTheme.INK);
        g.setFont(UiTheme.font(44, true));
        center(g, "Game Over!", width, y + 68);
        g.setFont(UiTheme.font(25, true));
        center(g, "Score: " + session.score, width, y + 125);
        g.setColor(UiTheme.CYAN);
        center(g, "High Score: " + highScore, width, y + 170);
        String instructions = "Press 'R' to Restart or 'Esc' to Go back to Home Screen";
        fit(g, instructions, cardWidth - 50, 17);
        g.setColor(UiTheme.MUTED);
        center(g, instructions, width, y + 245);
    }

    private static void fit(Graphics2D g, String text, int available, int size) {
        Font font = UiTheme.font(size, true);
        while (size > 10 && g.getFontMetrics(font).stringWidth(text) > available) {
            font = UiTheme.font(--size, true);
        }
        g.setFont(font);
    }

    private static void center(Graphics2D g, String text, int width, int y) {
        g.drawString(text, (width - g.getFontMetrics().stringWidth(text)) / 2, y);
    }

    private static void sprite(Graphics2D g, String path, GameSession.Actor actor) {
        g.drawImage(GameAssets.image(path), actor.x, actor.y, actor.size, actor.size, null);
    }

    private static String playerImage(GameSession session, Skin skin) {
        if (!session.player.moving) {
            return "/Images/" + skin.getName() + ".png";
        }
        boolean first = (session.elapsed / 120) % 2 == 0;
        return switch (session.player.direction) {
            case 'U' -> first ? skin.getUp1ImagePath() : skin.getUp2ImagePath();
            case 'D' -> first ? skin.getDown1ImagePath() : skin.getDown2ImagePath();
            case 'L' -> first ? skin.getLeft1ImagePath() : skin.getLeft2ImagePath();
            default -> first ? skin.getRight1ImagePath() : skin.getRight2ImagePath();
        };
    }

    private static String effectImage(GameSession.Effect effect) {
        return switch (effect) {
            case FREEZE -> "/Images/ice.png";
            case INVINCIBILITY -> "/Images/invincibility.png";
            case TRAIL -> "/Images/trail.png";
        };
    }
}
