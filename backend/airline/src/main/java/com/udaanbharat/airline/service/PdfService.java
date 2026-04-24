package com.udaanbharat.airline.service;
import com.itextpdf.text.pdf.draw.LineSeparator;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * ISSUE 6 FIX: generateTicketPdf now accepts only plain primitives and strings.
 * No Hibernate entity is passed — no lazy-loading risk in async thread.
 */
@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    private static final BaseColor NAVY        = new BaseColor(0,   35,  149);
    private static final BaseColor NAVY_LIGHT  = new BaseColor(239, 246, 255);
    private static final BaseColor GRAY_TEXT   = new BaseColor(107, 114, 128);
    private static final BaseColor GRAY_BG     = new BaseColor(249, 250, 251);
    private static final BaseColor GRAY_BORDER = new BaseColor(229, 231, 235);
    private static final BaseColor GREEN       = new BaseColor(  5, 150, 105);

    /**
     * Generate PDF ticket from plain data — NO entity proxies.
     * Safe to call from any thread (including @Async email thread).
     */
    public byte[] generateTicketPdf(
            int    bookingId,
            String flightNumber,
            String fromCode,   String fromCity,
            String toCode,     String toCity,
            String dep,        String arr,    String duration,
            String travelDate,
            List<EmailService.PassengerLine> passengers,
            double amount,     String payMethod) {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // ── Fonts ────────────────────────────────────────────────────────
            Font fontBrand   = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,   NAVY);
            Font fontTag     = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, BaseColor.LIGHT_GRAY);
            Font fontH2      = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   NAVY);
            Font fontLabel   = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   GRAY_TEXT);
            Font fontValue   = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   BaseColor.BLACK);
            Font fontNormal  = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
            Font fontMuted   = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, GRAY_TEXT);
            Font fontGreen   = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   GREEN);
            Font fontWhite   = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,   BaseColor.WHITE);
            Font fontWhiteSm = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(200,210,255));
            Font codeFont    = new Font(Font.FontFamily.HELVETICA, 32, Font.BOLD,   NAVY);
            Font arrowFont   = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD,   NAVY);

            // ── Travel date display ──────────────────────────────────────────
            String travelDateDisplay = formatDate(travelDate);

            // ── Fare calculation ─────────────────────────────────────────────
            double tax   = Math.round(amount * 0.18);
            double total = amount + tax + 99;

            // ═══════════════════════════════════════════════════════════════
            // HEADER BAR
            // ═══════════════════════════════════════════════════════════════
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{3f, 1f});
            header.setSpacingAfter(0);

            PdfPCell brandCell = new PdfPCell();
            brandCell.setBorder(Rectangle.NO_BORDER);
            brandCell.setBackgroundColor(NAVY);
            brandCell.setPadding(16);
            Paragraph brand = new Paragraph();
            brand.add(new Chunk("✈ Udaan Bharat\n", fontBrand));
            brand.add(new Chunk("Connecting India with Comfort", fontTag));
            brandCell.addElement(brand);
            header.addCell(brandCell);

            PdfPCell refCell = new PdfPCell();
            refCell.setBorder(Rectangle.NO_BORDER);
            refCell.setBackgroundColor(NAVY);
            refCell.setPadding(16);
            refCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            refCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Paragraph bref = new Paragraph();
            bref.add(new Chunk("BOOKING REF\n", fontWhiteSm));
            bref.add(new Chunk("#" + bookingId, fontWhite));
            bref.setAlignment(Element.ALIGN_RIGHT);
            refCell.addElement(bref);
            header.addCell(refCell);
            doc.add(header);

            // ═══════════════════════════════════════════════════════════════
            // ROUTE HERO
            // ═══════════════════════════════════════════════════════════════
            PdfPTable route = new PdfPTable(3);
            route.setWidthPercentage(100);
            route.setWidths(new float[]{2f, 1f, 2f});
            route.setSpacingBefore(0);
            route.setSpacingAfter(0);

            PdfPCell originCell = routeCell(Element.ALIGN_LEFT);
            Paragraph originP = new Paragraph();
            originP.add(new Chunk(fromCode + "\n", codeFont));
            originP.add(new Chunk(fromCity, fontMuted));
            originCell.addElement(originP);
            route.addCell(originCell);

            PdfPCell midCell = routeCell(Element.ALIGN_CENTER);
            midCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Paragraph midP = new Paragraph();
            midP.add(new Chunk("✈\n", arrowFont));
            midP.add(new Chunk(flightNumber + "\n", new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, NAVY)));
            midP.add(new Chunk(duration, fontMuted));
            midP.setAlignment(Element.ALIGN_CENTER);
            midCell.addElement(midP);
            route.addCell(midCell);

            PdfPCell destCell = routeCell(Element.ALIGN_RIGHT);
            Paragraph destP = new Paragraph();
            destP.add(new Chunk(toCode + "\n", codeFont));
            destP.add(new Chunk(toCity, fontMuted));
            destP.setAlignment(Element.ALIGN_RIGHT);
            destCell.addElement(destP);
            route.addCell(destCell);
            doc.add(route);

            addLine(doc, GRAY_BORDER);

            // ═══════════════════════════════════════════════════════════════
            // FLIGHT DETAILS GRID — includes travel date
            // ═══════════════════════════════════════════════════════════════
            doc.add(sectionTitle("Flight Details", fontH2));

            PdfPTable flightGrid = new PdfPTable(4);
            flightGrid.setWidthPercentage(100);
            flightGrid.setSpacingBefore(6);
            flightGrid.setSpacingAfter(12);

            addDetailCell(flightGrid, "Flight Number",  flightNumber,      fontLabel, fontValue);
            addDetailCell(flightGrid, "Travel Date",    travelDateDisplay, fontLabel, fontValue); // ← ISSUE 1+2
            addDetailCell(flightGrid, "Departure",      dep,               fontLabel, fontValue);
            addDetailCell(flightGrid, "Arrival",        arr,               fontLabel, fontValue);
            doc.add(flightGrid);

            // ═══════════════════════════════════════════════════════════════
            // PASSENGER LIST
            // ═══════════════════════════════════════════════════════════════
            doc.add(sectionTitle("Passenger Details", fontH2));

            PdfPTable paxTable = new PdfPTable(3);
            paxTable.setWidthPercentage(100);
            paxTable.setWidths(new float[]{0.5f, 3f, 2f});
            paxTable.setSpacingBefore(6);
            paxTable.setSpacingAfter(12);

            Font hdrFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
            for (String h : new String[]{"#", "Passenger Name", "Seat"}) {
                PdfPCell hc = new PdfPCell(new Phrase(h, hdrFont));
                hc.setBackgroundColor(NAVY);
                hc.setBorderColor(NAVY);
                hc.setPadding(8);
                paxTable.addCell(hc);
            }

            if (passengers == null || passengers.isEmpty()) {
                addPaxRow(paxTable, 1, "—", "—", fontNormal, false);
            } else {
                for (int i = 0; i < passengers.size(); i++) {
                    EmailService.PassengerLine p = passengers.get(i);
                    addPaxRow(paxTable, i + 1, nvl(p.name()), nvl(p.seatNumber()), fontNormal, i % 2 != 0);
                }
            }
            doc.add(paxTable);

            // ═══════════════════════════════════════════════════════════════
            // PAYMENT SUMMARY
            // ═══════════════════════════════════════════════════════════════
            doc.add(sectionTitle("Payment Summary", fontH2));

            PdfPTable payTable = new PdfPTable(2);
            payTable.setWidthPercentage(60);
            payTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            payTable.setSpacingBefore(6);
            payTable.setSpacingAfter(6);

            addPayRow(payTable, "Base Fare",       "₹" + fmt(amount), fontNormal, fontNormal, GRAY_BG);
            addPayRow(payTable, "GST (18%)",        "₹" + fmt(tax),   fontNormal, fontNormal, BaseColor.WHITE);
            addPayRow(payTable, "Convenience Fee", "₹99",              fontNormal, fontNormal, GRAY_BG);
            addPayRow(payTable, "Total Paid",      "₹" + fmt(total),
                    new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, NAVY),
                    new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, NAVY), NAVY_LIGHT);
            addPayRow(payTable, "Payment Method",  nvl(payMethod),     fontNormal, fontNormal, BaseColor.WHITE);
            addPayRow(payTable, "Payment Status",  "SUCCESS",          fontNormal, fontGreen,  GRAY_BG);
            doc.add(payTable);

            // ═══════════════════════════════════════════════════════════════
            // FOOTER
            // ═══════════════════════════════════════════════════════════════
            addLine(doc, GRAY_BORDER);
            Font fontInfo = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, GRAY_TEXT);
            Paragraph footer = new Paragraph();
            footer.setSpacingBefore(8);
            footer.add(new Chunk("Important Information\n",
                    new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, GRAY_TEXT)));
            footer.add(new Chunk(
                "• Please arrive at the airport at least 2 hours before departure\n" +
                "• Carry a valid government-issued ID proof\n" +
                "• Boarding gate closes 30 minutes before departure\n\n" +
                "This is a computer-generated e-ticket. No signature required.\n" +
                "For assistance: support@udaanbharat.com",
                fontInfo));
            doc.add(footer);

            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("PDF generation failed for booking #{}: {}", bookingId, e.getMessage(), e);
            return new byte[0];
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private PdfPCell routeCell(int align) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(NAVY_LIGHT);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(18);
        c.setHorizontalAlignment(align);
        return c;
    }

    private Paragraph sectionTitle(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(14);
        p.setSpacingAfter(2);
        return p;
    }

    private void addLine(Document doc, BaseColor color) throws DocumentException {
        LineSeparator ls = new LineSeparator();
        ls.setLineColor(color);
        doc.add(new Chunk(ls));
    }

    private void addDetailCell(PdfPTable t, String label, String value, Font lf, Font vf) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setBackgroundColor(GRAY_BG);
        c.setPadding(10);
        c.setBorderWidthRight(2);
        c.setBorderColorRight(BaseColor.WHITE);
        Paragraph p = new Paragraph();
        p.add(new Chunk(label.toUpperCase() + "\n", lf));
        p.add(new Chunk(value, vf));
        c.addElement(p);
        t.addCell(c);
    }

    private void addPaxRow(PdfPTable t, int num, String name, String seat, Font font, boolean alt) {
        BaseColor bg = alt ? GRAY_BG : BaseColor.WHITE;
        for (String val : new String[]{String.valueOf(num), name, seat}) {
            PdfPCell c = new PdfPCell(new Phrase(val, font));
            c.setBackgroundColor(bg);
            c.setPadding(8);
            c.setBorderColor(GRAY_BORDER);
            c.setBorderWidth(0.5f);
            t.addCell(c);
        }
    }

    private void addPayRow(PdfPTable t, String label, String value,
                           Font lf, Font vf, BaseColor bg) {
        PdfPCell lc = new PdfPCell(new Phrase(label, lf));
        lc.setBackgroundColor(bg); lc.setPadding(8);
        lc.setBorderColor(GRAY_BORDER); lc.setBorderWidth(0.5f);

        PdfPCell vc = new PdfPCell(new Phrase(value, vf));
        vc.setBackgroundColor(bg); vc.setPadding(8);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vc.setBorderColor(GRAY_BORDER); vc.setBorderWidth(0.5f);

        t.addCell(lc);
        t.addCell(vc);
    }

    /** "2026-04-24" → "24 April 2026" */
    private static String formatDate(String iso) {
        try {
            if (iso == null || iso.isBlank()) return "—";
            String[] parts = iso.split("-");
            int month = Integer.parseInt(parts[1]);
            String[] months = {"","January","February","March","April","May","June",
                               "July","August","September","October","November","December"};
            return parts[2] + " " + months[month] + " " + parts[0];
        } catch (Exception e) { return iso != null ? iso : "—"; }
    }

    private static String fmt(double n) { return String.format("%,d", Math.round(n)); }
    private static String nvl(String s)  { return s != null ? s : ""; }
}