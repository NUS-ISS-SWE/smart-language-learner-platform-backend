package com.nus.iss.service.impl;

import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import com.nus.iss.enums.Language;
import com.nus.iss.service.LanguageDetectorService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LanguageDetectorServiceImpl implements LanguageDetectorService {

    private final LanguageDetector detector;

    public LanguageDetectorServiceImpl() {
        this.detector = LanguageDetectorBuilder.fromAllLanguages()
                .withMinimumRelativeDistance(0.2)
                .build();
    }

    @Override
    public Optional<Language> detectLanguage(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        com.github.pemistahl.lingua.api.Language detectedLanguage = detector.detectLanguageOf(text);
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
