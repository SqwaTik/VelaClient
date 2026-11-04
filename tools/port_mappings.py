"""Ports Vela's Minecraft references from one game version to another and reports what did not carry over.

Reads Vela's compiled classes (constant pool, overrides and every Mixin annotation), resolves each Minecraft class, field and
method it uses in the source version's game jar, looks it up in the target version's jar (deobfuscated with Mojang's official
mappings when the target is obfuscated, i.e. 1.21.11 and older), and:
  * renames classes that moved or were renamed (same simple name elsewhere, or a new class with nearly the same members),
  * renames members whose name changed but whose descriptor did not (one unambiguous candidate),
  * lists everything else (removed classes, removed members, changed signatures, broken mixin selectors) with file:line.

  python -I tools/port_mappings.py 1.21.11                 report only -> build/port/1.21.11/report.md
  python -I tools/port_mappings.py 1.21.11 --apply         also writes the rewritten sources to build/port/1.21.11/src
  python -I tools/port_mappings.py 26.3 --from 26.2 --classes build/classes/java/main --sources src/main/java

Manual fixes go to tools/port/<version>.renames (old=new per line: a class `net.minecraft.a.Old=net.minecraft.b.New`, or a member
`net.minecraft.a.Owner#oldName=newName`); they win over the automatic matches. See docs/PORTING.md.
"""
import argparse, fnmatch, gzip, hashlib, json, re, shutil, struct, sys, urllib.request, zipfile
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MANIFEST = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'
MODEL_FORMAT = 3

# ---------------------------------------------------------------- class files

def parse_class(data):
    """Constant pool, members, annotations and signatures of one class file (no bytecode)."""
    p = 8
    n = struct.unpack_from('>H', data, p)[0]; p += 2
    cp: list = [None] * n
    i = 1
    while i < n:
        t = data[p]; p += 1
        if t == 1:
            ln = struct.unpack_from('>H', data, p)[0]; p += 2
            cp[i] = (1, data[p:p + ln].decode('utf-8', 'replace')); p += ln
        elif t in (3, 4):
            cp[i] = (t,); p += 4
        elif t in (5, 6):
            cp[i] = (t,); p += 8; i += 1
        elif t in (7, 8, 16, 19, 20):
            cp[i] = (t, struct.unpack_from('>H', data, p)[0]); p += 2
        elif t in (9, 10, 11, 12, 17, 18):
            cp[i] = (t,) + struct.unpack_from('>HH', data, p); p += 4
        elif t == 15:
            cp[i] = (t, data[p], struct.unpack_from('>H', data, p + 1)[0]); p += 3
        else:
            raise ValueError(f'bad constant pool tag {t}')
        i += 1
    utf = lambda k: cp[k][1]
    cname = lambda k: utf(cp[k][1]) if k else None
    access, this, sup, ni = struct.unpack_from('>HHHH', data, p); p += 8
    itfs = [cname(struct.unpack_from('>H', data, p + 2 * j)[0]) for j in range(ni)]; p += 2 * ni

    def element(p):
        tag = chr(data[p]); p += 1
        if tag in 'BCDFIJSZs':
            k = struct.unpack_from('>H', data, p)[0]; p += 2
            return (utf(k) if tag == 's' else None), p
        if tag == 'e':
            a, b = struct.unpack_from('>HH', data, p); p += 4
            return ('enum', utf(a), utf(b)), p
        if tag == 'c':
            k = struct.unpack_from('>H', data, p)[0]; p += 2
            return ('class', utf(k)), p
        if tag == '@':
            return annotation(p)
        if tag == '[':
            cnt = struct.unpack_from('>H', data, p)[0]; p += 2
            out = []
            for _ in range(cnt):
                v, p = element(p); out.append(v)
            return out, p
        raise ValueError('bad element tag ' + tag)

    def annotation(p):
        t, cnt = struct.unpack_from('>HH', data, p); p += 4
        vals = {}
        for _ in range(cnt):
            k = struct.unpack_from('>H', data, p)[0]; p += 2
            vals[utf(k)], p = element(p)
        return {'@': utf(t), **vals}, p

    def attributes(p):
        cnt = struct.unpack_from('>H', data, p)[0]; p += 2
        anns, extra = [], {}
        for _ in range(cnt):
            k, ln = struct.unpack_from('>HI', data, p); p += 6
            name = utf(k)
            if name in ('RuntimeVisibleAnnotations', 'RuntimeInvisibleAnnotations'):
                q = p + 2
                for _ in range(struct.unpack_from('>H', data, p)[0]):
                    a, q = annotation(q); anns.append(a)
            elif name in ('Signature', 'SourceFile'):
                extra[name] = utf(struct.unpack_from('>H', data, p)[0])
            p += ln
        return anns, extra, p

    def members(p):
        cnt = struct.unpack_from('>H', data, p)[0]; p += 2
        out = []
        for _ in range(cnt):
            acc, nm, ds = struct.unpack_from('>HHH', data, p); p += 6
            anns, extra, p = attributes(p)
            out.append({'acc': acc, 'name': utf(nm), 'desc': utf(ds), 'anns': anns, 'sig': extra.get('Signature')})
        return out, p

    fields, p = members(p)
    methods, p = members(p)
    anns, extra, p = attributes(p)
    refs = {'classes': set(), 'fields': set(), 'methods': set(), 'sam': set(), 'descs': set()}
    for e in cp:
        if not e:
            continue
        if e[0] == 7:
            refs['classes'].add(utf(e[1]))
        elif e[0] in (9, 10, 11):
            owner = cname(e[1]); nt = cp[e[2]]
            refs['fields' if e[0] == 9 else 'methods'].add((owner, utf(nt[1]), utf(nt[2])))
        elif e[0] == 18:
            nt = cp[e[2]]
            refs['sam'].add((utf(nt[1]), utf(nt[2])))
        elif e[0] == 12:
            refs['descs'].add(utf(e[2]))
        elif e[0] == 16:
            refs['descs'].add(utf(e[1]))
    return {'name': cname(this), 'super': cname(sup), 'itfs': itfs, 'acc': access, 'fields': fields, 'methods': methods,
            'anns': anns, 'sig': extra.get('Signature'), 'source': extra.get('SourceFile'), 'refs': refs}

DESC_CLASS = re.compile(r'L([\w/$]+)[;<]')

