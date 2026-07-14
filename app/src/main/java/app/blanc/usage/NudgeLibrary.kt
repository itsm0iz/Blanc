package app.blanc.usage

/**
 * The kind of moment a nudge fits. The engine picks a trigger from current
 * usage, then a random unseen message of that trigger.
 */
enum class NudgeTrigger {
    /** Social usage is high today. */
    SOCIAL_WARNING,

    /** Usage is trending healthier — celebrate it. */
    AFFIRMATION,

    /** A "what this much time could be" projection fact. */
    PROJECTION,

    /** A neutral awareness fact, always eligible as a fallback. */
    GENERAL,
}

/**
 * One nudge message. [template] may contain {tokens} the engine fills in:
 * {socialHours}, {weekHours}, {yearHours}, {yearDays}, {percent}.
 */
data class Nudge(val id: Int, val trigger: NudgeTrigger, val template: String)

/** Fill a template's {tokens} from [vars]; unknown tokens are left untouched. */
fun Nudge.format(vars: Map<String, String>): String {
    var text = template
    for ((key, value) in vars) text = text.replace("{$key}", value)
    return text
}

/** All messages of one [trigger]. Cheap; the list is small and static. */
fun nudgesFor(trigger: NudgeTrigger): List<Nudge> = NUDGES.filter { it.trigger == trigger }

/**
 * The message library. Adding a message is a single line — give it a unique id
 * and a trigger, and (optionally) any of the {tokens} above. Ids are grouped in
 * blocks of 100 per trigger purely so new entries have obvious homes; the engine
 * never relies on the numbering.
 */
