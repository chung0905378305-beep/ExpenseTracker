import { Response } from 'express';

export class HttpError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

export function ok<T>(res: Response, data: T, message = 'ok') {
  return res.json({ code: 0, message, data });
}

export function fail(res: Response, status: number, message: string) {
  return res.status(status).json({ code: status, message });
}
