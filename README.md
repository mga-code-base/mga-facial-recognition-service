# 😎 mga-facial-recognition-service

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot)](https://spring.io/)
[![Java Version](https://img.shields.io/badge/Java-17-007396?logo=openjdk)](https://openjdk.org/)
[![Build Tool](https://img.shields.io/badge/Build-Apache%20Maven%203.9.x-C71A36?logo=apachemaven)](https://maven.apache.org/)
[![AI Engine](https://img.shields.io/badge/AI%20Engine-Facial%20Recognition-FF6F00?logo=tensorflow)](https://tensorflow.org)
[![Vector DB](https://img.shields.io/badge/Database-Aiven%20SQL%20pgvector-blue?logo=postgresql)](https://aiven.io/)

## 📌 Service Overview

`mga-facial-recognition-service` powers the touchless biometric check-in and check-out system used by the MGA Face Hardware Kiosk App.

When a member registers their face, the service first executes a pre-verification call against `mga-people-crud-service` to ensure the member exists in the database. Upon verification, facial embeddings are stored in an Aiven Cloud-hosted PostgreSQL database using the `pgvector` extension for high-speed vector cosine distance matching during check-in.

### 🏛️ Architectural Features
* Pre-Verification Workflow: Executes an internal REST call to `mga-people-crud-service` prior to embedding creation to confirm valid member registration.
* Vector Cosine Distance Matching: Utilizes PostgreSQL with `pgvector` on Aiven Cloud for fast similarity searches and real-time face verification.
* Standardized Data Contract: Imports shared domain models directly from `mga-all-models-jars`.

---

## 🛠️ Technical Specifications
* Port: 8088 (Kiosk Ingress)
* Language/Build: Java 17 | Apache Maven 3.9.x
* Runtime Container: Standard JVM Container
* Vector Persistence: Aiven Cloud PostgreSQL with `pgvector` extension
* Upstream Service Call: mga-people-crud-service (Port 8080)
* Shared Contract: mga-all-models-jars

---

## ⚙️ Build & Deployment Instructions
# Standard JVM Build & Run
mvn clean package
java -jar target/mga-facial-recognition-service.jar

# Spring Boot Run
mvn spring-boot:run

```
