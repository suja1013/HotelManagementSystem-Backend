package com.akantara.AkantaraHotel.service.implementation;


import com.akantara.AkantaraHotel.dto.LoginRequest;
import com.akantara.AkantaraHotel.dto.Response;
import com.akantara.AkantaraHotel.dto.UserDTO;
import com.akantara.AkantaraHotel.entity.User;
import com.akantara.AkantaraHotel.exception.CustomException;
import com.akantara.AkantaraHotel.repository.UserRepository;
import com.akantara.AkantaraHotel.service.ServiceInterface.UserServiceInterface;
import com.akantara.AkantaraHotel.utils.JWTUtils;
import com.akantara.AkantaraHotel.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService implements UserServiceInterface {

    // Injects User Repository to access user data in the database
    @Autowired
    private UserRepository userRepository;

    // Used to encode passwords securely
    @Autowired
    private PasswordEncoder passwordEncoder;

    // Utility to generate and validate JWT tokens
    @Autowired
    private JWTUtils jwtUtils;

    // Handles authentication logic
    @Autowired
    private AuthenticationManager authenticationManager;

    // Registers a new user, encodes password, sets default role if missing, and returns user details
    @Override
    public Response register(User user) {
        Response response = new Response();
        try {
            if (user.getRole() == null || user.getRole().isBlank()) {
                user.setRole("USER");
            }
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                throw new CustomException("User Already Exists");
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            User savedUser = userRepository.save(user);
            UserDTO userDTO = Utils.mapUserEntityToUserDTO(savedUser);
            response.setStatusCode(200);
            response.setUser(userDTO);
        } catch (CustomException e) {
            response.setStatusCode(400);
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            response.setStatusCode(500);
            response.setMessage("Error Occurred: " + e.getMessage());

        }
        return response;
    }

    // Authenticates user, generates JWT token on successful login, and returns it
    @Override
    public Response login(LoginRequest loginRequest) {

        Response response = new Response();

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
            var user = userRepository.findByEmail(loginRequest.getEmail()).orElseThrow(() -> new CustomException("User Not Found"));

            var token = jwtUtils.generateToken(user);
            response.setStatusCode(200);
            response.setToken(token);
            response.setRole(user.getRole());
            response.setExpirationTime("7 Days");
            response.setMessage("successful");

        } catch (CustomException e) {
            response.setStatusCode(404);
            response.setMessage(e.getMessage());

        } catch (Exception e) {

            response.setStatusCode(500);
            response.setMessage("Error Occurred: " + e.getMessage());
        }
        return response;
    }

    // Retrieves a specific user’s booking history including booked rooms
    @Override
    public Response getUserBookingHistory(String userId) {

        Response response = new Response();


        try {
            User user = userRepository.findById(Long.valueOf(userId)).orElseThrow(() -> new CustomException("User Not Found"));
            UserDTO userDTO = Utils.mapUserEntityToUserDTOPlusUserBookingsAndRoom(user);
            response.setStatusCode(200);
            response.setMessage("successful");
            response.setUser(userDTO);

        } catch (CustomException e) {
            response.setStatusCode(404);
            response.setMessage(e.getMessage());

        } catch (Exception e) {

            response.setStatusCode(500);
            response.setMessage("Error Occurred: " + e.getMessage());
        }
        return response;
    }


    // Retrieves currently logged-in user info using email
    @Override
    public Response getMyInfo(String email) {

        Response response = new Response();

        try {
            User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException("User Not Found"));
            UserDTO userDTO = Utils.mapUserEntityToUserDTO(user);
            response.setStatusCode(200);
            response.setMessage("successful");
            response.setUser(userDTO);

        } catch (CustomException e) {
            response.setStatusCode(404);
            response.setMessage(e.getMessage());

        } catch (Exception e) {

            response.setStatusCode(500);
            response.setMessage("Error Occurred: " + e.getMessage());
        }
        return response;
    }
}