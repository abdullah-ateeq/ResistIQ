package com.example.resistiq.model

import androidx.compose.ui.graphics.Color

enum class ResistorColor(
    val displayName: String,
    val color: Color,
    val textColor: Color
) {
    BLACK("Black", Color(0xFF212121), Color.White),
    BROWN("Brown", Color(0xFF8D5B4C), Color.White),
    RED("Red", Color(0xFFE53935), Color.White),
    ORANGE("Orange", Color(0xFFFB8C00), Color.Black),
    YELLOW("Yellow", Color(0xFFFDD835), Color.Black),
    GREEN("Green", Color(0xFF43A047), Color.White),
    BLUE("Blue", Color(0xFF1E88E5), Color.White),
    VIOLET("Violet", Color(0xFF8E24AA), Color.White),
    GRAY("Gray", Color(0xFF757575), Color.White),
    WHITE("White", Color(0xFFEEEEEE), Color.Black),
    GOLD("Gold", Color(0xFFD4AF37), Color.Black),
    SILVER("Silver", Color(0xFFC0C0C0), Color.Black)
}
