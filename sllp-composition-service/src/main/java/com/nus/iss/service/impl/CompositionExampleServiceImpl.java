package com.nus.iss.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.nus.iss.dto.CompositionExampleResponse;
import com.nus.iss.enums.Language;
import com.nus.iss.service.CompositionExampleService;

/** Provides curated examples of good compositions at different proficiency levels. */
@Service
public class CompositionExampleServiceImpl implements CompositionExampleService {

    @Override
    public List<CompositionExampleResponse> getExamples(Language language) {
        return getAllExamples().stream()
                .filter(ex -> ex.language() == language)
                .collect(Collectors.toList());
    }

    @Override
    public List<CompositionExampleResponse> getExamplesByLevel(Language language, String proficiencyLevel) {
        return getAllExamples().stream()
                .filter(ex -> ex.language() == language)
                .filter(ex -> ex.proficiencyLevel().equalsIgnoreCase(proficiencyLevel))
                .collect(Collectors.toList());
    }

    @Override
    public List<CompositionExampleResponse> getAllExamples() {
        List<CompositionExampleResponse> examples = new ArrayList<>();

        // English - Beginner
        examples.add(new CompositionExampleResponse(
                "My Daily Routine",
                "I wake up at 7 AM every day. I eat breakfast and drink coffee. Then I go to work by bus. I work from 9 AM to 5 PM. After work, I go home and cook dinner. I eat with my family. In the evening, I watch TV and relax. I go to bed at 10 PM.",
                Language.ENGLISH,
                "BEGINNER",
                "Simple present tense, daily activities, clear structure"));

        examples.add(new CompositionExampleResponse(
                "My Favorite Food",
                "My favorite food is pizza. I like pizza because it is delicious. Pizza has cheese, sauce, and toppings. I like cheese and vegetables on my pizza. I eat pizza on weekends. Pizza makes me happy.",
                Language.ENGLISH,
                "BEGINNER",
                "Simple sentences, favorite topic, reasons and examples"));

        // English - Intermediate
        examples.add(new CompositionExampleResponse(
                "A Memorable Weekend Trip",
                "Last weekend, my family and I went to the beach. We drove for two hours to get there. When we arrived, the weather was beautiful with clear blue skies. We spent the afternoon swimming and building sandcastles. For lunch, we ate fresh seafood at a small restaurant near the shore. My younger brother enjoyed collecting seashells while I took photographs. As the sun set, we sat together watching the waves. It was a peaceful and memorable day that brought us closer as a family.",
                Language.ENGLISH,
                "INTERMEDIATE",
                "Past tense narrative, descriptive details, emotional reflection"));

        examples.add(new CompositionExampleResponse(
                "The Benefits of Learning a Foreign Language",
                "Learning a foreign language offers many advantages in today's world. First, it enhances career opportunities by making professionals more competitive in the job market. Second, it improves cognitive abilities such as memory and problem-solving skills. Additionally, learning a language opens doors to understanding different cultures and building meaningful international friendships. Finally, it increases confidence when traveling and communicating with people from diverse backgrounds.",
                Language.ENGLISH,
                "INTERMEDIATE",
                "Opinion essay, multiple reasons, formal tone"));

        // English - Advanced
        examples.add(new CompositionExampleResponse(
                "The Impact of Technology on Human Connection",
                "While technology has undeniably revolutionized communication, its impact on human connection remains paradoxical. On one hand, digital platforms have democratized access to information and enabled relationships that transcend geographical boundaries. Conversely, the proliferation of virtual interaction has inadvertently fostered superficial engagement, often diminishing the depth of face-to-face communication. Contemporary society faces a critical juncture where technological advancement must be harmonized with the cultivation of genuine interpersonal relationships. The challenge lies not in rejecting technology, but rather in leveraging it judiciously to enhance rather than replace authentic human connection.",
                Language.ENGLISH,
                "ADVANCED",
                "Complex argumentation, nuanced perspective, sophisticated vocabulary"));

        // Chinese - Beginner
        examples.add(new CompositionExampleResponse(
                "我的家人",
                "我有一个幸福的家庭。我爸爸是医生，妈妈是老师。我有一个哥哥。我们一起住在北京。我爸爸喜欢读书，妈妈喜欢做饭。我哥哥喜欢打篮球，我喜欢画画。我们都很爱我们的家。",
                Language.CHINESE,
                "BEGINNER",
                "Family introduction, simple present tense, basic adjectives"));

        examples.add(new CompositionExampleResponse(
                "我的周末",
                "这个周末很有趣。星期六早上，我和朋友去公园。我们在公园里踢足球。下午，我在家做作业。晚上，我和家人一起看电影。星期日早上，我去学英语。下午，我去购物。晚上，我睡得很早。",
                Language.CHINESE,
                "BEGINNER",
                "Weekend activities, past tense narrative, chronological order"));

        // Chinese - Intermediate
        examples.add(new CompositionExampleResponse(
                "旅游经历",
                "去年夏天，我和我的家人去云南旅游。那是一个美丽而令人难忘的地方。我们参观了古城大理，欣赏了洱海的壮观景色。当地的人们非常友善，他们推荐了许多好吃的传统食物。我最喜欢的是鲜花饼，香甜可口。这次旅行让我了解了中国的多样文化，也增进了我与家人的感情。我真希望能再次前往云南。",
                Language.CHINESE,
                "INTERMEDIATE",
                "Travel narrative, descriptive phrases, personal reflection"));

        return examples;
    }
}
