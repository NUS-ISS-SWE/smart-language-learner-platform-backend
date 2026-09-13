package com.nus.iss.guide;

import org.springframework.ai.document.Document;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlatformFeatureCatalog {

    private static final String DOCUMENT_TYPE = "platform-feature";

    private PlatformFeatureCatalog() {
    }

    public static List<Document> documents() {
        return List.of(
                feature("overview", """
                        The Smart Language Learner Platform helps users practise language and writing skills.
                        Users can manage compositions and learning documents, generate compositions, request
                        AI-assisted grading, review feedback, manage subscriptions, and ask the Product Guide how
                        to use the application. Administrators can manage users, rubrics, plans, and usage data.
                        """),
                feature("account", """
                        Account and profile features: Users can register for an account, activate it, sign in,
                        recover or reset a forgotten password, and view their profile. The profile includes the
                        user's learning difficulty level.
                        """),
                feature("composition", """
                        Composition features: Users can create a composition, list their compositions, open one,
                        edit it, delete it, or ask the application to generate one. A composition records a title
                        and written content for language learning practice.
                        """),
                feature("grading", """
                        Composition grading features: Users can request an AI assessment for a composition and
                        follow its grading progress. The completed result contains a score and suggestions based
                        on the composition language, the user's difficulty level, and the applicable rubric.
                        """),
                feature("documents", """
                        Document features: Users can upload PDF, DOCX, PPTX, and TXT learning documents. They can
                        list their documents, view extracted text, download the original file, and delete a
                        document that they own.
                        """),
                feature("subscriptions", """
                        Subscription features: Users can view available pricing plans, select a plan, check their
                        current subscription and subscription history, and cancel a subscription.
                        """),
                feature("administration", """
                        Administrator features: Administrators can manage users and their status or roles, inspect
                        compositions and documents, manage grading rubric criteria, review usage metrics, and
                        manage pricing plans.
                        """),
                feature("product-guide", """
                        Product Guide feature: Signed-in users can ask how to use the Smart Language Learner
                        Platform through the API gateway. The guide streams its answer, remembers recent messages
                        separately for each username, and answers from the stored platform feature catalogue.
                        """));
    }

    private static Document feature(String name, String content) {
        String id = UUID.nameUUIDFromBytes(
                ("sllp-platform-feature:" + name).getBytes(StandardCharsets.UTF_8)).toString();

        return Document.builder()
                .id(id)
                .text(content.strip())
                .metadata(Map.of("type", DOCUMENT_TYPE, "feature", name))
                .build();
    }
}
