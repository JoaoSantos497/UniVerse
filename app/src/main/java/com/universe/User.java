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
    }

    // --- GETTERS E SETTERS ---

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; } // RESOLVE O ERRO DO SEARCH

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }

    public String getCurso() { return curso; }
    public void setCurso(String curso) { this.curso = curso; }

    public String getUniversidade() { return universidade; }
    public void setUniversidade(String universidade) { this.universidade = universidade; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public long getUltimaTrocaUsername() { return ultimaTrocaUsername; }
}