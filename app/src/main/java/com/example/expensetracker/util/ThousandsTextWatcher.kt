package com.example.expensetracker.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.NumberFormat
import java.util.Locale

/**
 * TextWatcher yang otomatis nambahin titik ribuan pas user ngetik angka,
 * contoh: ketik "700000" otomatis jadi "700.000".
 *
 * Cara pakai:
 *   editText.addTextChangedListener(ThousandsTextWatcher(editText))
 *
 * Pas mau ambil nilai aslinya (tanpa titik) buat disimpan ke database, pakai:
 *   ThousandsTextWatcher.parseRawNumber(editText.text.toString())
 */
class ThousandsTextWatcher(private val editText: EditText) : TextWatcher {

    private var current = ""

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        val str = s.toString()
        if (str == current) return

        editText.removeTextChangedListener(this)

        val cleanString = str.replace(".", "")
        if (cleanString.isEmpty()) {
            current = ""
            editText.addTextChangedListener(this)
            return
        }

        val parsed = cleanString.toLongOrNull()
        if (parsed == null) {
            // Kalau ada karakter aneh (bukan angka), balikin ke teks sebelumnya
            editText.setText(current)
            editText.setSelection(current.length)
            editText.addTextChangedListener(this)
            return
        }

        val formatted = formatWithDots(parsed)
        current = formatted
        editText.setText(formatted)
        editText.setSelection(formatted.length)

        editText.addTextChangedListener(this)
    }

    companion object {
        fun formatWithDots(value: Long): String {
            val formatter = NumberFormat.getNumberInstance(Locale("in", "ID"))
            return formatter.format(value)
        }

        /** Ambil angka mentah (tanpa titik) dari teks yang udah diformat, buat disimpan ke database */
        fun parseRawNumber(formattedText: String): Double {
            val clean = formattedText.replace(".", "")
            return clean.toDoubleOrNull() ?: 0.0
        }
    }
}
