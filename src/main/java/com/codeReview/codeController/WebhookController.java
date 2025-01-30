package com.codeReview.codeController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.codeReview.code.Project;
import com.codeReview.code.Report;
import com.codeReview.code.User;
import com.codeReview.codeService.GitHubService;
import com.codeReview.codeService.GitLabService;
import com.codeReview.codeService.ProjectService;
import com.codeReview.codeService.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final ProjectService projectService;
    private final GitHubService gitHubService;
    private final GitLabService gitLabService;
    private final UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    public WebhookController(ProjectService projectService, GitHubService gitHubService, 
                             GitLabService gitLabService, UserService userService) {
        this.projectService = projectService;
        this.gitHubService = gitHubService;
        this.gitLabService = gitLabService;
        this.userService = userService;
    }

    @PostMapping("/github")
    public ResponseEntity<String> handleGitHubWebhook(@RequestBody String payload) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(payload);

            String repositoryUrl = root.path("repository").path("html_url").asText(); 
            String eventType = root.path("action").asText();
            String branchName = root.path("ref").asText(); 

            String senderLogin = root.path("sender").path("login").asText(); 
            User user = userService.findUserByEmail(senderLogin); 

            String accessToken = user.getGithubAccessToken(); 
            gitHubService.fetchCodeFromGitHub(repositoryUrl, accessToken)
                    .subscribe();

            return ResponseEntity.ok("GitHub webhook received and processed.");
        } catch (Exception e) {
            logger.error("Error processing GitHub webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error processing GitHub webhook.");
        }
    }


    @PostMapping("/gitlab")
    public ResponseEntity<String> handleGitLabWebhook(@RequestBody String payload) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(payload);

            // Extract repository URL and other relevant information
            String repositoryUrl = root.path("project").path("http_url").asText(); 
            String eventType = root.path("object_kind").asText(); 
            String branchName = root.path("ref").asText(); 

            // Extract user ID from GitLab webhook payload
            String userId = root.path("user").path("id").asText();
            User user = userService.findUserById(Long.parseLong(userId)); 
            if (user == null) {
                logger.error("User not found for GitLab userId: {}", userId);
                return ResponseEntity.status(404).body("User not found.");
            }

            // Fetch the code from GitLab
            String accessToken = user.getGitlabAccessToken();
            gitLabService.fetchCodeFromGitLab(repositoryUrl, accessToken, branchName)
                    .subscribe();

            return ResponseEntity.ok("GitLab webhook received and processed.");
        } catch (Exception e) {
            logger.error("Error processing GitLab webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error processing GitLab webhook.");
        }
    }

    private void handleProjectCreationOrUpdate(String repositoryUrl, String code, User user, JsonNode root) {
        Project project = projectService.findProjectByGitRepositoryUrl(repositoryUrl);
        if (project == null) {
            // If project doesn't exist, create a new one
            project = new Project();
            project.setName(root.path("repository").path("name").asText());
            project.setGitRepositoryUrl(repositoryUrl);
            project.setUser(user);
            project = projectService.createProject(project, user.getEmail());
            logger.info("New project created: {}", project.getProjectName());
        } else {
            // If project exists, update it
            project.setCode(code);
            project = projectService.saveProject(project);
            logger.info("Project updated: {}", project.getProjectName());
        }

        // Trigger code analysis
        Report report = projectService.generateReport(project);
        logger.info("Code analysis initiated for project: {}", project.getProjectName());
    }
}
