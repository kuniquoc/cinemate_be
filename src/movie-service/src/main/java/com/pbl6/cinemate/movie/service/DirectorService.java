package com.pbl6.cinemate.movie.service;

import java.util.List;
import java.util.UUID;

import com.pbl6.cinemate.movie.dto.request.DirectorCreationRequest;
import com.pbl6.cinemate.movie.dto.request.DirectorUpdateRequest;
import com.pbl6.cinemate.movie.dto.response.DirectorResponse;

public interface DirectorService {
    DirectorResponse createDirector(DirectorCreationRequest request);

    List<DirectorResponse> getAllDirectors();

    DirectorResponse getDirectorById(UUID id);

    DirectorResponse updateDirector(UUID id, DirectorUpdateRequest request);

    void deleteDirector(UUID id);
}
