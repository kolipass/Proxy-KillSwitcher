---
name: kotlinStyle/lambda
description: Lambda parameter naming conventions.
---

```yaml
rules:

    -   id: LA01
        rule: "Use named lambda parameters, not implicit `it`"
        do: |
            val authStateListener = FirebaseAuth.AuthStateListener { auth ->
                trySend(auth.currentUser != null)
            }
        dont: |
            val authStateListener = FirebaseAuth.AuthStateListener {
                trySend(it.currentUser != null)  # implicit `it` hides what we're working with
            }
```
