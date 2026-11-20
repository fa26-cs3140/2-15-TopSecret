"""Builds the UML class diagram (HTML, ready to print to PDF) from the compiled classes.

Usage, from the repository root:
    ./gradlew compileJava
    python3 docs/tools/make_class_diagram.py build/classes/java/main HW7 build/ClassDiagram.html
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless=new \
        --no-pdf-header-footer --print-to-pdf=docs/HW7-ClassDiagram.pdf build/ClassDiagram.html

The class boxes come straight from javap, so they always match the code. The drawn
architecture overview uses the positions in POS; add a class there to draw it.
"""
import html
import pathlib
import re
import subprocess
import sys

root, version, out_path = pathlib.Path(sys.argv[1]), sys.argv[2], pathlib.Path(sys.argv[3])

VIS = {"public": "+", "private": "-", "protected": "#"}
PACKAGES = {
    "org.runtime": "Entry point",
    "org.controller": "Controller: startup, shared control logic, HTTP server, sessions, editing",
    "org.controller.messaging": "Controller: agent messaging",
    "org.view": "View: terminal menus, HTML template and forms",
    "org.view.messaging": "View: terminal message display",
    "org.managers": "Data access: SQLite managers, accounts, audit log, files",
    "org.model": "Model: records and value types",
    "org.model.messaging": "Model: messages and message service",
}


def simplify(text):
    text = re.sub(r"java\.(lang|util|io|net|time|sql|nio\.file|security)\.", "", text)
    text = re.sub(r"(util\.function|com\.sun\.net\.httpserver)\.", "", text)
    return re.sub(r"org\.[a-z.]*\.([A-Z])", r"\1", text).replace("$", ".")


def parse_class(fq):
    text = subprocess.run(["javap", "-p", "-cp", str(root), fq], capture_output=True, text=True).stdout
    lines = [simplify(l.strip()) for l in text.splitlines() if l.strip() and not l.startswith("Compiled")]
    header, name = lines[0], fq.rsplit(".", 1)[1]
    kind = ("interface" if " interface " in header else "enum" if "extends Enum" in header
            else "record" if "extends Record" in header else
            "abstract class" if "abstract class" in header else "class")
    ext = re.search(r"extends (\w+)", header)
    impl = re.search(r"implements ([\w., <>]+?) \{", header)
    c = dict(name=name, pkg=fq.rsplit(".", 1)[0], kind=kind, fields=[], methods=[], consts=[],
             extends=ext.group(1) if ext and kind not in ("enum", "record") else None,
             implements=[re.sub(r"<.*>", "", item.strip()) for item in impl.group(1).split(",")] if impl else [],
             generic=(re.search(r"class \w+(<\w+>)", header) or [None, ""])[1])
    for line in lines[1:-1]:
        line = line.rstrip(";")
        if re.search(r"lambda\.|\$VALUES|\$values|^static \{\}|Object addRecord\(Object\)", line):
            continue
        if kind == "record" and re.search(r"(toString|hashCode|equals)\(", line):
            continue
        vis, static = VIS.get(line.split()[0], "~"), " static " in f" {line} "
        abstract = " abstract " in f" {line} " and kind != "interface"
        bare = re.sub(r"\b(public|private|protected|static|final|abstract|synchronized)\s+", "", line)
        bare = re.sub(r" throws .*", "", bare)
        if "(" in bare:
            ret, mname, params = re.match(r"(?:(.+?)\s+)?([\w.]+)\((.*)\)$", bare).groups()
            mname = mname.rsplit(".", 1)[-1]  # nested-class constructors print as Outer.Inner(...)
            if kind == "enum" and mname in ("values", "valueOf"):
                continue
            sig = f"{vis} {mname}({params})" + (f": {ret}" if ret and mname != name else "")
            c["methods"].append((sig, static, abstract))
        else:
            typ, fname = bare.rsplit(" ", 1)
            if kind == "enum" and typ == name:
                c["consts"].append(fname)
            else:
                c["fields"].append((f"{vis} {fname}: {typ}", static))
    return c


classes = {}
for f in sorted(root.rglob("*.class")):
    nested = "$" in f.name
    if nested and not re.search(r"\$[A-Z]\w*\.class$", f.name):
        continue  # skip anonymous and lambda classes, keep named nested types
    fq = ".".join(f.relative_to(root).with_suffix("").parts)
    c = parse_class(fq)
    if nested:
        c["name"] = f.stem.replace("$", ".")
    classes[c["name"]] = c

assoc = []
for c in classes.values():
    for sig, _ in c["fields"]:
        typ = sig.split(": ", 1)[1]
        for other in classes:
            if other != c["name"] and re.search(rf"(?<![\w.]){re.escape(other)}\b", typ):
                many = "[*]" if re.search(r"\[\]|List|Map", typ) else ""
                assoc.append((c["name"], other, sig.split(": ")[0][2:], many))


