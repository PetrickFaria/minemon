package br.com.petrickfaria.minemon.menu;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Shared by the terminal renderer and its formatting tests. */
public final class SpawnChanceLabel {
    private SpawnChanceLabel() { }
    public static String format(double chance, long increments, boolean enabled, Locale locale) {
        if (!enabled) return "0%";
        double percent = Double.isFinite(chance) ? Math.max(0, Math.min(.5D, chance)) * 100 : 0;
        DecimalFormat format = new DecimalFormat("0.####", DecimalFormatSymbols.getInstance(locale));
        return format.format(percent) + "% (" + Math.max(0, increments) + ")";
    }
}
