package com.pbl6.cinemate.movie.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbl6.cinemate.movie.dto.request.DirectorCreationRequest;
import com.pbl6.cinemate.movie.dto.request.DirectorUpdateRequest;
import com.pbl6.cinemate.movie.dto.response.DirectorResponse;
import com.pbl6.cinemate.movie.entity.Director;
import com.pbl6.cinemate.movie.repository.DirectorRepository;
import com.pbl6.cinemate.movie.repository.MovieDirectorRepository;
import com.pbl6.cinemate.movie.service.DirectorService;
import com.pbl6.cinemate.shared.exception.NotFoundException;

@Service
public class DirectorServiceImpl implements DirectorService {

    private final DirectorRepository directorRepository;
    private final MovieDirectorRepository movieDirectorRepository;

    public DirectorServiceImpl(DirectorRepository directorRepository, MovieDirectorRepository movieDirectorRepository) {
        this.directorRepository = directorRepository;
        this.movieDirectorRepository = movieDirectorRepository;
    }

    @Override
    public DirectorResponse createDirector(DirectorCreationRequest request) {
        Director director = new Director(
                request.fullname(),
                request.biography(),
                request.avatar(),
                request.dateOfBirth());

        Director savedDirector = directorRepository.save(director);

        return mapToDirectorResponse(savedDirector);
    }

    @Override
    public List<DirectorResponse> getAllDirectors() {
        return directorRepository.findAll().stream()
                .filter(director -> director.getDeletedAt() == null)
                .map(this::mapToDirectorResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DirectorResponse getDirectorById(UUID id) {
        Director director = directorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Director not found with id: " + id));

        if (director.getDeletedAt() != null) {
            throw new NotFoundException("Director not found with id: " + id);
        }

        return mapToDirectorResponse(director);
    }

    @Override
    public DirectorResponse updateDirector(UUID id, DirectorUpdateRequest request) {
        Director director = directorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Director not found with id: " + id));

        if (director.getDeletedAt() != null) {
            throw new NotFoundException("Director not found with id: " + id);
        }

        director.setFullname(request.fullname());
        director.setBiography(request.biography());
        director.setAvatar(request.avatar());
        director.setDateOfBirth(request.dateOfBirth());

        Director updatedDirector = directorRepository.save(director);

        return mapToDirectorResponse(updatedDirector);
    }

    @Override
    @Transactional
    public void deleteDirector(UUID id) {
        if (!directorRepository.existsById(id)) {
            throw new NotFoundException("Director not found with id: " + id);
        }
        // Delete all movie-director relationships first
        movieDirectorRepository.deleteByDirectorId(id);
        movieDirectorRepository.flush();
        // Then delete the director
        directorRepository.deleteById(id);
    }

    private DirectorResponse mapToDirectorResponse(Director director) {
        return new DirectorResponse(
                director.getId(),
                director.getFullname(),
                director.getBiography(),
                director.getAvatar(),
                director.getDateOfBirth(),
                director.getCreatedAt(),
                director.getUpdatedAt());
    }
}
