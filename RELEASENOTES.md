### **Highlights**

- **SSO**: Added native SSO support

### **What's New?**

#### New Features and Improvements

- [Backend/Frontend]: Added support for SSO using [Authentik](https://authentik.0x2b.de/), allowing users to log in using their existing credentials from the authentication provider.
- [UI/UX]: Improved Service worker caching strategy, resulting in faster load times and better performance when navigating through the application.
- [Chart]: Improved the helm chart to support the new SSO configuration

#### Bug Fixes

- [UI/UX]: Fixed an issue where the mobile view of the application was not working properly.

#### Dependency Updates

- org.jsoup:jsoup to v1.22.2
- registry.access.redhat.com/ubi9/openjdk-25-runtime docker tag to v1.24-2.1778501186
- quarkus.platform.version to v3.35.3
- registry.access.redhat.com/ubi9/ubi-minimal docker tag to v9.7-1778562320

### **Breaking Changes**

- application does not longer work without SSO configuration. Maybe this will be changed in the future, but for now SSO is a hard requirement.
