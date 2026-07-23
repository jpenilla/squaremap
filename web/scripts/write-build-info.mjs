import { writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const buildInfoPath = join(__dirname, "../data/weiran-gis/build.json");

/** @param {Date} [date] */
function formatLocalDate(date = new Date()) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

const builtAt = formatLocalDate();

writeFileSync(
    buildInfoPath,
    `${JSON.stringify({ builtAt }, null, 4)}\n`,
    "utf8",
);

console.log(`[write-build-info] builtAt=${builtAt} → ${buildInfoPath}`);
