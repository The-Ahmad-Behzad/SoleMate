# Setup Fix Applied

## Issue
`ts-node-dev` doesn't properly support ES modules (`"type": "module"`), causing the error:
```
Must use import to load ES Module
```

## Solution
Replaced `ts-node-dev` with `tsx` which has better ES module support.

## Changes Made
1. Updated `package.json` scripts:
   - `dev`: Changed from `ts-node-dev --respawn --transpile-only src/index.ts` to `tsx watch src/index.ts`
   - `seed`: Changed from `ts-node-dev --transpile-only src/scripts/seed.ts` to `tsx src/scripts/seed.ts`

2. Updated devDependencies:
   - Removed: `ts-node-dev@^2.0.0`
   - Added: `tsx@^4.19.1`

## Next Steps

1. Install the new dependency:
   ```bash
   cd backend
   npm install
   ```

2. Now you can run:
   ```bash
   npm run seed
   ```

3. The seed script should work properly now!

 rake


