// npm test and npm test -- --watch share the same Jasmine configuration as CI.
const { spawnSync } = require("node:child_process");
const path = require("node:path");

const args = process.argv.slice(2);
const watchArgs = args.includes("--watch") ? [
    "--watch-path=src/main/resources/static/js",
    "--watch-path=src/main/resources/templates",
    "--watch-path=src/test/js",
] : [];
const result = spawnSync(process.execPath, [
    ...watchArgs,
    path.resolve(__dirname, "../../../node_modules/jasmine/bin/jasmine.js"),
    "--config=jasmine.json",
    ...args.filter((arg) => arg !== "--watch"),
], { stdio: "inherit" });

if (result.error) {
    throw result.error;
}
process.exit(result.status ?? 1);
