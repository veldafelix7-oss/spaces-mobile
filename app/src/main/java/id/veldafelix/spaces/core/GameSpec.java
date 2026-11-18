package id.veldafelix.spaces.core;

public final class GameSpec {
    public final String id, title, subtitle;
    public GameSpec(String id, String title, String subtitle) { this.id=id; this.title=title; this.subtitle=subtitle; }
    public static final GameSpec[] ALL = {
        new GameSpec("com.tencent.igfit", "PUBGM FLASH", "BATTLE ROYALE"),
        new GameSpec("com.netease.newspike", "Blood Strike", "FAST-PACED FPS")
    };
    public static GameSpec find(String id) {
        for (GameSpec game : ALL) if (game.id.equals(id)) return game;
        return null;
    }
}