def box(c):
    stereo = {"interface": "«interface»", "enum": "«enumeration»", "record": "«record»",
              "abstract class": "«abstract»"}.get(c["kind"], "")
    italic = "it" if c["kind"] in ("interface", "abstract class") else ""
    rel = ([f"extends {c['extends']}"] if c["extends"] else []) + (
        ["implements " + ", ".join(c["implements"])] if c["implements"] else [])
    out = f'<div class="box"><div class="title">{stereo + "<br>" if stereo else ""}' \
          f'<b class="{italic}">{html.escape(c["name"] + c["generic"])}</b></div>'
    if rel:
        out += f'<div class="rel">{html.escape("; ".join(rel))}</div>'
    if c["kind"] == "enum":
        constants = c["consts"]
        visible = constants[:8] if len(constants) > 12 else constants
        out += '<div class="sec">' + "".join(f"<div>{html.escape(value)}</div>" for value in visible)
        if len(visible) < len(constants):
            out += f'<div>... {len(constants) - len(visible)} further constants in {html.escape(c["name"])}.java</div>'
        out += "</div>"
    out += '<div class="sec">' + ("".join(f'<div class="{"u" if s else ""}">{html.escape(f)}</div>'
                                         for f, s in c["fields"]) or "&nbsp;") + "</div>"
    out += '<div class="sec">' + ("".join(f'<div class="{"u" if s else ""} {"it" if a else ""}">'
                                         f'{html.escape(m)}</div>' for m, s, a in c["methods"]) or "&nbsp;")
    return out + "</div></div>"


# Drawn overview: top-left corners on a 1000 x 820 canvas.
W, H = 120, 30
POS = {
    "AgencyInterface": (60, 50), "TerminalMessageView": (60, 100), "MessageView": (60, 150),
    "MessageHtmlView": (60, 200), "FormField": (60, 255), "FormView": (60, 305), "HtmlPage": (60, 355),
    "RelatedRecordView": (60, 405),
    "TopSecret": (500, 30), "ProgramControl": (500, 95), "LaunchOptions": (700, 95), "MessageController": (320, 215),
    "MessageWebHandler": (320, 140),
    "WebServer": (700, 165), "LoginHandler": (580, 245), "SessionManager": (850, 245), "PageHandler": (700, 305),
    "EditHandler": (850, 345), "RecordForms": (850, 405), "AppController": (440, 395),
    "RecordEditor": (20, 480), "SqliteRecordStore": (165, 480), "IDatabaseManager": (330, 480),
    "RecordRelationships": (820, 480), "RecordImport": (625, 480), "RecordEditor.Store": (470, 480),
    "BriefManager": (20, 550), "AgentManager": (150, 550),
    "FacilityManager": (280, 550), "MissionManager": (410, 550), "UserAccountManager": (595, 550),
    "AuditLogManager": (742, 550), "SqliteMessageStore": (870, 550),
    "MessageService": (540, 630), "AuthenticatedMessageService": (760, 630),
    "Brief": (20, 700), "Agent": (150, 700), "Facility": (280, 700), "Mission": (410, 700), "UserAccount": (540, 700),
    "AuditLogEntry": (670, 700), "Message": (800, 700), "Country": (280, 770),
}
WIDE = {"AuthenticatedMessageService": 190, "TerminalMessageView": 140, "UserAccountManager": 135,
        "SqliteMessageStore": 118, "RelatedRecordView": 140, "MessageHtmlView": 140, "MessageWebHandler": 125,
        "RecordRelationships": 140, "SqliteRecordStore": 125}
