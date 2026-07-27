package com.aether.android.ui.theme

import androidx.compose.ui.graphics.Color

// Deep indigo-black backdrop — retro-arcade dark, not flat OLED black.
val AetherBackground = Color(0xFF12101B)
val AetherSurface1 = Color(0xFF1D1930)
val AetherSurface2 = Color(0xFF262040)

// Text on the dark backdrop.
val AetherTextPrimary = Color(0xFFF5F1E8)
val AetherTextSecondary = Color(0xFFA79FC0)

// Thick block outline — pops against the dark backdrop.
val AetherOutline = Color(0xFFF5F1E8)

// Ink used for text sitting on top of the bright block colors below.
val AetherInk = Color(0xFF181521)

// Bold, flat, retro block colors — one per module, used as solid fills
// (not translucent glass). Chosen bright enough that AetherInk stays readable.
val AetherAmber = Color(0xFFFFC145)   // Life Score
val AetherCoral = Color(0xFFFF6B4A)   // Today's Mission (pending)
val AetherTeal = Color(0xFF3DDC97)    // Today's Mission (done) / Journal
val AetherMagenta = Color(0xFFFF4FA0) // Goals
val AetherSky = Color(0xFF5EC8F2)     // Selected nav / highlights
val AetherViolet = Color(0xFF8C7CFA)  // Coming-soon / not-yet-built modules
