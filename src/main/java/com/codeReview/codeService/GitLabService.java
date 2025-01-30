package com.codeReview.codeService;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

@Service
public class GitLabService {

    @Value("${gitlab.clientId}")
    private String clientId;

    @Value("${gitlab.clientSecret}")
    private String clientSecret;

    @Value("${gitlab.apiUrl:https://gitlab.com/api/v4}")
    private String gitlabApiUrl;

    private final WebClient webClient;

    public GitLabService() {
        this.webClient = WebClient.builder()
            .baseUrl("https://gitlab.com")
            .defaultHeader(HttpHeaders.ACCEPT, "application/json")
            .build();
    }

    /**
     * Constructs the GitLab OAuth2 authorization URL.
     * 
     * @param redirectUri The URI to redirect to after authorization.
     * @return The authorization URL.
     */
    public String getGitLabAuthorizationUrl(String redirectUri) {
        return UriComponentsBuilder
            .fromHttpUrl("https://gitlab.com/oauth/authorize")
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("response_type", "code")
            .queryParam("scope", "read_repository")
            .build()
            .toUriString();
    }

    /**
     * Exchanges the GitLab OAuth2 authorization code for an access token.
     * 
     * @param code The authorization code.
     * @param redirectUri The redirect URI used in the authorization flow.
     * @return A Mono emitting the access token.
     */
    public Mono<String> exchangeGitLabCodeForToken(String code, String redirectUri) {
        return webClient
            .post()
            .uri("/oauth/token")
            .bodyValue(Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code,
                "grant_type", "authorization_code",
                "redirect_uri", redirectUri
            ))
            .retrieve()
            .onStatus(
                status -> status.isError(),  // Check for errors
                clientResponse -> {
                    // Handle error responses
                    return clientResponse.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new GitLabServiceException("Failed to exchange code for token: " + body)));
                }
            )
            .bodyToMono(TokenResponse.class)
            .map(TokenResponse::getAccessToken);
    }

    /**
     * Fetches the repository content from GitLab using the provided access token.
     * 
     * @param repositoryUrl The GitLab repository URL.
     * @param accessToken The OAuth2 access token.
     * @param branch The GitLab branch to fetch the repository from.
     * @return A Mono emitting the repository content as a string.
     */
    public Mono<String> fetchCodeFromGitLab(String repositoryUrl, String accessToken, String branch) {
        String projectPath = extractProjectPath(repositoryUrl);
        String apiUrl = String.format("%s/projects/%s/repository/files", gitlabApiUrl, encodeProjectPath(projectPath));

        return webClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path(apiUrl)
                .queryParam("ref", branch)
                .build())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .onStatus(status -> ((HttpStatus) status).series() == HttpStatus.Series.CLIENT_ERROR || ((HttpStatus) status).series() == HttpStatus.Series.SERVER_ERROR, response -> {
                return response.bodyToMono(String.class).flatMap(body -> {
                    return Mono.error(new GitLabServiceException("Error fetching code from GitLab: " + body));
                });
            })
            .bodyToMono(String.class);
    }

    /**
     * Extracts the GitLab project path from the repository URL.
     * 
     * @param repositoryUrl The GitLab repository URL.
     * @return The project path.
     */
    private String extractProjectPath(String repositoryUrl) {
        // Removes the GitLab base URL and .git extension if present
        return repositoryUrl.replace("https://gitlab.com/", "").replace(".git", "");
    }

    /**
     * Encodes the project path to make it URL-safe for API requests.
     * 
     * @param projectPath The project path.
     * @return The URL-encoded project path.
     */
    private String encodeProjectPath(String projectPath) {
        // GitLab API requires project path to be URL-encoded
        return projectPath.replace("/", "%2F");
    }

    /**
     * Inner class to handle the token response from GitLab's OAuth2 endpoint.
     */
    private static class TokenResponse {
        private String access_token;

        public String getAccessToken() {
            return access_token;
        }

        public void setAccessToken(String access_token) {
            this.access_token = access_token;
        }
    }

    /**
     * Custom exception class for handling GitLab-specific errors.
     */
    public static class GitLabServiceException extends RuntimeException {
        public GitLabServiceException(String message) {
            super(message);
        }

        public GitLabServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

	public Mono<String> fetchCodeFromGitLab(String repositoryUrl, String accessToken) {
		// TODO Auto-generated method stub
		return null;
	}
}
