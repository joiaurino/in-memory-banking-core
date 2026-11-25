package br.com.banking.core.cli;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class MessageResolver {

    private final ResourceBundle bundle;

    public MessageResolver(String baseName) {
        this.bundle = ResourceBundle.getBundle(baseName);
    }

    public String get(String key, Object... args) {
        String pattern = bundle.getString(key);
        return args.length == 0 ? pattern : MessageFormat.format(pattern, args);
    }
}