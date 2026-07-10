# StudyBuddy AI

A personal learning assistant backend built with Java and Spring Boot. Upload your
study notes or course PDFs, ask questions about them in plain language, generate
quizzes automatically, and get email reminders for topics you are weak on.

This version runs entirely on free, local AI models through Ollama — no API key,
no billing, no internet needed once the models are downloaded.

## How it works

1. You upload a document (PDF, docx, or text file).
2. Apache Tika extracts the raw text.
3. The text is split into overlapping chunks.
4. Each chunk is converted into a vector embedding using a local Ollama model
   (`nomic-embed-text`) and stored in PostgreSQL using the pgvector extension.
5. When you ask a question, your question is also embedded, and the most similar
   chunks are retrieved using vector similarity search.
6. Those chunks are passed to a local LLM (`llama3.2` via Ollama) along with your
   question, so the answer is grounded in your own material rather than the
   model's general knowledge.

## Requirements

- Java 17 or newer
- Maven 3.8+
- Docker (for running PostgreSQL with pgvector)
- Ollama (free, local AI models, no API key needed) — download from ollama.com

## Setup

### 1. Start PostgreSQL with pgvector using Docker

```
docker run -d --name studybuddy-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=studybuddy -p 5432:5432 pgvector/pgvector:pg16
```

Then apply the schema:

```
docker cp src/main/resources/schema.sql studybuddy-db:/schema.sql
docker exec -it studybuddy-db psql -U postgres -d studybuddy -f /schema.sql
```

### 2. Install Ollama and pull the models

Download Ollama from https://ollama.com/download and install it.

Then pull the two models this project needs (one time only, downloads locally):

```
ollama pull nomic-embed-text
ollama pull llama3.2
```

Ollama runs a local server on `http://localhost:11434` automatically once installed.
Keep it running in the background whenever you use the app.

### 3. Set JWT secret

```
set JWT_SECRET=someLongRandomStringHere
```

(use `export` instead of `set` on Mac/Linux)

### 4. Build and run

```
mvn clean install
mvn spring-boot:run
```

The app starts on `http://localhost:8080`.

## API endpoints

### Auth
- `POST /api/auth/signup` — body: `{ "name", "email", "password" }`
- `POST /api/auth/login` — body: `{ "email", "password" }`, returns a JWT token

All endpoints below require an `Authorization: Bearer <token>` header.

### Documents
- `POST /api/documents/upload` — multipart file upload, field name `file`
- `GET /api/documents` — list your uploaded documents and their status
- `GET /api/documents/{id}` — get a single document's details

### Chat
- `POST /api/chat` — body: `{ "question": "...", "documentIds": [1, 2] }`
  (documentIds is optional, defaults to all your ready documents)

### Quiz
- `POST /api/quiz/generate/{documentId}?count=5` — generates quiz questions
- `GET /api/quiz/document/{documentId}` — list quizzes for a document
- `POST /api/quiz/{quizId}/attempt` — body: `{ "selectedAnswer": "A" }`

### Dashboard
- `GET /api/dashboard/summary` — total attempts, correct answers, accuracy

## Project structure

```
src/main/java/com/studybuddy/
├── config/        JWT and Spring Security setup
├── controller/     REST API endpoints
├── service/        business logic: chunking, embeddings, RAG, quiz generation
├── repository/    Spring Data JPA repositories
├── entity/        database table mappings
└── dto/           request and response objects
```

## Notes on the vector storage approach

Hibernate does not have native support for the pgvector `vector` column type,
so document_chunks is created manually through `schema.sql` and all reads and
writes to that table go through `VectorStoreService` using plain JDBC (via
`JdbcTemplate`) instead of a JPA repository. Everything else uses standard
Spring Data JPA.

## What's intentionally left simple for now

- Document processing runs as a Spring `@Async` task rather than a separate
  queue like Kafka or RabbitMQ. Fine for a single-instance deployment; if this
  needs to scale to many concurrent uploads, that would be the next thing to add.
- The quiz answer parsing uses a regex against the LLM's response rather than
  strict JSON mode. Works reliably in practice but if you want to harden it,
  switch the prompt to request JSON output and parse that instead.
- No file storage service (S3 etc) is wired in — uploaded files are processed
  in memory and only the extracted chunks are persisted, not the original file.
  Add that if you need to keep the original documents around.