def desc_classes(desc):
    return DESC_CLASS.findall(desc or '')

def map_desc(desc, cmap):
    return DESC_CLASS.sub(lambda m: 'L' + cmap(m.group(1)) + m.group(0)[-1], desc)

# ---------------------------------------------------------------- game models

def fetch(url, dest, sha1=None):
    if dest.exists() and (not sha1 or hashlib.sha1(dest.read_bytes()).hexdigest() == sha1):
        return dest
    dest.parent.mkdir(parents=True, exist_ok=True)
    print('  download', url, file=sys.stderr)
    with urllib.request.urlopen(url, timeout=120) as r:
        data = r.read()
    if sha1 and hashlib.sha1(data).hexdigest() != sha1:
        raise SystemExit(f'checksum mismatch for {url}')
    dest.write_bytes(data)
    return dest

JAVA_PRIM = {'void': 'V', 'boolean': 'Z', 'byte': 'B', 'char': 'C', 'short': 'S', 'int': 'I', 'long': 'J', 'float': 'F', 'double': 'D'}

def java_to_desc(t):
    dims = t.count('[]'); t = t.replace('[]', '')
    return '[' * dims + (JAVA_PRIM.get(t) or 'L' + t.replace('.', '/') + ';')

def parse_proguard(text):
    """Mojang's named -> obfuscated mapping file: (classes obf->named, fields, methods) keyed by obfuscated owner."""
    classes, fields, methods, cur = {}, {}, {}, None
    for line in text.splitlines():
        if not line or line.startswith('#'):
            continue
        if not line[0].isspace():
            named, obf = line.rstrip(':').split(' -> ')
            cur = obf.replace('.', '/'); classes[cur] = named.replace('.', '/')
            continue
        left, obf = line.strip().split(' -> ')
        left = re.sub(r'^\d+:\d+:', '', left)
        if '(' in left:
            ret, rest = left.split(' ', 1)
            name, args = rest.split('(', 1)
            args = args.split(')')[0]
            desc = '(' + ''.join(java_to_desc(a) for a in args.split(',') if a) + ')' + java_to_desc(ret)
            methods[(cur, obf, desc)] = name
        else:
            typ, name = left.split(' ')
            fields[(cur, obf, java_to_desc(typ))] = name
    return classes, fields, methods

def jar_classes(path):
    with zipfile.ZipFile(path) as z:
        for n in z.namelist():
            if n.endswith('.class') and not n.startswith('META-INF/') and not n.endswith('module-info.class'):
                yield z.read(n)

def build_model(version, cache):
    """{class: {s: super, i: [interfaces], a: access, f: {name: desc}, m: {name: [desc]}}} in Mojang names."""
    out = cache / version / 'model.json.gz'
    if out.exists():
        model = json.loads(gzip.decompress(out.read_bytes()))
        if model.get('format') == MODEL_FORMAT:
            return model
    manifest = json.loads(fetch(MANIFEST, cache / 'version_manifest_v2.json').read_bytes())
    entry = next((v for v in manifest['versions'] if v['id'] == version), None)
    if not entry:
        fetch_fresh = cache / 'version_manifest_v2.json'; fetch_fresh.unlink()
        manifest = json.loads(fetch(MANIFEST, fetch_fresh).read_bytes())
        entry = next((v for v in manifest['versions'] if v['id'] == version), None)
        if not entry:
            raise SystemExit(f'unknown Minecraft version {version}')
    meta = json.loads(fetch(entry['url'], cache / version / f'{version}.json', entry.get('sha1')).read_bytes())
    jars = [fetch(meta['downloads']['client']['url'], cache / version / 'client.jar', meta['downloads']['client']['sha1'])]
    # Mojang's own libraries (authlib, brigadier, DFU, renderpearl...) are part of the API Vela compiles against
    for lib in meta.get('libraries', []):
        art = lib.get('downloads', {}).get('artifact')
        if art and lib['name'].startswith('com.mojang:') and 'rules' not in lib:
            jars.append(fetch(art['url'], cache / version / 'libs' / Path(art['path']).name, art['sha1']))
    mapping = meta['downloads'].get('client_mappings')
    cls_map, f_map, m_map = ({}, {}, {})
    if mapping:
        cls_map, f_map, m_map = parse_proguard(fetch(mapping['url'], cache / version / 'client.txt', mapping['sha1']).read_text('utf-8'))
    cmap = lambda c: cls_map.get(c, c)
    classes = {}
    for jar in jars:
        for data in jar_classes(jar):
            c = parse_class(data)
            obf = c['name']
            info = {'s': cmap(c['super']) if c['super'] else None, 'i': [cmap(x) for x in c['itfs']], 'a': c['acc'], 'f': {}, 'm': {}}
            for f in c['fields']:
                nd = map_desc(f['desc'], cmap)
                info['f'][f_map.get((obf, f['name'], nd), f['name'])] = nd
            for m in c['methods']:
                nd = map_desc(m['desc'], cmap)
                info['m'].setdefault(m_map.get((obf, m['name'], nd), m['name']), []).append(nd)
            classes[cmap(obf)] = info
    model = {'format': MODEL_FORMAT, 'version': version, 'obfuscated': bool(mapping), 'java': meta.get('javaVersion', {}).get('majorVersion'), 'classes': classes}
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(gzip.compress(json.dumps(model, separators=(',', ':')).encode()))
    return model

class Model:
    def __init__(self, raw):
        self.version = raw['version']; self.java = raw.get('java'); self.c = raw['classes']
        self.by_simple = defaultdict(list)
        for name in self.c:
            self.by_simple[simple(name)].append(name)

    def chain(self, cls):
        """The class and every supertype known to this model, nearest first."""
        seen, todo = [], [cls]
        while todo:
            x = todo.pop(0)
            if x in self.c and x not in seen:
                seen.append(x); todo += [self.c[x]['s']] + self.c[x]['i']
        return seen

    def field(self, cls, name):
        return next(((x, self.c[x]['f'][name]) for x in self.chain(cls) if name in self.c[x]['f']), None)

    def method(self, cls, name, desc=None):
        for x in (self.chain(cls) if name != '<init>' else [cls] if cls in self.c else []):
            ds = self.c[x]['m'].get(name)
            if ds and (desc is None or desc in ds):
                return x, (desc or ds[0])
        return None

    def method_descs(self, cls, name):
        return sorted({d for x in self.chain(cls) for d in self.c[x]['m'].get(name, [])})

