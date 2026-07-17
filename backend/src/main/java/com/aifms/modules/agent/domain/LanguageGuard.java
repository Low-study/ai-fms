package com.aifms.modules.agent.domain;

/**
 * 语言守卫工具——检测文本语种，AI 输出语言不一致时强制重试。
 */
public final class LanguageGuard {

    private LanguageGuard() {}

    /** 检测文本语种 */
    public static String detect(String text) {
        if (text == null || text.isBlank()) return "en";
        if (text.codePoints().anyMatch(cp -> (cp >= 0x3040 && cp <= 0x30FF))) return "ja";
        if (text.codePoints().anyMatch(cp -> cp >= 0x4E00 && cp <= 0x9FFF)) return "zh";
        return "en";
    }

    /** 生成"语言不匹配"重试强调 Prompt */
    public static String retryEmphasis(String expectedLang) {
        String lang = switch (expectedLang) {
            case "ja" -> "Japanese";
            case "zh" -> "Chinese";
            default -> "English";
        };
        return "\n\n[IMPORTANT — RETRY] Your previous response was in the WRONG language. "
                + "You MUST respond in " + lang + ". Do NOT translate. "
                + "Write the output directly in " + lang + ".";
    }
}
