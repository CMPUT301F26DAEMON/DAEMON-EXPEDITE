package com.example.daemon_expedite.voip.filters

/**
 * The filters a caller can put on their own camera during a video call. They are applied to the
 * frames before they are sent, so the other person sees them too.
 *
 * Colour filters change the picture's colours. Face filters track the caller's face (on the
 * phone, with ML Kit; see [FaceTracker]) and draw a sticker on it (see [Stickers]).
 *
 * @param label Shown under the filter's button.
 * @param badge Shown inside the filter's button.
 * @param tracksFace True for filters that need face tracking.
 */
enum class VideoFilter(
    val label: String,
    val badge: String,
    val tracksFace: Boolean = false,
) {
    NONE("None", "✕"),

    // Face filters
    SHADES("Shades", "🕶️", tracksFace = true),
    PUPPY("Puppy", "🐶", tracksFace = true),
    CROWN("Crown", "👑", tracksFace = true),
    STACHE("Stache", "🥸", tracksFace = true),
    LOVE("Love", "😍", tracksFace = true),

    // Colour filters
    MONO("Mono", "◐"),
    NOIR("Noir", "●"),
    SEPIA("Sepia", "☕"),
    WARM("Warm", "☀️"),
    COOL("Cool", "❄️"),
    VIVID("Vivid", "🌈"),
}
