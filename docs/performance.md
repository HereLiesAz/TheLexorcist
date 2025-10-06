# Performance and Build Efficiency

This document outlines critical guidelines related to build and test performance to ensure efficient development.

---

## Build and Testing Constraints

-   **Minimize Builds:** Do not run a build or a test without explicit instruction. The goal is to minimize the number of builds required. An ideal workflow requires **at most one build**, if any at all.
-   **Confidence in Code:** Rely on thorough analysis and code quality to avoid the need for frequent builds. A build should be a final verification step, not a debugging tool.

---

## Review Cadence

-   **Frequent Code Reviews:** Request a code review after every **10 changes**. These changes can be spread across multiple files or be concentrated in a single file.
-   **Purpose:** This cadence is designed to catch issues early and reduce the likelihood of needing a build to diagnose problems, thereby minimizing build frequency.