package com.codeReview.codeController;

import com.codeReview.codeService.GitHubService;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GitHubController {

    private final GitHubService gitHubService;

    @Autowired
    public GitHubController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    // Endpoint to get the GitHub authorization URL
    @GetMapping("/github/auth")
    public ResponseEntity<String> getGitHubAuthUrl(@RequestParam("redirectUri") String redirectUri) {
        String authUrl = gitHubService.getGitHubAuthorizationUrl(redirectUri);
        return ResponseEntity.ok(authUrl);
    }

    // Endpoint to exchange the GitHub code for an access token
    @GetMapping("/github/token")
    public Mono<Object> exchangeCodeForToken(
            @RequestParam("code") String code,
            @RequestParam("redirectUri") String redirectUri
    ) {
        // Using Mono to handle the asynchronous token exchange
        return gitHubService.exchangeGitHubCodeForToken(code, redirectUri)
                .map(accessToken -> {
                    System.out.println("Access token received: " + accessToken);
                    // ... (You might need to call a UserService method to update the user with the token)
                    return "Token exchange initiated."; 
                })
                .onErrorReturn("Error during token exchange.") // Handle errors
                .map(ResponseEntity::ok); // Wrap the result in ResponseEntity
    }

    // Endpoint to fetch code from the GitHub repository
    @GetMapping("/github/code")
    public Mono<Object> fetchCodeFromRepository(
            @RequestParam("repositoryUrl") String repositoryUrl,
            @RequestParam("accessToken") String accessToken
    ) {
        // Using Mono to handle the asynchronous code fetching
        return gitHubService.fetchCodeFromGitHub(repositoryUrl, accessToken)
                .map(code -> {
                    System.out.println("Code fetched: " + code);
                    // ... (You might need to call a ProjectService method to update the project with the code)
                    return "Code fetching initiated."; 
                })
                .onErrorReturn("Error during code fetching.") // Handle errors
                .map(ResponseEntity::ok); // Wrap the result in ResponseEntity
    }
}