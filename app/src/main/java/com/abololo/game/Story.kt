package com.abololo.game

/** All story text: briefings, cutscene dialogue, objectives, endings. Arabic first, English second. */
object Story {
    fun t(ar: String, en: String) = L.t(ar, en)

    fun missionTitle(m: Int): String = when (m) {
        1 -> t("خزّان الماء", "The Water Tank")
        2 -> t("حماية المسّاحين", "Guard the Surveyors")
        3 -> t("معركة الخندق", "Battle of the Trench")
        4 -> t("خدعة دار صباح", "The Dar Sabah Ruse")
        else -> t("سقوط النخب", "Fall of the Elites")
    }

    fun missionHero(m: Int): String = when (m) {
        1 -> t("القائد السافنا", "Commander Savanna")
        2 -> t("القائد القبة", "Commander Qubba")
        3 -> t("السافنا • القبة • أبولو", "Savanna • Qubba • Apollo")
        4 -> t("السافنا والقبة", "Savanna & Qubba")
        else -> t("السافنا والقبة", "Savanna & Qubba")
    }

    fun brief(m: Int): String = when (m) {
        1 -> t(
            "تدور حرب بين الجنجا والنخب، واستعان النخب بالفلنقايات لحمايتهم ولحماية معسكرهم الرئيسي. كُلّف السافنا والقبة بتدمير معسكر الفلنقايات في مدينة الفاشرلاند.\n\nالمهمة: تقدّم مع فرقتك حتى خزّان المياه وأغلق صمّامه لقطع الإمداد عن المعسكر. استعمل الأسوار والسواتر، واطلب الإسناد المدفعي عند الحاجة.",
            "The Janja are at war with the Elites, who hired the Falanqayat to guard their main camp. Savanna and Qubba must destroy the Falanqayat camp in Fasherland.\n\nMission: push to the water tank with your squad and close its valve. Use cover, and call fire support when needed."
        )
        2 -> t(
            "تقود القبة قوة تحمي خبراء التدوين (المسح) أثناء رسم معسكر الفلنقايات. اجلس خلف مدفع 23 ملم على سيارة دفع رباعي، وأسقط طائرات النخب الداعمة للفلنقايات.\n\nالمهمة: أبقِ الخبراء أحياء حتى يكتمل التدوين، وأسقط الطائرات إلى أن يتهدّم المعسكر.",
            "Qubba commands troops guarding the survey experts while they map the camp. Fire the 23mm cannon on the 4x4 at the Elite aircraft.\n\nMission: keep the experts alive until the survey completes and shoot down aircraft until the camp is destroyed."
        )
        3 -> t(
            "الهجوم الشامل على المعسكر. يبدأ السافنا بتدمير غرفة التحكم الجوي، ثم تدور معركة الخندق: لا تدع الفلنقايات يصلون مخرجه. بعدها يجمع أبولو الناجين لمحاكمة ميدانية، وتُباد الفلنقايات ويُؤسَر المعسكر.",
            "The full assault. Savanna destroys the air-control room, then the trench battle: don't let the Falanqayat reach the exit. Then Apollo gathers the survivors for a field trial and the camp is captured."
        )
        4 -> t(
            "ينضم السافنا والقبة إلى معسكر النخب في دار صباح، ويخدعان الفلنقايات بمهمة وهمية لمهاجمة قاعدة الجنجا. في الطريق يقاتلانهم ويدمّرانهم، ثم يعودان للاحتفال مع النخب.",
            "Savanna and Qubba join the Elite camp at Dar Sabah and trick the Falanqayat with a fake mission against the Janja base. On the way they fight and destroy them, then return to celebrate with the Elites."
        )
        else -> t(
            "يتحالف السافنا والقبة مع ضباط النخب المنشقّين لإسقاط النخب وتدمير معسكرهم. يستدعي النخب قوات الدينار، ثم تسقط سلطتهم. يتولّى السافنا والقبة الحكم ويوقّعان السلام مع الجنجا.",
            "Savanna and Qubba ally with defecting Elite officers to overthrow the Elites. The Elites call in the Dinari, then lose power. Savanna and Qubba take the country and sign peace with the Janja."
        )
    }

    fun winText(m: Int): String = when (m) {
        1 -> t("أُغلق الصمّام وانقطع الماء عن معسكر الفلنقايات.", "The valve is shut and the Falanqayat camp is cut off.")
        2 -> t("اكتمل التدوين وسقط المعسكر تحت القصف.", "The survey is complete and the camp falls.")
        3 -> t("أُبيدت الفلنقايات عن آخرهم وأُخذ المعسكر.", "Every Falanqayat is destroyed. The camp is ours.")
        4 -> t("نجحت الخدعة واحتفل الجميع في دار صباح.", "The ruse worked and Dar Sabah celebrates.")
        else -> t("سقط النخب وعمّ السلام.", "The Elites have fallen. Peace at last.")
    }

    fun loseText(r: Int): String = when (r) {
        1 -> t("سقط القائد في الميدان.", "The commander has fallen.")
        2 -> t("دُمّرت المركبة.", "The vehicle was destroyed.")
        3 -> t("قُتل خبراء التدوين.", "The survey experts were killed.")
        4 -> t("اخترق العدو مخرج الخندق.", "The enemy broke through the trench exit.")
        else -> t("فشلت المهمة.", "Mission failed.")
    }

    fun epilogue(): String = t(
        "تولّى السافنا والقبة حكم البلاد ووقّعا السلام مع الجنجا، وعمّ الاحتفال إلا النخب والفلنقايات، فقد نُفوا إلى «السماء ذات البروج».\n\nالنهاية",
        "Savanna and Qubba took the country and signed peace with the Janja. Everyone celebrated, except the Elites and the Falanqayat, exiled to \"the sky of constellations\".\n\nThe End"
    )

