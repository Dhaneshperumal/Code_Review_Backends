package com.codeReview.codeController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codeReview.code.User;
import com.codeReview.codeService.GitHubService;
import com.codeReview.codeService.GitLabService;
import com.codeReview.codeService.UserService;

@RestController
@RequestMapping("/api/oauth")
public class OAuthController {

    private final GitHubService gitHubService;
    private final GitLabService gitLabService;
    private final UserService userService;

    @Autowired
    public OAuthController(GitHubService gitHubService, GitLabService gitLabService, UserService userService) {
        this.gitHubService = gitHubService;
        this.gitLabService = gitLabService;
        this.userService = userService;
    }

    // GitHub OAuth Redirect
    @GetMapping("/github/callback")
    public ResponseEntity<Void> githubCallback(@RequestParam("code") String code,
                                               @RequestParam("state") String state, // Optional state parameter
                                               @RequestParam("redirectUri") String redirectUri) {
        gitHubService.exchangeGitHubCodeForToken(code, redirectUri)
                .subscribe(accessToken -> {
                    // Update the user's GitHub access token in the database
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    String email = authentication.getName();
                    User user = userService.findUserByEmail(email);
                    if (user != null) {
                        user.setGithubAccessToken(accessToken);
                        userService.registerUser(user); // Use the overloaded method
                    }
                });
        return ResponseEntity.ok().build();
    }
    
    // GitLab OAuth Redirect
    @GetMapping("/gitlab/callback")
    public ResponseEntity<Void> gitlabCallback(@RequestParam("code") String code,
                                               @RequestParam("state") String state, // Optional state parameter
                                               @RequestParam("redirectUri") String redirectUri) {
        gitLabService.exchangeGitLabCodeForToken(code, redirectUri)
                .subscribe(accessToken -> {
                    // Update the user's GitLab access token in the database
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    String email = authentication.getName();
                    User user = userService.findUserByEmail(email);
                    if (user != null) {
                        user.setGitlabAccessToken(accessToken);
                        userService.registerUser(user); // Use the overloaded method
                    }
                });
        return ResponseEntity.ok().build();
    }
}
