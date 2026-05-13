const fs = require("fs/promises");
const path = require("path");

const root = path.resolve(__dirname, "..");
const dist = path.join(root, "dist");
const defaultApiBase = "https://predicted-production-3c23.up.railway.app";
const apiBase = (process.env.PREDICTED_API_BASE || defaultApiBase).replace(/\/+$/, "");

async function build() {
  await fs.rm(dist, { recursive: true, force: true });
  await fs.mkdir(path.join(dist, "assets"), { recursive: true });

  const source = await fs.readFile(path.join(root, "PredictED.html"), "utf8");
  const html = source.replaceAll('"__PREDICTED_API_BASE__"', JSON.stringify(apiBase));

  await fs.writeFile(path.join(dist, "index.html"), html);
  await fs.writeFile(path.join(dist, "PredictED.html"), html);

  const referencedSvgAssets = new Set(
    Array.from(html.matchAll(/assets\/([^"')\s?#]+\.svg)/gi), match => match[1])
  );
  await Promise.all(
    Array.from(referencedSvgAssets)
      .map(file => fs.copyFile(
        path.join(root, "assets", file),
        path.join(dist, "assets", file)
      ))
  );
}

build().catch(error => {
  console.error(error);
  process.exit(1);
});
