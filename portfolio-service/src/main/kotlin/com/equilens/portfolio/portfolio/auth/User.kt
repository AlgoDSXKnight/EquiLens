package com.equilens.portfolio.auth

import jakarta.persistence.*

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 120)
    var email: String = "",

    @Column(nullable = false, length = 120)
    var name: String = "",

    @Column(name = "password_hash", nullable = false, length = 255)
    var password: String = "",

    @Column(nullable = false, length = 50)
    var role: String = "USER"
)