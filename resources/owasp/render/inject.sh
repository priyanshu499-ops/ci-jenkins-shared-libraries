#!/bin/sh
# =============================================================================
# inject.sh — OWASP Dependency Check XML → Human-Readable HTML Report
# =============================================================================
set -e

XML_FILE="dependency-check-report.xml"
OUTPUT="owasp_report.html"

if [ ! -f "$XML_FILE" ]; then
  echo "❌  $XML_FILE not found in $(pwd)"
  exit 1
fi

GENERATED_AT=$(date "+%d %b %Y %H:%M:%S")

python3 - "$XML_FILE" "$GENERATED_AT" << 'PYEOF'
import sys, re, html as html_lib, datetime
import xml.etree.ElementTree as ET

XML_FILE     = sys.argv[1]
GENERATED_AT = sys.argv[2]

# ── Strip ALL namespaces from raw XML ────────────────────────────────────────
with open(XML_FILE, 'r', encoding='utf-8', errors='replace') as f:
    raw = f.read()
raw = re.sub(r'\s+xmlns(?::\w+)?="[^"]*"', '', raw)
raw = re.sub(r'<(/?)[\w.-]+:([\w.-]+)', r'<\1\2', raw)
root = ET.fromstring(raw)

# ── Helpers ───────────────────────────────────────────────────────────────────
def find_el(el, path):
    return el.find(path) if el is not None else None

def findall_el(el, path):
    return el.findall(path) if el is not None else []

def get_text(el, path, default='N/A'):
    node = find_el(el, path)
    if node is not None and node.text:
        return html_lib.escape(node.text.strip())
    return default

def get_attr(el, attr, default='N/A'):
    v = el.get(attr) if el is not None else None
    return html_lib.escape(v.strip()) if v else default

# ── Project info ──────────────────────────────────────────────────────────────
proj_name   = get_text(root, 'projectInfo/name',         default='N/A')
dc_version  = get_text(root, 'projectInfo/reportSchema', default='')
report_date = get_text(root, 'projectInfo/reportDate',   default=GENERATED_AT)

try:
    dt = datetime.datetime.fromisoformat(report_date.replace('Z', '+00:00'))
    report_date = dt.strftime('%d %b %Y %H:%M:%S UTC')
except Exception:
    pass

# ── Dependencies ──────────────────────────────────────────────────────────────
deps       = findall_el(root, 'dependencies/dependency')
total_deps = len(deps)
vuln_deps  = 0
vuln_count = 0
suppressed = 0

SEVERITY_ORDER = {'CRITICAL': 0, 'HIGH': 1, 'MEDIUM': 2, 'LOW': 3, 'INFO': 4, 'NONE': 5}

def badge(sev):
    s = (sev or 'NONE').upper()
    cls_map = {
        'CRITICAL': 'badge-critical',
        'HIGH':     'badge-high',
        'MEDIUM':   'badge-medium',
        'LOW':      'badge-low',
        'INFO':     'badge-info',
    }
    cls = cls_map.get(s, 'badge-none')
    return f'<span class="badge {cls}">{html_lib.escape(sev or "NONE")}</span>'

# ── Build VULNERABLE section ──────────────────────────────────────────────────
vuln_rows_html = []

# ── Build ALL-DEPS section ────────────────────────────────────────────────────
all_dep_cards = []

