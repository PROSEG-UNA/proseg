# AGENTS.md - Global Coding Instructions

## Scope
Use these rules for coding, debugging, refactoring, code review, and technical questions.

## Core
- Read relevant files before changing code.
- Prefer small edits over full rewrites.
- Keep solutions simple and direct.
- Test changes before saying they are done.
- Do not speculate. Verify from code, logs, or provided context.
- User instructions override this file.

## Output
- Put code first.
- Explain only when needed.
- No greetings, compliments, filler, or closing fluff.
- No boilerplate unless requested.
- Keep responses concise.

## Code
- Use the simplest working fix.
- Do not over-engineer.
- Do not add abstractions for one-time use.
- Do not add speculative features.
- Do not add docstrings or type annotations unless already used or requested.
- Use comments only when logic is not obvious.
- Avoid unnecessary error handling.
- Keep code copy-paste safe.

## Data fetching (app/sssi-app)
- Use TanStack Query for reads where the server is the source of truth: lists, catalogs, detail panels, dashboards.
- Use useState + useEffect to load one entity into an editable form. Copy the data into a local draft and reset it when the modal closes.
- Never derive an input value from the query cache through overrides. That is what made unsaved drafts reappear on reopen.
- Declare every key in src/common/query/queryKeys.js, hanging from the feature root. Do not inline key arrays in components.
- Invalidate with the narrowest key that covers the change.
- Return loading: isLoading for the empty state and fetching: isFetching for the background indicator. Never use isFetching as loading.
- Do not query per table row. If a row needs a flag, add it to the list endpoint.
- Pending migrations keep their key declared: assetPhotos, ticketDetail, ticketHistory, companyUsers, technicians.

## Review
- State the bug.
- Show the fix.
- Stop.
- Do not suggest unrelated improvements.

## Debugging
- Read the relevant code before diagnosing.
- State what was found, where it is, and the fix.
- If the cause is unclear, say so.

## Formatting
- Use plain hyphens.
- Use straight quotes.
- Avoid decorative Unicode.
- Accented characters are allowed when needed.