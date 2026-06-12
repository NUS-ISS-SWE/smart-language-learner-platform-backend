package com.nus.iss.service.impl;

import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import com.nus.iss.enums.Language;
import com.nus.iss.service.LanguageDetectorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class LanguageDetectorServiceImpl implements LanguageDetectorService {

    private final LanguageDetector detector;

    public LanguageDetectorServiceImpl() {
        // Only load English and Chinese models — much faster than fromAllLanguages()
        // and avoids UNKNOWN results for valid English/Chinese text caused by
        // competition with 75 other languages at the default confidence threshold.
        this.detector = LanguageDetectorBuilder.fromLanguages(
                com.github.pemistahl.lingua.api.Language.ENGLISH,
                com.github.pemistahl.lingua.api.Language.CHINESE)
                .build();
    }

    @Override
    public Optional<Language> detectLanguage(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        com.github.pemistahl.lingua.api.Language detectedLanguage = detector.detectLanguageOf(text);
        log.info("Detected language is {}", detectedLanguage.name());
        return mapToSupportedLanguage(detectedLanguage);
    }

    private Optional<Language> mapToSupportedLanguage(com.github.pemistahl.lingua.api.Language detectedLanguage) {
        return switch (detectedLanguage) {
            case ENGLISH -> Optional.of(Language.ENGLISH);
            case CHINESE -> Optional.of(Language.CHINESE);
            default -> Optional.empty();
        };
    }
}
