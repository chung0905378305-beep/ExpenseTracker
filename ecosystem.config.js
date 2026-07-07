// PM2 进程管理配置
// 安装：npm i -g pm2
// 启动：pm2 start ecosystem.config.js
// 保存：pm2 save
// 开机自启：pm2 startup
module.exports = {
  apps: [
    {
      name: 'expense-backend',
      cwd: './backend',
      script: 'dist/index.js',
      instances: 1,
      exec_mode: 'fork',
      env: {
        NODE_ENV: 'production',
        PORT: 4000,
      },
      // 自动重启
      max_memory_restart: '200M',
      // 日志
      error_file: './logs/backend-error.log',
      out_file: './logs/backend-out.log',
      merge_logs: true,
      // 定时重启（每天凌晨 4 点）
      cron_restart: '0 4 * * *',
    },
    {
      name: 'expense-backup',
      cwd: './backend',
      script: 'dist/backup-scheduler.js',
      instances: 1,
      exec_mode: 'fork',
      env: {
        NODE_ENV: 'production',
      },
      error_file: './logs/backup-error.log',
      out_file: './logs/backup-out.log',
    },
  ],
};
