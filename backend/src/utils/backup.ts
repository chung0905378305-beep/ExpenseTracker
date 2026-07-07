import * as fs from 'fs';
import * as path from 'path';
import { exec } from 'child_process';

const BACKUP_DIR = path.resolve(__dirname, '../../backups');
const DB_PATH = path.resolve(__dirname, '../../prisma/dev.db');

function ensureDir() {
  if (!fs.existsSync(BACKUP_DIR)) {
    fs.mkdirSync(BACKUP_DIR, { recursive: true });
  }
}

export function listBackups(): { name: string; size: number; date: string }[] {
  ensureDir();
  return fs.readdirSync(BACKUP_DIR)
    .filter(f => f.endsWith('.db'))
    .map(f => {
      const full = path.join(BACKUP_DIR, f);
      const stat = fs.statSync(full);
      return { name: f, size: stat.size, date: stat.mtime.toISOString() };
    })
    .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
}

export async function createBackup(): Promise<{ name: string; size: number; path: string }> {
  ensureDir();

  const ts = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
  const name = `backup-${ts}.db`;
  const dest = path.join(BACKUP_DIR, name);

  // SQLite .backup via sqlite3 CLI, or fallback to file copy
  try {
    await execPromise(`sqlite3 "${DB_PATH}" ".backup '${dest}'"`);
  } catch (err: any) {
    // sqlite3 CLI not available, fallback to file copy
    console.warn('sqlite3 CLI not found, using file copy backup. Install sqlite3 for proper .backup.');
    fs.copyFileSync(DB_PATH, dest);
  }

  const stat = fs.statSync(dest);
  return { name, size: stat.size, path: dest };
}

export function deleteBackup(name: string): boolean {
  const dest = path.join(BACKUP_DIR, name);
  if (!fs.existsSync(dest)) return false;
  if (!dest.startsWith(BACKUP_DIR)) return false; // safety check
  fs.unlinkSync(dest);
  return true;
}

export function restoreBackup(name: string): boolean {
  const src = path.join(BACKUP_DIR, name);
  if (!fs.existsSync(src)) return false;
  fs.copyFileSync(src, DB_PATH + '.before-restore');
  fs.copyFileSync(src, DB_PATH);
  return true;
}

export function cleanOldBackups(keepCount: number = 7): number {
  const list = listBackups();
  if (list.length <= keepCount) return 0;
  const toDelete = list.slice(keepCount);
  toDelete.forEach(b => deleteBackup(b.name));
  return toDelete.length;
}

function execPromise(cmd: string): Promise<void> {
  return new Promise((resolve, reject) => {
    exec(cmd, { timeout: 30000 }, (err) => {
      if (err) reject(err);
      else resolve();
    });
  });
}
