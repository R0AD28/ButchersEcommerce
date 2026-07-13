package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.exception.AccountLockedException;
import com.ecommerce.auth_service.model.User;
import com.ecommerce.auth_service.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private static final long LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;

    public LoginAttemptService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    public void verifyNotLocked(
            User user
    ) {

        Instant lockedUntil =
                user.getLockedUntil();

        if (
                lockedUntil != null &&
                lockedUntil.isAfter(Instant.now())
        ) {
            throw new AccountLockedException("La cuenta está bloqueada temporalmente");
        }
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void registerFailure(
            User user
    ) {

        User managedUser = userRepository
                .findById(user.getId())
                .orElseThrow();

        int newFailedAttempts =
                managedUser.getFailedLoginAttempts() + 1;

        Instant now =
                Instant.now();

        managedUser.setFailedLoginAttempts(
                newFailedAttempts
        );

        managedUser.setLastFailedLoginAt(
                now
        );

        if ( 
            newFailedAttempts >= MAX_FAILED_ATTEMPTS   
        ) {
            managedUser.setLockedUntil(
                    now.plus(
                            LOCK_DURATION_MINUTES,
                            ChronoUnit.MINUTES
                    )
            );
        }

        userRepository.saveAndFlush(
                managedUser
        );
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void registerSuccess(
            User user
    ) {

        User managedUser = userRepository
                .findById(user.getId())
                .orElseThrow();

        managedUser.setFailedLoginAttempts(0);
        managedUser.setLastFailedLoginAt(null);
        managedUser.setLockedUntil(null);

        userRepository.saveAndFlush(
                managedUser
        );
    }
}