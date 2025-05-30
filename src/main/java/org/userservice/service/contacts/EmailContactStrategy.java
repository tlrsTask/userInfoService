package org.userservice.service.contacts;

import org.springframework.stereotype.Component;
import org.userservice.entity.UserDetails;
import org.userservice.exception.BadRequestException;
import org.userservice.service.contacts.interfaces.ContactStrategy;

@Component
public class EmailContactStrategy implements ContactStrategy {

    @Override
    public boolean supports(String contactType) {
        return "email".equalsIgnoreCase(contactType);
    }

    @Override
    public void validateContact(String value) {
        if (value == null || !value.matches("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")) {
            throw new BadRequestException("Invalid phone format: " + value);
        }
    }

    @Override
    public void update(UserDetails details, String value) {
        details.setEmail(value);
    }

    @Override
    public void delete(UserDetails details) {
        details.setEmail(null);
    }

    @Override
    public String get(UserDetails details) {
        return details.getEmail();
    }
}
