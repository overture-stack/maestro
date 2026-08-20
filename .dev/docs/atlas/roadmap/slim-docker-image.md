# Slim the published Docker image

The server stage currently uses `COPY --from=prod-deps ${WORKDIR} .`, which pulls in TypeScript source files, the pnpm lockfile, and workspace config that Node.js does not need at runtime. Now that the CMD uses `node` directly (no pnpm), these can be excluded.

## Changes required

- Replace the full-workdir COPY with selective COPYs: `node_modules/`, `packages/*/dist/`, `packages/*/package.json`, `apps/server/package.json`
- Drop `corepack prepare pnpm@...` from the server base stage (pnpm is no longer needed in the final image)
- Pin or bump `node:22-alpine` to address the high vulnerability flagged by the image scanner

## Notes

- Workspace symlinks in `node_modules/@overture-stack/` point to `../../packages/*/`; those target directories must still exist in the final image (dist/ and package.json only)
- Verify with `docker image inspect` before and after to confirm size reduction
