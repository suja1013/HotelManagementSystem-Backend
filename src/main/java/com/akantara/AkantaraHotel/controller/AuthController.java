package com.akantara.AkantaraHotel.controller;

import com.akantara.AkantaraHotel.dto.LoginRequest;
import com.akantara.AkantaraHotel.dto.Response;
import com.akantara.AkantaraHotel.entity.User;
import com.akantara.AkantaraHotel.service.ServiceInterface.UserServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    // Injects the UserService class
    @Autowired
    private UserServiceInterface userService;

    // Handles user registration by accepting a User object, calling the service, and returning the response
    @PostMapping("/register")
    public ResponseEntity<Response> register(@RequestBody User user) {
        Response response = userService.register(user);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // Handles user login by accepting login credentials, calling the service, and returning the response
    @PostMapping("/login")
    public ResponseEntity<Response> login(@RequestBody LoginRequest loginRequest) {
        Response response = userService.login(loginRequest);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