def simple(name):
    return name.rsplit('/', 1)[-1]

def dotted(name):
    return name.replace('/', '.')

MEMBER_NOISE = re.compile(r'^(<init>|<clinit>|lambda\$.*|values|valueOf|access\$.*|\$values|equals|hashCode|toString|[a-z]{1,2})$')

STOP_WORDS = {'get', 'set', 'is', 'has', 'with', 'to', 'for', 'of', 'on', 'add', 'create', 'make', 'new', 'render', 'extract', 'submit', 'state'}

def words(name):
    return {w.lower() for w in re.findall(r'[A-Z]?[a-z]+|[A-Z]+(?![a-z])|\d+', name)} - STOP_WORDS

def member_names(info):
    return {n for n in list(info['f']) + list(info['m']) if not MEMBER_NOISE.match(n)}

# ---------------------------------------------------------------- what Vela uses

MIXIN = 'Lorg/spongepowered/asm/mixin/Mixin;'
SHADOW = 'Lorg/spongepowered/asm/mixin/Shadow;'
OVERWRITE = 'Lorg/spongepowered/asm/mixin/Overwrite;'
ACCESSOR = 'Lorg/spongepowered/asm/mixin/gen/Accessor;'
INVOKER = 'Lorg/spongepowered/asm/mixin/gen/Invoker;'
DEFINITION = 'Lcom/llamalad7/mixinextras/expression/Definition;'
DEFINITIONS = 'Lcom/llamalad7/mixinextras/expression/Definitions;'
SELECTOR = re.compile(r'^(?:L([\w/$]+);)?([\w$<>*]*)(\(.*)?$')
TARGET = re.compile(r'^L([\w/$]+);([\w$<>]+)(?:(\(.*)|:(.+))$')

class Usage:
    """Every tracked reference: key -> set of Vela classes that make it."""
    def __init__(self):
        self.classes = defaultdict(set)   # class
        self.fields = defaultdict(set)    # (owner, name, desc or None)
        self.methods = defaultdict(set)   # (owner, name, desc or None)
        self.why = {}                     # key -> how it is used (call, override, @Inject method, @At target...)

    def add(self, kind, key, who, why):
        getattr(self, kind)[key].add(who)
        self.why.setdefault((kind, key), set()).add(why)

def scan(class_dirs, src):
    own, usage, mixins = {}, Usage(), {}
    for d in class_dirs:
        for f in sorted(Path(d).rglob('*.class')):
            c = parse_class(f.read_bytes()); own[c['name']] = c
    tracked = lambda x: x in src.c
    def own_chain(cls):
        """Walk Vela's own supertypes up to the first tracked (game) class."""
        seen, todo, hits = set(), [cls], []
        while todo:
            x = todo.pop(0)
            if x in seen or not x: continue
            seen.add(x)
            if tracked(x): hits.append(x)
            elif x in own: todo += [own[x]['super']] + own[x]['itfs']
        return hits
    def own_declares(cls, kind, mname, desc):
        """A member Vela itself declares on one of its own classes (calls to it are not game references)."""
        seen, todo = set(), [cls]
        while todo:
            x = todo.pop(0)
            if x in seen or x not in own: continue
            seen.add(x)
            if any(m['name'] == mname and (kind == 'fields' or m['desc'] == desc) for m in own[x][kind]): return True
            todo += [own[x]['super']] + own[x]['itfs']
        return False
    for name, c in own.items():
        r = c['refs']
        descs = set(r['descs']) | {m['desc'] for m in c['fields'] + c['methods']} | {m['sig'] for m in c['fields'] + c['methods'] if m['sig']}
        if c['sig']: descs.add(c['sig'])
        for x in set(r['classes']) | {y for d in descs for y in desc_classes(d)} | set([c['super']] + c['itfs']):
            x = x.lstrip('[')
            if x.startswith('L'): x = x[1:].rstrip(';')
            if tracked(x): usage.add('classes', x, name, 'type')
        for kind, find in (('fields', src.field), ('methods', src.method)):
            for owner, mname, desc in r[kind]:
                owner = owner.lstrip('[')
                if not tracked(owner) and own_declares(owner, kind, mname, desc): continue
                for o in ([owner] if tracked(owner) else own_chain(owner)):
                    hit = find(o, mname) if kind == 'fields' else find(o, mname, desc)
                    if hit:
                        usage.add(kind, (hit[0], mname, desc), name, 'call' if kind == 'methods' else 'access'); break
        for sam, factory in r['sam']:
            itf = factory.rsplit(')', 1)[1]
            if itf.startswith('L') and tracked(itf[1:-1]) and src.method(itf[1:-1], sam):
                usage.add('methods', (src.method(itf[1:-1], sam)[0], sam, None), name, 'lambda')
        mixin = next((a for a in c['anns'] if a['@'] == MIXIN), None)
        if mixin:
            targets = [v[1][1:-1] for v in mixin.get('value', []) if isinstance(v, tuple)] + [t.replace('.', '/') for t in mixin.get('targets', [])]
            mixins[name] = targets
            for t in targets:
                if tracked(t): usage.add('classes', t, name, '@Mixin target')
            scan_mixin(c, [t for t in targets if tracked(t)], usage, src)
        elif any(own_chain(x) for x in [c['super']] + c['itfs']):
            supers = [y for x in [c['super']] + c['itfs'] for y in (own_chain(x))]
            for m in c['methods']:
                if m['acc'] & 0x100A or m['name'].startswith('<'):  # static, private, synthetic
                    continue
                for s in supers:
                    hit = src.method(s, m['name'], m['desc'])
                    if hit:
                        usage.add('methods', (hit[0], m['name'], m['desc']), name, 'override'); break
    return own, usage, mixins

def ats(value):
    if isinstance(value, dict): return [value]
    if isinstance(value, list): return [a for v in value for a in ats(v)]
    return []

