/* ani resolver. ALL site-specific logic lives in this file (urls, regexes, keys, markup).
 * Fix breakage by editing only this file; the app loads it from RESOLVER_URL, then the cached copy, then the bundled one.
 *
 * Host contract (injected by the app before this file runs):
 *   http.get(url, headers)  -> Promise<{status:number, body:string, url:string}>   (body is text)
 *   util.b64decode(str)     -> number[] (bytes)
 *   util.utf8(bytes)        -> string
 *   util.xor(bytes, keyBytes) -> number[]  (key repeats)
 *
 * Exports (globals): version, search(query, mode), episodes(showId, mode), sources(showId, ep, mode),
 *   info(title), findSubs({title, season, ep, count, offset}), toVtt(text)
 * Every function returns a plain JSON-able value; the app JSON.stringify()s it.
 */
var version = 1;

var BASE = 'https://hianime.at';
var UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';
var XOR_KEY = 'otaku-embed-v1';
var CINE = 'https://v3-cinemeta.strem.io';
var OSUB = 'https://opensubtitles-v3.strem.io';

/* ---------------- helpers ---------------- */
function hdr(extra) {
  var h = { 'User-Agent': UA };
  if (extra) for (var k in extra) h[k] = extra[k];
  return h;
}
async function getText(url, headers) {
  var r = await http.get(url, hdr(headers));
  if (r.status < 200 || r.status >= 300) {
    if (/Just a moment/i.test(r.body || '')) throw new Error('Blocked by Cloudflare');
    throw new Error('HTTP ' + r.status + ' from ' + url);
  }
  if (/<title>Just a moment/i.test(r.body || '')) throw new Error('Blocked by Cloudflare');
  return r.body || '';
}
function decodeEntities(s) {
  return String(s).replace(/&#039;/g, "'").replace(/&quot;/g, '"').replace(/&amp;/g, '&');
}
function origin(u) {
  var m = /^(https?:\/\/[^\/]*)/.exec(u);
  return m ? m[1] : '';
}
function dirOf(u) {
  return u.replace(/[?#].*$/, '').replace(/\/[^\/]*$/, '');
}
function absUrl(rel, base) {
  if (/^https?:\/\//i.test(rel)) return rel;
  if (rel.charAt(0) === '/') return origin(base) + rel;
  return dirOf(base) + '/' + rel;
}
function keyBytes(s) {
  var o = [];
  for (var i = 0; i < s.length; i++) o.push(s.charCodeAt(i));
  return o;
}
var idNum0 = function (id) { var m = /-(\d+)$/.exec(String(id)); return m ? +m[1] : 0; };

/* ---------------- search ---------------- */
async function search(query, mode) {
  var q = encodeURIComponent(String(query || '').trim()).replace(/%20/g, '+');
  var page = await getText(BASE + '/search?keyword=' + q);
  var cut = page.indexOf('id="main-sidebar"');
  if (cut >= 0) page = page.slice(0, cut);
  var parts = page.replace(/\n/g, ' ').split('<div class="film-detail">');
  var results = [];
  for (var i = 1; i < parts.length; i++) {
    var m = /<h3 class="film-name">\s*<a href="[^"]*\/([^"\/]*)"\s*title="([^"]*)"/.exec(parts[i]);
    if (!m) continue;
    var thumb = '';
    var re = /(?:data-src|src)="(https?:[^"]+)"/g, x, last = null;
    while ((x = re.exec(parts[i - 1])) !== null) last = x[1];
    if (last) thumb = last.replace(/&amp;/g, '&');
    results.push({ id: m[1], name: decodeEntities(m[2]), thumb: thumb });
  }
  return { results: results, groups: group(results) };
}

/* ---------------- episodes ---------------- */
var EPMAP = {}; // showId -> [{number, epId}]
async function episodes(showId, mode) {
  var num = String(showId).replace(/^.*-/, '');
  var page = await getText(BASE + '/api/theme/episode/list/' + num);
  var chunks = page.replace(/\\/g, '').split('ep-item');
  var out = [], seen = {};
  var needle = '/watch/' + showId;
  for (var i = 1; i < chunks.length; i++) {
    var c = chunks[i].replace(/\n/g, ' ');
    var n = /data-number="([^"]*)"/.exec(c), d = /data-id="([0-9]+)"/.exec(c);
    if (!n || !d || c.indexOf(needle) < 0) continue;
    if (seen[n[1]]) continue;
    seen[n[1]] = 1;
    out.push({ number: n[1], epId: d[1] });
  }
  out.sort(function (a, b) { return parseFloat(a.number) - parseFloat(b.number); });
  EPMAP[showId] = out;
  return out;
}

