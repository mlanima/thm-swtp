package de.thm.swtp.api.thesis;

import de.thm.swtp.api.common.TxLogger;
import de.thm.swtp.api.project.ProjectUrlUtils;
import de.thm.swtp.api.tag.entity.TagEntity;
import de.thm.swtp.api.tag.repository.TagRepository;
import de.thm.swtp.api.thesis.domain.Thesis;
import de.thm.swtp.api.thesis.dto.request.CreateThesisRequest;
import de.thm.swtp.api.thesis.dto.request.UpdateThesisRequest;
import de.thm.swtp.api.thesis.dto.response.DeleteThesisResponse;
import de.thm.swtp.api.thesis.exception.ThesisInvalidStudentAssignmentException;
import de.thm.swtp.api.thesis.exception.ThesisInvalidUrlException;
import de.thm.swtp.api.thesis.exception.ThesisNotFoundByIdException;
import de.thm.swtp.api.thesis.exception.ThesisNotFoundException;
import de.thm.swtp.api.thesis.exception.ThesisStudentAlreadyAssignedException;
import de.thm.swtp.api.thesis.exception.ThesisStudentNotFoundException;
import de.thm.swtp.api.thesis.exception.ThesisTitleAlreadyExistsException;
import de.thm.swtp.api.thesis.exception.ThesisUrlAlreadyExistsException;
import de.thm.swtp.api.thesis.exception.ThesisUrlGenerationFailedException;
import de.thm.swtp.api.thesis.mapper.ThesisMapper;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThesisService {

    private final ThesisRepository thesisRepository;
    private final UserProfileRepository userProfileRepository;
    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public Page<Thesis> getAll(String title, Pageable pageable) {
        if (title != null && !title.isBlank()) {
            return thesisRepository.findByTitleContainingIgnoreCase(title, pageable)
                    .map(ThesisMapper::toDomain);
        }
        return thesisRepository.findAll(pageable).map(ThesisMapper::toDomain);
    }

    @Transactional(readOnly = true)
    public List<Thesis> getThesesByUsername(String username) {
        return thesisRepository.findBySupervisorUsername(username)
                .stream()
                .map(ThesisMapper::toDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public Thesis getById(UUID id) {
        return thesisRepository.findById(id)
                .map(ThesisMapper::toDomain)
                .orElseThrow(() -> new ThesisNotFoundByIdException(id));
    }

    @Transactional(readOnly = true)
    public Thesis getByUrl(String thesisUrl) {
        return thesisRepository.findByThesisUrl(thesisUrl)
                .map(ThesisMapper::toDomain)
                .orElseThrow(() -> new ThesisNotFoundException(thesisUrl));
    }

    @Transactional(readOnly = true)
    public boolean thesisUrlExists(String thesisUrl) {
        return thesisRepository.existsByThesisUrl(thesisUrl);
    }

    @Transactional
    public Thesis create(CreateThesisRequest request, UUID supervisorId) {
        if (thesisRepository.existsByTitle(request.title())) {
            throw new ThesisTitleAlreadyExistsException(request.title());
        }

        String thesisUrl;
        if (request.thesisUrl() == null || request.thesisUrl().isBlank()) {
            thesisUrl = resolveUniqueUrl(ProjectUrlUtils.generateProjectUrl(request.title()));
        } else {
            if (!ProjectUrlUtils.isValidUrl(request.thesisUrl())) {
                throw new ThesisInvalidUrlException(request.thesisUrl());
            }
            if (thesisRepository.existsByThesisUrl(request.thesisUrl())) {
                throw new ThesisUrlAlreadyExistsException(request.thesisUrl());
            }
            thesisUrl = request.thesisUrl();
        }

        UserProfile supervisor = userProfileRepository.findById(supervisorId)
                .orElseThrow(() -> new UserProfileNotFoundException(supervisorId.toString()));

        ThesisEntity thesis = ThesisEntity.builder()
                .title(request.title())
                .thesisUrl(thesisUrl)
                .description(request.description())
                .shortDescription(request.shortDescription())
                .supervisor(supervisor)
                .tags(resolveTags(request.tags()))
                .build();

        ThesisEntity saved = thesisRepository.save(thesis);
        TxLogger.afterCommit(log, "Thesis created: thesis={}, supervisor={}", saved.getId(), supervisorId);
        return ThesisMapper.toDomain(saved);
    }

    @Transactional
    public Thesis update(UUID id, UpdateThesisRequest request) {
        ThesisEntity thesis = thesisRepository.findById(id)
                .orElseThrow(() -> new ThesisNotFoundByIdException(id));

        if (request.getTitle() != null && !request.getTitle().equals(thesis.getTitle())
                && thesisRepository.existsByTitleAndIdNot(request.getTitle(), thesis.getId())) {
            throw new ThesisTitleAlreadyExistsException(request.getTitle());
        }

        if (request.getThesisUrl() != null && !request.getThesisUrl().equals(thesis.getThesisUrl())) {
            if (!ProjectUrlUtils.isValidUrl(request.getThesisUrl())) {
                throw new ThesisInvalidUrlException(request.getThesisUrl());
            }
            if (thesisRepository.existsByThesisUrlAndIdNot(request.getThesisUrl(), thesis.getId())) {
                throw new ThesisUrlAlreadyExistsException(request.getThesisUrl());
            }
            thesis.setThesisUrl(request.getThesisUrl());
        }

        if (request.getTitle() != null) {
            thesis.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            thesis.setDescription(request.getDescription());
        }
        if (request.getShortDescription() != null) {
            thesis.setShortDescription(request.getShortDescription());
        }
        if (request.getTags() != null) {
            thesis.setTags(resolveTags(request.getTags()));
        }

        ThesisEntity updated = thesisRepository.save(thesis);
        TxLogger.afterCommit(log, "Thesis updated: thesis={}", id);
        return ThesisMapper.toDomain(updated);
    }

    @Transactional
    public Thesis addStudent(UUID thesisId, UUID studentKeycloakId) {
        ThesisEntity thesis = thesisRepository.findById(thesisId)
                .orElseThrow(() -> new ThesisNotFoundByIdException(thesisId));

        UserProfile student = userProfileRepository.findById(studentKeycloakId)
                .orElseThrow(() -> new UserProfileNotFoundException(studentKeycloakId.toString()));

        if (student.isProfessor()) {
            throw new ThesisInvalidStudentAssignmentException(studentKeycloakId);
        }
        if (studentKeycloakId.equals(thesis.getSupervisor().getKeycloakId())) {
            throw new ThesisInvalidStudentAssignmentException(studentKeycloakId);
        }

        boolean alreadyAssigned = thesis.getStudents().stream()
                .anyMatch(s -> s.getKeycloakId().equals(studentKeycloakId));
        if (alreadyAssigned) {
            throw new ThesisStudentAlreadyAssignedException(studentKeycloakId, thesisId);
        }

        thesis.getStudents().add(student);
        ThesisEntity saved = thesisRepository.save(thesis);
        TxLogger.afterCommit(log, "Thesis student added: thesis={}, student={}", thesisId, studentKeycloakId);
        return ThesisMapper.toDomain(saved);
    }

    @Transactional
    public Thesis removeStudent(UUID thesisId, UUID studentKeycloakId) {
        ThesisEntity thesis = thesisRepository.findById(thesisId)
                .orElseThrow(() -> new ThesisNotFoundByIdException(thesisId));

        UserProfile student = userProfileRepository.findById(studentKeycloakId)
                .orElseThrow(() -> new UserProfileNotFoundException(studentKeycloakId.toString()));

        boolean assigned = thesis.getStudents().stream()
                .anyMatch(s -> s.getKeycloakId().equals(studentKeycloakId));
        if (!assigned) {
            throw new ThesisStudentNotFoundException(studentKeycloakId, thesisId);
        }

        thesis.getStudents().remove(student);
        ThesisEntity saved = thesisRepository.save(thesis);
        TxLogger.afterCommit(log, "Thesis student removed: thesis={}, student={}", thesisId, studentKeycloakId);
        return ThesisMapper.toDomain(saved);
    }

    @Transactional
    public DeleteThesisResponse delete(UUID id) {
        ThesisEntity thesis = thesisRepository.findById(id)
                .orElseThrow(() -> new ThesisNotFoundByIdException(id));
        thesisRepository.delete(thesis);
        TxLogger.afterCommit(log, "Thesis deleted: thesis={}", id);
        return DeleteThesisResponse.builder()
                .thesisId(id)
                .message("Abschlussarbeit erfolgreich gelöscht.")
                .build();
    }

    private String resolveUniqueUrl(String baseSlug) {
        if (baseSlug == null || baseSlug.isBlank()) {
            baseSlug = "abschlussarbeit";
        }
        if (baseSlug.length() < 3) {
            baseSlug = baseSlug + "-arbeit";
        }

        if (!thesisRepository.existsByThesisUrl(baseSlug)) {
            return baseSlug;
        }

        int counter = 1;
        String candidate;
        do {
            String suffix = "-" + counter;
            int baseLength = Math.min(baseSlug.length(), 30 - suffix.length());
            candidate = baseSlug.substring(0, baseLength) + suffix;
            counter++;
        } while (thesisRepository.existsByThesisUrl(candidate) && counter <= 99);

        if (thesisRepository.existsByThesisUrl(candidate)) {
            throw new ThesisUrlGenerationFailedException(baseSlug);
        }

        return candidate;
    }

    private Set<TagEntity> resolveTags(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }
        Set<TagEntity> tags = new HashSet<>();
        for (String name : tagNames) {
            String cleaned = name.trim();
            TagEntity tag = tagRepository.findByNameIgnoreCase(cleaned)
                    .orElseGet(() -> tagRepository.save(new TagEntity(cleaned)));
            tags.add(tag);
        }
        return tags;
    }
}
