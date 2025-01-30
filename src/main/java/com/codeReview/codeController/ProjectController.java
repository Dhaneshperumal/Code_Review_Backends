package com.codeReview.codeController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.codeReview.code.Project;
import com.codeReview.codeService.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "http://localhost:5173")
public class ProjectController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
    private final ProjectService projectService;

    @Autowired
    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProject(
            @RequestParam("projectName") String projectName,
            @RequestParam(value = "projectFile", required = false) MultipartFile projectFile,
            @RequestParam(value = "gitRepositoryUrl", required = false) String gitRepositoryUrl,
            @RequestParam("email") String email) { // Ensure email is required
        try {
            if (projectName == null || projectName.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Project name is required.");
            }

            Project project = new Project();
            project.setName(projectName);

            if (projectFile != null && !projectFile.isEmpty()) {
                if (!projectFile.getContentType().equals(MediaType.TEXT_PLAIN_VALUE)) {
                    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                            .body("Only text files are supported.");
                }
                project.setCode(new String(projectFile.getBytes(), StandardCharsets.UTF_8));
            }

            if (gitRepositoryUrl != null && !gitRepositoryUrl.trim().isEmpty()) {
                project.setGitRepositoryUrl(gitRepositoryUrl);
            }

            Project createdProject = projectService.createProject(project, email);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProject);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating project: " + e.getMessage());
        }
    }   
    
    @PostMapping("/webhooks/github")
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestBody String payload, @RequestHeader("X-Hub-Signature-256") String signature) {
        try {
            boolean isValid = projectService.validateGitHubSignature(payload, signature);
            if (!isValid) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Invalid GitHub signature");
            }

            projectService.processGitHubWebhook(payload);
            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            logger.error("Failed to process GitHub webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process webhook: " + e.getMessage());
        }
    }
}
    // Uncomment the following method if you want to enable the commented-out functionality
    /*
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // @PreAuthorize("isAuthenticated()") // Commenting out authentication requirement
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Project created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "415", description = "Unsupported Media Type"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<?> createProject(
            @RequestParam("projectName") String projectName,
            @RequestParam(value = "projectFile", required = false) MultipartFile projectFile,
            @RequestParam(value = "gitRepositoryUrl", required = false) String gitRepositoryUrl) {

        // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // String email = authentication.getName(); // Declare the email variable for authenticated user
        try {
            // Retrieve the current authenticated user (Commented out)
            // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            // email = authentication.getName(); // Assign the email variable here

            // Validate project name
            if (projectName == null || projectName.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .header("Error-Message", "Project name must not be empty")
                        .body(null);
            }

            // Create and populate project
            Project project = new Project();
            project.setName(projectName);

            // Handle file upload if present and validate its type
            if (projectFile != null && !projectFile.isEmpty()) {
                if (projectFile.getContentType().equals(MediaType.TEXT_PLAIN_VALUE)) {
                    project.setCode(new String(projectFile.getBytes(), StandardCharsets.UTF_8));
                } else {
                    logger.warn("Unsupported file type: {}", projectFile.getContentType());
                    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                            .header("Error-Message", "Only text files are supported")
                            .body(null);
                }
            }

            // Handle git repository if present
            if (gitRepositoryUrl != null && !gitRepositoryUrl.trim().isEmpty()) {
                project.setGitRepositoryUrl(gitRepositoryUrl);
                projectService.fetchCodeFromGit(project);
            }

            // Create project (Passing null for email since authentication is disabled)
            Project createdProject = projectService.createProject(project, null);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProject);

        } catch (IOException e) {
            logger.error("File processing error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Error-Message", "Failed to process project file: " + e.getMessage())
                    .body(null);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("Error-Message", "Invalid input: " + e.getMessage())
                    .body(null);
        } catch (Exception e) {
            logger.error("Error creating project: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Error-Message", "Error creating project: " + e.getMessage())
                    .body(null);
        }
    }
    */

//    @PostMapping("/webhooks/github")
//    public ResponseEntity<String> handleGitHubWebhook(
//            @RequestBody String payload, @RequestHeader("X-Hub-Signature-256") String signature) {
//        try {
//            // Validate GitHub webhook signature
//            boolean isValid = projectService.validateGitHubSignature(payload, signature);
//            if (!isValid) {
//                return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                        .body("Invalid GitHub signature");
//            }
//
//            // Process GitHub webhook
//            projectService.processGitHubWebhook(payload);
//            return ResponseEntity.ok("Webhook processed successfully");
//
//        } catch (Exception e) {
//            logger.error("Failed to process GitHub webhook", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Failed to process webhook: " + e.getMessage());
//        }
//    }
//}