package io.quarkiverse.github.pm.util;

import java.util.Stack;

public class MarkdownToAnsi {
    public static final String REG = "\u001B[0";
    public static final String BOLD = "\u001B[1";
    public static final String DIM = "\u001B[2";
    public static final String ITALICS = "\u001B[3";
    public static final String UNDERLINE = "\u001B[4";
    public static final String STRIKE = "\u001B[9";

    public static final String BLACK = ";30";
    public static final String RED = ";31";
    public static final String GREEN = ";32";
    public static final String YELLOW = ";33";
    public static final String BLUE = ";34";
    public static final String MAGENTA = ";35";
    public static final String CYAN = ";36";
    public static final String WHITE = ";37";

    static boolean lastChars(String match, String buffer, int index) {
        if (index - match.length() - 1 < 0) {
            return false;
        }
        String last = buffer.substring(index - match.length() - 1, match.length());
        return match.equals(last);
    }

    public static String convert(String input) {
        boolean escaped = false;
        String buffer = input;
        int index = 0;
        Stack<String> effects = new Stack<>();

        StringBuffer outputBuffer = new StringBuffer();

        return outputBuffer.toString();
    }

    public static String render(String md) {
        return md
                // Bold
                .replaceAll("\\*\\*(.*?)\\*\\*", "\u001B[1;33m$1\u001B[0m")
                // Italic
                .replaceAll("\\*(.*?)\\*", "\u001B[3m$1\u001B[0m")
                // Underline
                .replaceAll("__(.*?)__", "\u001B[4m$1\u001B[0m")
                // Strikethrough
                .replaceAll("~~(.*?)~~", "\u001B[9m$1\u001B[0m")
                // Blockquote
                .replaceAll("(> ?.*)",
                        "\u001B[3m\u001B[34m\u001B[1m$1\u001B[22m\u001B[0m")
                // Lists (bold magenta number and bullet)
                .replaceAll("([\\d]+\\.|-|\\*) (.*)",
                        "\u001B[35m\u001B[1m$1\u001B[22m\u001B[0m $2")
                // Block code (black on gray)
                .replaceAll("(?s)```(\\w+)?\\n(.*?)\\n```",
                        "\u001B[3m\u001B[1m$1\u001B[22m\u001B[0m\n\u001B[57;107m$2\u001B[0m\n")
                // Inline code (black on gray)
                .replaceAll("`(.*?)`", "\u001B[2$1\u001B[0m")
                // Headers (cyan bold)
                .replaceAll("(#{1,6}) (.*?)\n",
                        "\u001B[36m\u001B[1m$1 $2\u001B[22m\u001B[0m\n")
                // Headers with a single line of text followed by 2 or more equal signs
                .replaceAll("(.*?\n={2,}\n)",
                        "\u001B[36m\u001B[1m$1\u001B[22m\u001B[0m\n")
                // Headers with a single line of text followed by 2 or more dashes
                .replaceAll("(.*?\n-{2,}\n)",
                        "\u001B[36m\u001B[1m$1\u001B[22m\u001B[0m\n")
                // Images (blue underlined)
                .replaceAll("!\\[(.*?)]\\((.*?)\\)",
                        "\u001B[34m$1\u001B[0m (\u001B[34m\u001B[4m$2\u001B[0m)")
                // Links (blue underlined)
                .replaceAll("!?\\[(.*?)]\\((.*?)\\)",
                        "\u001B[34m$1\u001B[0m (\u001B[34m\u001B[4m$2\u001B[0m)");
    }

}
