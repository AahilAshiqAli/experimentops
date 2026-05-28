/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: '#0f766e',
        secondary: '#f59e0b',
        background: '#020617',
        surface: '#0f172a',
        muted: '#334155',
      },
      boxShadow: {
        glow: '0 0 0 1px rgba(15, 118, 110, 0.2), 0 20px 40px -12px rgba(15, 118, 110, 0.35)',
      },
    },
  },
  plugins: [],
}
