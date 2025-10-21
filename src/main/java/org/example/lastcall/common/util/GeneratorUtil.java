package org.example.lastcall.common.util;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class GeneratorUtil {
    public UUID generatePublicId()
    {
        return UUID.randomUUID();
    }
}
