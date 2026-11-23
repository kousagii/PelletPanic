package pelletpanic;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** Shop view with cached reads and serialized, asynchronous save operations. */
final class ShopPanel extends UiTheme.SpacePanel {
    private static final long serialVersionUID = 1L;
    private final JLabel coins = UiTheme.label("", 21, true);
    private final JTabbedPane tabs = new JTabbedPane();
    private final JButton back = UiTheme.button("BACK", false);
    private final JButton reset = UiTheme.button("Reset Purchases", false);
    private CompletableFuture<Boolean> pending;
    private final Consumer<String> messages;

    ShopPanel(Runnable goBack) {
        this(goBack, null);
    }

    ShopPanel(Runnable goBack, Consumer<String> messages) {
        this.messages = messages == null ? text -> GameMessages.show(this, text) : messages;
        JPanel header = UiTheme.transparent();
        header.setLayout(new BorderLayout(24, 12));
        header.add(UiTheme.label("Shop", 42, true), BorderLayout.WEST);
        coins.setForeground(UiTheme.CYAN);
        header.add(coins, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        tabs.setFont(UiTheme.font(17, true));
        UiTheme.tabs(tabs);
        tabs.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 0, 0));
        tabs.addTab("Acquired Skins", new JPanel());
        tabs.addTab("Buy Skins", new JPanel());
        add(tabs, BorderLayout.CENTER);
        JPanel footer = UiTheme.transparent();
        footer.setLayout(new BorderLayout());
        back.setPreferredSize(new Dimension(160, 52));
        reset.setPreferredSize(new Dimension(235, 52));
        back.addActionListener(event -> whenIdle(goBack));
        reset.addActionListener(event -> mutate(() -> {
            DatabaseManager.resetPurchases();
            return true;
        }, "Your purchases have been reset."));
        footer.add(back, BorderLayout.WEST);
        footer.add(reset, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
        refresh();
    }

    void whenIdle(Runnable action) {
        if (pending != null && !pending.isDone()) {
            pending.whenComplete((ignored, failure) -> SwingUtilities.invokeLater(() -> {
                if (failure == null) {
                    action.run();
                }
            }));
        } else {
            action.run();
        }
    }

    private void refresh() {
        coins.setText("Coins: " + DatabaseManager.getCoins(1));
        String equipped = DatabaseManager.getEquippedSkin(1).getName();
        tabs.setComponentAt(0, UiTheme.scroll(cards(DatabaseManager.getAcquiredSkins(), true, equipped)));
        tabs.setComponentAt(1, UiTheme.scroll(cards(DatabaseManager.getAvailableSkins(), false, equipped)));
        revalidate();
        repaint();
    }

    private JPanel cards(List<Skin> skins, boolean acquired, String equipped) {
        JPanel grid = UiTheme.transparent();
        grid.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 14, 12, 14));
        grid.setLayout(new GridLayout(0, 3, 18, 18));
        for (Skin skin : skins) {
            JPanel card = UiTheme.card();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            JLabel name = UiTheme.label(skin.getName(), 18, true);
            name.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(name);
            card.add(Box.createVerticalStrut(12));
            JLabel image = new JLabel(new ImageIcon(GameAssets.image("/Images/" + skin.getName() + ".png")
                    .getScaledInstance(82, 82, Image.SCALE_REPLICATE)));
            image.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(image);
            card.add(Box.createVerticalStrut(12));
            if (!acquired) {
                JLabel price = UiTheme.label("Cost: " + skin.getPrice() + " Coins", 14, false);
                price.setForeground(UiTheme.MUTED);
                price.setAlignmentX(Component.CENTER_ALIGNMENT);
                card.add(price);
                card.add(Box.createVerticalStrut(12));
            }
            JButton button = UiTheme.button(acquired ? skin.getName().equals(equipped) ? "Equipped" : "Use" : "Buy", !acquired);
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.setMaximumSize(new Dimension(220, 48));
            button.setEnabled(!acquired || !skin.getName().equals(equipped));
            button.addActionListener(event -> {
                if (acquired) {
                    mutate(() -> {
                        DatabaseManager.saveEquippedSkin(1, skin.getName());
                        return true;
                    }, null);
                } else if (DatabaseManager.getCoins(1) < skin.getPrice()) {
                    GameAudio.play(Sound.ERROR);
                    messages.accept("You don't have enough coins to buy: " + skin.getName());
                } else {
                    mutate(() -> DatabaseManager.purchaseSkin(1, skin.getName(), skin.getPrice()), "You bought: " + skin.getName());
                }
            });
            card.add(button);
            grid.add(card);
        }
        // Anchor short catalogues at the top rather than stretching cards vertically.
        JPanel wrapper = UiTheme.transparent();
        wrapper.setLayout(new BorderLayout());
        wrapper.add(grid, BorderLayout.NORTH);
        return wrapper;
    }

    private void mutate(Callable<Boolean> operation, String successMessage) {
        if (pending != null && !pending.isDone()) {
            return;
        }
        enabled(this, false);
        pending = GamePersistence.submit(operation);
        pending.whenComplete((success, failure) -> SwingUtilities.invokeLater(() -> {
            enabled(this, true);
            refresh(); // Refreshes both tabs and equipment after every committed mutation.
            if (failure != null) {
                GameAudio.play(Sound.ERROR);
                Throwable cause = failure.getCause() == null ? failure : failure.getCause();
                GameMessages.show(this, cause.getMessage(), "Could not open local saves");
            } else if (Boolean.TRUE.equals(success) && successMessage != null) {
                GameAudio.play(Sound.PURCHASE);
                messages.accept(successMessage);
            } else if (Boolean.FALSE.equals(success)) {
                GameAudio.play(Sound.ERROR);
            }
        }));
    }

    private static void enabled(java.awt.Container parent, boolean value) {
        for (Component child : parent.getComponents()) {
            child.setEnabled(value);
            if (child instanceof java.awt.Container container) {
                enabled(container, value);
            }
        }
    }
}