for dep in deps:
    file_name  = get_text(dep, 'fileName',  default='')
    file_path  = get_text(dep, 'filePath',  default='')
    md5        = get_text(dep, 'md5',       default='')
    sha1       = get_text(dep, 'sha1',      default='')
    sha256     = get_text(dep, 'sha256',    default='')
    license_   = get_text(dep, 'license',   default='')
    description = get_text(dep, 'description', default='')

    vulns     = findall_el(dep, 'vulnerabilities/vulnerability')
    sup_vulns = findall_el(dep, 'suppressedVulnerabilities/vulnerability')
    suppressed += len(sup_vulns)

    # Evidence rows
    evidences = findall_el(dep, 'evidenceCollected/evidence')
    evidence_rows = ''
    for ev in evidences:
        ev_type  = get_text(ev, 'type',       default='')
        ev_src   = get_text(ev, 'source',     default='')
        ev_name  = get_text(ev, 'name',       default='')
        ev_val   = get_text(ev, 'value',      default='')
        ev_conf  = get_attr(ev, 'confidence', default='')
        evidence_rows += f'''
          <tr>
            <td style="color:#94a3b8;">{ev_type}</td>
            <td style="color:#94a3b8;">{ev_src}</td>
            <td style="color:#cbd5e1;">{ev_name}</td>
            <td style="color:#e2e8f0;">{ev_val}</td>
            <td>{ev_conf}</td>
          </tr>'''

    # Identifiers
    identifiers = findall_el(dep, 'identifiers/identifier')
    id_list = []
    for idf in identifiers:
        id_type = get_attr(idf, 'type', default='')
        id_name = get_text(idf, 'name', default='')
        id_url  = get_text(idf, 'url',  default='')
        if id_url and id_url != 'N/A':
            id_list.append(f'<a class="cve-tag" href="{id_url}" target="_blank" style="background:#052e16;color:#86efac;border-color:#166534;">{id_name}</a>')
        else:
            id_list.append(f'<span class="cve-tag" style="background:#1e293b;color:#94a3b8;border-color:#334155;">{id_name if id_name != "N/A" else id_type}</span>')
    id_html = '<div class="cve-list">' + ''.join(id_list) + '</div>' if id_list else '<span style="color:#475569;">None</span>'

    # Hash display
    md5_disp    = f'<span class="hash-val">{md5}</span>'       if md5    != 'N/A' else '<span style="color:#475569;">—</span>'
    sha1_disp   = f'<span class="hash-val">{sha1}</span>'      if sha1   != 'N/A' else '<span style="color:#475569;">—</span>'
    sha256_disp = f'<span class="hash-val">{sha256[:32]}…</span>' if (sha256 != 'N/A' and len(sha256) > 32) else (f'<span class="hash-val">{sha256}</span>' if sha256 != 'N/A' else '<span style="color:#475569;">—</span>')

    # Vulnerable block for this dep
    has_vulns = len(vulns) > 0
    if has_vulns:
        vuln_deps += 1
        highest_sev = 'NONE'
        cve_tags    = []
        inner_vuln_rows = []

        for v in vulns:
            vuln_count += 1
            name     = get_text(v, 'name',        default='N/A')
            severity = get_text(v, 'severity',    default='UNKNOWN')
            cvss2    = get_text(v, 'cvssScore',   default='')
            cvss3_el = find_el(v, 'cvssV3')
            cvss3    = get_text(cvss3_el, 'baseScore', default='') if cvss3_el is not None else ''
            score    = cvss3 or cvss2 or '–'
            desc     = get_text(v, 'description', default='No description available.')
            if len(desc) > 320:
                desc = desc[:320] + '…'

            if name.startswith('CVE-'):
                cve_link = f'<a class="cve-tag" href="https://nvd.nist.gov/vuln/detail/{html_lib.escape(name)}" target="_blank">{html_lib.escape(name)}</a>'
            else:
                cve_link = f'<span class="cve-tag">{html_lib.escape(name)}</span>'
            cve_tags.append(cve_link)

            sev_rank = SEVERITY_ORDER.get((severity or '').upper(), 5)
            cur_rank = SEVERITY_ORDER.get(highest_sev.upper(), 5)
            if sev_rank < cur_rank:
                highest_sev = severity

            inner_vuln_rows.append(f'''
              <tr>
                <td><code style="font-size:12px;color:#7dd3fc;">{html_lib.escape(name)}</code></td>
                <td>{badge(severity)}</td>
                <td style="color:#94a3b8;">{score}</td>
                <td style="font-size:12px;color:#cbd5e1;">{desc}</td>
              </tr>''')

        vuln_rows_html.append(f'''
          <tr>
            <td class="filepath">{file_name}</td>
            <td>{badge(highest_sev)}</td>
            <td><div class="cve-list">{''.join(cve_tags)}</div></td>
            <td class="hash-val">{md5[:12] + '…' if md5 != 'N/A' else '–'}</td>
            <td style="font-size:12px;color:#94a3b8;">{license_}</td>
          </tr>
          <tr>
            <td colspan="5" style="padding:0;">
              <details style="padding:8px 16px;background:#060f1e;">
                <summary style="cursor:pointer;font-size:12px;color:#38bdf8;padding:6px 0;">
                  ▶ {len(vulns)} vulnerability detail(s)
                </summary>
                <table style="margin:8px 0;width:100%;background:#060f1e;border:1px solid #1e293b;border-radius:4px;">
                  <thead><tr><th>CVE / ID</th><th>Severity</th><th>CVSS</th><th>Description</th></tr></thead>
                  <tbody>{''.join(inner_vuln_rows)}</tbody>
                </table>
              </details>
            </td>
          </tr>''')

    # ── All-deps card ─────────────────────────────────────────────────────────
    status_badge = badge('HIGH') if has_vulns else '<span class="badge badge-none">CLEAN</span>'
    ev_table = f'''
              <table style="width:100%;border-collapse:collapse;margin-top:8px;background:#060f1e;border:1px solid #1e293b;border-radius:4px;">
                <thead>
                  <tr>
                    <th style="padding:8px;font-size:11px;color:#facc15;background:#1e293b;text-align:left;">Type</th>
                    <th style="padding:8px;font-size:11px;color:#facc15;background:#1e293b;text-align:left;">Source</th>
                    <th style="padding:8px;font-size:11px;color:#facc15;background:#1e293b;text-align:left;">Name</th>
                    <th style="padding:8px;font-size:11px;color:#facc15;background:#1e293b;text-align:left;">Value</th>
                    <th style="padding:8px;font-size:11px;color:#facc15;background:#1e293b;text-align:left;">Confidence</th>
                  </tr>
                </thead>
                <tbody>{evidence_rows}</tbody>
              </table>''' if evidence_rows else '<p style="color:#475569;font-size:12px;padding:6px 0;">No evidence collected</p>'

    all_dep_cards.append(f'''
        <div class="dep-card">
          <div class="dep-card-header">
            <div>
              <span class="dep-name">📦 {file_name}</span>
              <span style="margin-left:10px;">{status_badge}</span>
            </div>
            <span style="font-size:11px;color:#64748b;">{len(evidences)} evidence entries</span>
          </div>
          <div class="dep-card-body">

            <div class="info-grid">
              <div class="info-row">
                <span class="info-label">📂 File Path</span>
                <span class="info-value filepath-sm">{file_path}</span>
              </div>
              <div class="info-row">
                <span class="info-label">📄 License</span>
                <span class="info-value">{license_}</span>
              </div>
              <div class="info-row">
                <span class="info-label">🔑 Identifiers</span>
                <span class="info-value">{id_html}</span>
              </div>
            </div>

            <div class="hash-grid">
              <div class="hash-box">
                <span class="hash-label">MD5</span>
                {md5_disp}
              </div>
              <div class="hash-box">
                <span class="hash-label">SHA-1</span>
                {sha1_disp}
              </div>
              <div class="hash-box">
                <span class="hash-label">SHA-256</span>
                {sha256_disp}
              </div>
            </div>

            <details style="margin-top:12px;">
              <summary style="cursor:pointer;font-size:12px;color:#38bdf8;padding:4px 0;">
                ▶ Evidence Collected ({len(evidences)} entries)
              </summary>
              {ev_table}
            </details>

          </div>
        </div>''')

