CodeLens – AI-Powered Code Review Assistant

## Overview

CodeLens is a microservices-based AI-powered code review platform that helps developers analyze, review, and improve their code using Generative AI. The system provides automated code quality assessment, issue detection, code ratings, and actionable recommendations.

The platform is built using Spring Boot, Spring Cloud Gateway, Thymeleaf, and Groq AI integration.

---

## Features

### User Management

* User registration and login
* JWT-based authentication
* Secure API access
* User profile management

### AI Code Review

* Submit source code for review
* AI-powered code analysis using Groq LLM
* Detection of:

  * Bugs and potential errors
  * Code smells
  * Security vulnerabilities
  * Performance issues
  * Maintainability concerns

### Review Analytics

* Code quality scoring
* Detailed issue reports
* Improvement recommendations
* Review history tracking

### Microservices Architecture

* API Gateway
* User Service
* Review Service
* Frontend Service

---

## Architecture

```text
┌─────────────┐
│  Frontend   │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ API Gateway │
└──────┬──────┘
       │
 ┌─────┴─────┐
 ▼           ▼
User      Review
Service   Service
              │
              ▼
          Groq AI
```

---

## Technology Stack

### Backend

* Java 17
* Spring Boot 3
* Spring Security
* Spring Cloud Gateway
* Spring Data JPA

### Frontend

* Thymeleaf
* HTML5
* CSS3
* JavaScript

### Database

* H2 Database

### Authentication

* JWT Authentication

### AI Integration

* Groq API

### Build Tool

* Maven

---

## Project Structure

```text
CodeLens
│
├── api-gateway
├── frontend
├── review-service
├── user-service
├── pom.xml
└── schema.sql
```

---

## Prerequisites

* Java 17+
* Maven 3.8+
* Git
* Groq API Key

---

## Installation

### Clone Repository

```bash
git clone https://github.com/Saurabh8400/CodeLens.git
cd CodeLens
```

### Configure Environment Variable

Windows PowerShell:

```powershell
$env:GROQ_API_KEY="YOUR_GROQ_API_KEY"
```

Linux/Mac:

```bash
export GROQ_API_KEY=YOUR_GROQ_API_KEY
```

---

## Running the Application

### Start User Service

```bash
cd user-service
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8081
```

### Start Review Service

```bash
cd review-service
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8082
```

### Start Frontend

```bash
cd frontend
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8083
```

### Start API Gateway

```bash
cd api-gateway
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8080
```

---

## API Endpoints

### Authentication

| Method | Endpoint           |
| ------ | ------------------ |
| POST   | /api/auth/register |
| POST   | /api/auth/login    |

### Reviews

| Method | Endpoint             |
| ------ | -------------------- |
| POST   | /api/reviews/analyze |
| GET    | /api/reviews/history |
| GET    | /api/reviews/{id}    |

---

## Environment Variables

| Variable     | Description                   |
| ------------ | ----------------------------- |
| GROQ_API_KEY | Groq AI API Key               |
| JWT_SECRET   | JWT Signing Secret (Optional) |

---

## Future Enhancements

* PostgreSQL integration
* Docker support
* Kubernetes deployment
* GitHub repository integration
* Multi-language code analysis
* Team collaboration features
* CI/CD pipeline support

---

## Author

**Saurabh Kumar**

GitHub: https://github.com/Saurabh8400

---

## License

This project is intended for educational and portfolio purposes.
