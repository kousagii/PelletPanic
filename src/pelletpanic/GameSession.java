package pelletpanic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.EnumSet;
import java.util.Set;

/** Pure session rules. All deadlines use simulation time, so pause freezes everything. */
final class GameSession {
    static final int TILE = 32;
    static final int STEP_MS = 40;
    static final long EFFECT_MS = 10_000;
    enum Effect { FREEZE, INVINCIBILITY, TRAIL }

    static class Actor {
        int x;
        int y;
        final int startX;
        final int startY;
        final int size;
        char direction = 'U';
        boolean moving;
        long inactiveUntil;

        Actor(int x, int y, int size) {
            this.x = startX = x;
            this.y = startY = y;
            this.size = size;
        }

        void reset() {
            x = startX;
            y = startY;
            moving = false;
            inactiveUntil = 0;
        }
    }

    static final class Pickup extends Actor {
        final long spawnedAt;
        final Effect effect;

        Pickup(int x, int y, long now, Effect effect) {
            super(x, y, TILE);
            spawnedAt = now;
            this.effect = effect;
        }
    }

    static final class Trail extends Actor {
        final long expiresAt;

        Trail(int x, int y, long now) {
            super(x, y, TILE);
            expiresAt = now + 2_000;
        }
    }

    final List<Actor> walls = new ArrayList<>();
    final List<Actor> foods = new ArrayList<>();
    final List<Actor> astronauts = new ArrayList<>();
    final List<Pickup> coins = new ArrayList<>();
    final List<Pickup> powerUps = new ArrayList<>();
    final List<Trail> fireTrail = new ArrayList<>();
    final int durationSeconds;
    final String[] map;
    final Random random;
    private final EnumSet<GameEvent> events = EnumSet.noneOf(GameEvent.class);
    Actor player;
    int score;
    int lives = 3;
    int coinsCollected;
    long elapsed;
    long roundStartedAt;
    long freezeUntil;
    long invincibleUntil;
    long trailUntil;
    long nextTrailAt;
    long nextCoinAt;
    long nextPowerUpAt;
    boolean running;
    boolean paused;
    boolean gameOver;

