Stage all changes, write a meaningful commit message, and push to origin.

## Current state

**Status:**
!`git status`

**Diff summary:**
!`git diff HEAD --stat`

**Recent commits (for message style reference):**
!`git log --oneline -5`

## Instructions

1. Stage all changes with `git add`, but skip obvious non-code files like `*.dat`, `PROJECT_SUMMARY.md`, and anything that looks like secrets.
2. If `$ARGUMENTS` is provided, use it as the commit message summary line. Otherwise write a concise imperative summary (≤72 chars) based on the diff above, matching the style of the recent commits.
3. Use the AskUserQuestion tool to ask the user to confirm before committing. Show the proposed commit message and ask "Proceed with this commit?". If the user does not confirm, stop and do not commit or push.
4. Commit using a HEREDOC. Append this trailer on a new line:
   `Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>`
5. Push with `git push origin HEAD`.
6. Confirm by printing `git log --oneline -3`.
