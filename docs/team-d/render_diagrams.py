"""Render Team D's current PDFs from compiled APIs and reviewed relationship maps.

Usage (repository root, with the bundled Python/ReportLab runtime):
  ./gradlew compileJava
  python docs/team-d/render_diagrams.py --source-ref <verified-source-commit>
The source ref labels the code snapshot; it does not fetch or alter source files.
"""
import argparse
import re
import subprocess
import textwrap
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import landscape, letter
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfgen import canvas
from reportlab.platypus import (
    BaseDocTemplate, Frame, PageTemplate, Paragraph, Preformatted, Spacer, KeepTogether,
)

parser = argparse.ArgumentParser()
parser.add_argument("--source-ref", required=True)
args = parser.parse_args()
OUT = Path(__file__).resolve().parent
ROOT = OUT.parents[1]
W, H = landscape(letter)
INK = colors.HexColor("#17334a")
PALE = colors.HexColor("#e9f0f5")
GRAY = colors.HexColor("#f1f1f1")


def header(c, title, subtitle):
    c.setFillColor(INK)
    c.setFont("Helvetica-Bold", 18)
    c.drawString(34, H - 38, title)
    c.setFont("Helvetica", 9)
    c.drawString(34, H - 55, f"HW7 | source {args.source_ref} | {subtitle}")
    c.setStrokeColor(colors.HexColor("#a5b7c4"))
    c.line(34, H - 65, W - 34, H - 65)


def footer(c, note, page_number=None):
    c.setFillColor(colors.HexColor("#475b68"))
    c.setFont("Helvetica", 8)
    c.drawString(34, 23, note)
    c.drawRightString(W - 34, 23, str(page_number or c.getPageNumber()))


def box(c, x, y, w, h, title, lines=(), external=False):
    c.setFillColor(GRAY if external else PALE)
    c.setStrokeColor(INK)
    c.rect(x, y, w, h, fill=1)
    c.setFillColor(INK)
    c.setFont("Helvetica-Bold", 10)
    c.drawCentredString(x + w / 2, y + h - 17, title)
    if lines:
        c.line(x, y + h - 25, x + w, y + h - 25)
    c.setFont("Helvetica", 8.5)
    for i, line in enumerate(lines):
        c.drawString(x + 8, y + h - 40 - 12 * i, line)


def arrow(c, start, end, label="", dashed=False, triangle=False):
    import math
    x1, y1 = start
    x2, y2 = end
    c.setStrokeColor(INK)
    c.setLineWidth(.8)
    c.setDash(3, 2) if dashed else c.setDash()
    c.line(x1, y1, x2, y2)
    c.setDash()
    angle = math.atan2(y2 - y1, x2 - x1)
    size = 8
    p = c.beginPath()
    p.moveTo(x2, y2)
    a = (x2 - size * math.cos(angle - .45), y2 - size * math.sin(angle - .45))
    b = (x2 - size * math.cos(angle + .45), y2 - size * math.sin(angle + .45))
    p.lineTo(*a)
    if triangle:
        p.lineTo(*b)
        p.close()
        c.setFillColor(colors.white)
        c.drawPath(p, fill=1)
    else:
        c.drawPath(p)
        c.line(x2, y2, *b)
    if label:
        c.setFillColor(INK)
        c.setFont("Helvetica", 8)
        c.drawCentredString((x1 + x2) / 2, (y1 + y2) / 2 + 6, label)


