package com.minishopify.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

public class ImageFileValidator implements ConstraintValidator<ValidImage, MultipartFile> {

    private static final long MAX_SIZE_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES =
        Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        // Image is optional
        if (file == null || file.isEmpty()) {
            return true;
        }

        String contentType = file.getContentType();
        return file.getSize() <= MAX_SIZE_BYTES
            && contentType != null
            && ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
    }
}
