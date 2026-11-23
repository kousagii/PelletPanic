package pelletpanic;

enum Sound {
    CLICK("click.wav", .65f, 40),
    PELLET("pellet.wav", .32f, 80),
    COIN("coin.wav", .8f, 60),
    POWER_UP("power-up.wav", .65f, 80),
    HIT("hit.wav", .7f, 120),
    ENEMY_BURN("enemy-burn.wav", .6f, 100),
    ROUND_CLEAR("round-clear.wav", .8f, 100),
    GAME_OVER("game-over.wav", .9f, 100),
    PURCHASE("purchase.wav", .8f, 80),
    ERROR("error.wav", .65f, 100);

    final String file;
    final float gain;
    final long cooldownNanos;

    Sound(String file, float gain, long cooldownMillis) {
        this.file = file;
        this.gain = gain;
        cooldownNanos = cooldownMillis * 1_000_000;
    }
}
