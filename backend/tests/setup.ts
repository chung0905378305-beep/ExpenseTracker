import 'dotenv/config';

// Use a test-specific database
process.env.DATABASE_URL = process.env.DATABASE_URL || 'file:./test.db';
process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-secret-key-for-vitest';
process.env.APP_API_KEY = process.env.APP_API_KEY || 'ak-test-key-for-vitest-automation';

// Silence console during tests except for errors
const originalLog = console.log;
const originalWarn = console.warn;
console.log = (...args: any[]) => { if (process.env.VERBOSE) originalLog(...args); };
console.warn = (...args: any[]) => { if (process.env.VERBOSE) originalWarn(...args); };
