"""Writes CHANGELOG.md from src/main/resources/assets/velaclient/changelog.json (the in-game "What's new" source).

Run from the project root after editing the JSON: python tools/changelog_md.py
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
data = json.load(open(os.path.join(ROOT, 'src/main/resources/assets/velaclient/changelog.json'), encoding='utf-8'))
NAMES = {'new': 'New', 'improved': 'Improved', 'fixed': 'Fixed'}
out = ['# Changelog', '', 'Generated from `src/main/resources/assets/velaclient/changelog.json`, which is also shown in game under "What\'s new".', '']
for v in data['versions']:
    out.append(f"## {v['version']} — {v['date']}")
    out.append('')
    out.append(f"**{v['title']['en']}**")
    out.append('')
    for section in v['sections']:
        out.append(f"### {NAMES[section['kind']]}")
        out.extend(f"- {item['en']}" for item in section['items'])
        out.append('')
with open(os.path.join(ROOT, 'CHANGELOG.md'), 'w', encoding='utf-8', newline='\n') as f:
    f.write('\n'.join(out))
print('CHANGELOG.md:', len(data['versions']), 'versions')