def scan_mixin(c, targets, usage, src):
    who = c['name']
    def selector(sel, why):
        m = SELECTOR.match(sel.replace(' ', ''))
        if not m: return
        owner, name, desc = m.groups()
        if not name or '*' in name: return
        for t in ([owner] if owner else targets):
            if src.c.get(t) is None: continue
            if name in src.c[t]['f'] and not desc:
                usage.add('fields', (t, name, None), who, why); continue
            hit = src.method(t, name, desc) or src.method(t, name)
            usage.add('methods', (hit[0] if hit else t, name, desc), who, why)
    def target(at, why):
        val, tgt = at.get('value', ''), at.get('target')
        if not tgt: return
        if val == 'NEW':
            cls = tgt[1:].split(';')[0] if tgt.startswith('L') else tgt.rsplit(')L', 1)[-1].rstrip(';') if ')' in tgt else tgt
            cls = cls.replace('.', '/')
            if cls in src.c: usage.add('classes', cls, who, why)
            return
        m = TARGET.match(tgt.replace(' ', ''))
        if not m or m.group(1) not in src.c: return
        owner, name, mdesc, fdesc = m.groups()
        if fdesc:
            hit = src.field(owner, name)
            usage.add('fields', (hit[0] if hit else owner, name, fdesc), who, why)
        else:
            hit = src.method(owner, name, mdesc)
            usage.add('methods', (hit[0] if hit else owner, name, mdesc), who, why)
    def definition(d):
        for k in ('method', 'field'):
            for s in (d.get(k) or []):
                target({'value': 'FIELD' if k == 'field' else 'INVOKE', 'target': s}, '@Definition')
    for a in c['anns']:
        if a['@'] == DEFINITIONS:
            for d in a.get('value', []): definition(d)
    for m in c['methods']:
        for a in m['anns']:
            short = a['@'].rsplit('/', 1)[-1].rstrip(';')
            if a['@'] == SHADOW or a['@'] == OVERWRITE:
                name = m['name'][len(a.get('prefix', 'shadow$')):] if m['name'].startswith(a.get('prefix', 'shadow$')) else m['name']
                for t in targets:
                    hit = src.method(t, name, m['desc'])
                    usage.add('methods', (hit[0] if hit else t, name, m['desc']), who, '@' + short)
            elif a['@'] in (ACCESSOR, INVOKER):
                name = a.get('value')
                if not name:
                    name = re.sub(r'^(get|is|set|call|invoke)', '', m['name']); name = name[:1].lower() + name[1:]
                for t in targets:
                    if a['@'] == ACCESSOR:
                        hit = src.field(t, name)
                        usage.add('fields', (hit[0] if hit else t, name, None), who, '@Accessor')
                    elif name not in ('new', 'create', '<init>'):
                        hit = src.method(t, name)
                        usage.add('methods', (hit[0] if hit else t, name, None), who, '@Invoker')
            elif a['@'] == DEFINITION:
                definition(a)
            elif a['@'] == DEFINITIONS:
                for d in a.get('value', []): definition(d)
            elif 'method' in a or 'at' in a:
                for s in a.get('method', []) or []:
                    if isinstance(s, str): selector(s, f'@{short} method')
                for at in ats(a.get('at')):
                    target(at, f'@{short} @At')
                sl = a.get('slice')
                for s in ats(sl):
                    for k in ('from', 'to'):
                        for at in ats(s.get(k)): target(at, '@Slice')
    for f in c['fields']:
        if any(a['@'] == SHADOW for a in f['anns']):
            for t in targets:
                hit = src.field(t, f['name'])
                usage.add('fields', (hit[0] if hit else t, f['name'], f['desc']), who, '@Shadow')

# ---------------------------------------------------------------- matching

class Port:
    def __init__(self, src, dst, manual):
        self.src, self.dst = src, dst
        self.cmap, self.how = {}, {}
        self.manual_members = {}
        for k, v in manual.items():
            if '#' in k:
                owner, name = k.split('#'); self.manual_members[(owner.replace('.', '/'), name)] = v
            else:
                self.cmap[k.replace('.', '/')] = v.replace('.', '/'); self.how[k.replace('.', '/')] = 'manual'
        self.added = [c for c in dst.c if c not in src.c]

    def cls(self, name):
        if name in self.cmap:
            return self.cmap[name]
        if name not in self.src.c or name in self.dst.c:
            return name
        res, how = None, None
        self.cmap[name] = None  # in progress: an outer and its nested classes ask each other
        if '$' in name:  # a nested class follows its outer class
            outer, inner = name.rsplit('$', 1)
            mo = self.cls(outer)
            if mo and mo + '$' + inner in self.dst.c:
                res, how = mo + '$' + inner, 'outer class renamed'
        if not res:
            cands = [c for c in self.dst.by_simple.get(simple(name), []) if c not in self.src.c]
            if len(cands) == 1:
                res, how = cands[0], 'moved (same name)'
        if not res:  # an outer class follows its nested classes (GuiGraphicsExtractor$ScissorStack -> GuiGraphics$ScissorStack)
            nested = [n for n in self.src.c if n.startswith(name + '$') and '$' not in n[len(name) + 1:] and not n[len(name) + 1:].isdigit()]
            outs = {m.rsplit('$', 1)[0] for m in map(self.cls, nested) if m and '$' in m and not m.startswith(name + '$')}
            if len(outs) == 1 and next(iter(outs)) in self.added:
                res, how = next(iter(outs)), 'its nested classes moved there'
        if not res:
            mine = member_names(self.src.c[name])
            best, second = (0, None), 0
            if len(mine) >= 3:
                for c in self.added:
                    theirs = member_names(self.dst.c[c])
                    if not theirs: continue
                    score = len(mine & theirs) / len(mine | theirs)
                    if score > best[0]: best, second = (score, c), best[0]
                    elif score > second: second = score
            if best[0] >= 0.6 and best[0] - second >= 0.15:
                res, how = best[1], f'renamed (members {best[0]:.0%} alike)'
        self.cmap[name], self.how[name] = res, how
        return res

    def desc(self, desc):
        return map_desc(desc, lambda c: self.cls(c) or c) if desc else desc

    def member(self, kind, owner, name, desc):
        """-> (status, new_owner, new_name, detail)"""
        downer = self.cls(owner)
        def guess(old, new, why):
            # applied only when the names share a word (mainCamera -> getMainCamera); otherwise just suggested
            if words(old) & words(new):
                return 'renamed', downer, new, 'guess: ' + why
            return 'missing', downer, None, f'maybe `{new}` ({why})'
        if not downer:
            return 'owner-missing', None, None, ''
        manual = self.manual_members.get((owner, name)) or self.manual_members.get(('*', name))
        ddesc = self.desc(desc)
        if manual:
            found = self.dst.field(downer, manual) if kind == 'fields' else self.dst.method(downer, manual, ddesc) or self.dst.method(downer, manual)
            if found:
                return 'renamed', downer, manual, 'manual'
        if kind == 'fields':
            hit = self.dst.field(downer, name)
            if hit and (not ddesc or hit[1] == ddesc):
                return 'ok', downer, name, ''
            if hit:
                return 'changed', downer, name, f'type {desc} -> {hit[1]}'
            if ddesc:
                old = self.src.c[owner]['f'] if owner in self.src.c else {}
                cands = [n for n, d in self.dst.c.get(downer, {'f': {}})['f'].items() if d == ddesc and n not in old]
                if len(cands) == 1:
                    return guess(name, cands[0], 'only new field of that type')
            mhit = self.dst.method_descs(downer, name)
            return 'missing', downer, None, ('now a method ' + ', '.join(mhit[:2])) if mhit else ''
        if self.dst.method(downer, name, ddesc):
            return 'ok', downer, name, ''
        if name in ('<init>', '<clinit>'):
            now = self.dst.c.get(downer, {}).get('m', {}).get(name, [])
            return ('changed', downer, name, 'constructors now: ' + ', '.join(now[:4])) if now else ('missing', downer, None, '')
        others = self.dst.method_descs(downer, name)
        if others:
            return 'changed', downer, name, 'now ' + ', '.join(others[:4])
        if ddesc and downer in self.dst.c:
            old = self.src.c[owner]['m'] if owner in self.src.c else {}
            cands = [n for n, ds in self.dst.c[downer]['m'].items() if ddesc in ds and n not in old and not n.startswith('lambda$')]
            if len(cands) == 1:
                return guess(name, cands[0], 'only new method with that descriptor')
        fhit = self.dst.field(downer, name)
        return 'missing', downer, None, f'now a field {fhit[1]}' if fhit else ''

