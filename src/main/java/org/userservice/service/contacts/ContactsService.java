package org.userservice.service.contacts;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.userservice.dto.contact.ContactInfoResponseDto;
import org.userservice.dto.contact.ContactResponseDto;
import java.util.Map;
import java.util.UUID;

/**
 * Сервис для управления контактами пользователей.
 * <p>
 * Делегирует операции сервису {@link ContactCrudService}, обеспечивая
 * получение, обновление и удаление контактов пользователя.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ContactsService {

    private final ContactCrudService contactCrudService;

    /**
     * Получить все контакты пользователя.
     * @param userId уникальный идентификатор пользователя
     * @return DTO с полным списком контактов пользователя
     */
    public ContactResponseDto getAllUserContacts(UUID userId) {
        return contactCrudService.getAllUserContacts(userId);
    }

    /**
     * Обновить контакты пользователя.
     * @param userId   уникальный идентификатор пользователя
     * @param contacts карта с типами контактов и их значениями для обновления
     * @return DTO с обновлённым списком контактов пользователя
     */
    public ContactResponseDto updateContacts(UUID userId, Map<String, String> contacts) {
        return contactCrudService.updateContacts(userId,contacts);
    }

    /**
     * Удалить все контакты пользователя.
     * @param userId уникальный идентификатор пользователя
     */
    public void deleteAllContacts(UUID userId) {
       contactCrudService.deleteAllContacts(userId);
    }

    /**
     * Получить конкретный контакт пользователя по типу.
     * @param userId      уникальный идентификатор пользователя
     * @param contactType тип контакта (например, email, phone)
     * @return DTO с информацией по конкретному контакту
     */
    public ContactInfoResponseDto getContact(UUID userId, String contactType) {
        return contactCrudService.getContact(userId, contactType);
    }

    /**
     * Удалить конкретный контакт пользователя по типу.
     * @param userId      уникальный идентификатор пользователя
     * @param contactType тип контакта для удаления
     */
    public void deleteContact(UUID userId, String contactType) {
        contactCrudService.deleteContact(userId, contactType);
    }
}