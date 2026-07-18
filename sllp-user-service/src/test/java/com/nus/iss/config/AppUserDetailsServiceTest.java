package com.nus.iss.config;

import com.nus.iss.entity.AppUser;
import com.nus.iss.enums.Role;
import com.nus.iss.enums.Status;
import com.nus.iss.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Test
    void loadUserByUsername_whenUserIsActiveReturnsUserDetails() {
        AppUserDetailsService service = new AppUserDetailsService(appUserRepository);
        AppUser user = AppUser.builder()
                .username("ming")
                .password("encoded-password")
                .status(Status.ACTIVE)
                .role(Role.USER_FREE_TRIAL)
                .build();
        when(appUserRepository.findByUsername("ming")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("ming");

        assertEquals("ming", userDetails.getUsername());
        assertEquals("encoded-password", userDetails.getPassword());
        assertEquals(
                List.of("ROLE_USER_FREE_TRIAL"),
                userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
        );
    }

    @Test
    void loadUserByUsername_whenUserIsNotActiveThrowsUsernameNotFoundException() {
        AppUserDetailsService service = new AppUserDetailsService(appUserRepository);
        AppUser user = AppUser.builder()
                .username("ming")
                .password("encoded-password")
                .status(Status.PENDING_EMAIL_VERIFICATION)
                .role(Role.USER_FREE_TRIAL)
                .build();
        when(appUserRepository.findByUsername("ming")).thenReturn(Optional.of(user));

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("ming"));
    }

    @Test
    void loadUserByUsername_whenUserDoesNotExistThrowsUsernameNotFoundException() {
        AppUserDetailsService service = new AppUserDetailsService(appUserRepository);
        when(appUserRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing"));
    }
}
