/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{html,ts}"],
  theme: {
    extend: {
      colors: {
        // Palette reprise du frontend assoue-backend (fond crème, encre, accent cramoisi)
        ink: '#19171b',
        paper: '#fffaf8',
        line: '#eadfe1',
        assoue: {
          orange: '#d7193f',
          vert: '#16a34a',
        },
      },
      fontFamily: {
        sans: ['Manrope', 'sans-serif'],
        display: ['"Space Grotesk"', 'sans-serif'],
        mono: ['"DM Mono"', 'monospace'],
      },
    },
    // On remplace les échelles grises et oranges par défaut de Tailwind par des tons
    // chauds proches de assoue-backend, pour que bg-gray-*, text-gray-*, bg-orange-*,
    // text-orange-*, hover:border-orange-*, hover:text-orange-* changent partout d'un coup.
    colors: ({ colors }) => ({
      ...colors,
      gray: {
        50: '#fefaf8',
        100: '#f7f1ee',
        200: '#eadfe1',
        300: '#dcd0d2',
        400: '#b7abae',
        500: '#948a8d',
        600: '#756d72',
        700: '#5c565a',
        800: '#383336',
        900: '#19171b',
      },
      orange: {
        50: '#fff0f3',
        100: '#ffe1e7',
        200: '#ffc2cf',
        300: '#ff93a8',
        400: '#f25c7c',
        500: '#d7193f',
        600: '#b81336',
        700: '#970f2d',
        800: '#7a0f27',
        900: '#650f23',
      },
    }),
  },
  plugins: [],
}
