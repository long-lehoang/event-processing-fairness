---
name: code-review
description: Review code changes for bugs, security issues, performance, and quality. Use when the user asks for a code review, PR review, or wants feedback on code changes.
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash, Agent
---

# Code Review

Perform a thorough code review on the specified target. The target can be:
- A file or directory path: review that code directly
- A PR number or branch name: review the diff
- No argument: review all uncommitted changes (staged + unstaged)

## Review Target

`$ARGUMENTS`

## Step 1: Gather Changes

Based on the target:
- **No arguments**: Run `git diff` and `git diff --cached` to get all uncommitted changes. Also run `git status` to see untracked files.
- **File/directory path**: Read the specified files.
- **PR number**: Run `gh pr diff <number>` to get the diff.
- **Branch name**: Run `git diff main...<branch>` to get the diff.

If the diff is large, focus on the most impactful changes first.

## Step 2: Understand Context

- Read the full files that were changed (not just the diff) to understand surrounding context
- Check for related test files
- Look at imports and dependencies to understand the broader impact

## Step 3: Review Checklist

Evaluate the code against each category:

### Correctness
- Logic errors, off-by-one, null/nil handling
- Edge cases not covered
- Race conditions in concurrent code
- Error handling gaps (especially in Go: unchecked errors; in Java: swallowed exceptions)

### Security
- SQL injection, command injection
- Unvalidated user input
- Hardcoded secrets or credentials
- Improper authentication/authorization checks
- Sensitive data in logs

### Performance
- N+1 queries or unnecessary database calls
- Missing indexes for new query patterns
- Unbounded collections or memory leaks
- Blocking calls in async/event-processing paths

### Code Quality
- Readability and naming clarity
- DRY violations (duplicated logic)
- Overly complex functions that should be split
- Dead code or unused imports
- Consistency with existing codebase patterns

### Testing
- Are changes covered by tests?
- Are edge cases and error paths tested?
- Are tests meaningful (not just asserting true == true)?

### Architecture
- Does the change fit the existing patterns in the codebase?
- Are concerns properly separated?
- Are interfaces/abstractions appropriate (not over/under-engineered)?

## Step 4: Output

Present the review in this format:

---

### Review Summary
Brief description of what the changes do and their overall quality.

### Critical Issues
Issues that must be fixed before merging. These are bugs, security vulnerabilities, or data-loss risks.

### Improvements
Suggestions that would meaningfully improve the code but are not blockers.

### Nits
Minor style or preference issues. Low priority.

### Positive Highlights
Good patterns or decisions worth calling out.

---

For each finding, include:
1. The file and line reference (as a clickable markdown link)
2. What the issue is
3. A concrete suggestion or fix (with code snippet when helpful)

Be specific and actionable. Avoid vague feedback like "consider improving this". Instead, show exactly what you'd change and why.
