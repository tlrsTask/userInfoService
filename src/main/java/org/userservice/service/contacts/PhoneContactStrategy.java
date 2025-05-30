package org.userservice.service.contacts;

import org.springframework.stereotype.Component;
import org.userservice.entity.UserDetails;
import org.userservice.exception.BadRequestException;
import org.userservice.service.contacts.interfaces.ContactStrategy;

@Component
public class PhoneContactStrategy implements ContactStrategy {

    @Override
    public boolean supports(String contactType) {
        return "phone".equalsIgnoreCase(contactType);
    }

    @Override
    public void validateContact(String value) {
        if (value == null || !value.matches("^\\+?[0-9]{11}$")) {
            throw new BadRequestException("Invalid phone format: " + value);
        }
    }

    @Override
    public void update(UserDetails details, String value) {
        details.setPhone(value);
    }

    @Override
    public void delete(UserDetails details) {
        details.setPhone(null);
    }

    @Override
    public String get(UserDetails details) {
        return details.getPhone();
    }
}
