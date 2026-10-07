package com.akshit.comefort.service;

import com.akshit.comefort.core.Project;
import com.akshit.comefort.core.ProjectSummary;
import com.akshit.comefort.core.enums.ActionType;
import com.akshit.comefort.core.enums.EntityType;
import com.akshit.comefort.exception.AmbiguousEntityException;
import com.akshit.comefort.exception.DuplicateEntityException;
import com.akshit.comefort.exception.EntityNotFoundException;
import com.akshit.comefort.repository.ProjectRepository;
import com.akshit.comefort.util.IdGenerator;

import java.util.List;
import java.util.Map;

/**
 * Business logic for managing projects.
 */
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ActivityService activityService;

    public ProjectService(ProjectRepository projectRepository, ActivityService activityService) {
        this.projectRepository = projectRepository;
        this.activityService = activityService;
    }

    /**
     * Creates a new project.
     *
     * @throws DuplicateEntityException if a project with the same name already exists
     */
    public Project create(String name, String description, String path) {
        // Check for duplicates
        if (projectRepository.findByName(name).isPresent()) {
            throw new DuplicateEntityException("Project", name);
        }

        Project project = Project.builder(IdGenerator.generate(), name)
                .description(description)
                .path(path)
                .build();

        projectRepository.save(project);

        activityService.log(EntityType.PROJECT, project.getId(),
                ActionType.CREATED, "Created project: " + name);

        return project;
    }

    /**
     * Returns all projects.
     */
    public List<Project> listAll() {
        return projectRepository.findAll();
    }

    /**
     * Returns project summaries with pre-aggregated task and note counts.
     * Prevents N+1 queries.
     */
    public List<ProjectSummary> getProjectSummaries() {
        return projectRepository.findProjectSummaries();
    }

    /**
     * Returns a map of project ID to project name in a single database query.
     */
    public Map<String, String> getProjectNameMap() {
        return projectRepository.getProjectNameMap();
    }

    /**
     * Finds a project by exact name (case-insensitive).
     */
    public Project getByName(String name) {
        return projectRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Project", name));
    }

    /**
     * Finds a project by ID.
     */
    public Project getById(String id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project", id));
    }

    /**
     * Resolves a project by name or name fragment.
     * First tries exact match, then fragment search.
     * If fragment matches multiple, returns the first match.
     */
    public Project resolve(String nameOrFragment) {
        // Try exact match first
        var exact = projectRepository.findByName(nameOrFragment);
        if (exact.isPresent()) return exact.get();

        // Try fragment search
        var matches = projectRepository.findByNameFragment(nameOrFragment);
        if (matches.isEmpty()) {
            throw new EntityNotFoundException("Project", nameOrFragment);
        }
        if (matches.size() == 1) {
            return matches.getFirst();
        }

        // Multiple matches
        List<String> candidates = matches.stream()
                .map(p -> String.format("%s (%s)", p.getName(), p.getId().substring(0, 8)))
                .toList();
        throw new AmbiguousEntityException("Project", nameOrFragment, candidates);
    }

    /**
     * Updates project details.
     */
    public Project update(String id, String name, String description, String path) {
        Project project = getById(id);

        if (name != null && !name.isBlank()) {
            // Check uniqueness if name is changing
            if (!project.getName().equalsIgnoreCase(name)) {
                if (projectRepository.findByName(name).isPresent()) {
                    throw new DuplicateEntityException("Project", name);
                }
            }
            project.setName(name);
        }
        if (description != null) project.setDescription(description);
        if (path != null) project.setPath(path);

        projectRepository.update(project);

        activityService.log(EntityType.PROJECT, project.getId(),
                ActionType.UPDATED, "Updated project: " + project.getName());

        return project;
    }

    /**
     * Deletes a project.
     */
    public void delete(String id) {
        Project project = getById(id);
        projectRepository.delete(id);

        activityService.log(EntityType.PROJECT, id,
                ActionType.DELETED, "Deleted project: " + project.getName());
    }

    /**
     * Returns the total number of projects.
     */
    public int count() {
        return projectRepository.count();
    }
}