# ---------------------------------------------------------------- sources

def source_file(cls, own, roots):
    c = own.get(cls)
    if not c or not c['source']: return None
    pkg = cls.rsplit('/', 1)[0] if '/' in cls else ''
    for r in roots:
        f = Path(r) / pkg / c['source']
        if f.exists(): return f
    return None

def locate(files, token):
    out = []
    pat = re.compile(r'(?<![\w$])' + re.escape(token) + r'(?![\w$])')
    for f in sorted(files):
        for i, line in enumerate(f.read_text('utf-8').splitlines(), 1):
            if pat.search(line):
                out.append(f'{f.relative_to(ROOT).as_posix() if f.is_relative_to(ROOT) else f}:{i}')
                if len(out) >= 6: return out
    return out or [f.relative_to(ROOT).as_posix() if f.is_relative_to(ROOT) else str(f) for f in sorted(files)][:6]

def class_forms(old, new):
    """Spellings of a class in Java source and in mixin strings: a.b.Outer.Inner, a.b.Outer$Inner, a/b/Outer$Inner."""
    java = (old.replace('/', '.').replace('$', '.'), new.replace('/', '.').replace('$', '.'))
    # a top-level class that became a nested one keeps the Java spelling (a.b.Outer.Inner) in source
    return [java] + ([(old.replace('/', '.'), new.replace('/', '.'))] if '$' in old else []) + [(old, new)]

def rewrite(text, class_renames, member_renames, gone=()):
    """class_renames: [(old, new)] internal names; member_renames: [(kind, old_name, new_name)] for this file;
    gone: dotted packages that no longer exist in the target (their `import a.*;` lines are removed)."""
    changes = 0
    # simple names first, by how the file imports the old class: `import a.Old;` -> Old, `import a.Outer;` -> Outer.Inner
    for old, new in class_renames:
        so, sn = simple(old).split('$'), simple(new).split('$')
        pkg = old.rsplit('/', 1)[0].replace('/', '.') + '.'
        for k in range(len(so), 0, -1):
            if re.search(r'^import\s+' + re.escape(pkg + '.'.join(so[:k])) + r'\s*;', text, re.M):
                used_old = '.'.join(so[k - 1:])
                used_new = '.'.join(sn[len(sn) - len(so) + k - 1:]) if len(sn) >= len(so) - k + 1 else '.'.join(sn)
                if used_old != used_new:
                    text, n = re.subn(r'(?<![\w$.])' + re.escape(used_old) + r'(?![\w$])', used_new, text); changes += n
                break
        else:  # `import a.*;`: name the new class explicitly
            wild = re.search(r'^import\s+' + re.escape(pkg) + r'\*\s*;[^\n]*\n', text, re.M)
            word = r'(?<![\w$.])' + re.escape('.'.join(so)) + r'(?![\w$])'
            if wild and re.search(word, text[wild.end():]):
                body, n = re.subn(word, sn[-1], text[wild.end():]); changes += n
                text = text[:wild.end()] + 'import ' + new.replace('/', '.').replace('$', '.') + ';\n' + body
    for g in gone:
        text, n = re.subn(r'^import\s+' + re.escape(g) + r'\.\*\s*;[^\n]*\n', '', text, flags=re.M); changes += n
    pairs = sorted({p for o, n in class_renames for p in class_forms(o, n)}, key=lambda p: -len(p[0]))
    if pairs:
        pat = re.compile(r'(?<![\w$.])(?:' + '|'.join(re.escape(a) for a, _ in pairs) + r')(?![\w$])')
        table = dict(pairs)
        text, k = pat.subn(lambda m: table[m.group()], text); changes += k
        # descriptors in mixin strings ("(ILa/b/Old;)V"): the L sits right before the name, so the guard above skips them
        desc = re.compile(r'L(' + '|'.join(re.escape(o) for o, _ in class_renames) + r')(?=;)')
        text, k = desc.subn(lambda m: 'L' + dict(class_renames)[m.group(1)], text); changes += k
    for kind, old, new in member_renames:
        w = re.escape(old)
        if kind == 'methods':
            text, k = re.subn(r'(?<![\w$])' + w + r'(?=\s*\()', new, text); changes += k
            text, k = re.subn(r'::' + w + r'(?![\w$])', '::' + new, text); changes += k
        else:
            text, k = re.subn(r'(?<=\.)' + w + r'(?![\w$(])', new, text); changes += k
        # mixin strings: "old", "old(...)", "Lowner;old(...)", "Lowner;old:Ltype;"
        text, k = re.subn(r'(?<=[";])' + w + r'(?=[(":])', new, text); changes += k
    return text, changes

