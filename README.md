# SkillBridge
O SkillBridge é uma plataforma inteligente que ajuda pessoas a descobrir suas competências, planejar seu crescimento profissional e se reconectar com o mercado através de trilhas personalizadas de aprendizado com a ia e oportunidades cadastradas no nosso sistema. Por meio de IA generativa, o usuario consegue estudar vagas detalhadamente com base nos requisitos das vagas publicadas no sistema e com ajuda da ia.

## 📌 Visão Geral do Projeto

O **SkillBridge Java** é uma aplicação backend que possui:

- CRUDs completos (como Vagas e candidaturas vinculadas a essas vagas).
- RabbitMQ para menssagerias nos campos de create,delete e update do nosso crud
- Integração com IA (OpenAI) para geração de conteúdo com basses nessas vagas e com base no nosso pdf.
- Autenticação OAuth2 (Google e GitHub).
- Integração com PostgreSQL.
- Estrutura de mensageria assincrona com RabbitMQ para as vagas
- migrations
- Seeder no banco para deixar 3 vagas ja cadastradas no sistema
- thymealif
- deploy em Render.

---

## 🧱 Tecnologias Utilizadas

- Java 17+
- Spring Boot
- Spring Data JPA
- Spring Security OAuth2
- PostgreSQL
- RabbitMQ
- Docker
- Render (deploy)




## Link do deploy no render
https://skillbridge-java.onrender.com






## passo a passo para rodar localmente

1. **Clone o repositório:**
   ```bash
   https://github.com/JuanPabloRuiz583/SkillBridge_java.git

2. **Configure as seguintes variaveis de ambiente:**
   ```bash
   GITHUB_CLIENT_ID = (suas credenciais)
   GITHUB_CLIENT_SECRET = (suas credenciais)
   GOOGLE_CLIENT_ID = (suas credenciais)
   GOOGLE_CLIENT_SECRET = (suas credenciais)
   OPENAI_API_KEY= (suas credenciais)

3. **Abra o docker desktop antes de executar o projeto**
   
4. **rode o projeto**

5. **Acesse no navegador:**

🔑 Login (autentique-se primeiro):
http://localhost:8080/login

 Vagas — Cadastro / Edição / Remoção / Listagem / Busca por titulo da vaga e por empresa:
http://localhost:8080/vaga
(se não estiver autenticado, será redirecionado para a tela de login)

📄 Formulário de vagas:
http://localhost:8080/vaga/form
(acessível também clicando no botão "Nova vaga")

Candidaturas —  Cadastro / Edição / Remoção / Listagem :
http://localhost:8080/candidatura

📝 Formulário de Candidaturas:
http://localhost:8080/candidatura/form
(acessível também clicando no botão "Cadastrar sensor")

Tela da ia:
http://localhost:8080/chat
(acessível também clicando no botão "Falar com a SkillBridge IA")

🔒 Logout:
http://localhost:8080/logout



## Integrantes

Juan Pablo Ruiz de Souza, rm: 557727 (2TDSPZ)

Rafael Rodrigues de Almeida, rm: 557837 (2TDSPZ)

Lucas Kenji Miyahira, rm: 555368 (2TDSPZ)


