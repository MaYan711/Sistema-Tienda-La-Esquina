package com.tiendalaesquina.security;

import com.tiendalaesquina.domain.model.RoleName;

public record JwtData(String email, long userId, RoleName role, int tokenVersion) {
}
