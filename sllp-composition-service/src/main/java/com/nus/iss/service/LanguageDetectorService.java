package com.nus.iss.service;

import com.nus.iss.enums.Language;

import java.util.Optional;

public interface LanguageDetectorService {

    Optional<Language> detectLanguage(String text);
}
