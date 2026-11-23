package pelletpanic;

import java.nio.file.Path;
import java.util.List;

/** Compatibility facade for existing screens; all persistence is local. */
public final class DatabaseManager {
    private static LocalGameStore store;

    private DatabaseManager() {
    }

    private static synchronized LocalGameStore store() {
        if (store == null) {
            store = new LocalGameStore(Path.of(System.getProperty("pelletpanic.saveDir", "data")));
        }
        return store;
    }

    public static void initialize() {
        store().getCoins(1);
    }

    public static void saveGameData(int coinsCollected, int score, int highScore, int playerId) {
        store().saveGameData(coinsCollected, score, highScore, playerId);
    }

    public static int getHighScore(int playerId) {
        return store().getHighScore(playerId);
    }

    public static int getCoins(int playerId) {
        return store().getCoins(playerId);
    }

    public static List<Skin> getAvailableSkins() {
        return store().getSkins(1, false);
    }

    public static List<Skin> getAcquiredSkins() {
        return store().getSkins(1, true);
    }

    public static boolean purchaseSkin(int playerId, String skinName, int price) {
        return store().purchaseSkin(playerId, skinName, price);
    }

    public static Skin getEquippedSkin(int playerId) {
        return store().getEquippedSkin(playerId);
    }

    public static void saveEquippedSkin(int playerId, String skinName) {
        store().equipSkin(playerId, skinName);
    }

    public static void resetPurchases() {
        store().resetPurchases(1);
    }
}
