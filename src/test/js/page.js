// Helper for testing the page scripts in src/main/resources/static/js with Jasmine + jsdom.
//
// Each call builds a fresh simulated browser page (jsdom) from the real Thymeleaf template, not
// from a hand-copied HTML fixture, so a test breaks if someone renames an id the script depends on.
// A fresh page per test also means no listeners or state leak from one test to the next.
const fs = require("fs");
const path = require("path");
const { JSDOM } = require("jsdom");

const TEMPLATES = path.resolve(__dirname, "../../main/resources/templates");
const SCRIPTS = path.resolve(__dirname, "../../main/resources/static/js");

/**
 * Loads a template as a page, e.g. loadPage("shops/new.html"), and returns its window and document.
 *
 * Thymeleaf doesn't run, so th:* attributes are ignored: every th:if block is shown, and rows
 * repeated with th:each are removed, so lists start empty as on a fresh form.
 * The page's own <script> tags don't run; call runScript to run the script under test.
 */
function loadPage(templatePath) {
    const html = fs.readFileSync(path.join(TEMPLATES, templatePath), "utf8");
    const dom = new JSDOM(html, { runScripts: "outside-only" });
    const { window } = dom;
    window.document.querySelectorAll("[th\\:each]").forEach((row) => row.remove());

    return {
        window,
        document: window.document,
        /** Runs a page script inside this page, as the browser does once the page has loaded. */
        runScript(scriptName) {
            const source = fs.readFileSync(path.join(SCRIPTS, scriptName), "utf8");
            window.eval(source);
        },
    };
}

module.exports = { loadPage };
