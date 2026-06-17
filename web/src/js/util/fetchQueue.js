/** @type {number} */
const maxConcurrent = Number(import.meta.env.VITE_MAP_TILE_CONCURRENCY) || 6;

/** @type {number} */
let active = 0;

/** @type {Array<() => void>} */
const queue = [];

/**
 * @param {string} url
 * @param {RequestInit} [init]
 * @returns {Promise<Response>}
 */
export function queuedFetch(url, init) {
    return new Promise((resolve, reject) => {
        const run = () => {
            active++;
            fetch(url, init)
                .then(resolve)
                .catch(reject)
                .finally(() => {
                    active--;
                    const next = queue.shift();
                    if (next) {
                        next();
                    }
                });
        };

        if (active < maxConcurrent) {
            run();
        } else {
            queue.push(run);
        }
    });
}
