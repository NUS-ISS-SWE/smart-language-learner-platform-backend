package com.nus.iss.config;

import com.nus.iss.entity.AppUser;
import com.nus.iss.enums.Status;
import com.nus.iss.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final AppUserRepository appUserRepository;

    @Autowired
    public AppUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<AppUser> user = appUserRepository.findByUsername(username);
        if (user.isPresent() && user.get().getStatus() == Status.ACTIVE) {
            return User.builder()
                    .username(user.get().getUsername())
                    .password(user.get().getPassword())
                    .roles(user.get().getRole().name())
                    .build();
        } else {
            throw new UsernameNotFoundException("User not found or not active: " + username);
        }
    }
}
