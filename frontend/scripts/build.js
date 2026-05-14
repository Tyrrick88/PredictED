const fs = require("fs/promises");
const path = require("path");

const root = path.resolve(__dirname, "..");
const dist = path.join(root, "dist");
const defaultApiBase = "https://predicted-production-3c23.up.railway.app";
const apiBase = (process.env.PREDICTED_API_BASE || defaultApiBase).replace(/\/+$/, "");
const demoLoginEnabled = String(process.env.PREDICTED_DEMO_LOGIN_ENABLED || "false").toLowerCase() === "true";
const googleClientId = process.env.PREDICTED_GOOGLE_CLIENT_ID || "";
const appleClientId = process.env.PREDICTED_APPLE_CLIENT_ID || "";
const appleRedirectUri = process.env.PREDICTED_APPLE_REDIRECT_URI || "";

async function build() {
  await fs.rm(dist, { recursive: true, force: true });
  await fs.mkdir(path.join(dist, "assets"), { recursive: true });

  const source = await fs.readFile(path.join(root, "PredictED.html"), "utf8");
  const html = source
    .replaceAll('"__PREDICTED_API_BASE__"', JSON.stringify(apiBase))
    .replaceAll('"__PREDICTED_DEMO_LOGIN_ENABLED__"', JSON.stringify(demoLoginEnabled))
    .replaceAll('"__PREDICTED_GOOGLE_CLIENT_ID__"', JSON.stringify(googleClientId))
    .replaceAll('"__PREDICTED_APPLE_CLIENT_ID__"', JSON.stringify(appleClientId))
    .replaceAll('"__PREDICTED_APPLE_REDIRECT_URI__"', JSON.stringify(appleRedirectUri));

  await fs.writeFile(path.join(dist, "index.html"), html);
  await fs.writeFile(path.join(dist, "PredictED.html"), html);

  const referencedAssets = new Set(
    Array.from(html.matchAll(/assets\/([^"')\s?#]+\.(?:svg|png))/gi), match => match[1])
  );
  await Promise.all(
    Array.from(referencedAssets)
      .map(async file => {
        const target = path.join(dist, "assets", file);
        await fs.mkdir(path.dirname(target), { recursive: true });
        await fs.copyFile(
          path.join(root, "assets", file),
          target
        );
      })
  );
}

build().catch(error => {
  console.error(error);
  process.exit(1);
});