# ── Assemble sections ─────────────────────────────────────────────────────────
vuln_class = 'danger' if vuln_deps > 0 else 'safe'

if vuln_deps == 0:
    vuln_banner  = '<div class="no-vuln">✅ No vulnerable dependencies were detected.</div>'
    vuln_section = ''
else:
    vuln_section = f'''
  <div class="section-title">⚠️  Vulnerable Dependencies ({vuln_deps})</div>
  <table>
    <thead>
      <tr>
        <th>Dependency</th><th>Highest Severity</th><th>CVE / IDs</th>
        <th>MD5 (partial)</th><th>License</th>
      </tr>
    </thead>
    <tbody>{''.join(vuln_rows_html)}</tbody>
  </table>'''
    vuln_banner = ''

all_deps_section = f'''
  <div class="section-title">📋  All Scanned Dependencies ({total_deps})</div>
  <div class="dep-cards-container">
    {''.join(all_dep_cards)}
  </div>''' if all_dep_cards else ''

# ── Read template & inject ────────────────────────────────────────────────────
with open('report.html', 'r') as f:
    html_out = f.read()

html_out = html_out.replace('{{PROJECT_NAME}}',    proj_name)
html_out = html_out.replace('{{TOTAL_DEPS}}',      str(total_deps))
html_out = html_out.replace('{{VULN_DEPS}}',       str(vuln_deps))
html_out = html_out.replace('{{VULN_COUNT}}',      str(vuln_count))
html_out = html_out.replace('{{SUPPRESSED}}',      str(suppressed))
html_out = html_out.replace('{{GENERATED_AT}}',    GENERATED_AT)
html_out = html_out.replace('{{DC_VERSION}}',      dc_version)
html_out = html_out.replace('{{REPORT_DATE}}',     report_date)
html_out = html_out.replace('{{VULN_CLASS}}',      vuln_class)
html_out = html_out.replace('{{VULN_BANNER}}',     vuln_banner)
html_out = html_out.replace('{{VULN_SECTION}}',    vuln_section)
html_out = html_out.replace('{{ALL_DEPS_SECTION}}', all_deps_section)

with open('owasp_report.html', 'w') as f:
    f.write(html_out)

print(f"✅  OWASP report generated: owasp_report.html")
print(f"    Project      : {proj_name}")
print(f"    Deps Scanned : {total_deps}")
print(f"    Vulnerable   : {vuln_deps}")
print(f"    CVEs Found   : {vuln_count}")
print(f"    Suppressed   : {suppressed}")
PYEOF

echo "✅ OWASP Dependency Check report generated: $OUTPUT"