# ---------------------------------------------------------------- report

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('target', help='Minecraft version to port to, e.g. 1.21.11')
    ap.add_argument('--from', dest='source', default=None, help='version the sources are written for (default: minecraft_version in gradle.properties)')
    ap.add_argument('--classes', action='append', help='compiled Vela classes (default: build/classes/java/main)')
    ap.add_argument('--sources', action='append', help='Java source roots (default: src/main/java)')
    ap.add_argument('--out', help='output folder (default: build/port/<target>)')
    ap.add_argument('--cache', default=str(Path.home() / '.cache/vela-port'), help='downloaded game jars and models')
    ap.add_argument('--apply', action='store_true', help='write the rewritten sources to <out>/src')
    a = ap.parse_args()
    props = dict(l.split('=', 1) for l in (ROOT / 'gradle.properties').read_text().splitlines() if '=' in l and not l.startswith('#'))
    source = a.source or props['minecraft_version'].strip()
    classes = a.classes or [str(ROOT / 'build/classes/java/main')]
    roots = [Path(r) if Path(r).is_absolute() else ROOT / r for r in (a.sources or ['src/main/java'])]
    out = Path(a.out) if a.out else ROOT / 'build/port' / a.target
    cache = Path(a.cache)
    print(f'models: {source} -> {a.target}', file=sys.stderr)
    src, dst = Model(build_model(source, cache)), Model(build_model(a.target, cache))
    manual_file = ROOT / 'tools/port' / f'{a.target}.renames'
    manual = dict(l.split('=', 1) for l in manual_file.read_text('utf-8').splitlines() if '=' in l and not l.startswith('#')) if manual_file.exists() else {}
    print('scanning Vela classes', file=sys.stderr)
    own, usage, mixins = scan(classes, src)
    port = Port(src, dst, manual)

    file_of = {c: source_file(c, own, roots) for c in own}
    files = lambda whos: {file_of[w] for w in whos if file_of.get(w)}
    rows = defaultdict(list)          # section -> [(title, detail, locations)]
    class_renames = {}
    for c in sorted(usage.classes):
        n = port.cls(c)
        if n == c: continue
        whos = usage.classes[c]
        if n:
            class_renames[c] = n
            rows['classes renamed'].append((f'`{dotted(c)}` -> `{dotted(n)}`', port.how.get(c, ''), locate(files(whos), simple(c).split('$')[-1])))
        else:
            rows['classes missing'].append((f'`{dotted(c)}`', ', '.join(sorted(usage.why[('classes', c)])), locate(files(whos), simple(c).split('$')[-1])))
    per_file = defaultdict(set)       # file -> {(kind, old, new)}
    blocked = defaultdict(set)        # file -> names also used for references that stay
    ok = 0
    results = []
    for kind in ('fields', 'methods'):
        for (owner, name, desc), whos in sorted(getattr(usage, kind).items(), key=lambda kv: (kv[0][0], kv[0][1], kv[0][2] or '')):
            results.append([kind, owner, name, desc, whos, *port.member(kind, owner, name, desc)])
    # two old members guessed onto the same new one: neither guess holds
    claims = defaultdict(set)
    for kind, owner, name, desc, whos, status, nowner, nname, detail in results:
        if status == 'renamed' and detail.startswith('guess'): claims[(kind, nowner, nname)].add(name)
    for r in results:
        if r[5] == 'renamed' and len(claims.get((r[0], r[6], r[7]), ())) > 1:
            r[5], r[8] = 'missing', f'maybe `{r[7]}` (ambiguous: {", ".join(sorted(claims[(r[0], r[6], r[7])]))} all fit)'
    for kind, owner, name, desc, whos, status, nowner, nname, detail in results:
        why = ', '.join(sorted(usage.why[(kind, (owner, name, desc))]))
        label = f'`{dotted(owner)}.{name}{"" if kind == "fields" else (desc or "(…)")}`'
        if status == 'ok':
            ok += 1
            for f in files(whos): blocked[f].add(name)
        elif status == 'renamed':
            rows['members renamed'].append((f'{label} -> `{nname}`', f'{why}; {detail}', locate(files(whos), name)))
            for f in files(whos): per_file[f].add((kind, name, nname))
        elif status != 'owner-missing':  # members of a missing class are listed with the class
            section = {'changed': 'signatures changed', 'missing': f'{kind} missing'}[status]
            rows[section].append((label, f'{why}' + (f'; {detail}' if detail else ''), locate(files(whos), name)))
    # a rename is applied in a file only if no reference that stays shares the old name there
    skipped = []
    for f, rs in per_file.items():
        for r in list(rs):
            if r[1] in blocked[f]:
                rs.discard(r); skipped.append((f, r))
    out.mkdir(parents=True, exist_ok=True)
    total = sum(len(v) for v in (usage.classes, usage.fields, usage.methods))
    manual_count = sum(len(rows[s]) for s in rows if s not in ('classes renamed', 'members renamed'))
    lines = [f'# Vela {source} -> {a.target}', '',
             f'Game references: {len(usage.classes)} classes, {len(usage.fields)} fields, {len(usage.methods)} methods ({total} total) '
             f'from {len(own)} classes, {len(mixins)} mixins.  ',
             f'Target Java: {dst.java} (source {src.java}).  ',
             f'Unchanged members: {ok}. Renamed automatically: {len(class_renames)} classes, {len(rows["members renamed"])} members. '
             f'**Needs a manual port: {manual_count}.**', '']
    if skipped:
        lines.append(f'{len(skipped)} member renames were not applied in a file that also uses the old name for an unchanged reference (listed under "renames not applied").')
        lines.append('')
    for f, r in skipped:
        rows['renames not applied'].append((f'`{r[1]}` -> `{r[2]}`', 'old name also used for a reference that stays', [f.relative_to(ROOT).as_posix() if f.is_relative_to(ROOT) else str(f)]))
    order = ['classes missing', 'fields missing', 'methods missing', 'signatures changed', 'renames not applied', 'classes renamed', 'members renamed']
    for s in order:
        if not rows[s]: continue
        lines += [f'## {s} ({len(rows[s])})', '']
        for title, detail, locs in rows[s]:
            lines.append(f'- {title}' + (f' — {detail}' if detail else '') + ('  \n  ' + ' '.join(locs) if locs else ''))
        lines.append('')
    (out / 'report.md').write_bytes(('\n'.join(lines) + '\n').encode())
    (out / 'renames.txt').write_bytes(''.join(f'{dotted(o)}={dotted(n)}\n' for o, n in sorted(class_renames.items())).encode())
    # port.txt: what `rewrite` applies when a version module is built (versions/<module>/port.txt)
    plan = [f'# Vela {source} -> {a.target}, written by tools/port_mappings.py {a.target}. Do not edit: fix tools/port/{a.target}.renames and run it again.']
    plan += [f'class {dotted(o)}={dotted(n)}' for o, n in sorted(class_renames.items())]
    have = {c.rsplit('/', 1)[0] for c in dst.c}
    gone = sorted({o.rsplit('/', 1)[0].replace('/', '.') for o in class_renames if o.rsplit('/', 1)[0] not in have})
    plan += [f'gone {g}' for g in gone]
    for f, rs in sorted(per_file.items()):
        root = next((r for r in roots if f.is_relative_to(r)), None)
        if root:
            plan += [f'member {f.relative_to(root).as_posix()} {k} {o}={n}' for k, o, n in sorted(rs)]
    (out / 'port.txt').write_bytes(('\n'.join(plan) + '\n').encode())
    (out / 'report.json').write_bytes(json.dumps({s: [{'what': t, 'detail': d, 'at': l} for t, d, l in rows[s]] for s in order}, indent=1).encode())
    if a.apply:
        dest = out / 'src'
        if dest.exists(): shutil.rmtree(dest)
        changed = 0
        for r in roots:
            for f in r.rglob('*'):
                if not f.is_file(): continue
                to = dest / r.name / f.relative_to(r)
                to.parent.mkdir(parents=True, exist_ok=True)
                if f.suffix != '.java':
                    shutil.copyfile(f, to); continue
                text = f.read_text('utf-8')
                new, k = rewrite(text, sorted(class_renames.items()), sorted(per_file.get(f, ())), gone)
                changed += new != text
                to.write_bytes(new.replace('\r\n', '\n').encode())
        print(f'rewritten sources: {dest} ({changed} files changed)', file=sys.stderr)
    print(f'{out / "report.md"}: {len(class_renames)} classes and {len(rows["members renamed"])} members renamed, {manual_count} to port by hand')

