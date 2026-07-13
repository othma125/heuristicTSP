// Author: Othmane

const $ = id => document.getElementById(id);
let tour = null;   // array of node ids (1-based) in visit order
let coords = null; // {id:[x,y]}
let vizShown = false; // the map appears only after the first Visualize click

// theme (dark default, persisted)
if (localStorage.theme) document.documentElement.dataset.theme = localStorage.theme;
$("theme").onclick = () => {
  const t = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
  document.documentElement.dataset.theme = t; localStorage.theme = t;
};

async function json(url) { return (await fetch(url)).json(); }

async function loadInstances() {
  const insts = await json("/api/instances");
  $("instance").innerHTML = insts.map(i => `<option>${i}</option>`).join("");
  await showInstance();
}
$("instance").onchange = showInstance;

async function loadCoords() {
  const file = $("instance").value;
  if (!file) return null;
  const text = await (await fetch(`/api/tsp?file=${encodeURIComponent(file)}`)).text();
  return parseCoords(text);
}
// draw the raw city scatter so the map is never empty (the hero, pre-solve)
async function showInstance() {
  if (running) return;
  coords = await loadCoords();
  tour = null; $("viz").disabled = true;
  if (vizShown) drawTour(coords, null);
}

let running = false;
function setRunning(on) {
  running = on;
  $("solve").textContent = on ? "Stop" : "Solve";
  $("solve").style.background = $("solve").style.borderColor = on ? "var(--danger)" : "var(--accent2)";
  $("instance").disabled = on;
}

$("solve").onclick = () => {
  if (running) { $("solve").disabled = true; fetch("/api/stop"); return; } // let the final result arrive over SSE
  const file = $("instance").value;
  const log = $("log"); log.textContent = ""; $("stats").textContent = "";
  $("solText").textContent = "";
  $("save").disabled = true; $("viz").disabled = true;
  tour = null;
  if (vizShown) drawTour(coords, null); // keep the scatter visible while solving
  setRunning(true);
  const es = new EventSource(`/api/solve?file=${encodeURIComponent(file)}`);
  es.addEventListener("log", e => { log.textContent += e.data + "\n"; log.scrollTop = log.scrollHeight; });
  es.addEventListener("result", e => {
    es.close(); setRunning(false); $("solve").disabled = false;
    const r = JSON.parse(e.data);
    if (r.tour && r.tour.length) {
      tour = r.tour; $("viz").disabled = false;
      if (vizShown) drawTour(coords, tour);
      const stat = (k, v, cls = "") => `<div class="stat"><span class="k">${k}</span><span class="v ${cls}">${v}</span></div>`;
      let html = stat("Length", r.cost) + stat("Cities", r.tour.length) + stat("Time", r.timeMs + " ms");
      if (r.optimal != null) html += stat("Best known", r.optimal);
      if (r.gap != null) html += stat("Gap", r.gap + "%", parseFloat(r.gap) <= 0.01 ? "good" : "warn");
      $("stats").innerHTML = html;
    } else {
      $("stats").innerHTML = `<span style="color:var(--danger)">No solution found.</span>`;
    }
  });
  es.addEventListener("sol", e => { $("solText").textContent = e.data; $("save").disabled = false; });
  es.onerror = () => { es.close(); setRunning(false); $("solve").disabled = false; };
};

$("save").onclick = async () => {
  const file = $("instance").value, text = $("solText").textContent;
  if (window.showSaveFilePicker) { // native "Save As" dialog, opens at Desktop
    try {
      const handle = await window.showSaveFilePicker({
        suggestedName: file + ".tour", startIn: "desktop",
        types: [{ description: "Tour file", accept: { "text/plain": [".tour"] } }],
      });
      const w = await handle.createWritable(); await w.write(text); await w.close();
      $("stats").innerHTML += ` &nbsp; <b>Saved:</b> ${handle.name}`;
    } catch (e) { if (e.name !== "AbortError") $("stats").innerHTML += ` &nbsp; <span style="color:var(--danger)">Save failed</span>`; }
    return;
  }
  const a = document.createElement("a"); // fallback (Firefox/Safari): plain download
  a.href = URL.createObjectURL(new Blob([text], { type: "text/plain" }));
  a.download = file + ".tour"; a.click(); URL.revokeObjectURL(a.href);
};

// save the rendered map as an image; the extension is chosen on the page (#imgExt),
// so the "Save As" dialog gets a single matching type (Windows only shows the first reliably)
const MIME = { jpg: "image/jpeg", png: "image/png", webp: "image/webp" };