/* ---------------- sources ---------------- */
function deobfuscate(blob) {
  var bytes = util.xor(util.b64decode(blob), keyBytes(XOR_KEY));
  return util.utf8(bytes);
}
async function sources(showId, ep, mode) {
  mode = mode === 'dub' ? 'dub' : 'sub';
  if (!EPMAP[showId]) await episodes(showId, mode);
  var hit = (EPMAP[showId] || []).filter(function (e) { return e.number === String(ep); })[0];
  if (!hit) throw new Error('Episode ' + ep + ' not found');
  var servers = (await getText(BASE + '/api/theme/episode/servers?episodeId=' + hit.epId))
    .replace(/\\"/g, '"').split('server-item');
  var hash = '';
  for (var i = 1; i < servers.length; i++) {
    var s = servers[i].replace(/\n/g, ' ');
    var m = /data-type="([^"]*)".*data-server-name="ZokoAnime".*data-hash="([^"]*)"/.exec(s);
    if (m && m[1] === mode) { hash = m[2]; break; }
  }
  if (!hash) throw new Error('No ' + mode + ' source for episode ' + ep);
  var embed = util.utf8(util.b64decode(hash)).trim();
  if (!embed) throw new Error('Empty embed url');
  var referer = origin(embed) + '/';
  var malM = /\/mal\/([0-9]+)\//.exec(embed);
  var emb = await getText(embed);
  var bm = /window\.__P="([^"]*)"/.exec(emb);
  if (!bm) throw new Error('Embed page changed (no __P blob)');
  var json = deobfuscate(bm[1]);
  var mm = /"src":"([^"]*\.m3u8[^"]*)"/.exec(json);
  if (!mm) throw new Error('No m3u8 in embed data');
  var master = mm[1].replace(/\\\//g, '/');
  var subtitles = [];
  try {
    var data = JSON.parse(json);
    var walk = function (o) {
      if (!o || typeof o !== 'object') return;
      if (Array.isArray(o.subtitles)) {
        o.subtitles.forEach(function (t) {
          if (t && t.src) subtitles.push({ lang: t.lang || t.label || 'Default', label: t.label || t.lang || 'Default', url: t.src, def: !!t['default'] });
        });
        return;
      }
      for (var k in o) walk(o[k]);
    };
    walk(data);
  } catch (e) { /* subtitles stay empty */ }
  // default subtitle first
  subtitles.sort(function (a, b) { return (b.def ? 1 : 0) - (a.def ? 1 : 0); });

  var pl = await getText(master, { Referer: referer });
  var lines = pl.split(/\r?\n/), qualities = [], audio = '';
  for (var j = 0; j < lines.length; j++) {
    var L = lines[j];
    if (L.indexOf('#EXT-X-STREAM-INF') === 0) {
      var res = /RESOLUTION=\d+x(\d+)/.exec(L);
      var uri = '';
      for (var k = j + 1; k < lines.length; k++) { if (lines[k] && lines[k].charAt(0) !== '#') { uri = lines[k].trim(); break; } }
      if (uri) qualities.push({ label: (res ? res[1] : '0') + 'p', height: res ? +res[1] : 0, url: absUrl(uri, master) });
    } else if (L.indexOf('#EXT-X-MEDIA:TYPE=AUDIO') === 0 && /DEFAULT=YES/.test(L) && !audio) {
      var am = /URI="([^"]*)"/.exec(L);
      if (am) audio = absUrl(am[1], master);
    }
  }
  qualities.sort(function (a, b) { return b.height - a.height; });
  return {
    m3u8: master,
    referer: referer,
    headers: { 'Referer': referer, 'User-Agent': UA },
    subtitles: subtitles,
    audio: audio || undefined,
    qualities: qualities,
    malId: malM ? malM[1] : ''
  };
}

