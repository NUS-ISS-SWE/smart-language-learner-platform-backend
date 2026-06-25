package com.nus.iss.service.impl;

import com.nus.iss.enums.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageDetectorServiceImplTest {

    private LanguageDetectorServiceImpl languageDetectorService;

    @BeforeEach
    void setUp() {
        languageDetectorService = new LanguageDetectorServiceImpl();
    }

    @Test
    void detectLanguage_whenTextIsBlankReturnsEmpty() {
        assertTrue(languageDetectorService.detectLanguage(" ").isEmpty());
    }

    @Test
    void detectLanguage_whenTextIsEnglishReturnsEnglish() {
        Optional<Language> language = languageDetectorService.detectLanguage(
                "This is a clear English paragraph about learning languages and writing compositions."
        );

        assertEquals(Optional.of(Language.ENGLISH), language);
    }

    @Test
    void detectLanguage_whenTextIsChineseReturnsChinese() {
        Optional<Language> language = languageDetectorService.detectLanguage(
                "这是一个关于学习语言和写作文的中文段落。"
        );

        assertEquals(Optional.of(Language.CHINESE), language);
    }
}
