package com.akshit.comefort.service;

import com.akshit.comefort.core.Project;
import com.akshit.comefort.db.DatabaseManager;
import com.akshit.comefort.exception.DuplicateEntityException;
import com.akshit.comefort.exception.EntityNotFoundException;
import com.akshit.comefort.repository.ActivityRepository;
import com.akshit.comefort.repository.ProjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProjectService Integration Tests")
class ProjectServiceTest {

    private DatabaseManager dbManager;
    private ProjectService projectService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        dbManager = new DatabaseManager(tempDir);
        dbManager.initialize();

        ProjectRepository projectRepository = new ProjectRepository(dbManager);
        ActivityRepository activityRepository = new ActivityRepository(dbManager);
        ActivityService activityService = new ActivityService(activityRepository);
        projectService = new ProjectService(projectRepository, activityService);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    @DisplayName("Create project and retrieve by name and ID")
    void testCreateAndGet() {
        Project project = projectService.create("HoneyChain", "Decentralized honey tracking", "C:\\HoneyChain");
        assertNotNull(project.getId());
        assertEquals("HoneyChain", project.getName());

        Project byName = projectService.getByName("HoneyChain");
        assertEquals(project.getId(), byName.getId());

        Project byId = projectService.getById(project.getId());
        assertEquals("HoneyChain", byId.getName());
    }

    @Test
    @DisplayName("Duplicate project name throws DuplicateEntityException")
    void testDuplicateProject() {
        projectService.create("ComeFort", "OS for devs", null);
        assertThrows(DuplicateEntityException.class, () ->
                projectService.create("ComeFort", "Duplicate", null));
    }

    @Test
    @DisplayName("Resolve project with case-insensitivity and fragment")
    void testResolveProject() {
        projectService.create("ProjectAlpha", "Alpha project", null);

        Project resolvedLower = projectService.resolve("projectalpha");
        assertEquals("ProjectAlpha", resolvedLower.getName());

        Project resolvedFrag = projectService.resolve("alpha");
        assertEquals("ProjectAlpha", resolvedFrag.getName());
    }

    @Test
    @DisplayName("Non-existent project throws EntityNotFoundException")
    void testNotFound() {
        assertThrows(EntityNotFoundException.class, () -> projectService.getByName("NonExistent"));
    }

    @Test
    @DisplayName("List all projects")
    void testListAll() {
        projectService.create("Proj1", null, null);
        projectService.create("Proj2", null, null);

        List<Project> all = projectService.listAll();
        assertEquals(2, all.size());
    }
}
