package com.logmonitor.service;

import com.logmonitor.entity.LogConfig;
import com.logmonitor.exception.CommandNotWhitelistedException;
import com.logmonitor.repository.LogConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class WhitelistedCommandResolver {

    private final LogConfigRepository logConfigRepository;

    public WhitelistedCommandResolver(LogConfigRepository logConfigRepository) {
        this.logConfigRepository = logConfigRepository;
    }

    @Transactional(readOnly = true)
    public String resolve(String commandKey, Map<String, String> placeholders) {
        LogConfig config = logConfigRepository.findByCommandKey(commandKey)
                .orElseThrow(() -> new CommandNotWhitelistedException(commandKey));

        String command = config.getCommandText();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            command = command.replace(entry.getKey(), entry.getValue());
        }

        if (command.contains("__")) {
            throw new IllegalStateException("Unresolved placeholder in command for key: " + commandKey);
        }

        return command;
    }
}
