module.exports = {
  darkMode: "class",
  content: ["./PredictED.html", "./scripts/**/*.js"],
  safelist: [
    "flex",
    "hidden",
    "opacity-70",
    "text-ink",
    "text-muted",
    "text-cobalt",
    "text-aqua",
    "text-mint",
    "text-gold",
    "text-coral",
    "text-plum",
    "bg-wash",
    "bg-navy",
    "bg-gold",
    "bg-mint",
    "bg-coral",
    {
      pattern: /^(bg|text|border)-(blue|emerald|amber|rose|cyan)-(50|200)$/
    }
  ],
  theme: {
    extend: {
      colors: {
        ink: "#071e27",
        muted: "#536471",
        cloud: "#f6fbff",
        panel: "#ffffff",
        line: "#d6e5ee",
        navy: "#111a5c",
        cobalt: "#1d4ed8",
        aqua: "#0f9fbc",
        mint: "#16a34a",
        gold: "#c97800",
        coral: "#cf3f4f",
        plum: "#6d3bbf",
        wash: "#edf7fb"
      },
      borderRadius: {
        DEFAULT: "0.125rem",
        md: "0.25rem",
        lg: "0.375rem",
        xl: "0.5rem",
        full: "999px"
      },
      boxShadow: {
        panel: "0 12px 36px rgba(7, 30, 39, 0.08)",
        soft: "0 8px 24px rgba(7, 30, 39, 0.06)"
      },
      fontFamily: {
        sans: ["JetBrains Mono", "ui-monospace", "SFMono-Regular", "monospace"],
        mono: ["JetBrains Mono", "ui-monospace", "SFMono-Regular", "monospace"]
      }
    }
  },
  plugins: [
    require("@tailwindcss/forms"),
    require("@tailwindcss/container-queries")
  ]
};
