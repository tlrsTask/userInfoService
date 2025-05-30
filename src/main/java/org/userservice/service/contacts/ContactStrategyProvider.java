package org.userservice.service.contacts;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.userservice.service.contacts.interfaces.ContactStrategy;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactStrategyProvider {

    private final List<ContactStrategy> strategies;

    public ContactStrategy getStrategy(String contactType) {
        return strategies.stream()
                .filter(s -> s.supports(contactType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported contact type: " + contactType));
    }

    public List<ContactStrategy> getAllStrategies() {
        return new ArrayList<>(strategies);
    }
}
