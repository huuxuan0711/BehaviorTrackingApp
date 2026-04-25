package com.xmobile.project2digitalwellbeing.data.usage.source.system

import com.xmobile.project2digitalwellbeing.domain.usage.model.SourceAppCategory

internal data class CategoryKeywordRule(
    val sourceCategory: SourceAppCategory,
    val keywords: Set<String>
)

internal val APP_CATEGORY_KEYWORD_RULES = listOf(
    CategoryKeywordRule(
        sourceCategory = SourceAppCategory.SOCIAL_NETWORKING,
        keywords = setOf(
            "tiktok", "instagram", "facebook", "messenger", "twitter", "snapchat",
            "reddit", "pinterest", "threads", "social", "bereal", "weibo", "tumblr",
            "linkedin", "vk", "xiaohongshu", "weverse", "clubhouse"
        )
    ),
    CategoryKeywordRule(
        sourceCategory = SourceAppCategory.MESSAGING,
        keywords = setOf(
            "whatsapp", "telegram", "line", "discord", "signal", "viber", "wechat",
            "kakaotalk", "skype", "chat", "mail", "gmail", "outlook", "message",
            "messaging", "sms", "contact", "meet", "zoom", "teams", "slack",
            "threema", "messengerlite", "im", "inbox"
        )
    ),
    CategoryKeywordRule(
        sourceCategory = SourceAppCategory.VIDEO_STREAMING,
        keywords = setOf(
            "youtube", "netflix", "disney", "primevideo", "video", "vlc", "player",
            "mxplayer", "twitch", "stream", "streaming", "hulu", "viu", "iqiyi",
            "bilibili", "plex", "crunchyroll", "hotstar", "movie"
        )
    ),
    CategoryKeywordRule(
        sourceCategory = SourceAppCategory.BROWSER,
        keywords = setOf(
            "chrome", "browser", "firefox", "opera", "edge", "brave", "duckduckgo",
            "samsungbrowser", "internet", "vivaldi", "kiwibrowser", "focus"
        )
    ),
    CategoryKeywordRule(
        sourceCategory = SourceAppCategory.MUSIC_AUDIO,
        keywords = setOf(
            "spotify", "music", "audio", "sound", "podcast", "radio", "mp3",
            "deezer", "soundcloud", "ytmusic", "applemusic", "tidal",
            "audiobook", "shazam"
        )
    ),
    CategoryKeywordRule(
        sourceCategory = SourceAppCategory.PRODUCTIVITY,
        keywords = setOf(
            "docs", "sheet", "slides", "calendar", "drive", "workspace", "notion",
            "office", "word", "excel", "powerpoint", "todo", "task", "keep",
            "evernote", "onenote", "trello", "asana", "jira", "planner",
            "reminder", "organizer", "workspace", "whiteboard"
        )
    ),
    CategoryKeywordRule(
        sourceCategory = SourceAppCategory.FINANCE,
        keywords = setOf(
            "bank", "banking", "wallet", "finance", "financial", "invest", "stock",
            "trading", "crypto", "coin", "paypal", "revolut", "binance", "coinbase",
            "mint", "robinhood"
        )
    ),
    CategoryKeywordRule(
        sourceCategory = SourceAppCategory.EDUCATION,
        keywords = setOf(
            "learn", "edu", "school", "classroom", "course", "study", "dictionary",
            "duolingo", "udemy", "coursera", "quizlet", "khan", "academy",
            "lecture", "lesson", "homework"
        )
    ),
    CategoryKeywordRule(
        sourceCategory = SourceAppCategory.READING_INFORMATION,
        keywords = setOf(
            "wikipedia", "wiki", "medium", "reader", "reading", "news", "article",
            "magazine", "book", "books", "ebook", "kindle", "reference", "dictionary"
        )
    ),
    CategoryKeywordRule(
        sourceCategory = SourceAppCategory.GAME,
        keywords = setOf(
            "game", "gaming", "playgames", "roblox", "minecraft", "candycrush",
            "pubg", "mobilelegends", "freefire", "genshin", "chess", "supercell",
            "arcade", "racing", "strategy", "puzzle"
        )
    ),
    CategoryKeywordRule(
        sourceCategory = SourceAppCategory.CREATIVITY,
        keywords = setOf(
            "design", "draw", "drawing", "paint", "art", "canvas", "canva",
            "editor", "photoeditor", "lightroom", "photoshop", "illustration"
        )
    ),
    CategoryKeywordRule(
        sourceCategory = SourceAppCategory.TOOLS_UTILITIES,
        keywords = setOf(
            "tool", "utility", "calculator", "clock", "setting", "settings",
            "launcher", "cleaner", "filemanager", "files", "scanner", "weather",
            "vpn", "notes", "recorder", "map", "maps", "translate",
            "keyboard", "camera", "compass"
        )
    )
)
