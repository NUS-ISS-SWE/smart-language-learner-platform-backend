package com.nus.iss.document.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs the real Apache Tika parsers, so a Tika upgrade that breaks extraction fails here. */
class TextExtractionServiceTest {

    private final TextExtractionService service = new TextExtractionService();

    @Test
    void extractsTextFromAPdf() throws IOException {
        String text = service.extract(new ByteArrayInputStream(pdf("Grammar notes for the essay")), "notes.pdf");

        assertTrue(text.contains("Grammar notes for the essay"), text);
    }

    @Test
    void extractsTextFromPlainText() {
        String text = service.extract(stream("  First line\nSecond line  \n"), "notes.txt");

        assertEquals("First line\nSecond line", text);
    }

    @Test
    void unreadableInputGivesNullInsteadOfFailingTheUpload() {
        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("stream closed");
            }
        };

        assertNull(service.extract(broken, "broken.pdf"));
    }

    private static InputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] pdf(String text) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 700);
                content.showText(text);
                content.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
