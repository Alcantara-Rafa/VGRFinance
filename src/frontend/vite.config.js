import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// O Vite repassa /api para o Spring: o navegador fala com um endereço só,
// então o cookie de sessão viaja sem configuração de CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173,
    proxy: {
      '/api': process.env.API_URL ?? 'http://localhost:8080',
    },
  },
});
