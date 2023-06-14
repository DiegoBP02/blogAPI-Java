# Blog API
This repository contains a project developed using Spring Boot, focusing on implementing Spring Security and writing tests using JUnit 5.

## Main purpose
This project was created with the aim of deepening knowledge and providing practical experience in building secure applications with Spring Security.

## About the project
It's a simple REST API for a blog with a login/registration system, including user, posts, and comments entities. It uses the relational database PostgreSQL to store the data. Authentication is done using JWT.

## Main features
- **Increase upvote**: Users can upvote posts or comments once.
- **Change password**: Users can change their password if they provide valid credentials.
- **Jail login**: User accounts are locked for 24 hours if there are 3 failed login attempts. Each time a user tries to login with invalid credentials, the failed login attempts increase by 1.
- **Rate Limit**: Access to authentication-related requests is limited to 10 per minute. This helps defend the API against overuse, whether unintentional or malicious. The functionality is implemented using the Bucket4j library.
- **Pagination**: Pagination and sorting are applied to the "get all posts" route.
- **Reset Password**: If a user forgets their password, they can send a request to `/auth/forgot-password` with their email as a parameter. A link will be sent to the user, redirecting them to `/auth/reset-password`, where they can set a new password to recover their account. The functionality to send an email is implemented using the JavaMailSender library.
- **Activate a new account by email**: The registration mechanism requires users to respond to a "confirm registration" link sent to their email in order to verify their email address and activate their account. Users need to click on the unique activation link sent to them via email. They will not be able to log into the application until this process is completed. This prevents users from registering using random or unauthorized emails.

## Main functionalities made with Spring Security and the learnings involved during the process
- **JWT Generation and validation**: Learned how to define Spring Security configuration using the `@Configuration` and `@EnableWebSecurity` annotations, `SecurityFilterChain` bean, JWT from the auth0 package, and `OncePerRequestFilter` filter.
- **Authentication exceptions**: Customized authentication exceptions and used a custom exception for expired tokens. Implemented a custom `AuthenticationEntryPoint` to customize the behavior for handling unauthorized requests.
- **Retrieve the current user**: Used `SecurityContextHolder` to get the authenticated principal, enabling the `checkOwnership` function that verifies if the current user is the same creator of a document, allowing or blocking them from performing update or delete actions.

## Access the Swagger UI Documentation
[Swagger UI](https://diegobp02.github.io/blogAPI-Java/index.html)

## Run Application Locally

### Pre requisites
- JDK 17 or higher
- Running instance of PostgreSQL

To run the project locally, please follow these steps:

1. Clone this repository and build the project.
2. Create a copy of the `application.properties.template` file located in `src/main/resources` and rename it to `application.properties`.
3. Open the `application.properties` file and provide the following information:

```properties
spring.jpa.properties.hibernate.dialect = org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.hibernate.show-sql=true
spring.datasource.url=jdbc:postgresql://localhost:your-db-port/your-db-name
spring.datasource.username=your-db-username
spring.datasource.password=your-db-password
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.userName=your-email
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```
3. Replace the placeholders with your actual database and email configuration details. Here's a description of each placeholder:
your-db-host: The host or IP address of your PostgreSQL database.
your-db-port: The port number of your PostgreSQL database.
your-db-name: The name of your PostgreSQL database.
your-db-username: The username for accessing your PostgreSQL database.
your-db-password: The password for accessing your PostgreSQL database.
your-email: Your email address.
your-password: Your email account password.

4. After successfully running the application, you should see log messages indicating the startup of the application. The logs will display the port on which the application is running.

## ⚠️ Security Warning

Please exercise caution when modifying the `application.properties` file and ensure that you do not inadvertently expose your sensitive information, such as passwords, to unauthorized individuals. 

To protect your credentials:
- Avoid committing the `application.properties` file to public repositories or sharing it with others.
- Double-check that you have replaced the placeholder values with your actual database and email configuration details.
- Keep your `application.properties` file secure and confidential.
