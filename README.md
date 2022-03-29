# ACS AEM Commons Integration Tests
This is a collection of tests that can be run to validate ACS AEM Commons.
Tests are written according to [Best practices](https://github.com/adobe/aem-testing-clients/wiki/Best-practices).

## How to use
Clone the repository and use maven for running each of the test modules.

The build also produces a `jar-with-dependencies` that can be run as a self-contained test module
(using java directly or a small maven pom with failsafe configured).

### Run the tests against localhost
```bash
mvn clean verify -Ptest-all
```

### Run the test against your AEM Cloud Service author and publish tiers
```bash
mvn clean verify -Ptest-all
-Dcloud.author.url=<your-aem-author-url> \
-Dcloud.author.user=admin \
-Dcloud.author.password=<your-admin-password> 
```
## Requirements

##### User

The test modules require the `admin` user or an admin-like user with enough privileges to create content, new users,
groups and replicate content.