# UML overview: relationship types plus an appendix of actual declarations.
c = canvas.Canvas(str(OUT / "TeamD-ClassDiagram.pdf"), pagesize=(W, H))
c.setTitle("HW7 Team D class diagram")
header(c, "Team D - Class Model", "solid: retained dependency; dashed: use or realization")
box(c, 34, 430, 180, 95, "AppController / terminal", ["External entry-point boundary", "Validates forms / current actor"], True)
box(c, 280, 430, 205, 95, "RecordEditor<T>", ["Authorizes administrator changes", "Creates AuditEvent" ])
box(c, 555, 430, 203, 95, "<<interface>> Store<T>", ["create(record, event): int", "update(id, record, event): boolean"])
box(c, 555, 270, 203, 100, "SqliteRecordStore", ["Implements Store<Object>", "Record + junctions + audit", "One SQLite transaction"])
box(c, 280, 270, 205, 100, "<<record>> AuditEvent", ["username, action, type, timestamp", "forRecord(id): AuditLogEntry"])
box(c, 34, 270, 180, 100, "AuditLogManager", ["Implements AuditLogService", "Uses AuditLogStore"])
box(c, 34, 110, 180, 90, "SqliteAuditLogStore", ["Implements AuditLogStore", "Persists AuditLogEntry"])
box(c, 280, 110, 205, 90, "RecordRelationships", ["Uses four agency managers", "Returns RelatedRecord values"])
box(c, 555, 110, 203, 90, "RecordSearch", ["Uses four agency managers", "Returns Match values"])
arrow(c, (214, 477), (280, 477), "creates", True)
arrow(c, (485, 477), (555, 477), "uses")
arrow(c, (657, 370), (657, 430), "realizes", True, True)
arrow(c, (382, 430), (382, 370), "creates", True)
arrow(c, (124, 270), (124, 200), "store boundary")
box(c, 280, 42, 478, 45, "BriefManager | AgentManager | FacilityManager | MissionManager", external=True)
arrow(c, (382, 110), (382, 87))
arrow(c, (657, 110), (657, 87))
c.setFont("Helvetica", 9)
c.drawString(280, 235, "RelatedRecordView renders related values as escaped internal links.")
c.drawString(280, 220, "Manager/model boundaries remain in their existing packages.")
footer(c, "Team D scope only. Gray boxes are shared boundaries; this is not the full-system UML.")
c.showPage()
header(c, "Team D - Accounts and Sessions", "current account permissions are reloaded before protected work")
box(c, 34, 415, 185, 100, "LoginHandler / PageHandler", ["External HTTP boundary", "Session cookie -> authenticated actor"], True)
box(c, 290, 405, 205, 120, "SessionManager", ["Random token -> in-memory Session", "30-minute sliding idle timeout", "List / revoke for current admins", "Cookie parsing and header helpers"])
box(c, 565, 415, 193, 100, "AgencyInterface", ["External terminal boundary", "Option 12: list / revoke"], True)
box(c, 290, 210, 205, 120, "UserAccountManager", ["Metadata via UserAccountStore", "Credential files via FileHandler", "Cipher encodes password envelope", "verifyLogin / changePassword"])
box(c, 34, 210, 185, 120, "<<interface>> Account store", ["UserAccountStore", "SqliteUserAccountStore implements", "Username + administrator metadata"])
box(c, 565, 210, 193, 120, "FileHandler / Cipher", ["Existing file/cipher boundaries", "One ciphered file per username", "No SQLite password column"], True)
box(c, 290, 75, 205, 70, "<<record>> SessionSummary", ["username, token, lastActivity"])
arrow(c, (219, 465), (290, 465), "uses")
arrow(c, (565, 465), (495, 465), "uses")
arrow(c, (392, 405), (392, 330), "reloads account")
arrow(c, (290, 270), (219, 270), "uses")
arrow(c, (495, 270), (565, 270), "uses")
c.setFont("Helvetica", 9)
c.drawString(34, 175, "Session stores an account snapshot and last activity; lookups revalidate persisted metadata.")
c.drawString(34, 160, "Tokens are process-local. Database schema and entity relations are documented separately.")
footer(c, "Full signatures, constructors, fields, getters and setters follow; external boundaries are not redefined.")
c.save()

# Build declaration appendix from compiled classes, then merge with overview.
classes = [
    "org.managers.RecordEditor", "org.managers.RecordEditor$Store", "org.managers.RecordEditor$AuditEvent",
    "org.managers.SqliteRecordStore", "org.managers.RecordRelationships", "org.managers.RecordSearch",
    "org.managers.RecordSearch$Match", "org.model.RelatedRecord", "org.view.RelatedRecordView",
    "org.controller.SessionManager", "org.controller.SessionManager$Session", "org.controller.SessionManager$SessionSummary",
    "org.managers.AuditLogService", "org.managers.AuditLogManager", "org.managers.AuditLogStore",
    "org.managers.SqliteAuditLogStore", "org.model.AuditLogEntry", "org.managers.UserAccountStore",
    "org.managers.UserAccountManager", "org.managers.SqliteUserAccountStore", "org.model.UserAccount",
]
api_file = ROOT / "build" / "TeamD-API.pdf"
api_file.parent.mkdir(exist_ok=True)
doc = BaseDocTemplate(str(api_file), pagesize=(W, H))
frames = [Frame(34, 42, 351, 480, leftPadding=4, rightPadding=4, topPadding=4, bottomPadding=4),
          Frame(407, 42, 351, 480, leftPadding=4, rightPadding=4, topPadding=4, bottomPadding=4)]
