package br.com.petrickfaria.minemon.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.*;

public class OwnershipNameplateTest {
    @Test public void nestedCustomNamesAndLevelsUseOwnershipColorWithoutChangingText() {
        Component custom = Component.literal("Minha ").append(Component.literal("Goldeen").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        for (boolean tame : new boolean[]{true, false}) {
            for (Component input : new Component[]{custom, Component.literal("Nível: 25")}) {
                Component output = OwnershipNameplate.tint(input, tame);
                assertEquals(input.getString(), output.getString());
                output.visit((style, text) -> {
                    assertEquals(tame ? 0x008CFF : 0xFF5555, style.getColor().getValue());
                    return Optional.empty();
                }, Style.EMPTY);
            }
        }
    }
}
