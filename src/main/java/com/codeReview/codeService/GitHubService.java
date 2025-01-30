package com.codeReview.codeService;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.codeReview.codeService.GitLabService.GitLabServiceException;

import reactor.core.publisher.Mono;

@Service
public class GitHubService {
	
	@Value("${github.secret}")
	private String githubSecret;


    @Value("${github.clientId}")
    private String clientId;

    @Value("${github.clientSecret}")
    private String clientSecret;

    private final WebClient webClient;
    private final UserService userService;

    @Autowired
    public GitHubService(UserService userService) {
        this.userService = userService;
        this.webClient = WebClient.builder()
            .baseUrl("https://api.github.com")
            .defaultHeader(HttpHeaders.ACCEPT, "application/json")
            .build();
    }

    /**
     * Constructs the GitHub OAuth2 authorization URL.
     *
     * @param redirectUri The URI to redirect to after authorization.
     * @return The authorization URL.
     */
    public String getGitHubAuthorizationUrl(String redirectUri) {
        return UriComponentsBuilder
            .fromHttpUrl("https://github.com/login/oauth/authorize")
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("scope", "repo")
            .build()
            .toUriString();
    }

    /**
     * Exchanges the GitHub OAuth2 authorization code for an access token.
     *
     * @param code        The authorization code.
     * @param redirectUri The redirect URI used in the authorization flow.
     * @return A Mono emitting the access token.
     */
    public Mono<String> exchangeGitHubCodeForToken(String code, String redirectUri) {
        return webClient
            .post()
            .uri("/login/oauth/access_token")
            .header(HttpHeaders.ACCEPT, "application/json")
            .bodyValue(Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code,
                "redirect_uri", redirectUri
            ))
            .retrieve()
            .onStatus(status -> ((HttpStatus) status).series() == HttpStatus.Series.CLIENT_ERROR || ((HttpStatus) status).series() == HttpStatus.Series.SERVER_ERROR, response -> {
                return response.bodyToMono(String.class).flatMap(body -> {
                    return Mono.error(new GitHubServiceException("Error fetching code from GitLab: " + body));
                });
            })
            .bodyToMono(TokenResponse.class)
            .map(TokenResponse::getAccessToken);
    }

    /**
     * Fetches the content from a GitHub repository using the provided access token.
     *
     * @param repositoryUrl The GitHub repository URL.
     * @param accessToken   The OAuth2 access token.
     * @return A Mono emitting the repository content as a string.
     */
    public Mono<String> fetchCodeFromGitHub(String repositoryUrl, String accessToken) {
        String apiUrl = repositoryUrl.replace("https://github.com", "/repos") + "/contents";

        return webClient
            .get()
            .uri(apiUrl)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .onStatus(status -> ((HttpStatus) status).series() == HttpStatus.Series.CLIENT_ERROR || ((HttpStatus) status).series() == HttpStatus.Series.SERVER_ERROR, response -> {
                return response.bodyToMono(String.class).flatMap(body -> {
                    return Mono.error(new GitHubServiceException("Error fetching code from GitLab: " + body));
                });
            })
            .bodyToMono(String.class);
    }

    /**
     * Inner class to handle the token response from GitHub's OAuth2 endpoint.
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
     * Custom exception for GitHub-specific errors.
     */
    public static class GitHubServiceException extends RuntimeException {
        public GitHubServiceException(String message) {
            super(message);
        }

        public GitHubServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
