# Autocycle Backend

Spring Boot API for brand collaborations and upcycled products. PostgreSQL stores the profiles, pgvector stores the embeddings, and S3 stores generated images and partner icons.

## Stack

- Java 25, Spring Boot 4.1
- Spring Web, Bean Validation, Actuator
- Spring Data JPA / Hibernate, Flyway
- PostgreSQL 17 with pgvector
- Lombok
- springdoc OpenAPI
- AWS SDK v2: S3, and Bedrock in `eu-central-1` for chat (Nova Pro), embeddings (Titan v2), and images
- JUnit 5, AssertJ, Mockito, Testcontainers, JaCoCo

## Layout

`controller` exposes the API. `service` runs the two flows: look up a stored brand or plastic, research it when the similarity score is below 0.8, then ask the model for collaborations or a product. `repository` and `entity` own PostgreSQL. Embeddings are written with JDBC because Hibernate does not map `vector`.

`POST /api/collaborations` returns up to five partners. Evidence and each partner’s domain come from the same Nova model in `AWS_REGION`. The partner icon is fetched from that site and stored in S3. `POST /api/products` returns a cached collaboration when one already has products, otherwise it writes a pitch, generates an image, stores the PNG in S3, and saves the row. Model failures respond with 502. A missing brand or product field responds with 400.

## Run

```bash
cp .env.example .env
# fill AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, and AWS_S3_BUCKET
make up
```

The API listens on port 8080. `make up` builds and starts it. Swagger UI is at `http://localhost:8080/api/docs`. Health probes are under `/actuator/health`.

Local S3 without AWS:

```bash
make local-aws
```

Set `AWS_S3_ENDPOINT=http://localstack:4566` and `AWS_ACCESS_KEY_ID=test` on the API. It creates the bucket on startup when that endpoint is set. Bedrock itself still needs a real AWS account; LocalStack does not serve Nova or Titan.

## Tests

```bash
make test
```

Unit tests cover the flows, the Bedrock client, and S3 uploads. `@WebMvcTest` covers the controllers. `@DataJpaTest` and `@SpringBootTest` run against a pgvector Postgres container.
