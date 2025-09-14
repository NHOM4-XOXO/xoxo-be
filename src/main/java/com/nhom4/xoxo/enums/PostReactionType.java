package com.nhom4.xoxo.enums;

public enum PostReactionType {
    LIKE("👍", "Like"),
    LOVE("❤️", "Love"),
    CARE("🤗", "Care"),
    HAHA("😂", "Haha"),
    WOW("😮", "Wow"),
    SAD("😢", "Sad"),
    ANGRY("😡", "Angry");

    private final String emoji;
    private final String displayName;

    PostReactionType(String emoji, String displayName) {
        this.emoji = emoji;
        this.displayName = displayName;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getDisplayName() {
        return displayName;
    }
}











