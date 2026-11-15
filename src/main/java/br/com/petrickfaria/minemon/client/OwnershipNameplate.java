package br.com.petrickfaria.minemon.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import java.util.Optional;

/** Explicit RGB avoids vanilla BLUE's purple-biased #5555ff. Preserve the name and other styles. */
public final class OwnershipNameplate {
    public static final int TAMED_BLUE = 0x008CFF;
    public static final int WILD_RED = 0xFF5555;
    private OwnershipNameplate() { }

    public static Component tint(Component text, boolean tame) {
        MutableComponent result = Component.empty();
        TextColor color = TextColor.fromRgb(tame ? TAMED_BLUE : WILD_RED);
        text.visit((style, content) -> {
            result.append(Component.literal(content).setStyle(style.withColor(color)));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }
}
