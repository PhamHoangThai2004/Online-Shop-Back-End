package com.group_2.onlineshop.controller;

import com.group_2.onlineshop.entity.User;
import com.group_2.onlineshop.security.JwtUtil;
import com.group_2.onlineshop.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        User user = userService.findByUsername(loginRequest.getUsername());
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        String id = user.getId().toString();
        String role = user.getRole();

        String token = jwtUtil.generateToken(id, loginRequest.getUsername(), role);
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        if (userService.findByUsername(registerRequest.getUsername()) != null) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword());
        user.setEmail(registerRequest.getEmail());
        user.setRole(registerRequest.getRole());
        user.setFullName(registerRequest.getFullName());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setAddress(registerRequest.getAddress());

        User savedUser = userService.registerUser(user);
        return ResponseEntity.ok("User registered successfully with ID: " + savedUser.getId());
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestHeader("Authorization") String authorizationHeader,
                                            @RequestBody ChangePasswordRequest changePasswordRequest) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Invalid Authorization header");
        }

        String token = authorizationHeader.substring(7);
        String username = jwtUtil.extractUsername(token);

        if (!jwtUtil.validateToken(token, username)) {
            return ResponseEntity.status(401).body("Invalid or expired token");
        }

        User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        user.setPassword(changePasswordRequest.getNewPassword());
        userService.updatePassword(user);
        return ResponseEntity.ok("Password changed successfully");
    }

    // Thay đổi thông tin user
    @PutMapping("/update")
    public ResponseEntity<?> updateUserInfo(@RequestHeader("Authorization") String authorizationHeader,
                                            @RequestBody UpdateUserRequest updateUserRequest) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Invalid Authorization header");
        }

        String token = authorizationHeader.substring(7);
        String username = jwtUtil.extractUsername(token);

        if (!jwtUtil.validateToken(token, username)) {
            return ResponseEntity.status(401).body("Invalid or expired token");
        }

        User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        // Update only allowed fields
        if (updateUserRequest.getEmail() != null) {
            user.setEmail(updateUserRequest.getEmail());
        }
        if (updateUserRequest.getFullName() != null) {
            user.setFullName(updateUserRequest.getFullName());
        }
        if (updateUserRequest.getBirthday() != null) {
            user.setBirthday(updateUserRequest.getBirthday());
        }
        if (updateUserRequest.getGender() != null) {
            user.setGender(updateUserRequest.getGender());
        }
        if (updateUserRequest.getPhoneNumber() != null) {
            user.setPhoneNumber(updateUserRequest.getPhoneNumber());
        }
        if (updateUserRequest.getAddress() != null) {
            user.setAddress(updateUserRequest.getAddress());
        }

        userService.updateUser(user);
        return ResponseEntity.ok("User information updated successfully");
    }

    @GetMapping("/user/info")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Invalid Authorization header");
        }

        String token = authorizationHeader.substring(7);
        String username = jwtUtil.extractUsername(token);

        if (!jwtUtil.validateToken(token, username)) {
            return ResponseEntity.status(401).body("Invalid or expired token");
        }

        User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        UserInfoResponse userInfo = new UserInfoResponse();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setRole(user.getRole());
        userInfo.setFullName(user.getFullName());
        userInfo.setBirthday(user.getBirthday());
        userInfo.setGender(user.getGender());
        userInfo.setPhoneNumber(user.getPhoneNumber());
        userInfo.setAddress(user.getAddress());

        return ResponseEntity.ok(userInfo);
    }


    // Get All account
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Invalid Authorization header");
        }

        String token = authorizationHeader.substring(7);
        String username = jwtUtil.extractUsername(token);

        if (!jwtUtil.validateToken(token, username)) {
            return ResponseEntity.status(401).body("Invalid or expired token");
        }

        User currentUser = userService.findByUsername(username);
        if (currentUser == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        List<User> users = userService.findAllUsers();
        List<UserInfoResponse> userInfoList = users.stream()
                .map(user -> {
                    UserInfoResponse userInfo = new UserInfoResponse();
                    userInfo.setId(user.getId());
                    userInfo.setUsername(user.getUsername());
                    userInfo.setEmail(user.getEmail());
                    userInfo.setRole(user.getRole());
                    userInfo.setFullName(user.getFullName());
                    userInfo.setPhoneNumber(user.getPhoneNumber());
                    userInfo.setAddress(user.getAddress());
                    return userInfo;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(userInfoList);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getAllUsers(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        UserInfoResponse userInfo = new UserInfoResponse();
        userInfo.setId(user.getId());
        userInfo.setEmail(user.getEmail());
        userInfo.setRole(user.getRole());
        userInfo.setFullName(user.getFullName());
        userInfo.setBirthday(user.getBirthday());
        userInfo.setGender(user.getGender());
        userInfo.setPhoneNumber(user.getPhoneNumber());
        userInfo.setAddress(user.getAddress());

        return ResponseEntity.ok(userInfo);
    }
}

class LoginRequest {
    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

class LoginResponse {
    private String token;

    public LoginResponse(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String role;
    private String fullName;
    private String phoneNumber;
    private String address;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}

class ChangePasswordRequest {
    private String newPassword;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}

class UpdateUserRequest {
    private String email;
    private String fullName;
    private String birthday;
    private String gender;
    private String phoneNumber;
    private String address;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}

class UserInfoResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String fullName;
    private String birthday;
    private String gender;
    private String phoneNumber;
    private String address;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}