def api_page(c, doc):
    header(c, "Team D - Compiled Class Declarations", "javap -p; package qualifiers shortened for readability")
    footer(c, "No API signatures are invented. Compiler lambda bodies are omitted; record accessors are retained.", c.getPageNumber() + 2)
doc.addPageTemplates([PageTemplate(id="api", frames=frames, onPage=api_page)])
style = ParagraphStyle("class", fontName="Helvetica-Bold", fontSize=9, leading=11, spaceAfter=5)
code_style = ParagraphStyle("code", fontName="Courier", fontSize=7.2, leading=8.7)
story = []
for cls in classes:
    raw = subprocess.check_output(["javap", "-p", "-cp", str(ROOT / "build/classes/java/main"), cls], text=True)
    lines = []
    for line in raw.splitlines():
        if line.startswith("Compiled from") or "lambda$" in line:
            continue
        line = re.sub(r"\bjava\.(?:lang|util|time|sql|io|security)(?:\.function)?\.", "", line)
        line = re.sub(r"\borg\.(?:managers|controller|model|view)\.", "", line).replace("$", ".")
        for wrapped in textwrap.wrap(line.replace(",", ", "), 77, subsequent_indent="    ", replace_whitespace=False):
            lines.append(wrapped)
    content = [Paragraph(cls.replace("$", "."), style), Preformatted("\n".join(lines), code_style), Spacer(1, 12)]
    story.append(KeepTogether(content))
doc.build(story)
from pypdf import PdfReader, PdfWriter
writer = PdfWriter()
for pdf in [OUT / "TeamD-ClassDiagram.pdf", api_file]:
    for page in PdfReader(pdf).pages:
        writer.add_page(page)
writer.add_metadata({"/Title": "HW7 Team D class diagram and compiled declarations"})
with (OUT / "TeamD-ClassDiagram.pdf").open("wb") as output:
    writer.write(output)

# Entity-only ERD: no column names or field boxes.
c = canvas.Canvas(str(OUT / "TeamD-ERD.pdf"), pagesize=(W, H))
c.setTitle("HW7 Team D entity relationship diagram")
header(c, "Agency Entity Relationships", "conceptual ERD: entity names and relationships only; columns are in DESIGN.md")
nodes = {
    "COUNTRIES": (34, 460, 130, 42), "FACILITIES": (240, 460, 130, 42),
    "AGENTS": (460, 460, 130, 42), "AGENT_MESSAGES": (640, 460, 130, 42),
    "MISSIONS": (240, 305, 130, 42), "J_MISSION_AGENTS": (450, 305, 150, 42),
    "USERS": (640, 305, 130, 42), "BRIEFS": (34, 150, 130, 42),
    "J_MISSION_BRIEFS": (230, 150, 150, 42), "AUDIT_LOG": (640, 150, 130, 42),
}
def relation(start, end, parent, child, label):
    x1,y1=start; x2,y2=end
    c.setStrokeColor(INK); c.line(x1,y1,x2,y2)
    c.setFillColor(INK); c.setFont("Helvetica", 8)
    if y1==y2:
        c.drawString(x1+4, y1+6, parent)
        c.drawRightString(x2-4, y2+6, child)
        c.drawCentredString((x1+x2)/2, y1-13, label)
    else:
        sign=1 if y2>y1 else -1
        c.drawString(x1+5,y1+sign*12,parent)
        c.drawString(x2+5,y2-sign*12,child)
        c.saveState();c.translate(x1-10,(y1+y2)/2);c.rotate(90);c.drawCentredString(0,0,label);c.restoreState()
relation((164,481),(240,481),"0..1","0..*","hosts")
relation((370,481),(460,481),"0..1","0..*","assigns")
relation((305,460),(305,347),"1","0..*","hosts")
relation((370,326),(450,326),"0..1","0..*","includes")
relation((525,460),(525,347),"0..1","0..*","participates")
relation((305,305),(305,192),"0..1","0..*","references")
relation((164,171),(230,171),"0..1","0..*","supports")
relation((705,305),(705,192),"1","0..*","acts")
relation((705,347),(705,460),"1","0..*","sends")
relation((590,481),(640,481),"1","0..*","receives")
for name, bounds in nodes.items():
    box(c,*bounds,name)