/* ---------------- extra metadata + subtitles (Cinemeta / OpenSubtitles via Stremio addons) ---------------- */
var LANG = { eng: 'English', ara: 'Arabic', fre: 'French', fra: 'French', spa: 'Spanish', spl: 'Spanish (LatAm)', por: 'Portuguese', pob: 'Portuguese (Brazil)', ger: 'German', deu: 'German', ita: 'Italian', rus: 'Russian', tur: 'Turkish', jpn: 'Japanese', kor: 'Korean', chi: 'Chinese', zht: 'Chinese (Traditional)', zhc: 'Chinese (Simplified)', zho: 'Chinese', hin: 'Hindi', ind: 'Indonesian', vie: 'Vietnamese', tha: 'Thai', pol: 'Polish', dut: 'Dutch', nld: 'Dutch', swe: 'Swedish', nor: 'Norwegian', dan: 'Danish', fin: 'Finnish', ell: 'Greek', gre: 'Greek', heb: 'Hebrew', per: 'Persian', fas: 'Persian', rum: 'Romanian', ron: 'Romanian', hun: 'Hungarian', cze: 'Czech', ces: 'Czech', ukr: 'Ukrainian', bul: 'Bulgarian', hrv: 'Croatian', srp: 'Serbian', may: 'Malay', msa: 'Malay', ben: 'Bengali', urd: 'Urdu', kur: 'Kurdish', cat: 'Catalan', fil: 'Filipino', tgl: 'Filipino' };
var MEM = {};
function normT(s) { return String(s).toLowerCase().replace(/[^a-z0-9À-￿]+/g, ' ').trim(); }
async function jget(url) {
  var r = await http.get(url, hdr());
  if (r.status < 200 || r.status >= 300) throw new Error('HTTP ' + r.status);
  return JSON.parse(r.body);
}
async function imdbFor(title) {
  var k = 's-' + normT(title);
  if (MEM[k] !== undefined) return MEM[k];
  var d = await jget(CINE + '/catalog/series/top/search=' + encodeURIComponent(title) + '.json');
  var ms = (d.metas || []).filter(function (m) { return m && m.imdb_id; }), t = normT(title);
  var best = ms.filter(function (m) { return normT(m.name) === t; })[0] ||
    ms.filter(function (m) { return normT(m.name).indexOf(t) === 0; })[0] || ms[0];
  return (MEM[k] = best ? { id: best.imdb_id, name: best.name } : null);
}
async function meta(imdb) {
  var k = 'm-' + imdb;
  if (MEM[k]) return MEM[k];
  var d = (await jget(CINE + '/meta/series/' + imdb + '.json')).meta || {};
  var seasons = {}, eps = {};
  (d.videos || []).forEach(function (v) {
    var s = +v.season, e = +(v.episode != null ? v.episode : v.number);
    if (!(s > 0) || !(e > 0)) return;
    seasons[s] = (seasons[s] || 0) + 1;
    (eps[s] = eps[s] || {})[e] = { t: v.title || v.name || '', th: v.thumbnail || '', ov: v.overview || '' };
  });
  return (MEM[k] = { id: imdb, name: d.name || '', year: d.year || d.releaseInfo || '', rating: d.imdbRating || '', desc: d.description || '', genres: d.genres || [], runtime: d.runtime || '', bg: d.background || '', seasons: seasons, eps: eps });
}
async function info(title) {
  try {
    var f = await imdbFor(title);
    return f ? await meta(f.id) : {};
  } catch (e) { return {}; }
}
function pickSeason(m, season, count) {
  if (season > 0) return season;
  var c = Object.keys(m.seasons).filter(function (s) { return +s > 0 && m.seasons[s] === count; });
  return c.length === 1 ? +c[0] : 0;
}
async function findSubs(o) {
  try {
    var m = await info(o.title);
    if (!m || !m.id) return { subs: [], reason: 'Show not found' };
    var s = pickSeason(m, +o.season || 0, +o.count || 0);
    if (!s) return { subs: [], reason: 'Could not tell which season this is' };
    var e = (+o.ep || 0) + (+o.offset || 0);
    if (!(e > 0)) return { subs: [], reason: 'Special episodes are not supported' };
    var d = await jget(OSUB + '/subtitles/series/' + m.id + ':' + s + ':' + e + '.json');
    var by = {}, order = [];
    (d.subtitles || []).forEach(function (t) {
      if (!t || !t.url || !t.lang) return;
      if (!by[t.lang]) { by[t.lang] = { lang: t.lang, label: LANG[t.lang] || String(t.lang).toUpperCase(), count: 0, url: t.url }; order.push(t.lang); }
      by[t.lang].count++;
    });
    var subs = order.map(function (k) { return by[k]; }).sort(function (a, b) {
      return (a.lang === 'ara' ? -1 : b.lang === 'ara' ? 1 : 0) || (a.lang === 'eng' ? -1 : b.lang === 'eng' ? 1 : 0) || a.label.localeCompare(b.label);
    });
    return { subs: subs, season: s, episode: e, imdb: m.id };
  } catch (err) { return { subs: [], reason: 'Subtitle service unreachable' }; }
}
function toVtt(txt) {
  txt = String(txt).replace(/^﻿/, '').replace(/\r\n?/g, '\n').trim();
  if (/^WEBVTT/.test(txt)) return txt + '\n';
  return 'WEBVTT\n\n' + txt.replace(/(\d{1,2}:\d{2}:\d{2}),(\d{1,3})/g, '$1.$2') + '\n';
}

