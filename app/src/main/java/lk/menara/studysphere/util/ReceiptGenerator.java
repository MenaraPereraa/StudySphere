package lk.menara.studysphere.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import lk.menara.studysphere.model.Order;
import lk.menara.studysphere.model.Payment;
import lk.menara.studysphere.model.Product;

public class ReceiptGenerator {

    private static final DeviceRgb C_PRIMARY     = new DeviceRgb(30,  90,  80);
    private static final DeviceRgb C_ACCENT      = new DeviceRgb(180, 225, 210);
    private static final DeviceRgb C_ACCENT_MID  = new DeviceRgb(100, 185, 160);
    private static final DeviceRgb C_SURFACE     = new DeviceRgb(242, 249, 246);
    private static final DeviceRgb C_TABLE_ALT   = new DeviceRgb(232, 245, 240);
    private static final DeviceRgb C_DIVIDER     = new DeviceRgb(200, 220, 215);
    private static final DeviceRgb C_TEXT        = new DeviceRgb(30,  40,  38);
    private static final DeviceRgb C_MUTED       = new DeviceRgb(105, 125, 120);
    private static final DeviceRgb C_WHITE       = new DeviceRgb(255, 255, 255);

    private static final float MARGIN_TOP    = 50f;
    private static final float MARGIN_BOTTOM = 50f;
    private static final float MARGIN_LEFT   = 40f;
    private static final float MARGIN_RIGHT  = 40f;
    private static final float CONTENT_W = PageSize.A4.getWidth() - MARGIN_LEFT - MARGIN_RIGHT;


