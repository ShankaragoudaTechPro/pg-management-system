package com.pgmanager.pgmanagement.security;

import com.pgmanager.pgmanagement.entity.Owner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Owner owner;

    public CustomUserDetails(Owner owner) {
        this.owner = owner;
    }

    public Owner getOwner() {
        return owner;
    }

    // =========================================================
    // AUTHORITIES / ROLE
    // =========================================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return List.of(
                new SimpleGrantedAuthority(
                        "ROLE_" + owner.getRole().name()
                )
        );
    }

    // =========================================================
    // PASSWORD
    // =========================================================

    @Override
    public String getPassword() {
        return owner.getPassword();
    }

    // =========================================================
    // USERNAME
    // =========================================================

    @Override
    public String getUsername() {
        return owner.getEmail();
    }

    // =========================================================
    // ACCOUNT STATUS
    // =========================================================

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return owner.getStatus().name().equals("ACTIVE");
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return owner.getStatus().name().equals("ACTIVE");
    }
}