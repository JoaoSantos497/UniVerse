package com.universe;

public class User {
    private String uid;
    private String nome;
    private String username;
    private String email;
    private String curso;
    private String universidade;
    private String photoUrl;
    private long ultimaTrocaUsername;
    private String universityDomain;
    private long followersCount;
    private long followingCount;

    // --- NOVO CAMPO PARA NOTIFICAÇÕES
    private String fcmToken;

    // Construtor padrão (Obrigatório para o Firebase)
    public User() {}

    // Construtor Completo
    public User(String uid, String nome, String username, String email, String curso, String universidade) {
        this.uid = uid;
        this.nome = nome;
        this.username = username;
        this.email = email;
        this.curso = curso;
        this.universidade = universidade;
        this.ultimaTrocaUsername = 0;
        this.followersCount = 0;
        this.followingCount = 0;

        if (email != null && email.contains("@")) {
            this.universityDomain = email.substring(email.indexOf("@") + 1);
        } else {
            this.universityDomain = "geral";
        }
    }

    // --- GETTERS E SETTERS ---

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCurso() { return curso; }
    public void setCurso(String curso) { this.curso = curso; }

    public String getUniversidade() { return universidade; }
    public void setUniversidade(String universidade) { this.universidade = universidade; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public long getUltimaTrocaUsername() { return ultimaTrocaUsername; }
    public void setUltimaTrocaUsername(long ultimaTrocaUsername) { this.ultimaTrocaUsername = ultimaTrocaUsername; }

    public String getUniversityDomain() { return universityDomain; }
    public void setUniversityDomain(String universityDomain) { this.universityDomain = universityDomain; }

    public long getFollowersCount() { return followersCount; }
    public void setFollowersCount(long followersCount) { this.followersCount = followersCount; }

    public long getFollowingCount() { return followingCount; }
    public void setFollowingCount(long followingCount) { this.followingCount = followingCount; }

    // --- GETTER E SETTER PARA O fcmToken ---
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
}