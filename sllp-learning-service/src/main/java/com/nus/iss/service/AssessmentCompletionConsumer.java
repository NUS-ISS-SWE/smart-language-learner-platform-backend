package com.nus.iss.service;

import com.nus.iss.contracts.grading.CompositionGradingCompletedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** A passive downstream consumer; it never invokes or alters A2A assessment agents. */
@Component
public class AssessmentCompletionConsumer {
    private final LearningProgressService progress;
    public AssessmentCompletionConsumer(LearningProgressService progress) { this.progress = progress; }
    @KafkaListener(topics = "${sllp.kafka.topics.composition-grading-completed}", containerFactory = "learningKafkaListenerContainerFactory")
    public void completed(CompositionGradingCompletedEvent event) { progress.recordAssessment(event); }
}