// composite the canvas onto the theme background (JPEG/WebP have no transparency)
function exportBlob(mime) {
  const src = $("canvas"), out = document.createElement("canvas");
  out.width = src.width; out.height = src.height;
  const ctx = out.getContext("2d");
  ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue("--bg").trim() || "#fff";
  ctx.fillRect(0, 0, out.width, out.height);
  ctx.drawImage(src, 0, 0);
  return new Promise(r => out.toBlob(r, mime, 0.95));
}

$("saveImg").onclick = async () => {
  const ext = $("imgExt").value, mime = MIME[ext];
  const name = ($("instance").value || "tsp") + "_tour." + ext;
  if (window.showSaveFilePicker) { // native "Save As" dialog, lets the user pick the folder
    try {
      const handle = await window.showSaveFilePicker({
        suggestedName: name, startIn: "desktop",
        types: [{ description: name, accept: { [mime]: ["." + ext] } }],
      });
      const w = await handle.createWritable(); await w.write(await exportBlob(mime)); await w.close();
      $("stats").innerHTML += ` &nbsp; <b>Saved:</b> ${handle.name}`;
    } catch (e) { if (e.name !== "AbortError") $("stats").innerHTML += ` &nbsp; <span style="color:var(--danger)">Save failed</span>`; }
    return;
  }
  const a = document.createElement("a"); // fallback (Firefox/Safari): plain download
  a.href = URL.createObjectURL(await exportBlob(mime)); a.download = name; a.click(); URL.revokeObjectURL(a.href);
};

// enabled only once a tour exists; first click reveals the map
$("viz").onclick = () => { vizShown = true; $("saveBar").style.display = "block"; drawTour(coords, tour); };

// parse TSPLIB NODE_COORD_SECTION -> {id:[x,y]}
function parseCoords(text) {
  const coords = {}; let inSection = false;
  for (const line of text.split("\n")) {
    const t = line.trim();
    if (/^NODE_COORD_SECTION/i.test(t)) { inSection = true; continue; }
    if (inSection) {
      if (/^[A-Z_]+/i.test(t) && !/^\d/.test(t)) break; // next section (EOF, EDGE_WEIGHT_SECTION, ...)
      const p = t.split(/\s+/);
      if (p.length >= 3) coords[+p[0]] = [+p[1], +p[2]];
    }
  }
  return coords;
}

// draw the cities, and — when a tour is given — the closed Hamiltonian cycle through them
function drawTour(coords, tourIds) {
  const cv = $("canvas"); cv.style.display = "block";
  const ctx = cv.getContext("2d");
  const dpr = window.devicePixelRatio || 1;
  const cssW = cv.clientWidth || 1040, cssH = 560;
  cv.width = cssW * dpr; cv.height = cssH * dpr; cv.style.height = cssH + "px"; // crisp on HiDPI
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0); ctx.clearRect(0, 0, cssW, cssH);

  const ids = Object.keys(coords || {}).map(Number);
  if (!ids.length) { // explicit-weight instance: no coordinates to plot
    ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue("--muted");
    ctx.font = "14px system-ui"; ctx.fillText("This instance has no coordinates to visualize.", 20, 30);
    return;
  }
  const xs = ids.map(i => coords[i][0]), ys = ids.map(i => coords[i][1]);
  const minX = Math.min(...xs), maxX = Math.max(...xs), minY = Math.min(...ys), maxY = Math.max(...ys);
  const pad = 30, W = cssW - 2 * pad, H = cssH - 2 * pad;
  const s = Math.min(W / ((maxX - minX) || 1), H / ((maxY - minY) || 1));
  const tx = x => pad + (x - minX) * s;
  const ty = y => cssH - pad - (y - minY) * s; // flip Y

  const css = getComputedStyle(document.documentElement);
  if (tourIds && tourIds.length) {
    ctx.strokeStyle = css.getPropertyValue("--accent"); ctx.lineWidth = 1.4; ctx.beginPath();
    const first = coords[tourIds[0]];
    ctx.moveTo(tx(first[0]), ty(first[1]));
    for (const id of tourIds) { const c = coords[id]; if (c) ctx.lineTo(tx(c[0]), ty(c[1])); }
    ctx.lineTo(tx(first[0]), ty(first[1])); ctx.stroke(); // close the cycle
  }
  // cities
  ctx.fillStyle = css.getPropertyValue("--muted");
  for (const id of ids) { const c = coords[id]; ctx.beginPath(); ctx.arc(tx(c[0]), ty(c[1]), 2.5, 0, 7); ctx.fill(); }
  // start city
  if (tourIds && tourIds.length) {
    const c = coords[tourIds[0]];
    ctx.fillStyle = css.getPropertyValue("--teal"); ctx.beginPath(); ctx.arc(tx(c[0]), ty(c[1]), 5, 0, 7); ctx.fill();
  }
}

loadInstances();