# ---------------------------------------------------------------- version modules

SOURCE_SETS = [('main', 'java'), ('main', 'resources'), ('test', 'java'), ('test', 'resources'), ('gametest', 'java'), ('gametest', 'resources')]

def substitute(text, old, new):
    """Replaces a plain string or a compiled regex everywhere; returns the text and the number of replacements."""
    if isinstance(old, str): return text.replace(old, new), text.count(old)
    return old.subn(new, text)


def read_layer(module):
    """One version layer's rules: port.txt (written by the report), then manual.txt (same format, hand-made), drop.txt."""
    classes, members, gone, patches, unpatch = [], defaultdict(list), [], defaultdict(list), []
    lines = [l for p in (module / 'port.txt', module / 'manual.txt') if p.exists() for l in p.read_text('utf-8').splitlines()]
    for line in lines:
        if line.startswith('gone '):
            gone.append(line[5:].strip())
        elif line.startswith('patch '):  # patch <path|*> <old text> => <new text>: replace every occurrence after the renames
            path, rest = line[6:].split(' ', 1); o, n = rest.split(' => ', 1); patches[path].append((o, n))
        elif line.startswith('unpatch '):  # unpatch <path|*> <old text>: a lower layer's patch with this old text is not applied
            path, o = line[8:].split(' ', 1); unpatch.append((path, o))
        elif line.startswith('patchre '):  # patchre <path|*> <regex> => <replacement>: same, as a regular expression
            path, rest = line[8:].split(' ', 1); o, n = rest.split(' => ', 1); patches[path].append((re.compile(o), n))
        elif line.startswith('class '):
            o, n = line[6:].split('=', 1); classes.append((o.replace('.', '/'), n.replace('.', '/')))
        elif line.startswith('member '):
            _, path, kind, on = line.split(' ', 3); o, n = on.split('=', 1); members[path].append((kind, o, n))
    drops = module / 'drop.txt'
    globs = [l.strip() for l in drops.read_text('utf-8').splitlines() if l.strip() and not l.startswith('#')] if drops.exists() else []
    everywhere = patches.pop('*', [])
    return dict(module=module, classes=classes, members=members, gone=gone, patches=patches, globs=globs, unpatch=unpatch,
                everywhere=everywhere, hits=[0] * len(everywhere))

def layers_of(module):
    """The module's layers, lowest first: a base.txt names the layers it builds on, one per line, lowest first (26.3-neoforge:
    26.3, then the shared neoforge layer). Each of them may have a base.txt of its own; a layer shows up once."""
    base = module / 'base.txt'
    below = []
    for name in base.read_text('utf-8').split() if base.exists() else ():
        below += [m for m in layers_of(module.parent / name) if m not in below]
    return below + [module]

