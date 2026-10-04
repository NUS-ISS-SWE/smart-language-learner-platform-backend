--liquibase formatted sql

--changeset codex:020-english-grading-anchor-seed
WITH inserted_anchors AS (
    INSERT INTO grading_anchor (
        language,
        difficulty_level,
        title,
        composition
    )
    VALUES
        (
            'ENGLISH',
            'BEGINNER',
            'My Lost Pencil',
            'Yesterday I lose my pencil. It is blue and nice. I look my bag and table but no pencil. I was sad because I need it for my work. I ask Ben. He say he do not know. Then teacher come. She find it under my chair. I was happy. I take pencil and write my work. I must keep it good next time.'
        ),
        (
            'ENGLISH',
            'BEGINNER',
            'A Rainy Recess',
            'It was recess and rain was very big. We cannot go playground. Me and Jia Min sit in class. We play a game with paper. The paper fly to the floor because the fan. Our teacher told us pick it up. Then we draw a house and a sun. Soon the bell ring. Rainy recess was not fun but the drawing is nice.'
        ),
        (
            'ENGLISH',
            'BEGINNER',
            'Helping at the Market',
            'On Sunday morning, I went to the market with Mum. The market was crowded and noisy. Mum bought carrots, fish and a bag of oranges. The bags looked heavy, so I carried the oranges for her. One orange rolled onto the floor. A kind uncle picked it up and gave it back to me. I thanked him. At home, Mum smiled and said that I was a helpful child. I felt proud because I had made her work easier.'
        ),
        (
            'ENGLISH',
            'BEGINNER',
            'The Stray Kitten',
            'I heard a soft cry near the stairs after school. A tiny grey kitten was hiding behind a flowerpot. It looked wet and hungry. I wanted to take it home, but I knew its owner might be looking for it. My father helped me dry the kitten with a towel. We made a sign and put it near the lift. That evening, a girl from the next block knocked on our door. She hugged the kitten and thanked us. I was glad that we had helped them find each other.'
        ),
        (
            'ENGLISH',
            'BEGINNER',
            'My Surprise Birthday',
            'When I reached home, the living room was dark and strangely quiet. I called for Mum, but nobody answered. Suddenly, the lights came on. My family and friends jumped out and shouted, “Surprise!” Colourful balloons covered the wall, and a chocolate cake waited on the table. I laughed because I had thought everyone had forgotten my birthday. We played musical chairs and shared the cake. Before my friends left, I opened a small box from my sister. Inside was the storybook I had wanted for weeks. I hugged her tightly. It was not the biggest party, but it was my happiest one.'
        ),
        (
            'ENGLISH',
            'INTERMEDIATE',
            'The Broken Window',
            'After school, Ravi and me played ball at the corridor. Teacher say we must not play there but outside was hot. Ravi kick the ball very hard. It fly past me and hit the classroom window. The glass make a loud sound and broke. We was scared. Ravi said we should run away, so we hide near the toilet. I keep thinking about the window and feel bad. Later, the cleaner saw us and asked why we were there. I told her nothing happened. Then our form teacher came. She knew somebody broke it because another boy see us. The class became quiet when they heard about it. Ravi blamed me and I blamed him. Teacher was angry because we lied. We say sorry and our parents had to pay. I learn not to play ball there and not tell lies because it make everything more worse.'
        ),
        (
            'ENGLISH',
            'INTERMEDIATE',
            'The Day I Was Late',
            'I wake up and saw the clock. It was already seven fifteen! I jump from bed and wear my uniform very fast, my books was still on the table. I did not eat breakfast because no time. My father already left for work so I run to the bus stop. When I reach there, the school bus was turning away. I wave my hands but the driver never see me.

            I felt worried and call Mum. She was upset but she take me to school in a taxi. There was a traffic jam and many car move slowly. I kept looking at my watch again and again. At last I arrived when the class was reading. Everyone look at me when I entered. I told teacher my alarm did not ring. She tell me check it before sleeping. That night I set two alarms and pack my bag early. I do not want to late again.'
        ),
        (
            'ENGLISH',
            'INTERMEDIATE',
            'Our Class Garden',
            'Our teacher gave the class an empty patch of soil behind the canteen. She challenged us to turn it into a garden before Open House. At first, we argued about what to plant. Some classmates wanted flowers, while others wanted vegetables. We finally divided the patch into two sections and formed teams.

            My team planted tomato seeds. Every morning, two pupils watered them and pulled out weeds. For many days, nothing appeared. We wondered if we had done something wrong. Then, after a rainy weekend, tiny green shoots pushed through the soil. We cheered as if we had won a prize.

            By Open House, yellow flowers and red tomatoes filled our section. The other team had grown bright sunflowers. Our garden was not perfectly neat, but every pupil had helped. When our parents visited, we proudly explained how patience and teamwork had changed an empty patch into a lively place.'
        ),
        (
            'ENGLISH',
            'INTERMEDIATE',
            'An Unexpected Kindness',
            'The afternoon bus was crowded, and I was tired after basketball practice. When a seat became empty, I hurried towards it. At the same moment, an elderly man carrying two grocery bags reached for the handrail beside me. I pretended not to notice him and sat down.

            At the next stop, the bus jerked suddenly. One of his bags tore, sending oranges rolling along the floor. Shame warmed my face. Before I could move, several passengers bent down to help. I quickly gathered the last two oranges and offered the man my seat. Instead of scolding me, he smiled and thanked me.

            We began talking. He told me that he was bringing fruit to his wife in hospital. When he left, he gave me one orange, saying that kindness should always be passed on. I did not feel that I deserved it, but I accepted it politely. The orange remained in my bag all evening, reminding me that a small selfish choice could be corrected by a better one.'
        ),
        (
            'ENGLISH',
            'INTERMEDIATE',
            'The Missing Wallet',
            'A brown wallet lay beneath the bench at the playground. I picked it up and found several fifty-dollar notes inside. For one exciting moment, I imagined buying the remote-controlled car displayed at the toy shop. Nobody seemed to be watching.

            Then I noticed a photograph tucked behind the notes. It showed an old woman smiling beside two children. A library card bore her name and address. My friend Hakim suggested taking the wallet to the police post, but it would close soon. Since the address was only three blocks away, our parents agreed to accompany us there.

            An anxious woman opened the door before we could knock twice. Her expression changed from worry to relief when she saw the wallet. She explained that the money was meant for her medical appointment. She tried to reward us, but we refused. Instead, she gave us cold drinks and called our parents to praise us.

            As I walked home, the toy car no longer seemed important. Returning the wallet had given me something money could not buy: the quiet satisfaction of knowing that I had made the honest choice when nobody was watching.'
        ),
        (
            'ENGLISH',
            'ADVANCED',
            'The Choice I Regretted',
            'I had a science project with Farid. We need to make a bridge from sticks. He wanted meet on Saturday but I wanted play games, so I told him I was sick. I stayed home and played for many hours. Farid did most of the bridge himself.

            On Monday, he brought it to school. It looked okay but one side was not strong. Our teacher asked us how we shared the work. I said I made the plan and helped a lot. Farid looked at me but did not say anything. During the test, teacher put weights on the bridge. It broke fast. Other groups bridges could hold more. I felt angry and said Farid did not build it properly. He became upset and told teacher the truth.

            Teacher was disappointed with me. She said marks were not the main problem. The problem was I lied and blamed my partner. I knew she was right. Farid had spend his Saturday doing our project while I only enjoyed myself. I said sorry to him, but he did not answer much.

            We got a low mark and had to redo the project. This time I went to his house. We made another bridge and it was stronger. Even though the project became better, Farid did not trust me like before. I regretted my choice because one afternoon of games caused many problems. A fast apology could not make our friendship normal at once. I learned that group work means doing your part and not making excuses.'
        ),
        (
            'ENGLISH',
            'ADVANCED',
            'A Misunderstanding Between Friends',
            'Nadia was my best friend since Primary Three. One week she suddenly stop sitting with me at recess. When I message her, she only answer with one word. I did not know why. I remember I got higher marks for mathematics, so I think maybe she was jealous. I told two classmates about it. They told another person and soon many people know.

            Nadia came to find me outside the library. She ask why I said those things. I said because she was avoiding me. Then she told me her grandmother was in hospital. Her family went there every day and she was very worried. It was not about my marks at all. I felt bad, but I also told her she should explain earlier. She became angry and walked away. Now we had another problem.

            We did not talk for one week. It was bad because we were in the same project group. We pass papers to each other but never really speak. Our teacher noticed it and ask what happened. I did not want to explain because it was embarrassing. After class, I wrote Nadia a note. I said sorry for guessing and telling other people.

            She read the note and said she forgive me, but she was still quiet. I shared lesson notes when she visit the hospital. After some days we started talking more. We became friends again, but not immediately. I learned I should ask my friend what is wrong. I should not make my own story and spread it because a misunderstanding can become bigger very fast.'
        ),
        (
            'ENGLISH',
            'ADVANCED',
            'The Competition That Changed Me',
            'When my teacher selected me for the inter-school speaking competition, I accepted immediately. I imagined standing beneath the stage lights while the audience applauded. Since I usually spoke confidently in class, I assumed that little preparation was needed.

            My attitude changed during the first rehearsal. I forgot an entire paragraph, rushed through my examples and stared at the floor. My teacher advised me to practise in front of others, but embarrassment made me avoid every opportunity. I kept rewriting the speech instead, hoping that better words would solve the problem.

            A week before the competition, my younger brother found me practising alone. He sat on my bed and listened. Each time I stumbled, he waited without laughing. Gradually, I invited my parents and then two classmates to become my audience. Their comments helped me slow down and make eye contact.

            On competition day, my hands still trembled. Halfway through the speech, I lost my place. Rather than freezing, I paused, took a breath and continued in my own words. I did not win, but I completed the speech clearly and received a commendation.

            At first, the result disappointed me because I had hoped to bring a trophy back to school. Later, I realised the competition had exposed both my overconfidence and my fear of failure. Careful practice had not guaranteed a prize, but it had taught me to accept criticism and recover from mistakes. I also understood that confidence comes from repeated effort, not simply from believing that I am talented. Those lessons mattered beyond one afternoon on a stage.'
        ),
        (
            'ENGLISH',
            'ADVANCED',
            'The Night of the Power Cut',
            'The ceiling fan slowed to a stop, and every light in our flat vanished at once. For a few seconds, the darkness seemed complete. Then phones began glowing in neighbouring windows, and surprised voices drifted along the corridor.

            My first thought was the history presentation stored on my computer. I had postponed saving a backup, and the blank screen now reflected my anxious face. While I complained, my grandmother calmly found a torch and suggested checking on Mr Lim, our elderly neighbour. He lived alone and depended on an electric mobility aid.

            We discovered him near his doorway, trying to reach a drawer where he kept spare batteries. My brother held the torch while I retrieved them. Other neighbours soon emerged. One family brought candles, another shared a battery-powered fan, and someone called the electricity provider. With the lifts unavailable, two adults climbed twelve floors to deliver medicine to a resident who had just returned from hospital.

            The corridor, usually a place of hurried greetings, became a gathering point. Children told stories while adults exchanged food that might spoil in warm refrigerators. Even Mr Lim began joking about our unexpected candlelight party.

            Power returned two hours later. Cheers broke out, followed by the familiar hum of appliances. I was relieved to discover that my presentation had survived, yet it no longer seemed like the evening’s greatest concern. The blackout had interrupted our routines, but it also illuminated a community hidden behind closed doors. Before going to bed, I saved my work twice and wrote down the names of neighbours I had finally come to know.'
        ),
        (
            'ENGLISH',
            'ADVANCED',
            'The Phone on the Bus',
            'The phone slid across the bus floor as the vehicle turned sharply. I trapped it beneath my shoe before it disappeared under a seat. Its case was cracked, but the screen lit up with a photograph of a laughing family and twelve missed calls from “Mum”. The owner had clearly left in a hurry.

            My stop was approaching, and I was expected at a team rehearsal. Handing the phone to the driver would have been simple, yet an uneasy thought stopped me: what if the owner returned to the interchange and could not trace which bus held it? I answered the next call. A breathless woman explained that the phone belonged to her son, who had alighted two stops earlier on his way to an examination.

            We agreed to meet at the interchange. I messaged my teammates, got off several stops beyond my destination and boarded a bus travelling back. By the time I arrived, rehearsal had begun. A boy in school uniform stood beside his mother, repeatedly checking the road. Relief transformed his face when I raised the phone.

            He thanked me, then admitted that his examination entry details were stored on it. His mother offered to pay for my taxi, but I declined and hurried to rehearsal. My teammates were annoyed by my late arrival. Once I explained, however, our captain quietly shifted the schedule so that I could join the final run.

            I had expected honesty to feel effortless and immediately rewarding. Instead, returning the phone cost time and inconvenienced others. That was precisely why the decision stayed with me. Doing the right thing is meaningful not when it is convenient, but when there is a genuine reason to walk away.'
        )
    RETURNING id, title
),
anchor_scores (title, criterion_name, score) AS (
    VALUES
        ('My Lost Pencil', 'TASK_IDEAS', 2),
        ('My Lost Pencil', 'ORGANISATION_COHESION', 1),
        ('My Lost Pencil', 'GRAMMAR_SENTENCE_CONTROL', 1),
        ('My Lost Pencil', 'VOCABULARY_WORD_CHOICE', 2),
        ('My Lost Pencil', 'MECHANICS', 1),
        ('My Lost Pencil', 'EXPRESSION_DESCRIPTION_VOICE', 1),

        ('A Rainy Recess', 'TASK_IDEAS', 2),
        ('A Rainy Recess', 'ORGANISATION_COHESION', 2),
        ('A Rainy Recess', 'GRAMMAR_SENTENCE_CONTROL', 2),
        ('A Rainy Recess', 'VOCABULARY_WORD_CHOICE', 1),
        ('A Rainy Recess', 'MECHANICS', 2),
        ('A Rainy Recess', 'EXPRESSION_DESCRIPTION_VOICE', 1),

        ('Helping at the Market', 'TASK_IDEAS', 3),
        ('Helping at the Market', 'ORGANISATION_COHESION', 3),
        ('Helping at the Market', 'GRAMMAR_SENTENCE_CONTROL', 3),
        ('Helping at the Market', 'VOCABULARY_WORD_CHOICE', 3),
        ('Helping at the Market', 'MECHANICS', 3),
        ('Helping at the Market', 'EXPRESSION_DESCRIPTION_VOICE', 3),

        ('The Stray Kitten', 'TASK_IDEAS', 4),
        ('The Stray Kitten', 'ORGANISATION_COHESION', 4),
        ('The Stray Kitten', 'GRAMMAR_SENTENCE_CONTROL', 4),
        ('The Stray Kitten', 'VOCABULARY_WORD_CHOICE', 4),
        ('The Stray Kitten', 'MECHANICS', 4),
        ('The Stray Kitten', 'EXPRESSION_DESCRIPTION_VOICE', 4),

        ('My Surprise Birthday', 'TASK_IDEAS', 5),
        ('My Surprise Birthday', 'ORGANISATION_COHESION', 4),
        ('My Surprise Birthday', 'GRAMMAR_SENTENCE_CONTROL', 4),
        ('My Surprise Birthday', 'VOCABULARY_WORD_CHOICE', 4),
        ('My Surprise Birthday', 'MECHANICS', 5),
        ('My Surprise Birthday', 'EXPRESSION_DESCRIPTION_VOICE', 4),

        ('The Broken Window', 'TASK_IDEAS', 2),
        ('The Broken Window', 'ORGANISATION_COHESION', 1),
        ('The Broken Window', 'GRAMMAR_SENTENCE_CONTROL', 2),
        ('The Broken Window', 'VOCABULARY_WORD_CHOICE', 2),
        ('The Broken Window', 'MECHANICS', 1),
        ('The Broken Window', 'EXPRESSION_DESCRIPTION_VOICE', 1),

        ('The Day I Was Late', 'TASK_IDEAS', 2),
        ('The Day I Was Late', 'ORGANISATION_COHESION', 2),
        ('The Day I Was Late', 'GRAMMAR_SENTENCE_CONTROL', 2),
        ('The Day I Was Late', 'VOCABULARY_WORD_CHOICE', 2),
        ('The Day I Was Late', 'MECHANICS', 2),
        ('The Day I Was Late', 'EXPRESSION_DESCRIPTION_VOICE', 2),

        ('Our Class Garden', 'TASK_IDEAS', 3),
        ('Our Class Garden', 'ORGANISATION_COHESION', 3),
        ('Our Class Garden', 'GRAMMAR_SENTENCE_CONTROL', 3),
        ('Our Class Garden', 'VOCABULARY_WORD_CHOICE', 3),
        ('Our Class Garden', 'MECHANICS', 4),
        ('Our Class Garden', 'EXPRESSION_DESCRIPTION_VOICE', 3),

        ('An Unexpected Kindness', 'TASK_IDEAS', 4),
        ('An Unexpected Kindness', 'ORGANISATION_COHESION', 4),
        ('An Unexpected Kindness', 'GRAMMAR_SENTENCE_CONTROL', 4),
        ('An Unexpected Kindness', 'VOCABULARY_WORD_CHOICE', 4),
        ('An Unexpected Kindness', 'MECHANICS', 4),
        ('An Unexpected Kindness', 'EXPRESSION_DESCRIPTION_VOICE', 5),

        ('The Missing Wallet', 'TASK_IDEAS', 5),
        ('The Missing Wallet', 'ORGANISATION_COHESION', 5),
        ('The Missing Wallet', 'GRAMMAR_SENTENCE_CONTROL', 4),
        ('The Missing Wallet', 'VOCABULARY_WORD_CHOICE', 4),
        ('The Missing Wallet', 'MECHANICS', 5),
        ('The Missing Wallet', 'EXPRESSION_DESCRIPTION_VOICE', 5),

        ('The Choice I Regretted', 'TASK_IDEAS', 1),
        ('The Choice I Regretted', 'ORGANISATION_COHESION', 2),
        ('The Choice I Regretted', 'GRAMMAR_SENTENCE_CONTROL', 2),
        ('The Choice I Regretted', 'VOCABULARY_WORD_CHOICE', 2),
        ('The Choice I Regretted', 'MECHANICS', 1),
        ('The Choice I Regretted', 'EXPRESSION_DESCRIPTION_VOICE', 1),

        ('A Misunderstanding Between Friends', 'TASK_IDEAS', 2),
        ('A Misunderstanding Between Friends', 'ORGANISATION_COHESION', 2),
        ('A Misunderstanding Between Friends', 'GRAMMAR_SENTENCE_CONTROL', 2),
        ('A Misunderstanding Between Friends', 'VOCABULARY_WORD_CHOICE', 3),
        ('A Misunderstanding Between Friends', 'MECHANICS', 2),
        ('A Misunderstanding Between Friends', 'EXPRESSION_DESCRIPTION_VOICE', 2),

        ('The Competition That Changed Me', 'TASK_IDEAS', 3),
        ('The Competition That Changed Me', 'ORGANISATION_COHESION', 3),
        ('The Competition That Changed Me', 'GRAMMAR_SENTENCE_CONTROL', 3),
        ('The Competition That Changed Me', 'VOCABULARY_WORD_CHOICE', 3),
        ('The Competition That Changed Me', 'MECHANICS', 4),
        ('The Competition That Changed Me', 'EXPRESSION_DESCRIPTION_VOICE', 3),

        ('The Night of the Power Cut', 'TASK_IDEAS', 4),
        ('The Night of the Power Cut', 'ORGANISATION_COHESION', 4),
        ('The Night of the Power Cut', 'GRAMMAR_SENTENCE_CONTROL', 4),
        ('The Night of the Power Cut', 'VOCABULARY_WORD_CHOICE', 4),
        ('The Night of the Power Cut', 'MECHANICS', 5),
        ('The Night of the Power Cut', 'EXPRESSION_DESCRIPTION_VOICE', 4),

        ('The Phone on the Bus', 'TASK_IDEAS', 5),
        ('The Phone on the Bus', 'ORGANISATION_COHESION', 5),
        ('The Phone on the Bus', 'GRAMMAR_SENTENCE_CONTROL', 5),
        ('The Phone on the Bus', 'VOCABULARY_WORD_CHOICE', 4),
        ('The Phone on the Bus', 'MECHANICS', 5),
        ('The Phone on the Bus', 'EXPRESSION_DESCRIPTION_VOICE', 5)
)
INSERT INTO grading_anchor_score (
    anchor_id,
    criterion_name,
    score
)
SELECT
    inserted_anchors.id,
    anchor_scores.criterion_name,
    anchor_scores.score
FROM inserted_anchors
JOIN anchor_scores ON anchor_scores.title = inserted_anchors.title;
