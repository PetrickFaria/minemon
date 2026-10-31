package br.com.petrickfaria.minemon.client;

/** Shared rules for Rocket-only eye overlays. Normal creature renders never call these rules. */
public final class RocketVisuals {
    public static boolean isEyeCubeBone(String rawName) {
        String name = rawName.toLowerCase(java.util.Locale.ROOT);
        if (name.contains("eyelid") || name.contains("eyebrow")) return false;
        return name.equals("eyes")
                || name.matches("(small_)?eye_(left|right)[0-9]*")
                || name.matches("kid_eye_(left|right)")
                || name.matches("kid_eye_(angry|sad)_(left|right)")
                || name.matches("eye_emote_(left|right)[0-9]*")
                || name.matches("iris(_emote)?_(left|right)")
                || name.matches("pupils?_(left|right)")
                || name.matches("pupil_(surprised|happy|top|bottom)_(left|right)")
                || name.matches("small_pupil_(left|right)")
                || name.matches("kid_pupil_(left|right)")
                || name.equals("eyes_ditto")
                || name.equals("eyes_blink_ditto")
                || name.equals("ditto_eyes");
    }

    private RocketVisuals() { }
}