    public static File generateReceipt(Context context, Order order,
                                       Payment payment, Map<String, Product> productMap) {
        try {
            File dir = new File(context.getFilesDir(), "Receipts");
            if (!dir.exists()) dir.mkdirs();

            File pdf = new File(dir, "Receipt_" + order.getOrderId() + ".pdf");

            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(pdf));
            pdfDoc.setDefaultPageSize(PageSize.A4);

            Document doc = new Document(pdfDoc, PageSize.A4);
            doc.setMargins(MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM, MARGIN_LEFT);

            PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont mono    = PdfFontFactory.createFont(StandardFonts.COURIER);

            buildHeader(doc, bold, regular);
            buildMeta(doc, bold, regular, mono, order, payment);
            divider(doc);
            buildCustomer(doc, bold, regular, order);
            divider(doc);
            buildItemsTable(doc, bold, regular, order, productMap);
            buildTotal(doc, bold, order);
            buildFooter(doc, bold, regular);

            doc.close();
            return pdf;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void shareReceipt(Context context, File pdfFile) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    pdfFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Open Receipt"));
        } catch (Exception e) {
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private static void buildHeader(Document doc, PdfFont bold, PdfFont regular) {

        Table banner = fullWidthTable(new float[]{60, 40})
                .setBackgroundColor(C_PRIMARY);

        Cell brandCell = noBorderCell()
                .setPaddingTop(16).setPaddingBottom(16)
                .setPaddingLeft(16).setPaddingRight(8);
        brandCell.add(new Paragraph("StudySphere")
                .setFont(bold).setFontSize(22).setFontColor(C_WHITE).setMarginBottom(2));
        brandCell.add(new Paragraph("Your Learning Marketplace")
                .setFont(regular).setFontSize(8).setFontColor(C_ACCENT));

        Cell badgeCell = noBorderCell()
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                .setPaddingRight(16);
        badgeCell.add(new Paragraph("RECEIPT")
                .setFont(bold).setFontSize(9)
                .setFontColor(C_PRIMARY).setBackgroundColor(C_ACCENT)
                .setPaddingTop(6).setPaddingBottom(6)
                .setPaddingLeft(12).setPaddingRight(12)
                .setCharacterSpacing(1.4f)
                .setTextAlignment(TextAlignment.CENTER));

        banner.addCell(brandCell);
        banner.addCell(badgeCell);

        Table strip = fullWidthTable(new float[]{100})
                .setBackgroundColor(C_ACCENT_MID).setHeight(3).setMarginBottom(16);
        strip.addCell(noBorderCell());

        doc.add(banner);
        doc.add(strip);
    }

    private static void buildMeta(Document doc, PdfFont bold, PdfFont regular,
                                  PdfFont mono, Order order, Payment payment) {
        // FIX: show the real order date (fall back to now only if it is missing)
        Date issued = (order.getOrderDate() != null)
                ? order.getOrderDate().toDate()
                : new Date();
        String date = new SimpleDateFormat("dd MMMM yyyy  •  HH:mm:ss",
                Locale.getDefault()).format(issued);

        Table t = fullWidthTable(new float[]{50, 50}).setMarginBottom(4);

        t.addCell(lbl("Order ID",       bold));
        t.addCell(lbl("Payment ID",     bold));
        t.addCell(val(order.getOrderId(),     mono));
        t.addCell(val(payment.getPaymentId(), mono));
        t.addCell(lbl("Transaction ID", bold));
        // FIX: label changed from "Issued On" to "Order Date"
        t.addCell(lbl("Order Date",     bold));
        t.addCell(val(payment.getTransactionId(), mono));
        t.addCell(val(date,                       regular));

        doc.add(t);
    }

    private static void buildCustomer(Document doc, PdfFont bold, PdfFont regular, Order order) {
        doc.add(sectionHeading("Customer Details", bold));

        if (order.getShippingAddress() == null) {
            doc.add(new Paragraph("No shipping address provided.")
                    .setFont(regular).setFontSize(9).setFontColor(C_MUTED));
            return;
        }

        Table card = fullWidthTable(new float[]{33, 34, 33})
                .setBackgroundColor(C_SURFACE)
                .setBorder(new SolidBorder(C_DIVIDER, 0.5f))
                .setMarginBottom(4);

        card.addCell(field("Name",    nvl(order.getShippingAddress().getName()),    bold, regular));
        card.addCell(field("Email",   nvl(order.getShippingAddress().getEmail()),   bold, regular));
        card.addCell(field("Contact", nvl(order.getShippingAddress().getContact()), bold, regular));

        doc.add(card);
    }

    private static void buildItemsTable(Document doc, PdfFont bold, PdfFont regular,
                                        Order order, Map<String, Product> productMap) {
        doc.add(sectionHeading("Order Items", bold));

        Table table = fullWidthTable(new float[]{7, 51, 14, 28});

        // Header
        String[]        hdrs   = {"#", "Item", "Qty", "Amount (LKR)"};
        TextAlignment[] aligns = {TextAlignment.CENTER, TextAlignment.LEFT,
                TextAlignment.CENTER, TextAlignment.RIGHT};
        for (int i = 0; i < hdrs.length; i++) {
            table.addHeaderCell(new Cell()
                    .setBackgroundColor(C_PRIMARY).setBorder(Border.NO_BORDER)
                    .setPaddingTop(8).setPaddingBottom(8)
                    .setPaddingLeft(i == 0 ? 6 : 8).setPaddingRight(i == 3 ? 10 : 8)
                    .add(new Paragraph(hdrs[i])
                            .setFont(bold).setFontSize(8)
                            .setFontColor(C_WHITE).setTextAlignment(aligns[i])));
        }

        if (order.getOrderItems() != null) {
            int row = 0;
            for (Order.OrderItem item : order.getOrderItems()) {
                Product   p      = productMap.get(item.getProductId());
                String    name   = p != null ? p.getName() : "Unknown Item";
                double    amount = item.getUnitPrice() * item.getQuantity();
                DeviceRgb bg     = (row % 2 == 0) ? C_WHITE : C_TABLE_ALT;

                table.addCell(dataCell(String.valueOf(row + 1),
                        bg, regular, 8.5f, TextAlignment.CENTER, 6, 6));
                table.addCell(dataCell(name,
                        bg, regular, 9f, TextAlignment.LEFT, 8, 8));
                table.addCell(dataCell(String.valueOf(item.getQuantity()),
                        bg, regular, 9f, TextAlignment.CENTER, 8, 8));
                table.addCell(dataCell(String.format("%,.2f", amount),
                        bg, regular, 9f, TextAlignment.RIGHT, 8, 10));
                row++;
            }
        }

        doc.add(table);
    }

    private static void buildTotal(Document doc, PdfFont bold, Order order) {
        Table wrapper = fullWidthTable(new float[]{45, 55})
                .setMarginTop(0).setMarginBottom(20);

        wrapper.addCell(noBorderCell()); // left spacer

        Table inner = new Table(UnitValue.createPercentArray(new float[]{52, 48}))
                .setWidth(UnitValue.createPointValue(CONTENT_W * 0.55f))
                .setBackgroundColor(C_PRIMARY);

        inner.addCell(noBorderCell().setPadding(12)
                .add(new Paragraph("TOTAL AMOUNT")
                        .setFont(bold).setFontSize(7.5f)
                        .setFontColor(C_ACCENT).setCharacterSpacing(0.8f)));

        inner.addCell(noBorderCell().setPadding(12)
                .add(new Paragraph("LKR " + String.format("%,.2f", order.getTotalAmount()))
                        .setFont(bold).setFontSize(10f)
                        .setFontColor(C_WHITE).setTextAlignment(TextAlignment.RIGHT)));

        wrapper.addCell(noBorderCell().add(inner));
        doc.add(wrapper);
    }

    private static void buildFooter(Document doc, PdfFont bold, PdfFont regular) {
        divider(doc);

        doc.add(new Paragraph("Thank you for shopping with StudySphere!")
                .setFont(bold).setFontSize(10)
                .setFontColor(C_PRIMARY).setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(8).setMarginBottom(5));

        doc.add(new Paragraph(
                "This is a computer-generated receipt and does not require a signature.\n"
                        + "For support, contact support@studysphere.lk")
                .setFont(regular).setFontSize(7.5f)
                .setFontColor(C_MUTED).setTextAlignment(TextAlignment.CENTER));
    }


    private static Table fullWidthTable(float[] cols) {
        return new Table(UnitValue.createPercentArray(cols))
                .setWidth(UnitValue.createPointValue(CONTENT_W));
    }

    private static Cell noBorderCell() {
        return new Cell().setBorder(Border.NO_BORDER);
    }

    private static void divider(Document doc) {
        LineSeparator sep = new LineSeparator(new SolidLine(0.6f));
        sep.setStrokeColor(C_DIVIDER);
        sep.setMarginTop(10).setMarginBottom(10);
        doc.add(sep);
    }

    private static Paragraph sectionHeading(String text, PdfFont bold) {
        return new Paragraph(text.toUpperCase())
                .setFont(bold).setFontSize(7.5f)
                .setFontColor(C_MUTED).setCharacterSpacing(1.1f).setMarginBottom(6);
    }

    private static Cell lbl(String text, PdfFont bold) {
        return noBorderCell().setPaddingTop(7).setPaddingBottom(1)
                .add(new Paragraph(text.toUpperCase())
                        .setFont(bold).setFontSize(7)
                        .setFontColor(C_MUTED).setCharacterSpacing(0.7f));
    }

    private static Cell val(String text, PdfFont font) {
        return noBorderCell().setPaddingBottom(3)
                .add(new Paragraph(nvl(text))
                        .setFont(font).setFontSize(9).setFontColor(C_TEXT));
    }

    private static Cell field(String label, String value, PdfFont bold, PdfFont regular) {
        Cell c = noBorderCell().setPadding(11);
        c.add(new Paragraph(label.toUpperCase())
                .setFont(bold).setFontSize(7)
                .setFontColor(C_MUTED).setCharacterSpacing(0.6f).setMarginBottom(4));
        c.add(new Paragraph(value)
                .setFont(regular).setFontSize(9.5f).setFontColor(C_TEXT));
        return c;
    }

    private static Cell dataCell(String text, DeviceRgb bg, PdfFont font,
                                 float size, TextAlignment align,
                                 float padLeft, float padRight) {
        return new Cell()
                .setBackgroundColor(bg).setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(C_DIVIDER, 0.4f))
                .setPaddingTop(8).setPaddingBottom(8)
                .setPaddingLeft(padLeft).setPaddingRight(padRight)
                .add(new Paragraph(nvl(text))
                        .setFont(font).setFontSize(size)
                        .setFontColor(C_TEXT).setTextAlignment(align));
    }

    private static String nvl(String s) { return s != null ? s : "—"; }
}