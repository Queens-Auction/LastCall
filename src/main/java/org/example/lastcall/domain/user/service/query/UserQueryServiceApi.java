package org.example.lastcall.domain.user.service.query;

import org.example.lastcall.domain.user.entity.User;

public interface UserQueryServiceApi {
    User findById(Long id);

    User findReferenceById(Long id);
}