val NUDGES: List<Nudge> = listOf(
    // --- Social warnings: fire when socials run high in a single day. ---
    Nudge(1, NudgeTrigger.SOCIAL_WARNING, "You've spent {socialHours}h on social apps today. What could the rest of the day be?"),
    Nudge(2, NudgeTrigger.SOCIAL_WARNING, "{socialHours} hours scrolling today — about two full movies' worth."),
    Nudge(3, NudgeTrigger.SOCIAL_WARNING, "Heads up: {socialHours}h on socials today. A short walk resets more than a refresh."),
    Nudge(4, NudgeTrigger.SOCIAL_WARNING, "{socialHours}h today. The feed will still be there tomorrow — will you remember it?"),
    Nudge(5, NudgeTrigger.SOCIAL_WARNING, "That's {socialHours}h in the feed today. Your attention is the product being sold."),
    Nudge(6, NudgeTrigger.SOCIAL_WARNING, "{socialHours} hours of socials today. Future you is quietly hoping you close the app."),
    Nudge(7, NudgeTrigger.SOCIAL_WARNING, "Big scroll day: {socialHours}h. What's one thing you meant to do instead?"),
    Nudge(8, NudgeTrigger.SOCIAL_WARNING, "{socialHours}h on socials. Boredom is where ideas come from — try sitting with it."),
    Nudge(9, NudgeTrigger.SOCIAL_WARNING, "{socialHours}h today. Nobody ever wished they'd scrolled more."),
    Nudge(10, NudgeTrigger.SOCIAL_WARNING, "You're at {socialHours}h of socials. The next hour could be yours instead."),
    Nudge(11, NudgeTrigger.SOCIAL_WARNING, "{socialHours}h in the feed. Notice you're the one holding the phone — you can set it down."),
    Nudge(12, NudgeTrigger.SOCIAL_WARNING, "That's {socialHours}h today. Endless feeds are built to never let you finish. You can."),
    Nudge(13, NudgeTrigger.SOCIAL_WARNING, "{socialHours}h scrolling. Your thumb ran a marathon; your legs haven't moved."),
    Nudge(14, NudgeTrigger.SOCIAL_WARNING, "Socials: {socialHours}h today. What would you tell a friend spending the same?"),
    Nudge(15, NudgeTrigger.SOCIAL_WARNING, "{socialHours}h and counting. A single deep breath breaks the loop faster than a swipe."),
    Nudge(16, NudgeTrigger.SOCIAL_WARNING, "You've given {socialHours}h to the feed. Give the next ten minutes to yourself."),
    Nudge(17, NudgeTrigger.SOCIAL_WARNING, "{socialHours}h today. The most interesting thing in the room probably isn't the screen."),
    Nudge(18, NudgeTrigger.SOCIAL_WARNING, "That's {socialHours}h. Comparison is the thief of joy, and the feed sells it wholesale."),
    Nudge(19, NudgeTrigger.SOCIAL_WARNING, "{socialHours}h on socials. Try the 'one good thing' test: did any of it matter?"),
    Nudge(20, NudgeTrigger.SOCIAL_WARNING, "Scrolled {socialHours}h today. Your calm is worth more than the next post."),
    Nudge(21, NudgeTrigger.SOCIAL_WARNING, "{socialHours}h in. The algorithm is patient — you don't have to be available."),
    Nudge(22, NudgeTrigger.SOCIAL_WARNING, "You're at {socialHours}h. Put it down for five minutes and see if you reach back."),
    Nudge(23, NudgeTrigger.SOCIAL_WARNING, "{socialHours}h of feed today. Real life loads faster than you think."),
    Nudge(24, NudgeTrigger.SOCIAL_WARNING, "That's {socialHours}h. Every 'just one more' was a choice — the next one can differ."),
    Nudge(25, NudgeTrigger.SOCIAL_WARNING, "{socialHours}h scrolling. Trade one scroll for one stretch. Your neck will thank you."),
    Nudge(26, NudgeTrigger.SOCIAL_WARNING, "Socials at {socialHours}h. The best posts you'll ever make happen away from the phone."),
    Nudge(27, NudgeTrigger.SOCIAL_WARNING, "{socialHours}h today. Attention is a muscle — the feed trains it to wander."),
    Nudge(28, NudgeTrigger.SOCIAL_WARNING, "You've spent {socialHours}h here today. Tomorrow's you is watching how tonight goes."),
    Nudge(29, NudgeTrigger.SOCIAL_WARNING, "{socialHours}h of socials. Somewhere on your list is something you'd rather have done."),
    Nudge(30, NudgeTrigger.SOCIAL_WARNING, "That's {socialHours}h in the scroll. The exit is one long-press away."),

    // --- Affirmations: fire on a healthier week (a real drop vs. last week). ---
    Nudge(100, NudgeTrigger.AFFIRMATION, "Your screen time dropped {percent}% this week. That's real progress. Keep going. 🌱"),
    Nudge(101, NudgeTrigger.AFFIRMATION, "Down {percent}% from last week. Small wins compound."),
    Nudge(102, NudgeTrigger.AFFIRMATION, "Lighter week — {weekHours}h total. Your attention is yours again."),
    Nudge(103, NudgeTrigger.AFFIRMATION, "You reached for your phone less this week. Notice how that feels."),
    Nudge(104, NudgeTrigger.AFFIRMATION, "{percent}% less this week. The point was never zero — it was intentional. Nice."),
    Nudge(105, NudgeTrigger.AFFIRMATION, "A calmer week. The world didn't end when you looked up. 🙂"),
    Nudge(106, NudgeTrigger.AFFIRMATION, "{percent}% down. You're proving the habit can bend. Keep leaning."),
    Nudge(107, NudgeTrigger.AFFIRMATION, "Fewer hours, more life. {percent}% lighter than last week — well done."),
    Nudge(108, NudgeTrigger.AFFIRMATION, "You spent {percent}% less time in the scroll this week — hours handed back to yourself."),
    Nudge(109, NudgeTrigger.AFFIRMATION, "Progress, not perfection: {percent}% down this week. That's the whole game."),
    Nudge(110, NudgeTrigger.AFFIRMATION, "This week was quieter — {weekHours}h. Quiet is where you hear yourself think."),
    Nudge(111, NudgeTrigger.AFFIRMATION, "{percent}% less. Whatever you did instead, do a little more of it."),
    Nudge(112, NudgeTrigger.AFFIRMATION, "You eased off {percent}% this week. Your future focus is compounding as we speak."),
    Nudge(113, NudgeTrigger.AFFIRMATION, "Lighter by {percent}%. The urge got smaller because you did the reps. 💪"),
    Nudge(114, NudgeTrigger.AFFIRMATION, "A gentler week — {weekHours}h. You're rewiring the reflex, one skipped scroll at a time."),
    Nudge(115, NudgeTrigger.AFFIRMATION, "{percent}% down and steady. This is what intentional looks like."),
    Nudge(116, NudgeTrigger.AFFIRMATION, "You looked up more this week. The people around you noticed, even if they didn't say."),
    Nudge(117, NudgeTrigger.AFFIRMATION, "{percent}% less this week — a win worth sitting with for a second. 🌿"),
    Nudge(118, NudgeTrigger.AFFIRMATION, "Down {percent}%. Momentum is on your side now; ride it."),
    Nudge(119, NudgeTrigger.AFFIRMATION, "This week: {weekHours}h on the phone, the rest on your actual life. Good trade."),
    Nudge(120, NudgeTrigger.AFFIRMATION, "{percent}% lighter. You're not fighting the phone anymore — you're just choosing."),
    Nudge(121, NudgeTrigger.AFFIRMATION, "Quieter week. The calm you feel isn't an accident; you built it."),
    Nudge(122, NudgeTrigger.AFFIRMATION, "{percent}% down. Keep the streak — future you is already grateful."),
    Nudge(123, NudgeTrigger.AFFIRMATION, "You gave yourself {percent}% more presence this week. That's the real metric."),
    Nudge(124, NudgeTrigger.AFFIRMATION, "A lighter week, {weekHours}h. Notice you didn't miss much, did you?"),
    Nudge(125, NudgeTrigger.AFFIRMATION, "{percent}% less time scrolling. That's a promise to yourself, kept."),

    // --- Projections: fire on the yearly pace (what all that time could become). ---
    Nudge(200, NudgeTrigger.PROJECTION, "At this pace: {yearHours} hours this year — about what it takes to reach conversational fluency in a new language."),
    Nudge(201, NudgeTrigger.PROJECTION, "{yearHours} hours a year — roughly {yearDays} full days. Imagine those days spent on something you'd be proud of."),
    Nudge(202, NudgeTrigger.PROJECTION, "This rate adds up to {yearHours} hours a year — enough to read around 200 books."),
    Nudge(203, NudgeTrigger.PROJECTION, "{yearHours} hours/year on your phone. A serious head start on any skill you've been putting off."),
    Nudge(204, NudgeTrigger.PROJECTION, "Projected {yearHours} hours this year. Learning to draw well takes a few hundred — you've got the time."),
    Nudge(205, NudgeTrigger.PROJECTION, "{yearHours} hours a year. Couch to marathon takes about 300 hours of running. Just saying."),
    Nudge(206, NudgeTrigger.PROJECTION, "At {yearHours} hours/year, you could learn the basics of an instrument twice over."),
    Nudge(207, NudgeTrigger.PROJECTION, "That's {yearHours} hours this year — the ~500 hours it takes to get genuinely good at a new skill fits inside that, with room to spare."),
    Nudge(208, NudgeTrigger.PROJECTION, "{yearHours} hours projected — about {yearDays} days. What would a whole free month be worth to you?"),
    Nudge(209, NudgeTrigger.PROJECTION, "At this rate, {yearHours} hours a year. A private pilot's license takes around 60 flight hours. Your call."),
    Nudge(210, NudgeTrigger.PROJECTION, "{yearHours} hours/year. That's enough to write a novel — 90,000 words at a gentle pace."),
    Nudge(211, NudgeTrigger.PROJECTION, "Projected {yearHours} hours. Woodworking, coding, cooking like a chef — all live under that number."),
    Nudge(212, NudgeTrigger.PROJECTION, "{yearHours} hours this year — roughly {yearDays} days awake. Days are the currency; spend on purpose."),
    Nudge(213, NudgeTrigger.PROJECTION, "At this pace you'll pass {yearHours} hours. Learning to swim well? A few dozen. You have oceans of time."),
    Nudge(214, NudgeTrigger.PROJECTION, "{yearHours} hours a year — about 250 hours to conversational Spanish, twice, with time left to travel."),
    Nudge(215, NudgeTrigger.PROJECTION, "Projected {yearHours} hours — enough to get fit, stay fit, and still have hundreds of hours over."),
    Nudge(216, NudgeTrigger.PROJECTION, "{yearHours} hours/year. The average person reads 12 books a year; you could read 100."),
    Nudge(217, NudgeTrigger.PROJECTION, "That's {yearHours} hours — about {yearDays} full days. Nobody's handing those back. Use them well."),
    Nudge(218, NudgeTrigger.PROJECTION, "At {yearHours} hours a year, you could learn to code and build the app you keep imagining."),
    Nudge(219, NudgeTrigger.PROJECTION, "{yearHours} hours projected. Mastering photography takes a few hundred hours of shooting. The camera's already in your pocket."),
    Nudge(220, NudgeTrigger.PROJECTION, "This pace: {yearHours} hours. A year of daily 20-minute meditation is under 130 hours. Calm is cheaper than you'd think."),
    Nudge(221, NudgeTrigger.PROJECTION, "{yearHours} hours/year — enough to learn to cook 100 dishes properly."),
    Nudge(222, NudgeTrigger.PROJECTION, "Projected {yearHours} hours — {yearDays} days. Small daily trades add up to a different you by next year."),
    Nudge(223, NudgeTrigger.PROJECTION, "{yearHours} hours this year. Touch-typing, chess, calligraphy — pick one and it's yours by December."),
    Nudge(224, NudgeTrigger.PROJECTION, "At this rate: {yearHours} hours. That's dozens of trips around the globe by flight time, spent on a screen."),
    Nudge(225, NudgeTrigger.PROJECTION, "{yearHours} hours a year. A part-time degree runs on less. Imagine the version of you who used them."),
    Nudge(226, NudgeTrigger.PROJECTION, "Projected {yearHours} hours — {yearDays} days. A different person is on the other side of these hours."),
    Nudge(227, NudgeTrigger.PROJECTION, "{yearHours} hours/year. Sketch, then paint, then sell it — all fits. The time isn't the problem."),
    Nudge(228, NudgeTrigger.PROJECTION, "At this pace, {yearHours} hours. Enough to build a side project, launch it, and learn from real users."),
    Nudge(229, NudgeTrigger.PROJECTION, "{yearHours} hours this year. The gap between 'I wish I could' and 'I can' is usually just these hours."),

    // --- General awareness: always eligible, the quiet fallback. ---
    Nudge(300, NudgeTrigger.GENERAL, "The average person checks their phone around 96 times a day. Awareness is the first step."),
    Nudge(301, NudgeTrigger.GENERAL, "Every notification is someone deciding your attention is theirs. You can decide back."),
    Nudge(302, NudgeTrigger.GENERAL, "It takes about 23 minutes to refocus after a distraction. Protect your deep work."),
    Nudge(303, NudgeTrigger.GENERAL, "Your attention is the most valuable thing you own. Spend it on purpose."),
    Nudge(304, NudgeTrigger.GENERAL, "A phone face-down is a small act of presence. Try it at your next meal."),
    Nudge(305, NudgeTrigger.GENERAL, "The best moments rarely happen through a screen. Go find one."),
    Nudge(306, NudgeTrigger.GENERAL, "Infinite scroll has no finish line by design. You get to draw your own."),
    Nudge(307, NudgeTrigger.GENERAL, "Notifications are optional. Silence a few today and feel the difference."),
    Nudge(308, NudgeTrigger.GENERAL, "Boredom isn't a problem to swipe away — it's the doorway to ideas."),
    Nudge(309, NudgeTrigger.GENERAL, "The apps are free because your time is the price. Spend it like it's expensive."),
    Nudge(310, NudgeTrigger.GENERAL, "One deep breath before you unlock the phone. Just one. It changes the reach."),
    Nudge(311, NudgeTrigger.GENERAL, "You don't have to answer the buzz the second it happens. It can wait; so can you."),
    Nudge(312, NudgeTrigger.GENERAL, "Try a screen-free hour before bed. Your sleep — and tomorrow — will notice."),
    Nudge(313, NudgeTrigger.GENERAL, "The feed refills forever. You don't have to be there when it does."),
    Nudge(314, NudgeTrigger.GENERAL, "Presence is a skill. Every time you look up, you practice it."),
    Nudge(315, NudgeTrigger.GENERAL, "Put the phone in another room for an hour. Notice how often your hand reaches for nothing."),
    Nudge(316, NudgeTrigger.GENERAL, "Most 'urgent' notifications aren't. Give the real ones room by muting the rest."),
    Nudge(317, NudgeTrigger.GENERAL, "Your focus is a garden. Every interruption is a footstep across it."),
    Nudge(318, NudgeTrigger.GENERAL, "A walk without headphones is a conversation with your own mind. Try it."),
    Nudge(319, NudgeTrigger.GENERAL, "The photo you don't take is sometimes the moment you actually keep."),
    Nudge(320, NudgeTrigger.GENERAL, "Checking less isn't missing out. It's being here for what's in front of you."),
    Nudge(321, NudgeTrigger.GENERAL, "Attention residue is real: half your mind stays on the last app you opened. Close the loop."),
    Nudge(322, NudgeTrigger.GENERAL, "You can love your phone and still put it down. Both are allowed."),
    Nudge(323, NudgeTrigger.GENERAL, "The scroll was designed by hundreds of engineers. Going outside was designed by no one — and it's better."),
    Nudge(324, NudgeTrigger.GENERAL, "Try 20-20-20: every 20 minutes, look 20 feet away for 20 seconds. Your eyes will thank you."),
    Nudge(325, NudgeTrigger.GENERAL, "Silence isn't empty. It's where your best thoughts have been waiting."),
    Nudge(326, NudgeTrigger.GENERAL, "The person in front of you is more interesting than the one on the screen. Usually by a lot."),
    Nudge(327, NudgeTrigger.GENERAL, "Single-tasking is a superpower now. Do one thing. Finish it. Feel that."),
    Nudge(328, NudgeTrigger.GENERAL, "You're not a machine that must check inputs constantly. Rest is productive."),
    Nudge(329, NudgeTrigger.GENERAL, "The habit loop is cue, routine, reward. Change the cue — leave the phone in your bag."),
)
