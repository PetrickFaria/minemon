package br.com.petrickfaria.minemon.menu;

/** Pure logical-to-screen geometry shared by the PokéC hit testing and tests. */
public final class PokecScrollbarGeometry {
    public static final int X = 8 + 136 - 8;
    public static final int Y = 27 + 13;
    public static final int WIDTH = 2;

    public static boolean containsScaledPoint(double mouseX, double mouseY,
                                              double originX, double originY, float scale,
                                              int logicalX, int logicalY, int logicalWidth, int logicalHeight) {
        return scale > 0
                && mouseX >= originX + logicalX * scale
                && mouseX < originX + (logicalX + logicalWidth) * scale
                && mouseY >= originY + logicalY * scale
                && mouseY < originY + (logicalY + logicalHeight) * scale;
    }

    private PokecScrollbarGeometry() { }
}