/* ---------------- season grouping (ported from group.js) ---------------- */
var STOP = { the: 1, a: 1, an: 1, of: 1, my: 1, no: 1, to: 1, and: 1, 'in': 1, on: 1, wa: 1, ga: 1, is: 1, 'for': 1 };
var TYPEKW = /\b(movie|movies|film|ova|oad|ona|special|specials|recap|recaps|pv|pvs|arc|arcs)\b/i;
var ORD = { first: 1, second: 2, third: 3, fourth: 4, fifth: 5, sixth: 6, seventh: 7, eighth: 8, ninth: 9, tenth: 10 };
var ROMAN = { ii: 2, iii: 3, iv: 4, v: 5, vi: 6, vii: 7, viii: 8 };
function nw(w) { return w.toLowerCase().replace(/[^a-z0-9À-￿]+/g, ''); }
function parseTitle(name) {
  var s = String(name).replace(/\((tv|tv series)\)/ig, ' ').replace(/\s+tv$/i, '').replace(/[–—]/g, ' - ');
  var toks = s.split(/\s+/).map(function (raw) { return { raw: raw, n: nw(raw) }; }).filter(function (t) { return t.n; });
  var season = 0, part = 0, fin = false, type = TYPEKW.test(s);
  var n = toks.map(function (t) { return t.n; }), drop = {};
  var mark = function (i, len) { len = len || 1; for (var k = 0; k < len; k++) drop[i + k] = 1; };
  for (var i = 0; i < n.length; i++) {
    var w = n[i], nx = n[i + 1], pv = n[i - 1];
    if (w === 'season' && /^\d+$/.test(nx || '')) { season = +nx; mark(i, 2); }
    else if (w === 'season' && ORD[nx]) { season = ORD[nx]; mark(i, 2); }
    else if (w === 'season' && ORD[pv] && !drop[i - 1]) { season = ORD[pv]; mark(i - 1, 2); }
    else if (w === 'season' && /^\d+(st|nd|rd|th)$/.test(pv || '') && !drop[i - 1]) { season = parseInt(pv, 10); mark(i - 1, 2); }
    else if (w === 'season' && pv === 'final') { fin = true; mark(i - 1, 2); if (n[i - 2] === 'the') mark(i - 2); }
    else if (/^\d+(st|nd|rd|th)$/.test(w) && nx === 'season') { }
    else if (w === 'part' && /^\d+$/.test(nx || '')) { part = +nx; mark(i, 2); }
    else if (w === 'cour' && /^\d+$/.test(nx || '')) { part = +nx; mark(i, 2); }
  }
  if (!season && !fin && !type) {
    var L = n.length - 1, lw = n[L];
    if (L > 0 && !drop[L]) {
      if (ROMAN[lw]) { season = ROMAN[lw]; mark(L); }
      else if (/^[2-9]$/.test(lw) && !/^\d+$/.test(n[L - 1])) { season = +lw; mark(L); }
    }
  }
  var core = toks.filter(function (_, i) { return !drop[i]; });
  return { core: core, season: season, part: part, fin: fin, type: type };
}
function keyOf(ts) { return ts.map(function (t) { return t.n; }).join(' '); }
function meaningful(ts) { return ts.some(function (t) { return !STOP[t.n]; }) && keyOf(ts).length >= 5; }
function cleanToks(ts) {
  return ts.map(function (t) { return t.raw; }).join(' ').replace(/^[\s:,\-–]+|[\s:,\-–]+$/g, '').replace(/\s*[:,]\s*/g, ': ').replace(/^:\s*/, '');
}
function isPrefix(a, b) { return a.length <= b.length && a.every(function (t, i) { return t.n === b[i].n; }); }

