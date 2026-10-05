package com.equilens.backend.auth

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_email",
            columnNames = ["email"]
        )
    ]
)
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 120)
    var name: String = "",

    @Column(nullable = false, unique = true, length = 180)
    var email: String = "",

    @JsonIgnore
    @Column(nullable = false, length = 255)
    var passwordHash: String = "",

    @Column(nullable = false, length = 30)
    var role: String = "USER"
)