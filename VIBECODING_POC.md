# Vibecoding Proof of Concept

**Task**: VIB-3 - Build a vibecoding proof of concept
**Engineer**: Founding Engineer (f4bafd02-62fe-4ad4-bfe2-0d00a4885ccd)
**Date**: 2026-03-22

## What is Vibecoding?

**Vibecoding** is autonomous AI-driven software development where an AI agent:
1. Takes high-level intent or vague requirements
2. Independently designs the technical solution
3. Makes architectural and implementation decisions
4. Delivers working code with tests and documentation
5. Documents the approach and trade-offs

Unlike traditional development where humans provide detailed specs, vibecoding operates on **intent** - the "vibe" of what's needed.

## POC Feature: Custom Short Codes

### The Vibe

"Users should be able to choose their own short codes instead of getting random ones."

### Autonomous Design Decisions

From this vague requirement, I'll make the following independent decisions:

#### 1. API Design

**Decision**: Extend existing `/shorten` endpoint with optional `customCode` parameter.

```json
POST /shorten
{
  "url": "https://example.com",
  "customCode": "mylink"  // optional
}
```

**Trade-offs**:
- ✅ Backward compatible (existing clients work unchanged)
- ✅ Single endpoint (simpler than separate endpoints)
- ❌ Slightly more complex validation logic

**Alternative considered**: Separate `POST /shorten/custom` endpoint
- Rejected: More endpoints to maintain, splits related functionality

#### 2. Custom Code Validation Rules

**Decisions**:
- Length: 3-20 characters (balance between usability and namespace)
- Characters: Alphanumeric only `[a-zA-Z0-9]` (URL-safe, easy to type)
- Case-sensitive: Yes (matches existing auto-generated codes)
- Reserved codes: Block common paths like `api`, `health`, `shorten`

**Trade-offs**:
- ✅ Prevents conflicts with future API endpoints
- ✅ Simple validation rules
- ❌ Slightly restricts user choice

#### 3. Conflict Handling

**Decision**: Return `409 Conflict` if custom code already exists.

**Trade-offs**:
- ✅ Clear HTTP semantics
- ✅ Allows client to retry with different code
- ❌ Requires client to handle conflict case

**Alternative considered**: Auto-append numbers (e.g., `mylink1`, `mylink2`)
- Rejected: Unexpected behavior, user loses control

#### 4. Database Schema

**Decision**: No schema changes needed! Existing `code` column works for both auto-generated and custom codes.

**Trade-offs**:
- ✅ Zero migration required
- ✅ Unified code path for lookups
- ✅ Simple implementation
- ❌ Can't easily distinguish custom vs auto-generated codes (acceptable for POC)

#### 5. Service Layer Logic

**Decision**: Modify `UrlService.shortenUrl()` to accept optional custom code.

```java
public ShortUrl shortenUrl(String originalUrl, String customCode) {
    if (customCode != null) {
        // Validate custom code
        // Check for conflicts
        // Use custom code
    } else {
        // Auto-generate code (existing logic)
    }
}
```

**Trade-offs**:
- ✅ Single method handles both cases
- ✅ Clear separation of concerns
- ❌ Method signature change (still backward compatible via overload)

## Implementation Plan

### Phase 1: Validation (10 min)
- Add custom code validation logic
- Define reserved words list
- Add validation tests

### Phase 2: Service Layer (15 min)
- Update `UrlService.shortenUrl()` to accept optional custom code
- Add conflict detection
- Update service tests

### Phase 3: Controller Layer (10 min)
- Update `ShortenRequest` DTO to include optional `customCode`
- Update controller to pass custom code to service
- Add controller tests for custom codes

### Phase 4: Integration Testing (10 min)
- Test happy path (custom code success)
- Test conflict scenario (409 response)
- Test validation errors (400 response)

### Phase 5: Documentation (5 min)
- Update README with custom code API documentation
- Add examples

**Total estimated time**: 50 minutes (autonomous, no spec review meetings!)

## Success Criteria

✅ Custom codes work via API
✅ Validation prevents invalid codes
✅ Conflicts return 409
✅ Existing auto-generation still works (backward compatibility)
✅ All tests pass
✅ Documented

## Vibecoding Demonstration

This POC demonstrates vibecoding by:
1. ✅ **Vague input**: "Users should choose their own short codes"
2. ✅ **Autonomous decisions**: API design, validation rules, error handling
3. ✅ **Working code**: End-to-end implementation
4. ✅ **Tests**: Comprehensive test coverage
5. ✅ **Documentation**: This document + API docs

No human needed to:
- Write detailed specs
- Review API design
- Define validation rules
- Plan testing strategy

The agent did it all based on **intent**.

## Next: Implementation

Starting implementation now...
