package com.udaanbharat.airline.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ISSUE 6 FIX: EmailData now receives only plain Strings (no entity proxies).
 * sendConfirmationEmail is @Async — runs in background thread safely because
 * all data was extracted inside the @Transactional block before this is called.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** One passenger line: name + seat. No entity references. */
    public record PassengerLine(String name, String seatNumber) {}

    /** One email recipient: address + display name. No entity references. */
    public record RecipientEntry(String email, String name) {}

    /**
     * ISSUE 6 FIX: All fields are plain Strings — no Hibernate proxies.
     * Added travelDate for display in the email body.
     */
    public record EmailData(
        String toAddress,
        String toName,
        String flightNumber,
        String fromCode,
        String fromCity,
        String toCode,
        String toCity,
        String departureDisplay,
        String arrivalDisplay,
        String duration,
        String travelDate,          // ← ISSUE 1+2: actual flight date
        List<PassengerLine> passengers,
        int bookingId,
        byte[] pdfBytes
    ) {}

    @Async
    public void sendConfirmationEmail(EmailData data) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setFrom("noreply@udaanbharat.com", "Udaan Bharat");
            helper.setTo(data.toAddress());
            helper.setSubject(String.format(
                "✈️ Udaan Bharat Booking Confirmed – %s | %s → %s",
                data.flightNumber(), data.fromCode(), data.toCode()
            ));
            helper.setText(buildHtmlBody(data), true);

            if (data.pdfBytes() != null && data.pdfBytes().length > 0) {
                helper.addAttachment(
                    "UdaanBharat_Ticket_" + data.bookingId() + ".pdf",
                    new ByteArrayResource(data.pdfBytes()),
                    "application/pdf"
                );
            }

            mailSender.send(msg);
            log.info("Confirmation email sent to {} for booking #{}", data.toAddress(), data.bookingId());

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to {} for booking #{}: {}",
                      data.toAddress(), data.bookingId(), e.getMessage());
        }
    }

    private String buildHtmlBody(EmailData d) {
        StringBuilder paxRows = new StringBuilder();
        for (int i = 0; i < d.passengers().size(); i++) {
            PassengerLine p = d.passengers().get(i);
            paxRows.append(String.format(
                "<tr><td style='padding:6px 0;color:#374151;font-size:14px'>Passenger %d</td>" +
                "<td style='padding:6px 0;font-weight:600;font-size:14px'>%s &mdash; " +
                "Seat <span style='color:#002395;font-weight:700'>%s</span></td></tr>",
                i + 1, esc(p.name()), esc(p.seatNumber())
            ));
        }

        // Format travel date for display: "2026-04-24" → "24 April 2026"
        String travelDateDisplay = d.travelDate() != null && !d.travelDate().isBlank()
                ? formatDate(d.travelDate()) : "";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/></head><body style='" +
               "margin:0;padding:0;background:#F3F4F6;font-family:Arial,sans-serif'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'>" +
               "<tr><td align='center' style='padding:32px 16px'>" +
               "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;" +
               "border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08)'>" +

               // Header
               "<tr><td style='background:linear-gradient(135deg,#002395,#0030d0);padding:28px 32px'>" +
               "<div style='font-size:24px;font-weight:800;color:#fff'>✈️ Udaan Bharat</div>" +
               "<div style='font-size:13px;color:rgba(255,255,255,0.65);margin-top:4px'>Connecting India with Comfort</div>" +
               "</td></tr>" +

               // Greeting
               "<tr><td style='padding:28px 32px 0'>" +
               "<p style='font-size:16px;color:#111827;margin:0 0 6px'>Dear <strong>" + esc(d.toName()) + "</strong>,</p>" +
               "<p style='font-size:14px;color:#374151;line-height:1.7;margin:0'>" +
               "Your booking with <strong>Udaan Bharat</strong> is successfully confirmed. 🎉<br/>" +
               "We&rsquo;re excited to have you on board!</p>" +
               "</td></tr>" +

               // Flight Details
               "<tr><td style='padding:20px 32px'>" +
               "<div style='background:#EFF6FF;border:1px solid #BFDBFE;border-radius:8px;padding:16px 20px'>" +
               "<div style='font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:.08em;" +
               "color:#1D4ED8;margin-bottom:10px'>🛫 Flight Details</div>" +
               "<table width='100%' cellpadding='0' cellspacing='0'>" +
               row("Flight Number",  d.flightNumber()) +
               row("Route", esc(d.fromCity()) + " (" + esc(d.fromCode()) + ") → " +
                             esc(d.toCity()) + " (" + esc(d.toCode()) + ")") +
               (travelDateDisplay.isEmpty() ? "" : row("Travel Date", travelDateDisplay)) +
               row("Departure", d.departureDisplay()) +
               row("Arrival",   d.arrivalDisplay()) +
               row("Duration",  d.duration()) +
               "</table></div></td></tr>" +

               // Passenger Details
               "<tr><td style='padding:0 32px 20px'>" +
               "<div style='background:#F9FAFB;border:1px solid #E5E7EB;border-radius:8px;padding:16px 20px'>" +
               "<div style='font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:.08em;" +
               "color:#374151;margin-bottom:10px'>👤 Passenger Details</div>" +
               "<table width='100%' cellpadding='0' cellspacing='0'>" + paxRows + "</table>" +
               "</div></td></tr>" +

               // PDF notice
               "<tr><td style='padding:0 32px 20px'>" +
               "<div style='background:#FFFBEB;border:1px solid #FDE68A;border-radius:8px;padding:14px 20px'>" +
               "<p style='margin:0;font-size:13px;color:#92400E'>📎 <strong>Your E-Ticket</strong><br/>" +
               "Your complete booking details, payment summary, and receipt are attached as a PDF.</p>" +
               "</div></td></tr>" +

               // Important info
               "<tr><td style='padding:0 32px 20px'>" +
               "<div style='font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:.08em;" +
               "color:#374151;margin-bottom:8px'>🛬 Important Information</div>" +
               "<ul style='margin:0;padding-left:20px;color:#374151;font-size:13px;line-height:2'>" +
               "<li>Please arrive at the airport at least 2 hours before departure</li>" +
               "<li>Carry a valid government-issued ID</li>" +
               "<li>Boarding gate closes 30 minutes before departure</li></ul></td></tr>" +

               // Footer
               "<tr><td style='background:#F9FAFB;padding:20px 32px;border-top:1px solid #E5E7EB'>" +
               "<p style='margin:0;font-size:13px;color:#374151;line-height:1.7'>" +
               "We wish you a smooth and pleasant journey with Udaan Bharat ✈️<br/>" +
               "<span style='color:#6B7280'>Warm regards,</span><br/>" +
               "<strong style='color:#002395'>Team Udaan Bharat</strong></p>" +
               "</td></tr>" +
               "</table></td></tr></table></body></html>";
    }

    private static String row(String label, String value) {
        return "<tr><td style='padding:5px 0;color:#6B7280;font-size:13px;width:130px'>" + label + "</td>" +
               "<td style='padding:5px 0;font-weight:600;font-size:13px;color:#111827'>" + value + "</td></tr>";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }

    /** "2026-04-24" → "24 April 2026" */
    private static String formatDate(String iso) {
        try {
            String[] parts = iso.split("-");
            int month = Integer.parseInt(parts[1]);
            String[] months = {"","January","February","March","April","May","June",
                               "July","August","September","October","November","December"};
            return parts[2] + " " + months[month] + " " + parts[0];
        } catch (Exception e) { return iso; }
    }
}