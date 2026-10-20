/**
 * Fetches and parses a JSON document, revalidating any cached copy.
 *
 * @param url the document to fetch
 * @returns the parsed document, or null if the request failed or timed out, failed requests are already logged by the browser
 */
export function fetchJSON(url: string): Promise<any> {
    return fetch(url, { cache: "no-cache", signal: AbortSignal.timeout(10_000) })
        .then((res) => (res.ok ? res.json() : null))
        .catch(() => null);
}
