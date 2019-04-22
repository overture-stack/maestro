# IMPORTANT
maven config format is important for the jenkins flow to read the version !! should be 3 lines

Example:
```
-Drevision=0.0.1
-Dsha1=
-Dchangelist=-SNAPSHOT
```

- revision is for the semantic version, changed on release when we PR to master
- sha1 to be populated by CI for current commit
- changelist is always snapshot locally, CI will remove it for releases
- hyphens need to be added here in the same property they preceed.

