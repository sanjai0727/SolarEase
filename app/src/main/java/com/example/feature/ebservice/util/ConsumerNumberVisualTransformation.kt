package com.example.feature.ebservice.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class ConsumerNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // format: XX XXX XXX XXXX
        val trimmed = if (text.text.length >= 12) text.text.substring(0..11) else text.text
        
        var output = ""
        for (i in trimmed.indices) {
            output += trimmed[i]
            if (i == 1 || i == 4 || i == 7) output += " "
        }
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset.coerceAtMost(output.length)
                if (offset <= 4) return (offset + 1).coerceAtMost(output.length)
                if (offset <= 7) return (offset + 2).coerceAtMost(output.length)
                return (offset + 3).coerceAtMost(output.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset.coerceAtMost(trimmed.length)
                if (offset <= 6) return (offset - 1).coerceAtMost(trimmed.length)
                if (offset <= 10) return (offset - 2).coerceAtMost(trimmed.length)
                return (offset - 3).coerceAtMost(trimmed.length)
            }
        }
        
        return TransformedText(AnnotatedString(output), offsetMapping)
    }
}
