package com.mulmi.backend.domain.blacklist.service;

import com.mulmi.backend.domain.blacklist.dto.request.RegisterBlacklistRequestDTO;
import com.mulmi.backend.domain.blacklist.dto.response.BlacklistPageResponseDTO;
import com.mulmi.backend.domain.blacklist.dto.response.BlacklistResponseDTO;

public interface BlacklistService {

    BlacklistResponseDTO register(Long administratorId, Long userId, RegisterBlacklistRequestDTO request);

    BlacklistPageResponseDTO getHistory(Long userId, int page, int size);

    BlacklistResponseDTO release(Long administratorId, Long blacklistId, String releaseReason);
}
