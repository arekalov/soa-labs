import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  // Приложение отдаётся nginx из подкаталога public_html, поэтому пути
  // к ассетам относительные — иначе они бы вели в корень se.ifmo.ru.
  base: './',
  // Импорты между слоями пишутся от корня src: '@/shared/ui' читается лучше '../../../shared/ui'.
  resolve: { alias: { '@': new URL('./src/', import.meta.url).pathname } },
  server: { port: 5173, open: true },
});
