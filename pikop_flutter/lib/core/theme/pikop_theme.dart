import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class PikopTheme {
  static const Color black = Color(0xFF0B0B0B);
  static const Color gold = Color(0xFFFF9F0A);
  static const Color orange = Color(0xFFFF5722);
  static const Color green = Color(0xFF8BC34A);
  static const Color white = Color(0xFFFFFFFF);
  static const Color grey = Color(0xFF64748B);

  static ThemeData get darkTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      primaryColor: gold,
      scaffoldBackgroundColor: black,
      colorScheme: const ColorScheme.dark(
        primary: gold,
        secondary: orange,
        surface: Color(0xFF1E293B),
        onPrimary: black,
      ),
      textTheme: GoogleFonts.interTextTheme(ThemeData.dark().textTheme).copyWith(
        headlineMedium: GoogleFonts.inter(
          fontWeight: FontWeight.w800,
          color: white,
        ),
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: black,
        elevation: 0,
        centerTitle: true,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: gold,
          foregroundColor: black,
          textStyle: const TextStyle(fontWeight: FontWeight.bold),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
      ),
    );
  }
}
