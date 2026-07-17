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
            case "ja" -> "JAPANESE (日本語)";
            case "zh" -> "CHINESE (中文)";
            default -> "ENGLISH";
        };
        return "\n\n[SYSTEM OVERRIDE] CRITICAL: Your previous output was in the WRONG LANGUAGE. "
                + "You MUST output in " + lang + " this time. "
                + "DO NOT output Japanese for non-Japanese inputs. "
                + "DO NOT output Chinese for non-Chinese inputs. "
                + "If the input is in English, you MUST write English. "
                + "This instruction overrides all other instructions.";
    }
}
