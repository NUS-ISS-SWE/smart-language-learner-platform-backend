package com.nus.iss.document.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Text extraction service using Apache Tika.
 * Supports PDF, DOCX, PPTX, and TXT files.
 */
@Service
@Slf4j
public class TextExtractionService {

    /**
     * Extract plain text from a document input stream.
     *
     * @param inputStream document byte stream
     * @param filename    original filename (used by Tika for format detection hints)
     * @return extracted text, or null if extraction fails
     */
    public String extract(InputStream inputStream, String filename) {
        try {
            AutoDetectParser parser = new AutoDetectParser();
            // -1 = no write-limit so large files are not truncated
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            metadata.set("resourceName", filename);
            ParseContext context = new ParseContext();
            context.set(AutoDetectParser.class, parser);

            parser.parse(inputStream, handler, metadata, context);
            String text = handler.toString().trim();
            log.info("Text extraction succeeded for '{}': {} chars", filename, text.length());
            return text;
        } catch (Exception e) {
            log.error("Text extraction failed for '{}': {}", filename, e.getMessage());
            return null;
        }
    }
}
