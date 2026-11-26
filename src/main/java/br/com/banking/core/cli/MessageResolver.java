package br.com.banking.core.cli;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

public class MessageResolver {

    private final ResourceBundle bundle;

    public MessageResolver(String baseName) {
        this.bundle = ResourceBundle.getBundle(baseName, Locale.US);
    }

    public String get(String key, Object... args) {
        if (args.length == 0) {
            return bundle.getString(key);
        }
        Object[] normalized = Arrays.stream(args)
                .map(arg -> arg instanceof BigDecimal bd ? bd.toPlainString() : arg)
                .toArray();

        return MessageFormat.format(bundle.getString(key), normalized);
    }
}