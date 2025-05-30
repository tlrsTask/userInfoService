package org.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.userservice.dto.contact.ContactInfoResponseDto;
import org.userservice.dto.contact.ContactResponseDto;
import org.userservice.service.contacts.ContactsService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactsController {

    private final ContactsService contactsService;

    @GetMapping("/{userId}/all")
    public ResponseEntity<ContactResponseDto> getAllUserContacts(@PathVariable UUID userId) {
        return ResponseEntity.ok(contactsService.getAllUserContacts(userId));
    }

    @GetMapping("/{userId}/{contactType}")
    public ResponseEntity<ContactInfoResponseDto> getContact(
            @PathVariable UUID userId,
            @PathVariable String contactType) {

        return ResponseEntity.ok(contactsService.getContact(userId, contactType));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ContactResponseDto> updateContacts(
            @PathVariable UUID userId,
            @Valid @RequestBody Map<String, String> contacts) {
        return ResponseEntity.ok(contactsService.updateContacts(userId, contacts));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteAllContacts(@PathVariable UUID userId) {
        contactsService.deleteAllContacts(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}/{contactType}")
    public ResponseEntity<Void> deleteContact(
            @PathVariable UUID userId,
            @PathVariable String contactType) {

        contactsService.deleteContact(userId, contactType);
        return ResponseEntity.noContent().build();
    }
}
