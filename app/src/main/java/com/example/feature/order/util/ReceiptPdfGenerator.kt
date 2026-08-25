package com.example.feature.order.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.feature.order.ui.OrderConfirmationUiState
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

object ReceiptPdfGenerator {
    fun generateAndSharePdf(context: Context, state: OrderConfirmationUiState) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        // Formatting
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        currencyFormat.maximumFractionDigits = 0

        // Title
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 24f
        paint.color = Color.BLACK
        canvas.drawText("SolarEase", 50f, 60f, paint)

        paint.textSize = 20f
        canvas.drawText("Order Receipt", 50f, 100f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 14f
        canvas.drawText("Reference ID: ${state.orderReference}", 50f, 130f, paint)
        canvas.drawText("Date: ${state.orderDate}", 50f, 150f, paint)

        // Customer Details
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 16f
        canvas.drawText("Customer Details", 50f, 200f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 14f
        canvas.drawText("Name: ${state.username}", 50f, 225f, paint)
        canvas.drawText("Mobile: ${state.mobileNumber}", 50f, 245f, paint)
        canvas.drawText("Address: ${state.installationAddress}", 50f, 265f, paint)

        // Solar System Details
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 16f
        canvas.drawText("Solar System Details", 50f, 315f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 14f
        canvas.drawText("Capacity: ${state.recommendedCapacityKW} kW", 50f, 340f, paint)
        canvas.drawText("System Type: ${state.systemType}", 50f, 360f, paint)
        canvas.drawText("Mount Type: ${state.mountType}", 50f, 380f, paint)
        canvas.drawText("Panel Type: ${state.panelType}", 50f, 400f, paint)

        // Payment Summary
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 16f
        canvas.drawText("Payment Summary", 50f, 450f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 14f
        canvas.drawText("Gross Total: ${currencyFormat.format(state.grossTotal)}", 50f, 475f, paint)
        canvas.drawText("Government Subsidy: -${currencyFormat.format(state.subsidyAmount)}", 50f, 495f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Net Amount Paid: ${currencyFormat.format(state.netPayable)}", 50f, 520f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Payment Method: ${state.paymentMethod}", 50f, 545f, paint)
        canvas.drawText("Payment Reference: ${state.paymentReferenceId}", 50f, 565f, paint)
        canvas.drawText("Payment Status: ${state.paymentStatus}", 50f, 585f, paint)

        // Footer
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.textSize = 14f
        canvas.drawText("Thank you for choosing SolarEase.", 50f, 650f, paint)

        pdfDocument.finishPage(page)

        // Save PDF to cache dir to share it easily
        val fileName = "SolarEase_Receipt_${state.orderReference}.pdf"
        val directory = File(context.cacheDir, "receipts")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, fileName)
        
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        pdfDocument.close()

        // Share the file
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "SolarEase Order Receipt")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Receipt PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
