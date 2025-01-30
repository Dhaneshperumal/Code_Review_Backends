package com.codeReview.codeService;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.codeReview.code.Project;
import com.codeReview.code.Report;
import com.codeReview.code.User;
import com.codeReview.codeRepository.ProjectRepository;
import com.codeReview.codeRepository.ReportRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);

    @Value("${github.secret}")
    private String githubSecret;

    private final ProjectRepository projectRepository;
    private final ReportRepository reportRepository;
    private final UserService userService;
    private final GitHubService gitHubService;
    private final GitLabService gitLabService;

    @Autowired
    public ProjectService(ProjectRepository projectRepository,
                          ReportRepository reportRepository,
                          UserService userService,
                          GitHubService gitHubService,
                          GitLabService gitLabService) {
        this.projectRepository = projectRepository;
        this.reportRepository = reportRepository;
        this.userService = userService;
        this.gitHubService = gitHubService;
        this.gitLabService = gitLabService;
    }

    public void processGitHubWebhook(String payload) {
        try {
            String repositoryUrl = extractRepositoryUrl(payload);
            Project project = findProjectByGitRepositoryUrl(repositoryUrl);

            if (project == null) {
                throw new IllegalArgumentException("No project found for repository URL: " + repositoryUrl);
            }

            fetchCodeFromGit(project);
            generateReport(project);
        } catch (Exception e) {
            logger.error("Error processing GitHub webhook: {}", e.getMessage(), e);
            throw new IllegalStateException("Webhook processing failed", e);
        }
    }

    public boolean validateGitHubSignature(String payload, String signature) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(githubSecret.getBytes(), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hmacBytes = mac.doFinal(payload.getBytes());
            String expectedSignature = "sha256=" + Hex.encodeHexString(hmacBytes);

            return MessageDigest.isEqual(expectedSignature.getBytes(), signature.getBytes());
        } catch (Exception e) {
            throw new IllegalStateException("Error validating GitHub signature", e);
        }
    }

    private String extractRepositoryUrl(String payload) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(payload);

            JsonNode repositoryNode = rootNode.path("repository");
            if (repositoryNode.isMissingNode()) {
                throw new IllegalArgumentException("Payload does not contain repository information");
            }

            JsonNode urlNode = repositoryNode.path("html_url");
            if (urlNode.isMissingNode() || urlNode.asText().isEmpty()) {
                throw new IllegalArgumentException("Repository URL is missing in the payload");
            }

            return urlNode.asText();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to extract repository URL from payload", e);
        }
    }

    public Report generateReport(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }

        Report report = new Report();
        report.setProject(project);
        report.setGenerationDate(LocalDateTime.now());
        report.setStatus("Pending");

        try {
            String analysisResult = performCodeAnalysis(project);
            report.setAnalysisResult(analysisResult);
            report.setStatus("Completed");
        } catch (Exception e) {
            report.setStatus("Failed");
            report.setAnalysisResult("Error during analysis: " + e.getMessage());
        }

        return reportRepository.save(report);
    }

    private String performCodeAnalysis(Project project) {
        // Implement the actual SonarQube analysis integration
        return "Sample Analysis Result from SonarQube";
    }

    public Project createProject(Project project, String email) {
        if (project == null || email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Project or email cannot be null or empty");
        }

        User user = userService.findUserByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("No user found with the provided email: " + email);
        }

        // Link the project with the user and set upload date
        project.setUser(user);
        project.setUploadDate(LocalDateTime.now());

        // Save the project to the repository
        return projectRepository.save(project);
    }

    public void fetchCodeFromGit(Project project) {
        if (project == null || project.getGitRepositoryUrl() == null || project.getGitRepositoryUrl().isEmpty()) {
            throw new IllegalArgumentException("Project or repository URL cannot be null or empty");
        }

        try {
            String repositoryUrl = project.getGitRepositoryUrl();
            String accessToken = retrieveAccessToken(project);
            logger.info("Received access token: {}", accessToken);

            if (repositoryUrl.startsWith("https://github.com")) {
                fetchCodeFromGitHub(repositoryUrl, accessToken, project);
            } else if (repositoryUrl.startsWith("https://gitlab.com")) {
                fetchCodeFromGitLab(repositoryUrl, accessToken, project);
            } else {
                throw new UnsupportedOperationException("Unsupported repository provider: " + repositoryUrl);
            }

            project.setUploadDate(LocalDateTime.now());
            projectRepository.save(project);
        } catch (Exception e) {
            logger.error("Error fetching code from repository: {}", e.getMessage(), e);
            throw new IllegalStateException("Error fetching code from repository: " + e.getMessage(), e);
        }
    }

    private void fetchCodeFromGitHub(String repositoryUrl, String accessToken, Project project) {
        gitHubService.fetchCodeFromGitHub(repositoryUrl, accessToken)
            .subscribe(
                code -> {
                    project.setCode(code);
                    logger.info("Code fetched successfully from GitHub");
                },
                throwable -> {
                    logger.error("Error fetching code from GitHub: {}", throwable.getMessage(), throwable);
                    throw new IllegalStateException("GitHub fetch failed", throwable);
                }
            );
    }

    private void fetchCodeFromGitLab(String repositoryUrl, String accessToken, Project project) {
        gitLabService.fetchCodeFromGitLab(repositoryUrl, accessToken)
            .subscribe(
                code -> {
                    project.setCode(code);
                    logger.info("Code fetched successfully from GitLab");
                },
                throwable -> {
                    logger.error("Error fetching code from GitLab: {}", throwable.getMessage(), throwable);
                    throw new IllegalStateException("GitLab fetch failed", throwable);
                }
            );
    }

    private String retrieveAccessToken(Project project) {
        if (project == null || project.getUser() == null || project.getUser().getEmail() == null) {
            throw new IllegalArgumentException("Project user or email is not set");
        }

        String email = project.getUser().getEmail();
        User user = userService.findUserByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("No user found with the provided email: " + email);
        }

        if (project.getGitRepositoryUrl().startsWith("https://github.com")) {
            return user.getGithubAccessToken();
        } else if (project.getGitRepositoryUrl().startsWith("https://gitlab.com")) {
            return user.getGitlabAccessToken();
        }

        throw new UnsupportedOperationException("Unsupported repository provider");
    }

    public Project findProjectByGitRepositoryUrl(String gitRepositoryUrl) {
        Optional<Project> project = projectRepository.findByGitRepositoryUrl(gitRepositoryUrl);
        return project.orElse(null);
    }

    public Project saveProject(Project project) {
        return projectRepository.save(project);
    }
}