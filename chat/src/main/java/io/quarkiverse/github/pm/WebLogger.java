package io.quarkiverse.github.pm;

import java.text.MessageFormat;

import io.quarkiverse.github.util.JbossLogger;

public class WebLogger extends JbossLogger {
    ChatContext chatContext;

    public WebLogger(String name, ChatContext chatContext) {
        super(name);
        this.chatContext = chatContext;
    }

    public WebLogger(Class clz, ChatContext chatContext) {
        super(clz);
        this.chatContext = chatContext;
    }

    @Override
    public void thinking(String msg) {
        chatContext.thinking(msg);
    }

    @Override
    public void thinkingf(String msg, Object... params) {
        chatContext.thinking(String.format(msg, params));
    }

    @Override
    public void thinkingv(String msg, Object... params) {
        chatContext.thinking(MessageFormat.format(msg, params));
    }

}
