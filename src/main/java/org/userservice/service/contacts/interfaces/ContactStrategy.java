package org.userservice.service.contacts.interfaces;

import org.userservice.entity.UserDetails;

public interface ContactStrategy {
    boolean supports(String contactType);
    void validateContact(String value);
    void update(UserDetails details, String value);
    void delete(UserDetails details);
    String get(UserDetails details);
}
