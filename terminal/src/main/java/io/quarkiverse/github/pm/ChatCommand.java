package io.quarkiverse.github.pm;

import java.util.Scanner;
import java.util.Stack;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jakarta.inject.Inject;

import io.quarkiverse.ai.github.chat.ChatWindow;
import io.quarkiverse.github.pm.util.BaseCommand;
import io.quarkiverse.github.pm.util.MarkdownToAnsi;
import io.quarkiverse.langchain4j.chatscopes.LocalChatRoutes;
import picocli.CommandLine;

@CommandLine.Command(name = "chat", description = "Start a chat session")
public class ChatCommand extends BaseCommand implements Runnable {
    public static final String BOLD_RED = "\u001B[1;31m";
    public static final String BOLD_YELLOW = "\u001B[1;33m";
    // Bold Green escape sequence
    public static final String BOLD_GREEN = "\u001B[1;32m";
    // Reset escape sequence
    public static final String RESET = "\u001B[0m";
    public static final String DIM = "\u001B[2m";

    @Inject
    LocalChatRoutes.Client client;
    Stack<String> placeholder = new Stack<>();

    ReadWriteLock lock = new ReentrantReadWriteLock();

    synchronized void messageHandler(String msg) {
        lock.readLock().lock();
        try {
            String render = MarkdownToAnsi.render(msg);
            System.out.println(render);
            System.out.flush();
        } finally {
            lock.readLock().unlock();
        }
    }

    synchronized void thinkingHandler(String thinking) {
        lock.readLock().lock();
        try {
            System.out.println(DIM + thinking + RESET);
            System.out.flush();
        } finally {
            lock.readLock().unlock();
        }
    }

    synchronized void errorHandler(String error) {
        lock.readLock().lock();
        try {
            System.err.println(BOLD_RED + error + RESET);
            System.err.flush();
        } finally {
            lock.readLock().unlock();
        }
    }

    synchronized void push(String msg) {
        lock.readLock().lock();
        try {
            placeholder.push(msg);
        } finally {
            lock.readLock().unlock();
        }
    }

    synchronized void pop(String msg) {
        lock.readLock().lock();
        try {
            placeholder.pop();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void run() {
        placeholder.add("Main menu");

        LocalChatRoutes.SessionBuilder builder = client.builder()
                .messageHandler(this::messageHandler)
                .thinkingHandler(this::thinkingHandler)
                .errorHandler(this::errorHandler)
                .eventHandler(ChatWindow.PUSH_CHAT_WINDOW, this::push)
                .eventHandler(ChatWindow.POP_CHAT_WINDOW, this::pop);

        LocalChatRoutes.Session session = builder.connect();

        System.out.println();
        System.out.println(BOLD_YELLOW + "Welcome to the AI Github Chat Bot" + RESET);
        System.out.println();
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        prompt();
        while (scanner.hasNextLine()) {
            String userMessage = scanner.nextLine().trim();
            if (!userMessage.isEmpty()) {
                System.out.println();
                session.chat(userMessage);
            }
            lock.writeLock().lock();
            System.out.println();
            prompt();
            lock.writeLock().unlock();
        }
    }

    private void prompt() {
        System.out.print("[" + BOLD_GREEN + placeholder.peek() + RESET + "]: ");
    }
}
