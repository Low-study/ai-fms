package com.aifms.shared.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.UUID;

/**
 * Base entity with common fields.
 * All modules' entities should reference this pattern.
 * NOT meant to be extended via JPA inheritance — use composition or copy fields.
 */
public abstract class BaseEntity {

    @Id
    private UUID id;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // ── Getters & Setters ──

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
