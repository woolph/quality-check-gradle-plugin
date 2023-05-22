# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased/Upcoming
### Added
- Printing the causing 1st level dependencies for vulnerabilities in transient dependencies

### Fixed
- fixed log messages with checkLicenses
- fixing note indentation on generatedSuppressionFile

## [1.2.2]() - 2023-03-01
### Fixed
- Fixing issue with generateSuppressionFile detecting whether an original suppression file exists

## [1.2.1]() - 2023-02-28
### Changed
- Adding a link to the notes of new vulnerabilities in the result of the `generateSuppressionFile` task

## [1.2.0]() - 2023-02-28
### Added
- Adding tasks for checking, generating, & updating the suppression file for dependency check

## [1.1.1]() - 2023-02-15
### Changed
- Updated Sonar Plugin from 3.5.0 to 4.0.0.2929 to support gradle 8.0
- LicenseCheck now also ignores `org.springframework.cloud:spring-cloud-dependencies` & 
`org.springframework.cloud:spring-cloud-sleuth-otel-dependencies` (besides 
`org.springframework.boot:spring-boot-dependencies`) because they are otherwise not detected as BOM artifacts which we
want to exclude from license checking

## [1.1.0]() - 2023-02-15
### Added
- Added a plugin to check for licenses of 3rd party dependencies

## [1.0.0]() - 2023-01-09
### Changed
- Updated Dependency Check plugin to 7.4.4 (formerly 7.1.1) to fix issues with updating the local NVD cache
- Updated Sonar Plugin from 3.4.0 to 3.5.0

## [0.1.2]() - 2022-07-29
### Changed
- Made database credentials optional (in case we can use SSO with AD account)

## [0.1.1]() - 2022-07-25
### Added
- JDBC-Driver for MSSQL to support 

## [0.1.0]() - 2022-06-21
### Added
- Initial version