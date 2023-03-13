# Client Service

> client-service

The client service provides the necessary means to display the resources of a resource server
for the resource owner.
The user has to authenticate himself first through the authorization and authentication server, before he is
allowed to access the resources.

## Background information

#### OAuth2 client implementation

The client service implementation is in its core an oauth 2 compliant client and uses the authorization code grant type
in order to get the authorization from the resource owner.
Even though the client is capable of keeping the client secret safe, the actual client of companyX is not, thus
we simulate the exact same requisites.
That's why this client implementation does not use the client-id and client-secret combination
in order to authenticate himself at the authorization server, but uses the PKCE extension of the oauth2 standard.
Before requesting the resources from the resource server, the client checks if it has a valid
authorization for the user, which currently uses the client.
This also means that the client manages the authentication information of a user and correlates
authorization with the authenticated user.
If no authenticated user exists or if no authorization can be found that correlates with the currently
authenticated user, then the client will first request a authorization from the authorization server,
exchange that code against a access token and correlate that to the authenticated user.
The access token will be used for all subsequent resource server requests.

#### Blacklisted and expired access tokens

The authorization server is capable of putting access tokens into a blacklist, which indicate to the
resource server, that these tokens should not be trusted anymore, for example because the access token got compromised
by an attacker.

The blacklist is managed by a redis server, which is an in memory key-value database and allows
quicker access to the contained information than a relational database.

Even though the resource server validates on its own all the received access tokens, the client
still does a validation for blacklisted tokens, so that these tokens are not even send to the resource server
in the first place and minimizing the traffic between client and resource server.

#### Cross Site Request Forgery (CSRF) protection

Our post endpoints are protected against CSRF attacks, by using the built-in
protection provided by [Spring CSRF](https://docs.spring.io/spring-security/reference/features/exploits/csrf.html).
In short: For each POST-Form a secure random generated value will be created, which represents
an CSRF Token.
Spring generates this secure value based on the users' session who is currently accessing the form and
injects it into a hidden field in that form and sends that value with the form data.
When the Server receives an POST request, it checks that the contained CSRF value matches with the value of the
session of the currently authenticated user.
If they don't match, then the request has been sent by an attacker and spring prevents the execution of the request.

## Prerequisites

* Java 17 or higher
* Maven
* Redis (blacklist)
* CompanyX authorization server
* CompanyX resource server

## Java 17

We are using Spring Boot 3.0 as the underlying framework, which requires at least Java 17.

## Maven

Maven is used as the main dependency manager for this project.
It is recommended to also use maven.

## Redis

Redis is a key-value based in memory database, which provides faster access times than relational databases.
Redis is used as the blacklist repository for access tokens.

Make sure that you have an installed redis server that is configured for localhost:6379

## Installing / Getting started with this project

1. Checkout this repository to a new directory.
2. Open the repository directory in IntelliJ or any other IDE.
3. Reload the maven project.
4. Be sure that java 17 is selected as the SDK and the java language is at least 17.
5. `mvn clean compile` (either via terminal or using the sidebar Maven goals in IntelliJ)
6. Let IntelliJ build the project.

## Developing

In order to start the client service and test new features or new bugs, the following steps are necessary:

* Make sure to import the necessary database test data in the resource server.
    * Start the server with the dev profile in order to automatically import the test data.
* Start the Redis-Server (necessary for the blacklist).
* Start the authentication server
* Start the resource server

## JAR-File

In order to start the client service trough the jar, the following command has to be executed:
* java -jar client-service-1.0.0

## Links

The following projects are part of this implementation:

* [Spring Boot 3.0.2](https://spring.io/projects/spring-boot)
* [Spring Security 6.0.1](https://spring.io/projects/spring-security)
* [Lombok 1.18.24](https://projectlombok.org)
* [Spotbugs 4.7.3](https://spotbugs.github.io)
* [Thymeleaf (Part of Spring Boot 3.0.2)](https://www.thymeleaf.org)
* [Redis 7.0](https://redis.io)
