# 🎓 Universe

> **Onde a Universidade encontra o Universo.**

![Status](https://img.shields.io/badge/Status-Em_Desenvolvimento-yellow)
![Language](https://img.shields.io/badge/Language-Java-orange)
![Platform](https://img.shields.io/badge/Platform-Android-green)

---

## 📖 Sobre o Projeto

O **Universe** é uma rede social académica desenvolvida no âmbito da cadeira de **Programação de Dispositivos Móveis**. 

A ideia central nasce da fusão entre "Universidade" e "Universo", criando um espaço exclusivo onde estudantes podem partilhar momentos, interagir e fortalecer a comunidade académica. Funciona com uma dinâmica familiar (semelhante ao Instagram ou Twitter), mas focada inteiramente na nossa realidade universitária.

### 🎯 O Problema & A Solução
Redes sociais genéricas dispersam a atenção. O **Universe** resolve isto ao focar na partilha de experiências entre alunos, permitindo encontrar colegas pelo nome, username ou email institucional, sem o ruído do mundo exterior.

---

## ✨ Funcionalidades Principais

O Universe oferece um conjunto completo de ferramentas para a interação social:

* **🔐 Autenticação Segura:** Sistema robusto de Login, Registo e Recuperação de Password.
* **📱 Feed em Tempo Real:** Publicações de texto que se atualizam automaticamente para mostrar as novidades da comunidade.
* **❤️ Interação Social:** Sistema de 'Likes' e comentários em tempo real para gerar discussão.
* **👤 Perfis Personalizáveis:**
    * Foto de perfil (upload via Cloud).
    * Dados académicos (Curso, Universidade).
    * Contador de Seguidores/A Seguir.
* **🔍 Pesquisa & Follow:** Encontra outros estudantes e personaliza o teu feed seguindo apenas quem te interessa.

---

## 🛠️ Arquitetura e Tecnologias

Este projeto foi desenvolvido com foco em performance e conceitos modernos de desenvolvimento móvel:

| Categoria | Tecnologia | Detalhes |
| :--- | :--- | :--- |
| **Linguagem** | ![Java](https://img.shields.io/badge/Java-ED8B00?style=flat&logo=java&logoColor=white) | Desenvolvimento Android Nativo. |
| **Database** | ![Firebase Firestore](https://img.shields.io/badge/Firebase_Firestore-FFCA28?style=flat&logo=firebase&logoColor=black) | Base de dados NoSQL para sincronização em tempo real. |
| **Storage** | ![Firebase Storage](https://img.shields.io/badge/Firebase_Storage-FFCA28?style=flat&logo=firebase&logoColor=black) | Armazenamento e gestão de imagens de perfil. |
| **Interface** | **XML / Glide** | RecyclerViews dinâmicas e carregamento otimizado de imagens com a biblioteca Glide. |
| **IDE** | ![Android Studio](https://img.shields.io/badge/Android_Studio-3DDC84?style=flat&logo=android-studio&logoColor=white) | Ambiente de desenvolvimento. |

---

## 📸 Screenshots

| Login / Registo | Feed Principal | Perfil do Utilizador |
|:---:|:---:|:---:|
| <img src="caminho_para_print1.png" width="200"> | <img src="caminho_para_print2.png" width="200"> | <img src="caminho_para_print3.png" width="200"> |

---

## 🚀 Como Executar o Projeto

Para testar a aplicação no teu dispositivo ou emulador:

1.  **Clonar o repositório:**
    ```bash
    git clone [https://github.com/teu-usuario/universe.git](https://github.com/teu-usuario/universe.git)
    ```
2.  **Configurar o Firebase:**
    * Este projeto requer o ficheiro `google-services.json`.
    * Cria um projeto no [Firebase Console](https://console.firebase.google.com/).
    * Adiciona o ficheiro `google-services.json` na pasta `app/`.
3.  **Compilar:**
    * Abre o projeto no Android Studio e deixa o Gradle sincronizar.
    * Executa a app (`Shift + F10`).
---

## 🔮 Roadmap (Futuro)

* [ ] Comunidade Académica com outras universadades (ESES, ESA, ESS, ESDRM
* [ ] Sistema de notificações Push.
* [ ] Chat privado (Direct Messages).

---

## 📝 Conclusão

O **Universe** é uma plataforma social funcional, conectada à cloud, que aplica os conceitos fundamentais de desenvolvimento móvel: Interfaces dinâmicas, persistência de dados remota e gestão de utilizadores.

---
Desenvolvido por **João Santos, Daniel Nunes e Alexandre Silva** para a cadeira de PDM.
