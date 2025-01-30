package com.codeReview.codeController;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codeReview.code.User;
import com.codeReview.codeDto.UserLoginDto;
import com.codeReview.codeDto.UserRegistrationDto;
import com.codeReview.codeService.GitHubService;
import com.codeReview.codeService.GitLabService;
import com.codeReview.codeService.UserService;

import org.springframework.security.authentication.BadCredentialsException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final GitHubService gitHubService;
    private final GitLabService gitLabService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    public UserController(UserService userService,
                         AuthenticationManager authenticationManager,
                         GitHubService gitHubService,
                         GitLabService gitLabService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.gitHubService = gitHubService;
        this.gitLabService = gitLabService;
    }

    // GitHub OAuth Redirect
    @GetMapping("/github/callback")
    public ResponseEntity<Void> githubCallback(@RequestParam("code") String code,
                                               @RequestParam("state") String state,
                                               @RequestParam("redirectUri") String redirectUri) {
        try {
            gitHubService.exchangeGitHubCodeForToken(code, redirectUri)
                    .subscribe(accessToken -> {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        String email = authentication.getName();
                        User user = userService.findUserByEmail(email);
                        if (user != null) {
                            user.setGithubAccessToken(accessToken);
                            userService.registerUser(user); // Register the updated user
                        }
                    });
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("GitHub callback error: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GitLab OAuth Redirect
    @GetMapping("/gitlab/callback")
    public ResponseEntity<Void> gitlabCallback(@RequestParam("code") String code,
                                               @RequestParam("state") String state,
                                               @RequestParam("redirectUri") String redirectUri) {
        try {
            gitLabService.exchangeGitLabCodeForToken(code, redirectUri)
                    .subscribe(accessToken -> {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        String email = authentication.getName();
                        User user = userService.findUserByEmail(email);
                        if (user != null) {
                            user.setGitlabAccessToken(accessToken);
                            userService.registerUser(user); // Register the updated user
                        }
                    });
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("GitLab callback error: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserRegistrationDto userRegistrationDto) {
        try {
            userService.registerUser(userRegistrationDto);
            return new ResponseEntity<>("User registered successfully!", HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("User registration error: ", e);
            return new ResponseEntity<>("User registration failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @CrossOrigin(origins = "http://localhost:5173")
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody UserLoginDto userLoginDto) {
        Map<String, String> response = new HashMap<>();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userLoginDto.getEmail(), userLoginDto.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Ensure compatibility by checking the type
            User authenticatedUser;
            if (authentication.getPrincipal() instanceof User) {
                authenticatedUser = (User) authentication.getPrincipal();
            } else if (authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                authenticatedUser = userService.findUserByEmail(userDetails.getUsername());
            } else {
                throw new RuntimeException("Unexpected principal type: " + authentication.getPrincipal().getClass());
            }

            String jwtToken = userService.generateJwtToken(authenticatedUser);
            response.put("token", jwtToken);
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException ex) {
            logger.warn("Bad credentials: {}", userLoginDto.getEmail());
            response.put("error", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            logger.error("Login error: ", e);
            response.put("error", "An error occurred during login: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