    GameSession(int durationSeconds, String[] map, Random random) {
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("Game duration must be positive.");
        }
        validateMap(map);
        this.durationSeconds = durationSeconds;
        this.map = map.clone();
        this.random = random;
        loadRound();
    }

    void start() {
        if (!gameOver) {
            running = true;
        }
    }

    Set<GameEvent> drainEvents() {
        Set<GameEvent> result = EnumSet.copyOf(events);
        events.clear();
        return result;
    }

    void stop() {
        running = false;
    }

    void setPaused(boolean paused) {
        if (running && !gameOver) {
            this.paused = paused;
        }
    }

    int timeRemaining() {
        return (int) Math.max(0, durationSeconds - (elapsed - roundStartedAt) / 1_000);
    }

    boolean active(Effect effect) {
        return switch (effect) {
            case FREEZE -> elapsed < freezeUntil;
            case INVINCIBILITY -> elapsed < invincibleUntil;
            case TRAIL -> elapsed < trailUntil;
        };
    }

    void turn(char direction) {
        if (!running || paused || gameOver) {
            return;
        }
        if (canMove(player, direction)) {
            player.direction = direction;
            player.moving = true;
        }
    }

    void tick() {
        if (!running || paused || gameOver) {
            return;
        }
        elapsed += STEP_MS;
        if (timeRemaining() == 0) {
            finish();
            return;
        }
        coins.removeIf(coin -> elapsed - coin.spawnedAt >= 20_000);
        powerUps.removeIf(power -> elapsed - power.spawnedAt >= 20_000);
        fireTrail.removeIf(trail -> elapsed >= trail.expiresAt);
        if (!active(Effect.TRAIL)) {
            fireTrail.clear();
        }
        if (player.moving) {
            move(player);
        }
        for (Iterator<Actor> iterator = foods.iterator(); iterator.hasNext();) {
            if (collision(player, iterator.next())) {
                iterator.remove();
                score += 10;
                events.add(GameEvent.PELLET);
            }
        }
        for (Iterator<Pickup> iterator = coins.iterator(); iterator.hasNext();) {
            if (collision(player, iterator.next())) {
                iterator.remove();
                coinsCollected++;
                events.add(GameEvent.COIN);
            }
        }
        for (Iterator<Pickup> iterator = powerUps.iterator(); iterator.hasNext();) {
            Pickup power = iterator.next();
            if (collision(player, power)) {
                apply(power.effect);
                iterator.remove();
            }
        }
        if (active(Effect.TRAIL) && elapsed >= nextTrailAt) {
            fireTrail.add(new Trail(player.x, player.y, elapsed));
            nextTrailAt = elapsed + 200;
        }
        for (Actor astronaut : astronauts) {
            if (astronaut.inactiveUntil > elapsed) {
                continue;
            }
            if (astronaut.inactiveUntil != 0) {
                astronaut.reset();
                chooseDirection(astronaut);
            }
            if (!active(Effect.FREEZE)) {
                if (!move(astronaut)) {
                    chooseDirection(astronaut);
                }
            }
            boolean burned = false;
            for (Iterator<Trail> iterator = fireTrail.iterator(); iterator.hasNext();) {
                if (collision(astronaut, iterator.next())) {
                    iterator.remove();
                    astronaut.inactiveUntil = elapsed + EFFECT_MS;
                    events.add(GameEvent.ENEMY_BURN);
                    burned = true;
                    break;
                }
            }
            if (!burned && collision(player, astronaut) && !active(Effect.INVINCIBILITY)) {
                lives--;
                events.add(GameEvent.HIT);
                if (lives == 0) {
                    finish();
                    return;
                }
                resetPositions();
                break; // One life lost per collision step, even with overlapping enemies.
            }
        }
        if (foods.isEmpty()) {
            events.add(GameEvent.ROUND_CLEAR);
            loadRound();
            return;
        }
        if (elapsed >= nextCoinAt) {
            spawn(false);
            nextCoinAt = elapsed + 4_500;
        }
        if (elapsed >= nextPowerUpAt) {
            spawn(true);
            nextPowerUpAt = elapsed + 20_000;
        }
    }

    void apply(Effect effect) {
        events.add(GameEvent.POWER_UP);
        switch (effect) {
            case FREEZE -> freezeUntil = elapsed + EFFECT_MS;
            case INVINCIBILITY -> invincibleUntil = elapsed + EFFECT_MS;
            case TRAIL -> {
                trailUntil = elapsed + EFFECT_MS;
                nextTrailAt = elapsed;
            }
        }
    }

    void finish() {
        if (!gameOver) {
            events.add(GameEvent.GAME_OVER);
        }
        gameOver = true;
        running = false;
        paused = false;
    }

    private void loadRound() {
        walls.clear();
        foods.clear();
        astronauts.clear();
        coins.clear();
        powerUps.clear();
        fireTrail.clear();
        freezeUntil = invincibleUntil = trailUntil = 0;
        roundStartedAt = nextCoinAt = nextPowerUpAt = elapsed;
        nextTrailAt = elapsed;
        for (int row = 0; row < map.length; row++) {
            for (int column = 0; column < map[row].length(); column++) {
                int x = column * TILE;
                int y = row * TILE;
                switch (map[row].charAt(column)) {
                    case 'X' -> walls.add(new Actor(x, y, TILE));
                    case ' ' -> foods.add(new Actor(x + 14, y + 14, 4));
                    case 'A' -> astronauts.add(new Actor(x, y, TILE));
                    case 'P' -> player = new Actor(x, y, TILE);
                    default -> throw new IllegalArgumentException("Unexpected map tile.");
                }
            }
        }
        resetPositions();
    }

    private void resetPositions() {
        player.reset();
        for (Actor astronaut : astronauts) {
            astronaut.reset();
            chooseDirection(astronaut);
        }
    }

    private void chooseDirection(Actor actor) {
        char[] directions = {'U', 'D', 'L', 'R'};
        actor.direction = directions[random.nextInt(directions.length)];
        actor.moving = true;
    }

    private boolean move(Actor actor) {
        if (!canMove(actor, actor.direction)) {
            return false;
        }
        actor.x += dx(actor.direction);
        actor.y += dy(actor.direction);
        return true;
    }

    private boolean canMove(Actor actor, char direction) {
        int x = actor.x + dx(direction);
        int y = actor.y + dy(direction);
        if (x < 0 || y < 0 || x + actor.size > map[0].length() * TILE || y + actor.size > map.length * TILE) {
            return false;
        }
        for (Actor wall : walls) {
            if (x < wall.x + wall.size && x + actor.size > wall.x && y < wall.y + wall.size && y + actor.size > wall.y) {
                return false;
            }
        }
        return true;
    }

    private void spawn(boolean power) {
        int row = random.nextInt(map.length);
        int column = random.nextInt(map[0].length());
        if (map[row].charAt(column) == ' ') {
            Effect effect = power ? Effect.values()[random.nextInt(Effect.values().length)] : null;
            Pickup pickup = new Pickup(column * TILE, row * TILE, elapsed, effect);
            (power ? powerUps : coins).add(pickup);
        }
    }

    static boolean collision(Actor a, Actor b) {
        return a.x < b.x + b.size && a.x + a.size > b.x && a.y < b.y + b.size && a.y + a.size > b.y;
    }

    private static int dx(char direction) {
        return direction == 'L' ? -8 : direction == 'R' ? 8 : 0;
    }

    private static int dy(char direction) {
        return direction == 'U' ? -8 : direction == 'D' ? 8 : 0;
    }

    private static void validateMap(String[] rows) {
        if (rows == null || rows.length == 0 || rows[0].isEmpty()) {
            throw new IllegalArgumentException("Map must not be empty.");
        }
        int players = 0;
        for (String row : rows) {
            if (row.length() != rows[0].length()) {
                throw new IllegalArgumentException("Map rows must have equal width.");
            }
            for (char tile : row.toCharArray()) {
                if (tile == 'P') {
                    players++;
                } else if (tile != ' ' && tile != 'X' && tile != 'A') {
                    throw new IllegalArgumentException("Unexpected map tile.");
                }
            }
        }
        if (players != 1) {
            throw new IllegalArgumentException("Map must contain one player.");
        }
    }
}