EDGES = [  # (from, to, kind): inh = extends, real = implements, assoc = holds a reference, dep = creates/calls
    ("BriefManager", "IDatabaseManager", "inh"), ("AgentManager", "IDatabaseManager", "inh"),
    ("FacilityManager", "IDatabaseManager", "inh"), ("MissionManager", "IDatabaseManager", "inh"),
    ("TerminalMessageView", "MessageView", "real"), ("AuthenticatedMessageService", "MessageService", "real"),
    ("TopSecret", "ProgramControl", "dep"), ("ProgramControl", "LaunchOptions", "dep"),
    ("ProgramControl", "AgencyInterface", "dep"), ("ProgramControl", "WebServer", "dep"),
    ("WebServer", "LoginHandler", "dep"), ("WebServer", "PageHandler", "dep"), ("WebServer", "SessionManager", "assoc"),
    ("LoginHandler", "SessionManager", "assoc"), ("PageHandler", "SessionManager", "assoc"),
    ("PageHandler", "AppController", "assoc"), ("PageHandler", "EditHandler", "dep"),
    ("EditHandler", "AppController", "assoc"), ("EditHandler", "RecordForms", "dep"),
    ("EditHandler", "FormView", "dep"), ("RecordForms", "FormField", "dep"), ("FormView", "FormField", "dep"),
    ("AgencyInterface", "AppController", "assoc"), ("AgencyInterface", "MessageController", "assoc"),
    ("MessageController", "MessageView", "assoc"), ("MessageController", "MessageService", "assoc"),
    ("LoginHandler", "HtmlPage", "dep"), ("PageHandler", "HtmlPage", "dep"),
    ("LoginHandler", "UserAccountManager", "assoc"), ("AppController", "IDatabaseManager", "assoc"),
    ("AppController", "AuditLogManager", "assoc"), ("AuthenticatedMessageService", "SqliteMessageStore", "assoc"),
    ("BriefManager", "Brief", "dep"), ("AgentManager", "Agent", "dep"), ("FacilityManager", "Facility", "dep"),
    ("MissionManager", "Mission", "dep"), ("UserAccountManager", "UserAccount", "dep"),
    ("AuditLogManager", "AuditLogEntry", "dep"), ("SqliteMessageStore", "Message", "dep"),
    ("SessionManager", "UserAccount", "assoc"),
    ("AppController", "RecordImport", "dep"),
    ("AppController", "RecordEditor", "dep"), ("RecordEditor", "RecordEditor.Store", "assoc"),
    ("SqliteRecordStore", "RecordEditor.Store", "real"),
    ("AppController", "RecordRelationships", "dep"), ("PageHandler", "RelatedRecordView", "dep"),
    ("WebServer", "MessageWebHandler", "dep"), ("MessageWebHandler", "MessageHtmlView", "dep"),
    ("Country", "Facility", "assoc"),
]


def rect(n):
    return (*POS[n], WIDE.get(n, W), H)


def center(n):
    x, y, w, h = rect(n)
    return x + w / 2, y + h / 2


def clip(n, tx, ty):
    x, y, w, h = rect(n)
    cx, cy = x + w / 2, y + h / 2
    dx, dy = tx - cx, ty - cy
    s = min((w / 2) / abs(dx) if dx else 1e9, (h / 2) / abs(dy) if dy else 1e9)
    return cx + dx * s, cy + dy * s


svg = ['<svg viewBox="0 0 1000 820" xmlns="http://www.w3.org/2000/svg" font-family="Helvetica, Arial" font-size="11">'
       '<defs><marker id="open" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="9" markerHeight="9" '
       'orient="auto"><path d="M0,0 L10,5 L0,10" fill="none" stroke="#000"/></marker>'
       '<marker id="tri" viewBox="0 0 12 12" refX="12" refY="6" markerWidth="12" markerHeight="12" orient="auto">'
       '<path d="M0,0 L12,6 L0,12 Z" fill="#fff" stroke="#000"/></marker></defs>']
for label, x, y, w, h in [("org.view", 10, 10, 250, 440), ("org.controller (+ .messaging, org.runtime)", 280, 10, 710, 440),
                          ("org.managers", 10, 460, 980, 135), ("org.model (+ .messaging)", 10, 605, 980, 210)]:
    svg.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="none" stroke="#888" stroke-dasharray="6 3"/>'
               f'<text x="{x + 6}" y="{y + 14}" font-weight="bold" fill="#444">{label}</text>')
for a, b, kind in EDGES:
    if a not in classes or b not in classes:
        continue
    (ax, ay), (bx, by) = center(a), center(b)
    x1, y1 = clip(a, bx, by)
    x2, y2 = clip(b, ax, ay)
    dash = ' stroke-dasharray="5 3"' if kind in ("dep", "real") else ""
    svg.append(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="#000"{dash} '
               f'marker-end="url(#{"tri" if kind in ("inh", "real") else "open"})"/>')
for n in POS:
    if n not in classes:
        continue
    x, y, w, h = rect(n)
    stereo = {"interface": "«interface»", "abstract class": "«abstract»", "record": "«record»"}.get(classes[n]["kind"], "")
    svg.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="#fff" stroke="#000" stroke-width="1.4"/>')
    if stereo:
        it = ' font-style="italic"' if stereo != "«record»" else ""
        svg.append(f'<text x="{x + w / 2}" y="{y + 11}" text-anchor="middle" font-size="8">{stereo}</text>'
                   f'<text x="{x + w / 2}" y="{y + 24}" text-anchor="middle" font-weight="bold"{it}>{n}</text>')
    else:
        svg.append(f'<text x="{x + w / 2}" y="{y + 19}" text-anchor="middle" font-weight="bold">{n}</text>')
