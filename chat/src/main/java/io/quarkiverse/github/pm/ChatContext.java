package io.quarkiverse.github.pm;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.websockets.next.WebSocketConnection;

@SessionScoped
public class ChatContext {

    @Inject
    WebSocketConnection connection;

    @Inject
    ObjectMapper objectMapper;

    public void thinking(String message) {
        event(new ChatEvent("thinking", message));
    }

    public void message(String message) {
        event(new ChatEvent("message", message));
    }

    public void console(String message) {
        event(new ChatEvent("console", message));
    }

    public void markdown(String markdown) {
        event(new ChatEvent("message", markdownToHtml(markdown)));
    }

    public void event(String type, Object data) {
        event(new ChatEvent(type, data));
    }

    public void event(ChatEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            connection.sendTextAndAwait(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    ManagedChatService.ManagedChat currentChat;
    String currentChatName;

    public ManagedChatService.ManagedChat currentChat() {
        return currentChat;
    }

    public void currentChat(String name, ManagedChatService.ManagedChat chat) {
        this.currentChatName = name;
        this.currentChat = chat;
    }

    public String currentChatName() {
        return currentChatName;
    }

    public void clearCurrentChat() {
        this.currentChatName = null;
        this.currentChat = null;
    }

    String currentRepository;

    public String currentRepository() {
        return currentRepository;
    }

    public void currentRepository(String repository) {
        this.currentRepository = repository;
    }

    static Parser parser;
    static HtmlRenderer renderer;

    static {
        parser = Parser.builder().build();
        renderer = HtmlRenderer.builder().build();
    }

    public static String markdownToHtml(String markdown) {
        try {
            Node document = parser.parse(markdown);
            return renderer.render(document);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
