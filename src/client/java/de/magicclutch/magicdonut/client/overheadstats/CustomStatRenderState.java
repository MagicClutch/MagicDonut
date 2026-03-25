package de.magicclutch.magicdonut.client.overheadstats;

public interface CustomStatRenderState {
    String magicdonut$getStatText();

    void magicdonut$setStatText(String text);

    int magicdonut$getStatColor();

    void magicdonut$setStatColor(int color);

    boolean magicdonut$hasStatText();

    void magicdonut$clearDisplayData();

    String magicdonut$getCanonicalUsername();

    void magicdonut$setCanonicalUsername(String username);

    boolean magicdonut$hasDrawn();

    void magicdonut$setDrawn(boolean drawn);
}