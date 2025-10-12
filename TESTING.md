# Testing Documentation for Ms-Event-Seating-Projection

This document provides comprehensive information about the testing approach, methodologies, and frameworks used in the Ms-Event-Seating-Projection microservice.

## Table of Contents

1. [Testing Overview](#testing-overview)
2. [Testing Libraries and Frameworks](#testing-libraries-and-frameworks)
3. [Test Categories](#test-categories)
4. [Testing Patterns](#testing-patterns)
5. [Test Configurations](#test-configurations)
6. [Mocking Strategies](#mocking-strategies)
7. [Test Execution](#test-execution)
8. [CI/CD Integration](#cicd-integration)
9. [Test Coverage](#test-coverage)
10. [Future Improvements](#future-improvements)

## Testing Overview

The Ms-Event-Seating-Projection microservice implements a comprehensive testing strategy to ensure code quality and reliability. Testing is primarily focused on unit and integration tests that verify the functionality of services, controllers, and consumers within the reactive Spring Boot application.

The project uses JUnit 5 as the primary testing framework, along with Mockito for mocking dependencies and Reactor Test for testing reactive streams. The tests validate the behavior of the application components in isolation (unit tests) and in combination (integration tests).

## Testing Libraries and Frameworks

The project uses the following testing libraries and frameworks:

- **JUnit 5**: The core testing framework for writing and running tests
- **Mockito**: Mocking framework for creating test doubles (mocks, stubs)
- **Reactor Test**: Provides StepVerifier for testing reactive streams in a non-blocking manner
- **Spring Boot Test**: Testing utilities for Spring Boot applications
- **Spring Kafka Test**: For testing Kafka integration

These libraries are included as dependencies in the project's `pom.xml` file.

## Test Categories

The test suite includes the following categories of tests:

### Unit Tests

Unit tests focus on testing individual components in isolation. Dependencies are mocked using Mockito to ensure tests only validate the component under test. Examples include:

- Service unit tests (`EventQueryServiceTest`, `SeatServiceTest`)
- Controller unit tests (`EventQueryControllerTest`, `InternalQueryControllerTest`)
- Consumer unit tests (`SeatStatusConsumerTest`, `DebeziumEventConsumerTest`)

### Integration Tests

Integration tests validate the interaction between multiple components and with external dependencies. These tests use Spring's testing capabilities to set up a test environment with necessary configurations.

### Application Context Tests

Tests that verify that the Spring application context loads correctly (`MsEventSeatingProjectionApplicationTests`).

## Testing Patterns

The project follows several key testing patterns:

### BDD-Style Testing

Many tests are structured in the Behavior-Driven Development (BDD) style with the Arrange-Act-Assert pattern:

1. **Arrange**: Set up test data and mocks
2. **Act**: Execute the method under test
3. **Assert**: Verify the expected outcome

### Reactive Testing

The project uses StepVerifier from Reactor Test to test reactive streams:

```java
StepVerifier.create(eventQueryService.searchEvents(...))
    .assertNext(page -> {
        assert page.getTotalElements() == 2;
        assert page.getContent().size() == 2;
    })
    .verifyComplete();
```

### Mock-based Testing

The project uses Mockito's @Mock, @InjectMocks, and @ExtendWith(MockitoExtension.class) annotations to create mock objects and inject them into the components being tested.

## Test Configurations

### TestConfig

The project includes a dedicated test configuration class (`TestConfig.java`) that provides mock implementations for external dependencies like the Google Analytics Service. This allows tests to run without requiring actual external service connections.

### Application Properties

The project has a dedicated `application.yml` in the test resources folder that configures the test environment settings, including database connections, Kafka, and other service configurations.

## Mocking Strategies

Several mocking strategies are used throughout the test suite:

1. **Direct Dependency Mocking**: Using @Mock to create mock instances of dependencies
2. **Behavior Stubbing**: Using `when(...).thenReturn(...)` to define mock behavior
3. **Argument Capturing**: Using ArgumentCaptor to capture and validate arguments passed to mocked methods
4. **Verification**: Using `verify()` to ensure methods are called with the expected arguments

Example:
```java
verify(seatService).updateSeatStatus(eq(sessionId), eq(seatIds), eq(ReadModelSeatStatus.LOCKED));
```

## Test Execution

### Running Tests

Tests can be executed using Maven:

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=EventQueryServiceTest

# Run specific test method
mvn test -Dtest=EventQueryServiceTest#searchEvents_shouldReturnPageOfEventThumbnails
```

### Test Reports

Maven Surefire plugin generates test reports in the `target/surefire-reports` directory. These reports provide detailed information about test execution, including failures and execution time.

## CI/CD Integration

Test execution is integrated into the CI/CD pipeline. Tests are automatically run on code commits and pull requests to ensure code quality before merging changes.

## Test Coverage

The project aims for comprehensive test coverage across all service layers. Key areas covered include:

1. **Services**: Business logic and data processing
2. **Controllers**: API endpoints and request handling
3. **Consumers**: Kafka message consumers and event handling
4. **Exception Handling**: Error scenarios and edge cases

## Future Improvements

Potential improvements to the testing strategy include:

1. **Property-based Testing**: Implement property-based testing for more robust validation
2. **API Contract Testing**: Add contract tests to verify API compatibility
3. **Performance Testing**: Include performance tests for critical paths
4. **End-to-End Testing**: Add end-to-end tests for critical user journeys
5. **Chaos Testing**: Introduce failure injection to test resilience

## Best Practices

The testing approach follows these best practices:

1. **Test Isolation**: Tests should not depend on each other and should be able to run independently
2. **Readable Tests**: Test names should clearly describe what is being tested
3. **Fast Tests**: Tests should execute quickly to provide rapid feedback
4. **Deterministic Tests**: Tests should produce the same result on each execution
5. **Test One Thing**: Each test should focus on validating one aspect of behavior

---

*This documentation is maintained by the Ticketly development team and should be updated as testing practices evolve.*