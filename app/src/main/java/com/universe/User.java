package com.universe;

public class User {
    private String uid;
    private String nome;
    private String username; // <--- NOVO CAMPO
    private String email;
    private String curso;
    private String universidade;

    public User() {}

    // Construtor Atualizado
    public User(String uid, String nome, String username, String email, String curso, String universidade) {
        this.uid = uid;
        this.nome = nome;
        this.username = username; // <--- Guardar aqui
        this.email = email;
        this.curso = curso;
        this.universidade = universidade;
    }

    // Getters e Setters
    public String getUid() { return uid; }
    public String getNome() { return nome; }

    public String getUsername() { return username; } // <--- Novo Getter
    public void setUsername(String username) { this.username = username; } // <--- Novo Setter

    public String getEmail() { return email; }
    public String getCurso() { return curso; }
    public String getUniversidade() { return universidade; }
}