svg.append("</svg>")

pkgs = [p for p in PACKAGES if any(c["pkg"] == p for c in classes.values())]
overview = "".join(f'<div class="pkg"><div class="pname">{p}</div><div class="prole">{PACKAGES[p]}</div>'
                   + "".join(f'<div>{c["name"]}</div>' for c in classes.values() if c["pkg"] == p) + "</div>"
                   for p in pkgs)
rows = [(c["name"], "generalization (extends)", c["extends"]) for c in classes.values() if c["extends"]]
rows += [(c["name"], "realization (implements)", i) for c in classes.values() for i in c["implements"]]
rows += [(a, f"association: {f} {m}".strip(), b) for a, b, f, m in assoc]
rows += [(a, "dependency (creates / calls)", b) for a, b, k in EDGES if k == "dep" and a in classes and b in classes]
table = "".join(f"<tr><td>{a}</td><td>{html.escape(r)}</td><td>{b}</td></tr>" for a, r, b in rows)
detail = "".join(f'<section class="brk"><h2>package {p}</h2><p class="prole">{PACKAGES[p]}</p><div class="grid">'
                 + "".join(box(c) for c in classes.values() if c["pkg"] == p) + "</div></section>" for p in pkgs)

out_path.write_text(f"""<!DOCTYPE html><html><head><meta charset="utf-8"><title>{version} Class Diagram</title><style>
@page {{ size: letter landscape; margin: 0.4in; }}
body {{ font-family: Helvetica, Arial, sans-serif; font-size: 8.5pt; color: #000; }}
h1 {{ font-size: 18pt; margin: 0 0 4px; }} h2 {{ font-size: 12pt; margin: 0 0 2px; }}
.ov {{ display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; font-size: 7.5pt; line-height: 1.1; }}
.pkg {{ border: 1.5px solid #000; padding: 6px; break-inside: avoid; }}
.pname {{ font-weight: bold; border-bottom: 1px solid #000; margin-bottom: 3px; }}
.prole {{ color: #333; font-style: italic; margin: 0 0 4px; }}
.legend {{ margin-top: 8px; color: #333; }}
svg {{ display: block; width: 100%; height: 6.2in; margin-top: 6px; }}
table {{ border-collapse: collapse; width: 100%; margin-top: 8px; }}
td, th {{ border: 1px solid #666; padding: 2px 5px; text-align: left; }} th {{ background: #e5e5e5; }}
.brk {{ break-before: page; }}
.grid {{ display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; align-items: start; }}
.box {{ border: 1.5px solid #000; break-inside: avoid; font-family: Menlo, Consolas, monospace; font-size: 7.3pt; }}
.title {{ text-align: center; background: #eee; padding: 3px; border-bottom: 1px solid #000;
          font-family: Helvetica, Arial, sans-serif; font-size: 9pt; }}
.rel {{ font-family: Helvetica, Arial, sans-serif; text-align: center; padding: 2px; border-bottom: 1px solid #000;
        font-style: italic; }}
.sec {{ padding: 3px 4px; border-bottom: 1px solid #000; overflow-wrap: anywhere; }} .sec:last-child {{ border-bottom: none; }}
.u {{ text-decoration: underline; }} .it {{ font-style: italic; }}
</style></head><body>
<h1>Top Secret: {version} Class Diagram</h1>
<p>CS 3140 Team 2-15. Generated from the compiled classes, so it matches the current code.
Model-View-Controller packages: <b>org.model</b>, <b>org.view</b>, <b>org.controller</b>, with <b>org.managers</b> for database access.</p>
<h2>Package overview</h2><div class="ov">{overview}</div>
<p class="legend">Notation: + public, - private, # protected, ~ package-private; <u>underlined</u> = static;
<i>italic</i> = abstract or interface. Parameter lists show types only.</p>
<section class="brk"><h2>Architecture</h2>
<p class="legend">Solid line, open arrow = association (holds a reference). Dashed line, open arrow = dependency
(creates or calls). Solid line, hollow triangle = extends. Dashed line, hollow triangle = implements.
Terminal: ProgramControl &rarr; AgencyInterface. Web (-web): WebServer &rarr; LoginHandler / PageHandler &rarr;
EditHandler for create and edit forms, MessageWebHandler for messaging. Both interfaces share AppController,
which saves each create or edit through RecordEditor and SqliteRecordStore (record and audit entry in one transaction).</p>{"".join(svg)}</section>
<section class="brk"><h2>Relationships</h2><table><tr><th>From</th><th>Relationship</th><th>To</th></tr>{table}</table></section>
{detail}</body></html>""", encoding="utf-8")
print(f"{len(classes)} classes, {len(rows)} relationships -> {out_path}")
