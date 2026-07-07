import 'dotenv/config';

export const CONFIG = {
  port: Number(process.env.PORT) || 4000,
  jwtSecret: process.env.JWT_SECRET || 'dev-secret-change-me',
  jwtExpiresIn: process.env.JWT_EXPIRES_IN || '7d',
  databaseUrl: process.env.DATABASE_URL || '',
  webOrigin: process.env.WEB_ORIGIN || '*',
  appApiKey: process.env.APP_API_KEY || 'app-secret-change-me',
};
