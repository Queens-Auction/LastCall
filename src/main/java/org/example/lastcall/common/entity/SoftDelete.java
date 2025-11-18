package org.example.lastcall.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
public class SoftDelete {
    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    public void softDelete() {
        this.deleted = true;
    }
}