def rewrite_main(argv):
    """The sources a version module compiles: Vela's src/ with the module's port.txt applied, minus the files the module
    replaces (its own src/ at the same path) or drops (drop.txt globs, e.g. dev/sqwat/vela/render/vk/**). Mixin configs lose
    the mixins whose class is gone. A module with a base.txt sits on another module: that module's rules apply first, its
    own files are copied in with the upper layers' rules applied, and the upper layers replace or drop files of either.
    Needs no downloads; Gradle runs it before compiling a module."""
    ap = argparse.ArgumentParser(prog='port_mappings.py rewrite', description=rewrite_main.__doc__)
    ap.add_argument('module', help='version module folder, e.g. versions/26.3-fabric')
    ap.add_argument('into', help='output folder (gets main/java, main/resources, test/java ...)')
    a = ap.parse_args(argv)
    module, into = Path(a.module).resolve(), Path(a.into).resolve()
    layers = [read_layer(m) for m in layers_of(module)]
    for i, layer in enumerate(layers):  # an upper layer's `unpatch` lines take patches out of the layers below it
        for path, o in layer['unpatch']:
            hit = 0
            for low in layers[:i]:
                if path == '*':
                    keep = [(k, (po, pn)) for k, (po, pn) in enumerate(low['everywhere']) if getattr(po, 'pattern', po) != o]
                    hit += len(low['everywhere']) - len(keep)
                    low['everywhere'] = [x for _, x in keep]; low['hits'] = [0] * len(keep)
                else:
                    before = low['patches'].get(path, [])
                    after = [(po, pn) for po, pn in before if getattr(po, 'pattern', po) != o]
                    hit += len(before) - len(after)
                    if path in low['patches']:
                        if after: low['patches'][path] = after
                        else: del low['patches'][path]
            if not hit: sys.exit(f'{layer["module"].name}: unpatch for {path} matches no lower patch: {o}')
    written, changed = set(), 0
    def put(to, data):
        nonlocal changed
        written.add(to)
        if to.exists() and to.read_bytes() == data: return
        to.parent.mkdir(parents=True, exist_ok=True); to.write_bytes(data); changed += 1
    # sources: Vela's src/ (every layer's rules), then each lower layer's own src/ (the rules of the layers above it);
    # the module's own src/ is compiled as it is. A file is taken from the highest source that has it.
    sources = [(ROOT, layers)] + [(layer['module'], layers[i + 1:]) for i, layer in enumerate(layers[:-1])]
    for sset, kind in SOURCE_SETS:
        for k, (base, rules) in enumerate(sources):
            src = base / 'src' / sset / kind
            if not src.is_dir(): continue
            higher = [m / 'src' / sset / kind for m, _ in sources[k + 1:]] + [module / 'src' / sset / kind]
            for f in src.rglob('*'):
                rel = f.relative_to(src).as_posix()
                if not f.is_file(): continue
                if any((h / rel).exists() for h in higher) or any(fnmatch.fnmatchcase(rel, g) for layer in rules for g in layer['globs']):
                    for layer in layers[:-1]: layer['patches'].pop(rel, None)  # a lower layer's patches for a file replaced or dropped above
                    continue
                data = f.read_bytes()
                if f.suffix == '.java':
                    text = data.decode('utf-8').replace('\r\n', '\n')
                    for layer in rules:
                        text = rewrite(text, layer['classes'], layer['members'].get(rel, ()), layer['gone'])[0]
                        for o, n in layer['patches'].pop(rel, ()):
                            text, hit = substitute(text, o, n)
                            if not hit: sys.exit(f'{layer["module"].name}: patch for {rel} does not match: {getattr(o, "pattern", o)}')
                        for i, (o, n) in enumerate(layer['everywhere']):  # `patch * ...`: every compiled file
                            text, hit = substitute(text, o, n); layer['hits'][i] += hit
                    data = text.encode('utf-8')
                put(into / sset / kind / rel, data)
    # root mixin configs: keep the mixins whose class this module still compiles
    props = module / 'gradle.properties'
    release = re.search(r'^java_release=(\d+)', props.read_text('utf-8'), re.M) if props.is_file() else None
    for sset in ('main', 'gametest'):
        res = into / sset / 'resources'
        for cfg in res.glob('*.json') if res.is_dir() else ():
            if not cfg.name.endswith('mixins.json'): continue
            if cfg not in written: continue  # left over from an earlier run and dropped since
            d = json.loads(cfg.read_text('utf-8'))
            # a JAVA_25 config fails to load on a version that runs on Java 21 (NeoForge starts the game on java_release)
            level = re.fullmatch(r'JAVA_(\d+)', d.get('compatibilityLevel', ''))
            if release and level and int(level[1]) > int(release[1]): d['compatibilityLevel'] = f'JAVA_{release[1]}'
            pkg = d.get('package', '').replace('.', '/')
            # written this run, not merely left over from an earlier one (a file dropped since is deleted only at the end)
            has = lambda e: (into / sset / 'java' / f'{pkg}/{e.replace(".", "/")}.java') in written or (module / 'src' / sset / 'java' / f'{pkg}/{e.replace(".", "/")}.java').exists()
            for side in ('mixins', 'client', 'server'):
                if side in d: d[side] = [e for e in d[side] if has(e)]
            # every layer's own mixins in this config's package join the client side
            listed = {e for side in ('mixins', 'client', 'server') for e in d.get(side, ())}
            for m in layers_of(module):
                own_pkg = m / 'src' / sset / 'java' / pkg
                for f in sorted(own_pkg.rglob('*.java')) if pkg and own_pkg.is_dir() else ():
                    e = f.relative_to(own_pkg).with_suffix('').as_posix().replace('/', '.')
                    if e not in listed and has(e) and '@Mixin(' in f.read_text('utf-8'): d.setdefault('client', []).append(e); listed.add(e)
            put(cfg, (json.dumps(d, indent=2, ensure_ascii=False) + '\n').encode('utf-8'))
    # fabric.mod.json lists only the mixin configs this module still has (drop.txt may drop one, e.g. vela-opt.mixins.json)
    fmj = into / 'main' / 'resources' / 'fabric.mod.json'
    if fmj in written:
        text = fmj.read_text('utf-8')
        gone_cfgs = [n for n in re.findall(r'"([\w.-]+mixins[\w.-]*\.json)"', text)
                     if (into / 'main' / 'resources' / n) not in written and not (module / 'src' / 'main' / 'resources' / n).exists()]
        for n in gone_cfgs: text = re.sub(r',\s*"' + re.escape(n) + '"|"' + re.escape(n) + r'",\s*', '', text)
        if gone_cfgs: put(fmj, text.encode('utf-8'))
    for layer in layers:
        if layer['patches']: sys.exit(f'{layer["module"].name}: patches for files that are not compiled: {", ".join(layer["patches"])}')
        for (o, _), h in zip(layer['everywhere'], layer['hits']):
            if not h: sys.exit(f'{layer["module"].name}: patch for * matches no file: {getattr(o, "pattern", o)}')
    for f in into.rglob('*') if into.is_dir() else ():
        if f.is_file() and f not in written: f.unlink()
    print(f'{module.name}: {len(written)} files, {changed} written', file=sys.stderr)

if __name__ == '__main__':
    rewrite_main(sys.argv[2:]) if sys.argv[1:2] == ['rewrite'] else main()
