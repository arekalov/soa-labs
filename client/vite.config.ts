import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // Приложение отдаётся nginx из подкаталога public_html, поэтому пути
  // к ассетам относительные — иначе они бы вели в корень se.ifmo.ru.
  base: './',
  server: { port: 5173, open: true },
});