function group(results) {
  var items = results.map(function (r, i) { return { id: r.id, name: r.name, thumb: r.thumb, i: i, p: parseTitle(r.name) }; });
  var roots = [];
  var sorted = items.slice().sort(function (a, b) { return a.p.core.length - b.p.core.length || a.i - b.i; });
  var assign = new Map();
  sorted.forEach(function (it) {
    var c = it.p.core, root = null;
    for (var r = 0; r < roots.length; r++) if (isPrefix(roots[r].toks, c)) { root = roots[r]; break; }
    if (!root) {
      for (var oi = 0; oi < sorted.length; oi++) {
        var o = sorted[oi];
        if (o === it || !assign.has(o)) continue;
        var k = 0, oc = o.p.core;
        while (k < c.length && k < oc.length && c[k].n === oc[k].n) k++;
        if (k >= 2 && meaningful(c.slice(0, k))) {
          var toks = c.slice(0, k);
          root = roots.filter(function (r2) { return isPrefix(r2.toks, toks) && r2.toks.length === toks.length; })[0] || null;
          if (!root) {
            root = { toks: toks, items: [] };
            roots.push(root);
            var old = assign.get(o);
            if (old !== root) { old.items = old.items.filter(function (x) { return x !== o; }); root.items.push(o); assign.set(o, root); }
          }
          break;
        }
      }
    }
    if (!root) { root = { toks: c.length ? c : [{ raw: it.name, n: nw(it.name) }], items: [] }; roots.push(root); }
    root.items.push(it);
    assign.set(it, root);
  });
  var groups = [];
  roots.forEach(function (root) {
    if (!root.items.length) return;
    var rk = keyOf(root.toks), its = root.items;
    its.forEach(function (it) {
      var rem = it.p.core.slice(root.toks.length);
      it.rem = rem; it.remKey = keyOf(rem);
      it.extra = it.p.type || (rem.length > 0 && !it.p.season && !it.p.fin && !it.p.part);
    });
    var s1 = its.filter(function (x) { return !x.p.season && !x.p.fin && !x.p.part && !x.p.type && x.rem.length === 0; })[0];
    if (!s1) {
      var cand = its.filter(function (x) { return !x.p.season && !x.p.fin && !x.p.type && !TYPEKW.test(x.name) && !/\barc\b/i.test(x.name); });
      cand.sort(function (a, b) { return keyOf(a.p.core).length - keyOf(b.p.core).length || idNum0(a.id) - idNum0(b.id); });
      s1 = cand[0];
    }
    var s1rem = s1 ? s1.rem.map(function (t) { return t.n; }) : [];
    if (s1rem.length) its.forEach(function (it) {
      if (it !== s1 && it.rem.length > s1rem.length && s1rem.every(function (w, k) { return it.rem[k].n === w; })) it.rem = it.rem.slice(s1rem.length);
    });
    var maxNum = Math.max.apply(null, [1].concat(its.map(function (x) { return x.p.season || 0; })));
    var seasons = its.map(function (it) {
      var kind, num = 0;
      if (it === s1) { kind = 'season'; num = 1; }
      else if (it.p.type) kind = 'other';
      else if (it.p.fin) { kind = 'season'; num = maxNum + 1; }
      else if (it.p.season) { kind = 'season'; num = it.p.season; }
      else if (it.p.part && it.rem.length === 0) { kind = 'season'; num = 1; }
      else kind = 'other';
      return { id: it.id, name: it.name, thumb: it.thumb, kind: kind, num: num, part: it.p.part || 0, fin: it.p.fin, rem: it.rem, type: it.p.type, i: it.i };
    });
    var bynum = {};
    seasons.filter(function (s) { return s.kind === 'season'; }).forEach(function (s) { (bynum[s.num] = bynum[s.num] || []).push(s); });
    seasons.forEach(function (s) {
      if (s.kind === 'season') {
        var multi = bynum[s.num].length > 1;
        s.label = (s.fin ? 'Final Season' : 'Season ' + s.num) + (s.part && multi ? ' · Part ' + s.part : '');
      } else s.label = cleanToks(s.rem) || s.name;
      if (s.label.length > 34) s.label = s.label.slice(0, 33) + '…';
    });
    seasons.sort(function (a, b) {
      var ka = a.kind === 'season' ? 0 : a.type ? 2 : 1, kb = b.kind === 'season' ? 0 : b.type ? 2 : 1;
      return ka - kb || (a.num - b.num) || (a.part - b.part) || (idNum0(a.id) - idNum0(b.id)) || a.i - b.i;
    });
    var lead = seasons.filter(function (s) { return s.kind === 'season'; })[0] || seasons[0];
    var base = (lead.kind === 'season' && s1 && s1.id === lead.id) ? cleanToks(s1.p.core) : cleanToks(root.toks.map(function (t, i) { return its[0].p.core[i] || t; }));
    var withThumb = seasons.filter(function (s) { return s.thumb; })[0];
    groups.push({
      key: rk.replace(/\s+/g, '-'),
      name: base || lead.name,
      thumb: lead.thumb || (withThumb ? withThumb.thumb : ''),
      seasons: seasons.map(function (s) { return { id: s.id, name: s.name, thumb: s.thumb, kind: s.kind, num: s.num, part: s.part, label: s.label, fin: !!s.fin, type: !!s.type }; }),
      first: Math.min.apply(null, its.map(function (x) { return x.i; }))
    });
  });
  return groups.sort(function (a, b) { return a.first - b.first; });
}
