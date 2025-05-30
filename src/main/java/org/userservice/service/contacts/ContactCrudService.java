package org.userservice.service.contacts;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.userservice.dto.contact.ContactInfoResponseDto;
import org.userservice.dto.contact.ContactResponseDto;
import org.userservice.entity.UserDetails;
import org.userservice.exception.ResourceNotFoundException;
import org.userservice.repo.UserDetailsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.userservice.service.contacts.interfaces.ContactStrategy;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
@RequiredArgsConstructor
public class ContactCrudService {

    private final UserDetailsRepository userDetailsRepository;
    private final ContactStrategyProvider contactStrategyProvider;

    public ContactResponseDto updateContacts(UUID userId, Map<String, String> contacts) {
        UserDetails details = userDetailsRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User details not found for id=" + userId));
        for (Map.Entry<String, String> entry : contacts.entrySet()) {
            String contactType = entry.getKey();
            String value = entry.getValue();
            ContactStrategy strategy = contactStrategyProvider.getStrategy(contactType);
            strategy.validateContact(value);
            strategy.update(details, value);
        }
        userDetailsRepository.save(details);
        return new ContactResponseDto(details.getEmail(), details.getPhone());
    }

    public void deleteContact(UUID userId, String contactType) {
        UserDetails details = userDetailsRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User details not found for id=" + userId));
        ContactStrategy strategy = contactStrategyProvider.getStrategy(contactType);
        strategy.delete(details);
        userDetailsRepository.save(details);
    }

    @Transactional(readOnly = true)
    public ContactResponseDto getAllUserContacts(UUID userId) {
        UserDetails details = userDetailsRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User details not found for id=" + userId));
        return new ContactResponseDto(details.getEmail(), details.getPhone());
    }

    public void deleteAllContacts(UUID userId) {
        userDetailsRepository.findByUserId(userId).ifPresent(details -> {
            for (ContactStrategy strategy : contactStrategyProvider.getAllStrategies()) {
                String oldValue = strategy.get(details);
                if (oldValue != null && !oldValue.isEmpty()) {
                    strategy.delete(details);
                }
            }
            userDetailsRepository.save(details);
        });
    }

    @Transactional(readOnly = true)
    public ContactInfoResponseDto getContact(UUID userId, String contactType) {
        UserDetails details = userDetailsRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User details not found for id=" + userId));
        ContactStrategy strategy = contactStrategyProvider.getStrategy(contactType);
        String value = strategy.get(details);
        return new ContactInfoResponseDto(value, value != null && !value.isEmpty());
    }
}
