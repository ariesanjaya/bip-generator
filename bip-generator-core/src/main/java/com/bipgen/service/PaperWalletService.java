package com.bipgen.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PaperWalletService {

    private static final float MARGIN = 50;
    private static final int QR_SIZE = 200;

    public byte[] generatePdf(String mnemonic, String address, String privateKey, String path, String publicKey,
            String passphrase, String masterPrivateKey, String masterPublicKey) throws IOException, WriterException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page);

            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();

            BufferedImage addressQr = generateQrCode(address);
            BufferedImage privateKeyQr = generateQrCode(privateKey);

            PDImageXObject addressQrImage = LosslessFactory.createFromImage(document, addressQr);
            PDImageXObject privateKeyQrImage = LosslessFactory.createFromImage(document, privateKeyQr);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                // Border
                cs.setLineWidth(2);
                cs.addRect(MARGIN - 10, MARGIN - 10, pageWidth - 2 * MARGIN + 20, pageHeight - 2 * MARGIN + 20);
                cs.stroke();

                // Title
                float titleY = pageHeight - MARGIN - 10;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 24);
                String title = "BITCOIN PAPER WALLET";
                float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(title) / 1000 * 24;
                cs.newLineAtOffset((pageWidth - titleWidth) / 2, titleY);
                cs.showText(title);
                cs.endText();

                // Divider under title
                float dividerY = titleY - 15;
                cs.setLineWidth(1);
                cs.moveTo(MARGIN, dividerY);
                cs.lineTo(pageWidth - MARGIN, dividerY);
                cs.stroke();

                // --- LEFT SIDE: PUBLIC ---
                float leftCenterX = pageWidth * 0.28f;
                float qrY = dividerY - 30 - QR_SIZE;

                // Section label
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                String pubLabel = "PUBLIC ADDRESS";
                float pubLabelWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(pubLabel) / 1000 * 14;
                cs.newLineAtOffset(leftCenterX - pubLabelWidth / 2, dividerY - 20);
                cs.showText(pubLabel);
                cs.endText();

                // QR code
                float qrX = leftCenterX - QR_SIZE / 2f;
                cs.drawImage(addressQrImage, qrX, qrY, QR_SIZE, QR_SIZE);

                // Address text (wrapped)
                float addressTextY = qrY - 20;
                drawWrappedText(cs, address, PDType1Font.COURIER, 8, leftCenterX, addressTextY, 220);

                // --- RIGHT SIDE: PRIVATE ---
                float rightCenterX = pageWidth * 0.72f;

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                String privLabel = "PRIVATE KEY (WIF)";
                float privLabelWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(privLabel) / 1000 * 14;
                cs.newLineAtOffset(rightCenterX - privLabelWidth / 2, dividerY - 20);
                cs.showText(privLabel);
                cs.endText();

                // QR code
                float privQrX = rightCenterX - QR_SIZE / 2f;
                cs.drawImage(privateKeyQrImage, privQrX, qrY, QR_SIZE, QR_SIZE);

                // Private key text (wrapped)
                drawWrappedText(cs, privateKey, PDType1Font.COURIER, 8, rightCenterX, addressTextY, 220);

                // --- Vertical divider ---
                float midX = pageWidth / 2;
                cs.setLineWidth(0.5f);
                cs.moveTo(midX, dividerY - 5);
                cs.lineTo(midX, addressTextY - 20);
                cs.stroke();

                // --- FOLD LINE ---
                float foldY = addressTextY - 40;
                cs.setLineDashPattern(new float[] { 5, 5 }, 0);
                cs.moveTo(MARGIN, foldY);
                cs.lineTo(pageWidth - MARGIN, foldY);
                cs.stroke();
                cs.setLineDashPattern(new float[] {}, 0);

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
                String foldText = "FOLD HERE";
                float foldTextWidth = PDType1Font.HELVETICA_OBLIQUE.getStringWidth(foldText) / 1000 * 10;
                cs.newLineAtOffset((pageWidth - foldTextWidth) / 2, foldY + 5);
                cs.showText(foldText);
                cs.endText();

                // --- BOTTOM SECTION ---
                float bottomY = foldY - 30;

                // Mnemonic
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                cs.newLineAtOffset(MARGIN, bottomY);
                cs.showText("Mnemonic: ");
                cs.setFont(PDType1Font.COURIER, 9);
                cs.showText(truncateForLine(mnemonic, 110));
                cs.endText();

                // Second line of mnemonic if needed
                if (mnemonic.length() > 90) {
                    bottomY -= 15;
                    String remaining = mnemonic.length() > 90 ? mnemonic.substring(90) : "";
                    if (!remaining.isEmpty()) {
                        cs.beginText();
                        cs.setFont(PDType1Font.COURIER, 9);
                        cs.newLineAtOffset(MARGIN + 65, bottomY);
                        cs.showText(truncateForLine(remaining, 110));
                        cs.endText();
                    }
                }

                // Path
                bottomY -= 18;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                cs.newLineAtOffset(MARGIN, bottomY);
                cs.showText("Derivation Path: ");
                cs.setFont(PDType1Font.COURIER, 10);
                cs.showText(path);
                cs.endText();

                // Public Key
                bottomY -= 18;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                cs.newLineAtOffset(MARGIN, bottomY);
                cs.showText("Public Key: ");
                cs.setFont(PDType1Font.COURIER, 7);
                cs.showText(publicKey);
                cs.endText();

                // Passphrase
                if (passphrase != null && !passphrase.isEmpty()) {
                    bottomY -= 18;
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                    cs.newLineAtOffset(MARGIN, bottomY);
                    cs.showText("Passphrase: ");
                    cs.setFont(PDType1Font.COURIER, 9);
                    cs.showText(truncateForLine(passphrase, 100));
                    cs.endText();
                }

                // Master Private Key (xprv)
                if (masterPrivateKey != null && !masterPrivateKey.isEmpty()) {
                    bottomY -= 18;
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                    cs.newLineAtOffset(MARGIN, bottomY);
                    cs.showText("Master Private Key: ");
                    cs.setFont(PDType1Font.COURIER, 6);
                    cs.showText(truncateForLine(masterPrivateKey, 130));
                    cs.endText();
                }

                // Master Public Key (xpub)
                if (masterPublicKey != null && !masterPublicKey.isEmpty()) {
                    bottomY -= 18;
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                    cs.newLineAtOffset(MARGIN, bottomY);
                    cs.showText("Master Public Key: ");
                    cs.setFont(PDType1Font.COURIER, 6);
                    cs.showText(truncateForLine(masterPublicKey, 130));
                    cs.endText();
                }

                // Warning
                bottomY -= 25;
                cs.setLineWidth(1);
                cs.addRect(MARGIN, bottomY - 10, pageWidth - 2 * MARGIN, 30);
                cs.stroke();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                cs.newLineAtOffset(MARGIN + 10, bottomY + 5);
                cs.showText(
                        "WARNING: Store this document securely. Never share your private key or mnemonic phrase with anyone.");
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private BufferedImage generateQrCode(String data) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    private void drawWrappedText(PDPageContentStream cs, String text, PDType1Font font, float fontSize,
            float centerX, float y, float maxWidth) throws IOException {
        float charWidth = font.getStringWidth("A") / 1000 * fontSize;
        int charsPerLine = (int) (maxWidth / charWidth);

        int start = 0;
        float currentY = y;
        while (start < text.length()) {
            int end = Math.min(start + charsPerLine, text.length());
            String line = text.substring(start, end);
            float lineWidth = font.getStringWidth(line) / 1000 * fontSize;

            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(centerX - lineWidth / 2, currentY);
            cs.showText(line);
            cs.endText();

            start = end;
            currentY -= fontSize + 2;
        }
    }

    private String truncateForLine(String text, int maxChars) {
        if (text.length() <= maxChars)
            return text;
        return text.substring(0, maxChars);
    }
}
