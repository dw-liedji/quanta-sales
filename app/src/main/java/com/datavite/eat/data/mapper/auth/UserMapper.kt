package com.datavite.eat.data.mapper.auth

import com.datavite.eat.domain.model.auth.DomainUser
import com.datavite.eat.data.remote.model.auth.AuthUser

class UserMapper {

    // Remote to Domain
    fun mapRemoteToDomain(remote: AuthUser): DomainUser {
        return DomainUser(
            id = remote.id,
            email = remote.email,
            username = remote.username,
        )
    }

    // Domain to Remote
    fun mapDomainToRemote(domain: DomainUser): AuthUser {
        return AuthUser(
            id = domain.id,
            email = domain.email,
            username = domain.username
        )
    }

}