    fun objective(id: Int): String = when (id) {
        1 -> t("تقدّم نحو خزّان الماء", "Advance to the water tank")
        2 -> t("قف عند الصمّام لإغلاقه", "Stand at the valve to close it")
        3 -> t("احمِ الخبراء حتى اكتمال التدوين", "Protect the experts until the survey ends")
        4 -> t("دمّر غرفة التحكم الجوي", "Destroy the air-control room")
        5 -> t("امنع الفلنقايات من بلوغ مخرج الخندق", "Keep the Falanqayat from the trench exit")
        6 -> t("أبِد الفلنقايات المتبقين", "Wipe out the remaining Falanqayat")
        7 -> t("رافق الفلنقايات نحو قاعدة الجنجا", "Escort the Falanqayat toward the Janja base")
        8 -> t("اقضِ على الفلنقايات الخائنين", "Destroy the Falanqayat")
        9 -> t("عُد إلى معسكر النخب", "Return to the Elite camp")
        10 -> t("دمّر غرف التحكم الجوي", "Destroy the air-control rooms")
        11 -> t("تصدَّ لموجات الدينار", "Hold off the Dinari waves")
        else -> ""
    }

    fun speaker(key: String): String = when (key) {
        "savanna" -> t("السافنا", "Savanna")
        "qubba" -> t("القبة", "Qubba")
        "apollo" -> t("أبولو", "Apollo")
        "elite" -> t("ضابط من النخب", "Elite officer")
        "defector" -> t("ضابط منشق", "Defecting officer")
        "falan" -> t("قائد الفلنقايات", "Falanqayat chief")
        "dinari" -> t("قائد الدينار", "Dinari commander")
        else -> t("الراوي", "Narrator")
    }

    /** each line: speaker key, Arabic, English */
    fun cut(id: Int): Array<Array<String>> = when (id) {
        31 -> arrayOf(
            arrayOf("savanna", "سقطت غرفة التحكم! لن تحميهم طائراتهم بعد الآن.", "The control room is down! Their air cover is gone."),
            arrayOf("qubba", "الفلنقايات يتدفقون نحو الخندق. تولّ المخرج يا قبة وأمسكه.", "The Falanqayat pour toward the trench. Hold the exit."),
            arrayOf("narr", "معركة الخندق: لا تدعهم يصلون المخرج.", "Trench battle: keep them from the exit.")
        )
        32 -> arrayOf(
            arrayOf("qubba", "صمد الخندق. انتهى هجومهم.", "The trench held. Their attack is broken."),
            arrayOf("apollo", "أنا أجمع الناجين للمحاكمة الميدانية، ثم نحسم الأمر.", "I'll gather the survivors for the field trial, then we finish it."),
            arrayOf("narr", "أبولو يتقدّم لإبادة ما تبقّى من الفلنقايات.", "Apollo advances to wipe out the rest.")
        )
        41 -> arrayOf(
            arrayOf("elite", "أهلاً بكم في دار صباح. الفلنقايات جاهزون للمسير.", "Welcome to Dar Sabah. The Falanqayat are ready to march."),
            arrayOf("savanna", "سنقول لهم إنها مهمة لمهاجمة قاعدة الجنجا.", "We'll tell them it's a mission to attack the Janja base."),
            arrayOf("qubba", "وعند منتصف الطريق نكشف الخدعة.", "And halfway there we spring the trap.")
        )
        42 -> arrayOf(
            arrayOf("savanna", "الآن! الخدعة انكشفت، لا تُبقوا منهم أحداً!", "Now! The ruse is revealed, leave none of them!"),
            arrayOf("falan", "خُنّا! إلى السلاح!", "Betrayed! To arms!")
        )
        43 -> arrayOf(
            arrayOf("elite", "عاد الأبطال! الاحتفال في دار صباح!", "The heroes return! Celebrate in Dar Sabah!"),
            arrayOf("savanna", "ما زال الغدر مستمراً... لكن لنرقص الليلة.", "The scheming isn't over... but tonight we dance.")
        )
        51 -> arrayOf(
            arrayOf("defector", "كثيرون منّا سئموا النخب. نحن معكما.", "Many of us are sick of the Elites. We're with you."),
            arrayOf("savanna", "دمّروا غرف التحكم الجوي، ثم نصل إلى معسكرهم.", "Destroy the control rooms, then we reach their camp."),
            arrayOf("qubba", "لنُسقط النخب ونصنع السلام.", "Let's overthrow the Elites and make peace.")
        )
        52 -> arrayOf(
            arrayOf("elite", "استدعينا الدينار! سيسحقونكم!", "We called the Dinari! They'll crush you!"),
            arrayOf("dinari", "جنود الدينار على الأرض. لا نريد الشرّ لكننا مأمورون.", "Dinari forces on the ground. We mean no harm, but we have orders."),
            arrayOf("savanna", "اثبتوا! هذه آخر موجة.", "Hold on! This is the last wave.")
        )
        53 -> arrayOf(
            arrayOf("dinari", "لقد سقط النخب. نُلقي السلاح.", "The Elites have fallen. We lay down our arms."),
            arrayOf("qubba", "تحيا البلاد! وقّعنا السلام مع الجنجا!", "Long live the country! Peace with the Janja is signed!"),
            arrayOf("narr", "نُفي النخب والفلنقايات إلى «السماء ذات البروج».", "The Elites and Falanqayat are exiled to the sky of constellations.")
        )
        else -> arrayOf()
    }
}
