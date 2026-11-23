package pelletpanic;

/** Distinct authored layouts; each passable cell is reachable from the player. */
enum Difficulty {
    EASY("Easy", 120, new String[] {
        "XXXXXXXXXXXXXXXXXXXXXXXXXXXX",
        "X                          X",
        "X P                        X",
        "X   XXXX    XXXX    XXXX   X",
        "X   XXXX    XXXX    XXXX   X",
        "X                          X",
        "X                          X",
        "X  XXXXXXX   XX   XXXXXXX  X",
        "X            XX            X",
        "X            XX            X",
        "X            XX            X",
        "X   XXXX     AA     XXXX   X",
        "X   XXXX     AA     XXXX   X",
        "X          XXXXXX          X",
        "X          XXXXXX          X",
        "X                          X",
        "XXXXXXXXXXXXXXXXXXXXXXXXXXXX"
    }),
    MEDIUM("Medium", 90, new String[] {
        "XXXXXXXXXXXXXXXXXXXXXXXXXXXX",
        "X            P             X",
        "X  XXXXXXXX      XXXXXXXX  X",
        "X     XX            XX     X",
        "X     XX  XXX  XXX  XX     X",
        "X XX  XX  XXX  XXX  XX  XX X",
        "X                          X",
        "X XXXXXXXX  X  X  XXXXXXXX X",
        "X           XA X           X",
        "X   XXXXX   XAAX   XXXXX   X",
        "X           XAAX           X",
        "X XXXXXXXXX      XXXXXXXXX X",
        "X                          X",
        "X    XX   XX    XX   XX    X",
        "X    XX   XX    XX   XX    X",
        "X                          X",
        "XXXXXXXXXXXXXXXXXXXXXXXXXXXX"
    }),
    HARD("Hard", 90, new String[] {
        "XXXXXXXXXXXXXXXXXXXXXXXXXXXX",
        "XP                         X",
        "XXXXXX XXXXXX XXXXXXX XXXXXX",
        "X         X                X",
        "XXX XXXXXX XXXXXX XXXXXX XXX",
        "X                 X        X",
        "XXXXXX XXXXXX XXXXXXX XXXXXX",
        "X      X                   X",
        "XXX XXXXXX XXXXXX XXXXXX XXX",
        "X          AAAAAA          X",
        "XXXXXX XXXXXX XXXXXXX XXXXXX",
        "X                   X      X",
        "XXX XXXXXX XXXXXX XXXXXX XXX",
        "X          X               X",
        "XXXXXX XXXXXX XXXXXXX XXXXXX",
        "X                          X",
        "XXXXXXXXXXXXXXXXXXXXXXXXXXXX"
    });

    final String label;
    final int seconds;
    private final String[] rows;

    Difficulty(String label, int seconds, String[] rows) {
        this.label = label;
        this.seconds = seconds;
        this.rows = rows.clone();
    }

    String[] map() {
        return rows.clone();
    }
}