c.setFillColor(INK);c.setFont("Helvetica",8.5)
notes = [
    "Multiplicities show physical FK optionality: nullable agent/country/junction references allow zero or one parent.",
    "Normal application mission links use positive IDs; SQL NULL remains possible in the existing junction DDL.",
    "Audit content identifiers are polymorphic text, not foreign keys to each record table. Sessions/passwords are not tables.",
    "Messaging is a Team C integration boundary; its entity references are included without redefining that feature.",
]
for i,line in enumerate(notes):c.drawString(34,101-i*13,line)
footer(c,"Columns, types, constraints, and legacy fields are recorded separately in Team D DESIGN.md.")
c.save()

# Team D wireframes: current related-data and terminal access-control screens.
c=canvas.Canvas(str(OUT/"TeamD-Wireframes.pdf"),pagesize=(W,H))
c.setTitle("HW7 Team D related-data search and session wireframes")
header(c,"Team D - Related Record Screens","brackets denote clickable internal links; create/edit actions require an administrator")
def screen(x,y,w,h,title,lines):
    c.setStrokeColor(INK);c.setFillColor(colors.white);c.rect(x,y,w,h,fill=1)
    c.setFillColor(PALE);c.rect(x,y+h-32,w,32,fill=1,stroke=0)
    c.setFillColor(INK);c.setFont("Helvetica-Bold",11);c.drawString(x+12,y+h-21,title)
    c.setFont("Courier",8)
    cursor=y+h-52
    for line in lines:
        for part in textwrap.wrap(line,66 if w>600 else 49,replace_whitespace=False) or [""]:
            c.drawString(x+12,cursor,part);cursor-=13
    assert cursor>=y+4, f"Screen overflow: {title}"
nav=["[Menu] [Briefs] [Agents] [Facilities] [Countries]", "[Facilities] [Missions] [Messages] [Audit] [Log out]"]
screen(34,130,350,380,"Facility detail",nav+["", "Name: Blackwood Annex", "Abbreviation: BA", "Country: Afghanistan", "[Edit facility]", "", "Related records", "[country /countries/1 - Afghanistan]", "[agent /agents/2 - ghost]", "[mission /missions/4 - Recon]"])
screen(408,130,350,380,"Mission detail",nav+["", "Title: Recon", "Brief Ids: 1", "Agent Ids: 2", "Facility Id: 1", "[Edit mission]", "", "Related records", "[facility /facilities/1 - Blackwood Annex]", "[brief /briefs/1 - Field notes]", "[agent /agents/2 - ghost]"])
c.setFillColor(INK);c.setFont("Helvetica",9)
c.drawString(34,97,"Agent detail links to facility and assigned missions; brief detail links to assigned missions.")
c.drawString(34,82,"Country detail links to facilities. No related records produces the current empty-related-data state.")
c.drawString(34,67,"The duplicated Facilities navigation link is current shared behavior and remains an owner handoff.")
footer(c,"Team C's inbox/compose/read-state screens and Team B's full edit workflow require their owners' wireframes.")
c.showPage()
header(c,"Team D - Terminal Search and Sessions","actual terminal workflows, plus browser outcomes after timeout or revocation")
screen(34,335,724,175,"Search records - main menu option 11",["Search text: nightbird", "brief 7. Field notes", "agent 12. Mira", "", "Case-insensitive literal match across current agency text fields.", "No matching records. appears when there are no results.", "Returns to the main menu after listing matches."])
screen(34,115,724,195,"Manage web sessions - option 12 in -web-admin",["Username | Token | Last activity (UTC)", "ghost | <full session token> | 2026-11-20T15:00:00Z", "Token to revoke, r to refresh, or Enter to return:", "<selected full token>", "Session revoked.", "", "Only current administrators can list/revoke tokens.", "Listing/refreshing this screen does not refresh a browser's timer."])
c.setFillColor(INK);c.setFont("Helvetica",9)
c.drawString(34,84,"Browser: active request refreshes last activity. At 30 minutes idle, the next request redirects to login.")
c.drawString(34,69,"Revoked token: next request redirects to login. Terminal exit stops its managed web server.")
footer(c,"A request already in progress is not retroactively cancelled; subsequent requests must authenticate again.")
c.save()
print("Rendered TeamD-ClassDiagram.pdf, TeamD-ERD.pdf, TeamD-Wireframes.pdf")
