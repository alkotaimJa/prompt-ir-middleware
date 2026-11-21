/*
 * Minimal, safe Markdown renderer for LLM responses.
 * All input is HTML-escaped first; only a small whitelist of Markdown
 * constructs is then converted, so model output can never inject markup.
 * Both responses in a pair are rendered identically, so rendering does not
 * bias the comparison.
 */
(function () {
  function esc(s) {
    return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
  }

  function inline(s) {
    // s is already escaped
    const codes = [];
    s = s.replace(/`([^`]+)`/g, (_, c) => { codes.push(c); return "\u0000" + (codes.length - 1) + "\u0000"; });
    s = s.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
         .replace(/__([^_]+)__/g, "<strong>$1</strong>")
         .replace(/(^|[^*])\*([^*\n]+)\*/g, "$1<em>$2</em>")
         .replace(/(^|[\s(])_([^_\n]+)_(?=[\s.,;:!?)]|$)/g, "$1<em>$2</em>");
    s = s.replace(/\u0000(\d+)\u0000/g, (_, i) => "<code>" + codes[+i] + "</code>");
    return s;
  }

  function isTableSep(line) { return /^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)*\|?\s*$/.test(line); }
  function cells(line) { return line.trim().replace(/^\||\|$/g, "").split("|").map(c => inline(c.trim())); }

  function render(md) {
    const lines = esc(md || "").replace(/\r\n?/g, "\n").split("\n");
    const out = [];
    let i = 0;
    while (i < lines.length) {
      let line = lines[i];
      if (/^```/.test(line)) {
        const buf = [];
        i++;
        while (i < lines.length && !/^```/.test(lines[i])) { buf.push(lines[i]); i++; }
        i++;
        out.push("<pre><code>" + buf.join("\n") + "</code></pre>");
        continue;
      }
      const h = line.match(/^(#{1,6})\s+(.*)$/);
      if (h) { const lvl = Math.min(h[1].length + 1, 4); out.push(`<h${lvl}>${inline(h[2])}</h${lvl}>`); i++; continue; }
      if (/^\s*([-*_])\s*\1\s*\1[\s\1]*$/.test(line)) { out.push("<hr>"); i++; continue; }
      if (line.includes("|") && i + 1 < lines.length && isTableSep(lines[i + 1])) {
        const head = cells(line); i += 2;
        const rows = [];
        while (i < lines.length && lines[i].includes("|") && lines[i].trim()) { rows.push(cells(lines[i])); i++; }
        out.push("<table><thead><tr>" + head.map(c => `<th>${c}</th>`).join("") + "</tr></thead><tbody>" +
          rows.map(r => "<tr>" + r.map(c => `<td>${c}</td>`).join("") + "</tr>").join("") + "</tbody></table>");
        continue;
      }
      if (/^\s*([-*+]|\d+[.)])\s+/.test(line)) {
        const ordered = /^\s*\d+[.)]\s+/.test(line);
        const items = [];
        while (i < lines.length && /^\s*([-*+]|\d+[.)])\s+/.test(lines[i])) {
          items.push(inline(lines[i].replace(/^\s*([-*+]|\d+[.)])\s+/, "")));
          i++;
          while (i < lines.length && /^\s{2,}\S/.test(lines[i]) && !/^\s*([-*+]|\d+[.)])\s+/.test(lines[i])) {
            items[items.length - 1] += " " + inline(lines[i].trim()); i++;
          }
        }
        const tag = ordered ? "ol" : "ul";
        out.push(`<${tag}>` + items.map(x => `<li>${x}</li>`).join("") + `</${tag}>`);
        continue;
      }
      if (/^&gt;\s?/.test(line)) {
        const buf = [];
        while (i < lines.length && /^&gt;\s?/.test(lines[i])) { buf.push(lines[i].replace(/^&gt;\s?/, "")); i++; }
        out.push("<blockquote>" + inline(buf.join(" ")) + "</blockquote>");
        continue;
      }
      if (!line.trim()) { i++; continue; }
      const para = [];
      while (i < lines.length && lines[i].trim() && !/^(#{1,6}\s|```|\s*([-*+]|\d+[.)])\s+|&gt;)/.test(lines[i])) {
        para.push(inline(lines[i])); i++;
      }
      if (!para.length) { para.push(inline(lines[i])); i++; } // safety: always make progress
      out.push("<p>" + para.join("<br>") + "</p>");
    }
    return out.join("\n");
  }

  window.renderMarkdown = render;
  window.escapeHtml = esc;
})();
