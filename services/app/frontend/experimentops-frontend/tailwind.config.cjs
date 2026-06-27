/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#2596be',
          100: '#2596bf',
          200: '#2596ce',
        },
        secondary: '#314261',
        background: '#f8fafc',
        surface: '#ffffff',
        muted: '#64748b',
      },
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
        heading: ['Georgia', 'serif'],
      },
      boxShadow: {
        card: '0 10px 30px -12px rgb(49 66 97 / 18%)',
        glow: '0 0 0 1px rgb(37 150 190 / 20%), 0 20px 40px -12px rgb(37 150 190 / 35%)',
      },
    },
  },
  plugins: [],
}
