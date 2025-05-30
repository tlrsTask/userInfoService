package org.userservice.service.contacts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.userservice.dto.contact.ContactInfoResponseDto;
import org.userservice.dto.contact.ContactResponseDto;
import org.userservice.exception.ResourceNotFoundException;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {ContactsServiceIntegrationTest.TestConfig.class, ContactsService.class})
class ContactsServiceIntegrationTest {

    @Autowired
    private ContactsService contactsService;

    @Autowired
    private ContactCrudService contactCrudService;

    private final UUID userId = UUID.randomUUID();

    @Configuration
    static class TestConfig {
        @Bean
        @Primary
        public ContactCrudService contactCrudService() {
            return mock(ContactCrudService.class);
        }
    }

    @BeforeEach
    void setUp() {
        reset(contactCrudService);
    }

    @Test
    void getAllUserContacts_shouldReturnContactResponseDto() {
        ContactResponseDto expected = new ContactResponseDto("test@mail.ru", "+3451233217");
        when(contactCrudService.getAllUserContacts(userId)).thenReturn(expected);
        ContactResponseDto result = contactsService.getAllUserContacts(userId);
        assertEquals(expected, result);
        verify(contactCrudService).getAllUserContacts(userId);
    }

    @Test
    void updateContacts_shouldCallCrudServiceWithValidData() {
        Map<String, String> contacts = Map.of(
                "email", "test@mail.ru",
                "phone", "+7987654321"
        );

        ContactResponseDto responseDto = new ContactResponseDto("test@mail.ru", "+7987654321");
        when(contactCrudService.updateContacts(userId, contacts)).thenReturn(responseDto);
        ContactResponseDto result = contactsService.updateContacts(userId, contacts);

        assertNotNull(result);
        assertEquals(responseDto.email(), result.email());
        assertEquals(responseDto.phone(), result.phone());
        verify(contactCrudService).updateContacts(userId, contacts);
    }

    @Test
    void deleteAllContacts_shouldCallCrudService() {
        contactsService.deleteAllContacts(userId);
        verify(contactCrudService).deleteAllContacts(userId);
    }

    @Test
    void getContact_shouldReturnContactInfoResponseDto() {
        String contactType = "email";
        String value = "john@example.com";
        ContactInfoResponseDto expected = new ContactInfoResponseDto(value, true);
        when(contactCrudService.getContact(userId, contactType))
                .thenReturn(expected);
        ContactInfoResponseDto result = contactsService.getContact(userId, contactType);
        assertEquals(expected, result);
        verify(contactCrudService).getContact(userId, contactType);
    }

    @Test
    void deleteContact_shouldCallCrudService() {
        String contactType = "phone";
        contactsService.deleteContact(userId, contactType);
        verify(contactCrudService).deleteContact(userId, contactType);
    }

    @Test
    void getContact_whenContactNotSet_returnsUnset() {
        String contactType = "phone";
        ContactInfoResponseDto expected = new ContactInfoResponseDto(null, false);
        when(contactCrudService.getContact(userId, contactType)).thenReturn(expected);
        ContactInfoResponseDto result = contactsService.getContact(userId, contactType);
        assertFalse(result.isSet());
        assertNull(result.contactValue());
        verify(contactCrudService).getContact(userId, contactType);
    }

    @Test
    void updateContacts_whenInvalidContactType_throwsException() {
        Map<String, String> contacts = Map.of("invalid", "value");
        doThrow(new IllegalArgumentException("Unsupported contact type: invalid"))
                .when(contactCrudService).updateContacts(userId, contacts);
        assertThrows(IllegalArgumentException.class, () -> {
            contactsService.updateContacts(userId, contacts);
        });
    }

    @Test
    void getAllUserContacts_whenUserNotFound_throwsResourceNotFoundException() {
        doThrow(new ResourceNotFoundException("User details not found for id=" + userId))
                .when(contactCrudService).getAllUserContacts(userId);
        assertThrows(ResourceNotFoundException.class, () -> {
            contactsService.getAllUserContacts(userId);
        });
    }

    @Test
    void getContact_whenUserNotFound_throwsResourceNotFoundException() {
        String contactType = "email";
        doThrow(new ResourceNotFoundException("User details not found for id=" + userId))
                .when(contactCrudService).getContact(userId, contactType);
        assertThrows(ResourceNotFoundException.class, () -> {
            contactsService.getContact(userId, contactType);
        });
    }
}