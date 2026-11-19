package com.mulmi.backend.domain.blacklist.dto.response;

import com.mulmi.backend.domain.blacklist.entity.Blacklist;
import com.mulmi.backend.domain.blacklist.enums.BlacklistReasonType;

import java.time.LocalDateTime;

public record BlacklistResponseDTO(
        Long blacklistId,
        Long userId,
        Long semesterId,
        String semesterName,
        Long rentalId,
        BlacklistReasonType reasonType,
        String reasonDetail,
        boolean active,
        Long registeredById,
        String registeredByName,
        LocalDateTime createdAt,
        LocalDateTime releasedAt,
        Long releasedById,
        String releasedByName,
        String releaseReason
) {
    public static BlacklistResponseDTO from(Blacklist blacklist) {
        return new BlacklistResponseDTO(
                blacklist.getId(),
                blacklist.getUser().getId(),
                blacklist.getSemester().getId(),
                blacklist.getSemester().getName(),
                blacklist.getRental() == null ? null : blacklist.getRental().getId(),
                blacklist.getReasonType(),
                blacklist.getReasonDetail(),
                blacklist.isActive(),
                blacklist.getRegisteredBy() == null ? null : blacklist.getRegisteredBy().getId(),
                blacklist.getRegisteredBy() == null ? null : blacklist.getRegisteredBy().getName(),
                blacklist.getCreatedAt(),
                blacklist.getReleasedAt(),
                blacklist.getReleasedBy() == null ? null : blacklist.getReleasedBy().getId(),
                blacklist.getReleasedBy() == null ? null : blacklist.getReleasedBy().getName(),
                blacklist.getReleaseReason()
        );
    }
}
