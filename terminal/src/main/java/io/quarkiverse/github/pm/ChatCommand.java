package io.quarkiverse.github.pm;

import io.quarkiverse.ai.github.chat.ChatWindow;
import io.quarkiverse.github.pm.util.BaseCommand;
import io.quarkiverse.langchain4j.chatscopes.LocalChatRoutes;
import jakarta.inject.Inject;
import picocli.CommandLine;

import java.util.Scanner;
import java.util.Stack;

@CommandLine.Command(name = "chat", description = "Start a chat session")
public class ChatCommand extends BaseCommand implements Runnable {
    public static final String BOLD_YELLOW = "\u001B[1;33m";
    // Bold Green escape sequence
    public static final String BOLD_GREEN = "\u001B[1;32m";
    // Reset escape sequence
    public static final String RESET = "\u001B[0m";
    public static final String DIM = "\u001B[2m";

    @Inject
    LocalChatRoutes.Client client;


    public void run() {
        Stack<String> placeholder = new Stack<>();
        placeholder.add("Book your vacation");

        LocalChatRoutes.SessionBuilder builder = client.builder()
                .messageHandler(message -> System.out.println(message))
                .thinkingHandler(thinking -> System.out.println(DIM + thinking + RESET))
                .eventHandler(ChatWindow.PUSH_CHAT_WINDOW, (event) -> placeholder.push((String)event))
                .eventHandler(ChatWindow.POP_CHAT_WINDOW, (event) -> placeholder.pop());

        LocalChatRoutes.Session session = builder.connect();

        System.out.println();
        System.out.println(BOLD_YELLOW + "Welcome to the AI Github Chat Bot" + RESET);
        System.out.println();
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        System.out.print(BOLD_GREEN + placeholder.peek() + RESET + ": ");
        while (scanner.hasNextLine()) {
            String userMessage = scanner.nextLine().trim();
            if (!userMessage.isEmpty()) {
                System.out.println();
                session.chat(userMessage);
            }
            System.out.println();
            System.out.print(BOLD_GREEN + placeholder.peek() + RESET + ": ");
        }
    }
}
