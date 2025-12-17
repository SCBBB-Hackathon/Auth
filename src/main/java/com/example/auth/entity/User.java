package com.example.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
// 구글 로그인 사용자 정보를 저장하는 엔터티. PK는 AUTO_INCREMENT 대리키를 사용한다.
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(nullable = false, unique = true, length = 100)
    private String providerId; // 구글 sub 값

    @Column(length = 200, unique = true)
    private String email;

    @Column(length = 100)
    private String name;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public User(AuthProvider provider, String providerId, String email, String name) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.name = name;
    }

    public void updateProfile(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
