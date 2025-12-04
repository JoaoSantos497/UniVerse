package com.universe;

public class User {
    private String uid;
    private String nome;
    private String email;
    private String curso;
    private String universidade;

    // Construtor Vazio (OBRIGATÓRIO para o Firebase)
    public User() {}

    // Construtor Completo
    public User(String uid, String nome, String email, String curso, String universidade) {
        this.uid = uid;
        this.nome = nome;
        this.email = email;
        this.curso = curso;
        this.universidade = universidade;
    }

    // Getters (Para ler os dados)
    public String getUid() { return uid; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getCurso() { return curso; }
    public String getUniversidade() { return universidade; }
}